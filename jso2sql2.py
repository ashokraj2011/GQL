from collections import OrderedDict
from copy import deepcopy
from textwrap import indent
import ast
import json
import re
import pandas as pd


RULE_METADATA_NAMESPACE = "rulemetadata"

# When a rule has no SQL after skipping datasource terms,
# return this SQL so count = 1.
EMPTY_RULE_SQL = "SELECT now()::date;"

# Datasources to skip while creating SQL.
SKIPPED_DATASOURCES = ["CASGraphQL"]


# NAMESPACE_CONFIG is now a dataframe.
# Required columns:
# namespace, table_name, primary_keys, mid_column
NAMESPACE_CONFIG_DF = pd.DataFrame([
    {
        "namespace": "moneyMovementEnriched",
        "table_name": "moneyMovementEnriched",
        "primary_keys": ["mid"],
        "mid_column": "mid",
    },
    {
        "namespace": "aoFundingAccounts",
        "table_name": "aoFundingAccounts",
        "primary_keys": ["mid"],
        "mid_column": "mid",
    },
    {
        "namespace": "accountRestrictions",
        "table_name": "accountRestrictions",
        "primary_keys": ["mid"],
        "mid_column": "mid",
    },
])


def true_sql():
    return EMPTY_RULE_SQL


def strip_sql_semicolon(sql):
    return sql.strip().rstrip(";")


def is_missing(value):
    if value is None:
        return True

    try:
        return bool(value != value)
    except Exception:
        return False


def normalize_primary_keys_value(value):
    if is_missing(value):
        raise ValueError("primary_keys is missing")

    if isinstance(value, list):
        return value

    if isinstance(value, tuple) or isinstance(value, set):
        return list(value)

    if isinstance(value, str):
        text = value.strip()

        if not text:
            raise ValueError("primary_keys is empty")

        try:
            parsed = ast.literal_eval(text)

            if isinstance(parsed, list):
                return parsed

            if isinstance(parsed, tuple) or isinstance(parsed, set):
                return list(parsed)

            if isinstance(parsed, str):
                return [parsed]

        except Exception:
            pass

        if "," in text:
            return [
                part.strip()
                for part in text.split(",")
                if part.strip()
            ]

        return [text]

    raise ValueError(f"Invalid primary_keys value: {value}")


def normalize_namespace_config(namespace_config):
    """
    Accepts dataframe config with columns:

    namespace
    table_name
    primary_keys
    mid_column

    Also accepts old dictionary style for backward compatibility.
    """

    if isinstance(namespace_config, dict):
        normalized = {}

        for namespace, config in namespace_config.items():
            table_name = (
                config.get("table_name")
                or config.get("table")
                or namespace
            )

            primary_keys = config.get(
                "primary_keys",
                config.get("primary keys", config.get("primary_key"))
            )

            mid_column = config.get("mid_column")

            normalized[namespace] = {
                **config,
                "namespace": namespace,
                "table_name": table_name,
                "table": table_name,
                "primary_keys": normalize_primary_keys_value(primary_keys),
                "mid_column": mid_column,
            }

        return normalized

    if not hasattr(namespace_config, "iterrows"):
        raise ValueError("namespace_config must be a dataframe or dictionary")

    normalized = {}

    for _, row in namespace_config.iterrows():
        namespace = row.get("namespace")

        if is_missing(namespace):
            raise ValueError(
                "namespace_config dataframe requires namespace column"
            )

        namespace = str(namespace)

        table_name = (
            row.get("table_name")
            if not is_missing(row.get("table_name"))
            else row.get("table")
        )

        if is_missing(table_name):
            table_name = namespace

        primary_keys = None

        for col in (
            "primary_keys",
            "primary keys",
            "primary_key",
            "primary key",
        ):
            if col in row and not is_missing(row.get(col)):
                primary_keys = row.get(col)
                break

        mid_column = row.get("mid_column") if "mid_column" in row else None

        if is_missing(mid_column):
            mid_column = None

        normalized[namespace] = {
            "namespace": namespace,
            "table_name": str(table_name),
            "table": str(table_name),
            "primary_keys": normalize_primary_keys_value(primary_keys),
            "mid_column": None if mid_column is None else str(mid_column),
        }

    return normalized


def normalize_operator(op):
    op = (op or "and").upper().strip()

    op_map = {
        "AND": "AND",
        "OR": "OR",
        "ALL": "AND",
        "ANY": "OR",
    }

    if op not in op_map:
        raise ValueError(f"Unsupported operator: {op}")

    return op_map[op]


def normalize_datasource(value):
    if value is None:
        return None

    return str(value).strip().lower()


def should_skip_term(term, skipped_datasources=None):
    if not skipped_datasources:
        return False

    skipped = {
        normalize_datasource(datasource)
        for datasource in skipped_datasources
    }

    datasource = term.get("field", {}).get("datasource")

    return normalize_datasource(datasource) in skipped


def safe_identifier(value):
    if not isinstance(value, str):
        raise ValueError(f"Invalid SQL identifier: {value}")

    if not re.match(r"^[A-Za-z_][A-Za-z0-9_]*$", value):
        raise ValueError(f"Invalid SQL identifier: {value}")

    return value


def safe_table_name(table_name):
    if not isinstance(table_name, str):
        raise ValueError(f"Invalid table name: {table_name}")

    return ".".join(
        safe_identifier(part)
        for part in table_name.split(".")
    )


def get_primary_keys(config):
    primary_keys = config.get("primary_keys", config.get("primary_key"))

    return normalize_primary_keys_value(primary_keys)


def relative_date_to_sql(value):
    if not isinstance(value, str):
        return None

    match = re.match(
        r"^CURRENT_DATE:(ago|from_now):(\d+):(day|week|month|year)\(s\)$",
        value,
    )

    if not match:
        return None

    direction, amount, unit = match.groups()
    operator = "-" if direction == "ago" else "+"

    return f"CURRENT_DATE {operator} INTERVAL '{amount} {unit}'"


def sql_literal(value):
    date_sql = relative_date_to_sql(value)

    if date_sql:
        return date_sql

    if value is None:
        return "NULL"

    if isinstance(value, bool):
        return "TRUE" if value else "FALSE"

    if isinstance(value, (int, float)):
        return str(value)

    if isinstance(value, str):
        if re.match(r"^-?\d+(\.\d+)?$", value):
            return value

        escaped = value.replace("'", "''")
        return f"'{escaped}'"

    raise ValueError(f"Unsupported SQL literal value: {value}")


def is_rulemetadata_term(term):
    return term.get("field", {}).get("namespace") == RULE_METADATA_NAMESPACE


def is_not_equal_term(term):
    return term.get("comp", "").lower().strip() == "not equal to"


def collect_leaf_terms(node, leaf_terms=None, skipped_datasources=None):
    if leaf_terms is None:
        leaf_terms = []

    for term in node.get("terms", []):
        if "terms" in term:
            collect_leaf_terms(
                node=term,
                leaf_terms=leaf_terms,
                skipped_datasources=skipped_datasources,
            )
        else:
            if should_skip_term(term, skipped_datasources):
                continue

            leaf_terms.append(term)

    return leaf_terms


def collect_namespaces(node, namespaces=None, skipped_datasources=None):
    """
    Collects normal table namespaces only.

    rulemetadata is skipped because it gets replaced by nested rule SQL.
    """
    if namespaces is None:
        namespaces = []

    for term in node.get("terms", []):
        if "terms" in term:
            collect_namespaces(
                node=term,
                namespaces=namespaces,
                skipped_datasources=skipped_datasources,
            )
        else:
            if should_skip_term(term, skipped_datasources):
                continue

            namespace = term["field"]["namespace"]

            if namespace == RULE_METADATA_NAMESPACE:
                continue

            if namespace not in namespaces:
                namespaces.append(namespace)

    return namespaces


def choose_base_namespace(namespaces, namespace_config):
    for namespace in namespaces:
        if namespace_config[namespace].get("mid_column"):
            return namespace

    return namespaces[0]


def build_from_and_joins(namespaces, namespace_config, alias_prefix="t"):
    base_namespace = choose_base_namespace(
        namespaces,
        namespace_config,
    )

    ordered_namespaces = [base_namespace] + [
        namespace
        for namespace in namespaces
        if namespace != base_namespace
    ]

    aliases = {
        namespace: f"{alias_prefix}{index + 1}"
        for index, namespace in enumerate(ordered_namespaces)
    }

    base_config = namespace_config[base_namespace]
    base_table = safe_table_name(
        base_config.get("table_name")
        or base_config.get("table")
        or base_namespace
    )
    base_alias = aliases[base_namespace]
    base_primary_keys = get_primary_keys(base_config)

    sql_parts = [
        f"FROM {base_table} {base_alias}"
    ]

    for namespace in ordered_namespaces[1:]:
        config = namespace_config[namespace]
        table = safe_table_name(
            config.get("table_name")
            or config.get("table")
            or namespace
        )
        alias = aliases[namespace]
        join_primary_keys = get_primary_keys(config)

        if len(base_primary_keys) != len(join_primary_keys):
            raise ValueError(
                f"Primary key count mismatch between "
                f"{base_namespace} and {namespace}"
            )

        join_conditions = [
            f"{base_alias}.{safe_identifier(base_pk)} = "
            f"{alias}.{safe_identifier(join_pk)}"
            for base_pk, join_pk
            in zip(base_primary_keys, join_primary_keys)
        ]

        sql_parts.append(
            f"JOIN {table} {alias} ON " + " AND ".join(join_conditions)
        )

    return "\n".join(sql_parts), aliases, ordered_namespaces


def build_mid_condition(
    ordered_namespaces,
    namespace_config,
    aliases,
    mid_expression=None,
):
    for namespace in ordered_namespaces:
        mid_column = namespace_config[namespace].get("mid_column")

        if mid_column:
            alias = aliases[namespace]
            column = f"{alias}.{safe_identifier(mid_column)}"

            if mid_expression:
                return f"{column} = {mid_expression}"

            return f"{column} = ?"

    raise ValueError("No referenced namespace has a mid_column configured")


def get_rule_name_from_term(term):
    field = term["field"]

    return (
        field.get("rule")
        or field.get("rule_name")
        or field.get("rule_id")
        or field["name"]
    )


def get_rule_def_from_lookup(
    rule_lookup_df,
    rule_name,
    rule_col="rule",
    rule_def_col="rule_def",
):
    """
    rule_lookup_df columns:

    rule
    rule_def

    rule_def may be a dict or JSON string.
    """
    if rule_lookup_df is None:
        raise ValueError("rule_lookup_df is required for rulemetadata terms")

    rows = rule_lookup_df[
        rule_lookup_df[rule_col].astype(str) == str(rule_name)
    ]

    if rows.empty:
        raise ValueError(f"Rule not found in lookup dataframe: {rule_name}")

    rule_def = rows.iloc[0][rule_def_col]

    if isinstance(rule_def, str):
        return json.loads(rule_def)

    return rule_def


def to_bool(value):
    if isinstance(value, bool):
        return value

    value = str(value).strip().lower()

    if value == "true":
        return True

    if value == "false":
        return False

    raise ValueError(f"Expected true or false, got: {value}")


def rulemetadata_expected_true(term):
    """
    Mapping:

    rulemetadata.some_rule equal to true        -> EXISTS
    rulemetadata.some_rule equal to false       -> NOT EXISTS
    rulemetadata.some_rule not equal to true    -> NOT EXISTS
    rulemetadata.some_rule not equal to false   -> EXISTS
    """
    comp = term["comp"].lower().strip()
    value = to_bool(term.get("value"))

    if comp == "equal to":
        return value

    if comp == "not equal to":
        return not value

    raise ValueError(
        "rulemetadata only supports equal to / not equal to, "
        f"got: {term['comp']}"
    )


def build_field_condition(term, aliases, force_equal=False):
    field = term["field"]
    namespace = field["namespace"]

    alias = aliases[namespace]
    column_name = safe_identifier(field["name"])
    column = f"{alias}.{column_name}"

    comp = "equal to" if force_equal else term["comp"].lower().strip()
    value = term.get("value")

    if comp == "equal to":
        if value is None:
            return f"{column} IS NULL"

        return f"{column} = {sql_literal(value)}"

    if comp == "not equal to":
        if value is None:
            return f"{column} IS NOT NULL"

        return f"{column} <> {sql_literal(value)}"

    if comp == "greater than":
        return f"{column} > {sql_literal(value)}"

    if comp == "greater than equal to":
        return f"{column} >= {sql_literal(value)}"

    if comp == "less than":
        return f"{column} < {sql_literal(value)}"

    if comp == "less than equal to":
        return f"{column} <= {sql_literal(value)}"

    if comp == "contains":
        escaped = str(value).replace("'", "''")
        return f"{column} LIKE '%{escaped}%'"

    if comp in ("has all of", "has any of", "in"):
        if not isinstance(value, list):
            value = [value]

        if len(value) == 0:
            return "1 = 0"

        values = ", ".join(
            sql_literal(item)
            for item in value
        )

        return f"{column} IN ({values})"

    if comp in ("not in", "has none of"):
        if not isinstance(value, list):
            value = [value]

        if len(value) == 0:
            return "1 = 1"

        values = ", ".join(
            sql_literal(item)
            for item in value
        )

        return f"{column} NOT IN ({values})"

    raise ValueError(f"Unsupported comparison: {term['comp']}")


def build_rulemetadata_condition(
    term,
    aliases,
    namespace_config,
    rule_lookup_df,
    params,
    skipped_datasources=None,
    rule_col="rule",
    rule_def_col="rule_def",
    seen_rules=None,
    mid_value=None,
    mid_expression=None,
):
    """
    Replaces a rulemetadata condition with nested rule SQL.
    """
    seen_rules = seen_rules or set()
    rule_name = get_rule_name_from_term(term)

    if rule_name in seen_rules:
        raise ValueError(f"Circular rule reference found: {rule_name}")

    expected_true = rulemetadata_expected_true(term)
    next_seen_rules = seen_rules | {rule_name}

    nested_rule_def = get_rule_def_from_lookup(
        rule_lookup_df=rule_lookup_df,
        rule_name=rule_name,
        rule_col=rule_col,
        rule_def_col=rule_def_col,
    )

    if aliases:
        outer_namespace = next(iter(aliases))
        outer_alias = aliases[outer_namespace]

        outer_mid_column = safe_identifier(
            namespace_config[outer_namespace].get("mid_column", "mid")
        )

        nested_mid_expression = f"{outer_alias}.{outer_mid_column}"
        nested_mid_value = None
    else:
        nested_mid_expression = mid_expression
        nested_mid_value = mid_value

    nested_sql, nested_params = build_query(
        data=nested_rule_def,
        namespace_config=namespace_config,
        mid_value=nested_mid_value,
        mid_expression=nested_mid_expression,
        select_clause="1",
        include_mid=True,
        skipped_datasources=skipped_datasources,
        rule_lookup_df=rule_lookup_df,
        rule_col=rule_col,
        rule_def_col=rule_def_col,
        seen_rules=next_seen_rules,
        alias_prefix=f"r{len(next_seen_rules)}_",
    )

    if nested_params:
        params.extend(nested_params)

    # Important:
    # top-level empty SQL is "SELECT now()::date;"
    # but inside EXISTS, we remove the semicolon.
    nested_sql = strip_sql_semicolon(nested_sql)

    exists_sql = "EXISTS (\n" + indent(nested_sql, "  ") + "\n)"

    if expected_true:
        return exists_sql

    return "NOT " + exists_sql


def build_condition(
    term,
    aliases,
    namespace_config,
    rule_lookup_df=None,
    params=None,
    skipped_datasources=None,
    rule_col="rule",
    rule_def_col="rule_def",
    seen_rules=None,
    mid_value=None,
    mid_expression=None,
):
    if params is None:
        params = []

    if is_rulemetadata_term(term):
        return build_rulemetadata_condition(
            term=term,
            aliases=aliases,
            namespace_config=namespace_config,
            rule_lookup_df=rule_lookup_df,
            params=params,
            skipped_datasources=skipped_datasources,
            rule_col=rule_col,
            rule_def_col=rule_def_col,
            seen_rules=seen_rules,
            mid_value=mid_value,
            mid_expression=mid_expression,
        )

    return build_field_condition(term, aliases)


def build_positive_equal_condition(
    term,
    aliases,
    namespace_config,
    rule_lookup_df=None,
    params=None,
    skipped_datasources=None,
    rule_col="rule",
    rule_def_col="rule_def",
    seen_rules=None,
    mid_value=None,
    mid_expression=None,
):
    """
    Used when multiple NOT EQUAL TO terms are in the same evaluation_group.

    Two NOT EQUAL TO terms become:

    NOT (
        condition1 = value1
        AND condition2 = value2
    )
    """
    if is_rulemetadata_term(term):
        positive_term = deepcopy(term)
        positive_term["comp"] = "equal to"

        return build_condition(
            term=positive_term,
            aliases=aliases,
            namespace_config=namespace_config,
            rule_lookup_df=rule_lookup_df,
            params=params,
            skipped_datasources=skipped_datasources,
            rule_col=rule_col,
            rule_def_col=rule_def_col,
            seen_rules=seen_rules,
            mid_value=mid_value,
            mid_expression=mid_expression,
        )

    return build_field_condition(term, aliases, force_equal=True)


def collect_conditions_by_eval_group(
    node,
    groups,
    parent_op,
    skipped_datasources=None,
):
    current_op = normalize_operator(
        node.get("op", parent_op)
    )

    for term in node.get("terms", []):
        if "terms" in term:
            collect_conditions_by_eval_group(
                node=term,
                groups=groups,
                parent_op=current_op,
                skipped_datasources=skipped_datasources,
            )
        else:
            if should_skip_term(term, skipped_datasources):
                continue

            evaluation_group = term["field"].get(
                "evaluation_group",
                "default",
            )

            if evaluation_group not in groups:
                groups[evaluation_group] = {
                    "op": current_op,
                    "terms": [],
                }

            group_op = groups[evaluation_group]["op"]

            if group_op != current_op:
                raise ValueError(
                    f"Conflicting operators for evaluation_group "
                    f"{evaluation_group}: {group_op} and {current_op}"
                )

            groups[evaluation_group]["terms"].append(term)


def build_eval_group_condition(
    group_data,
    aliases,
    namespace_config,
    rule_lookup_df=None,
    params=None,
    skipped_datasources=None,
    rule_col="rule",
    rule_def_col="rule_def",
    seen_rules=None,
    mid_value=None,
    mid_expression=None,
):
    inner_op = group_data["op"]
    terms = group_data["terms"]

    not_equal_terms = [
        term
        for term in terms
        if is_not_equal_term(term)
    ]

    if len(not_equal_terms) >= 2:
        positive_equal_conditions = []

        for term in not_equal_terms:
            condition = build_positive_equal_condition(
                term=term,
                aliases=aliases,
                namespace_config=namespace_config,
                rule_lookup_df=rule_lookup_df,
                params=params,
                skipped_datasources=skipped_datasources,
                rule_col=rule_col,
                rule_def_col=rule_def_col,
                seen_rules=seen_rules,
                mid_value=mid_value,
                mid_expression=mid_expression,
            )

            if condition:
                positive_equal_conditions.append(condition)

        if len(positive_equal_conditions) >= 2:
            not_equal_group_sql = (
                "NOT ("
                + " AND ".join(positive_equal_conditions)
                + ")"
            )

            final_conditions = []
            inserted_not_group = False

            for term in terms:
                if is_not_equal_term(term):
                    if not inserted_not_group:
                        final_conditions.append(not_equal_group_sql)
                        inserted_not_group = True
                    continue

                condition = build_condition(
                    term=term,
                    aliases=aliases,
                    namespace_config=namespace_config,
                    rule_lookup_df=rule_lookup_df,
                    params=params,
                    skipped_datasources=skipped_datasources,
                    rule_col=rule_col,
                    rule_def_col=rule_def_col,
                    seen_rules=seen_rules,
                    mid_value=mid_value,
                    mid_expression=mid_expression,
                )

                if condition:
                    final_conditions.append(condition)

            if not final_conditions:
                return None

            return "(" + f" {inner_op} ".join(final_conditions) + ")"

    conditions = []

    for term in terms:
        condition = build_condition(
            term=term,
            aliases=aliases,
            namespace_config=namespace_config,
            rule_lookup_df=rule_lookup_df,
            params=params,
            skipped_datasources=skipped_datasources,
            rule_col=rule_col,
            rule_def_col=rule_def_col,
            seen_rules=seen_rules,
            mid_value=mid_value,
            mid_expression=mid_expression,
        )

        if condition:
            conditions.append(condition)

    if not conditions:
        return None

    return "(" + f" {inner_op} ".join(conditions) + ")"


def build_eval_group_where(
    data,
    aliases,
    namespace_config,
    rule_lookup_df=None,
    params=None,
    skipped_datasources=None,
    rule_col="rule",
    rule_def_col="rule_def",
    seen_rules=None,
    mid_value=None,
    mid_expression=None,
):
    top_level_op = normalize_operator(
        data.get("op", "and")
    )

    groups = OrderedDict()

    collect_conditions_by_eval_group(
        node=data,
        groups=groups,
        parent_op=top_level_op,
        skipped_datasources=skipped_datasources,
    )

    grouped_conditions = []

    for group_data in groups.values():
        condition = build_eval_group_condition(
            group_data=group_data,
            aliases=aliases,
            namespace_config=namespace_config,
            rule_lookup_df=rule_lookup_df,
            params=params,
            skipped_datasources=skipped_datasources,
            rule_col=rule_col,
            rule_def_col=rule_def_col,
            seen_rules=seen_rules,
            mid_value=mid_value,
            mid_expression=mid_expression,
        )

        if condition:
            grouped_conditions.append(condition)

    if not grouped_conditions:
        return None

    return f" {top_level_op} ".join(grouped_conditions)


def build_single_rulemetadata_query(
    term,
    namespace_config,
    mid_value=None,
    mid_expression=None,
    select_clause="*",
    include_mid=True,
    skipped_datasources=None,
    rule_lookup_df=None,
    rule_col="rule",
    rule_def_col="rule_def",
    seen_rules=None,
    alias_prefix="t",
):
    seen_rules = seen_rules or set()
    rule_name = get_rule_name_from_term(term)

    if rule_name in seen_rules:
        raise ValueError(f"Circular rule reference found: {rule_name}")

    expected_true = rulemetadata_expected_true(term)

    nested_rule_def = get_rule_def_from_lookup(
        rule_lookup_df=rule_lookup_df,
        rule_name=rule_name,
        rule_col=rule_col,
        rule_def_col=rule_def_col,
    )

    nested_select_clause = select_clause if expected_true else "1"

    nested_sql, nested_params = build_query(
        data=nested_rule_def,
        namespace_config=namespace_config,
        mid_value=mid_value,
        mid_expression=mid_expression,
        select_clause=nested_select_clause,
        include_mid=include_mid,
        skipped_datasources=skipped_datasources,
        rule_lookup_df=rule_lookup_df,
        rule_col=rule_col,
        rule_def_col=rule_def_col,
        seen_rules=seen_rules | {rule_name},
        alias_prefix=alias_prefix,
    )

    if expected_true:
        return nested_sql, nested_params

    nested_sql = strip_sql_semicolon(nested_sql)

    sql = (
        "SELECT 1\n"
        "WHERE NOT EXISTS (\n"
        + indent(nested_sql, "  ")
        + "\n)"
    )

    return sql, nested_params


def build_single_term_query(
    term,
    namespace_config,
    mid_value=None,
    mid_expression=None,
    select_clause="*",
    include_mid=True,
    skipped_datasources=None,
    rule_lookup_df=None,
    rule_col="rule",
    rule_def_col="rule_def",
    seen_rules=None,
    alias_prefix="t",
):
    namespace = term["field"]["namespace"]

    if namespace == RULE_METADATA_NAMESPACE:
        return build_single_rulemetadata_query(
            term=term,
            namespace_config=namespace_config,
            mid_value=mid_value,
            mid_expression=mid_expression,
            select_clause=select_clause,
            include_mid=include_mid,
            skipped_datasources=skipped_datasources,
            rule_lookup_df=rule_lookup_df,
            rule_col=rule_col,
            rule_def_col=rule_def_col,
            seen_rules=seen_rules,
            alias_prefix=alias_prefix,
        )

    if namespace not in namespace_config:
        raise ValueError(f"Missing namespace config for: {namespace}")

    config = namespace_config[namespace]
    table = safe_table_name(
        config.get("table_name")
        or config.get("table")
        or namespace
    )

    alias = f"{alias_prefix}1"
    aliases = {
        namespace: alias
    }

    params = []
    where_conditions = []

    if include_mid:
        mid_column = config.get("mid_column")

        if not mid_column:
            raise ValueError(
                f"No mid_column configured for namespace: {namespace}"
            )

        if mid_expression:
            where_conditions.append(
                f"{alias}.{safe_identifier(mid_column)} = {mid_expression}"
            )
        else:
            where_conditions.append(
                f"{alias}.{safe_identifier(mid_column)} = ?"
            )
            params.append(mid_value)

    rule_condition = build_condition(
        term=term,
        aliases=aliases,
        namespace_config=namespace_config,
        rule_lookup_df=rule_lookup_df,
        params=params,
        skipped_datasources=skipped_datasources,
        rule_col=rule_col,
        rule_def_col=rule_def_col,
        seen_rules=seen_rules,
        mid_value=mid_value,
        mid_expression=mid_expression,
    )

    if rule_condition:
        where_conditions.append(rule_condition)

    if not where_conditions:
        return true_sql(), []

    sql = (
        f"SELECT {select_clause}\n"
        f"FROM {table} {alias}\n"
        "WHERE " + "\n  AND ".join(where_conditions)
    )

    return sql, params


def build_query(
    data,
    namespace_config,
    mid_value=None,
    mid_expression=None,
    select_clause="*",
    include_mid=True,
    skipped_datasources=None,
    rule_lookup_df=None,
    rule_col="rule",
    rule_def_col="rule_def",
    seen_rules=None,
    alias_prefix="t",
):
    """
    Final SQL builder.

    Empty/skipped rule behavior:
    - returns SELECT now()::date;
    - params = []
    """

    namespace_config = normalize_namespace_config(namespace_config)

    if skipped_datasources is None:
        skipped_datasources = SKIPPED_DATASOURCES

    if seen_rules is None:
        seen_rules = set()

    leaf_terms = collect_leaf_terms(
        node=data,
        skipped_datasources=skipped_datasources,
    )

    # Empty after skipping datasources means rule is true.
    if not leaf_terms:
        return true_sql(), []

    if len(leaf_terms) == 1:
        return build_single_term_query(
            term=leaf_terms[0],
            namespace_config=namespace_config,
            mid_value=mid_value,
            mid_expression=mid_expression,
            select_clause=select_clause,
            include_mid=include_mid,
            skipped_datasources=skipped_datasources,
            rule_lookup_df=rule_lookup_df,
            rule_col=rule_col,
            rule_def_col=rule_def_col,
            seen_rules=seen_rules,
            alias_prefix=alias_prefix,
        )

    namespaces = collect_namespaces(
        node=data,
        skipped_datasources=skipped_datasources,
    )

    params = []

    # This handles rules that contain only rulemetadata terms.
    if not namespaces:
        rule_where = build_eval_group_where(
            data=data,
            aliases={},
            namespace_config=namespace_config,
            rule_lookup_df=rule_lookup_df,
            params=params,
            skipped_datasources=skipped_datasources,
            rule_col=rule_col,
            rule_def_col=rule_def_col,
            seen_rules=seen_rules,
            mid_value=mid_value,
            mid_expression=mid_expression,
        )

        if not rule_where:
            return true_sql(), []

        actual_select = "1" if select_clause == "*" else select_clause

        sql = f"""SELECT {actual_select}
WHERE {rule_where}"""

        return sql, params

    for namespace in namespaces:
        if namespace not in namespace_config:
            raise ValueError(f"Missing namespace config for: {namespace}")

    from_and_joins, aliases, ordered_namespaces = build_from_and_joins(
        namespaces=namespaces,
        namespace_config=namespace_config,
        alias_prefix=alias_prefix,
    )

    where_conditions = []

    if include_mid:
        mid_condition = build_mid_condition(
            ordered_namespaces=ordered_namespaces,
            namespace_config=namespace_config,
            aliases=aliases,
            mid_expression=mid_expression,
        )

        where_conditions.append(mid_condition)

        if not mid_expression:
            params.append(mid_value)

    rule_where = build_eval_group_where(
        data=data,
        aliases=aliases,
        namespace_config=namespace_config,
        rule_lookup_df=rule_lookup_df,
        params=params,
        skipped_datasources=skipped_datasources,
        rule_col=rule_col,
        rule_def_col=rule_def_col,
        seen_rules=seen_rules,
        mid_value=mid_value,
        mid_expression=mid_expression,
    )

    if rule_where:
        where_conditions.append(
            f"(\n    {rule_where}\n  )"
        )

    sql = f"""SELECT {select_clause}
{from_and_joins}"""

    if where_conditions:
        sql += "\nWHERE " + "\n  AND ".join(where_conditions)

    return sql, params


def print_conditions_for_namespace(data, namespace):
    """
    Simple helper that prints:

    namespace.attribute condition value

    No alias.
    """
    def walk(node):
        for term in node.get("terms", []):
            if "terms" in term:
                yield from walk(term)
            elif term["field"]["namespace"] == namespace:
                yield term

    op_map = {
        "equal to": "=",
        "not equal to": "<>",
        "greater than": ">",
        "greater than equal to": ">=",
        "less than": "<",
        "less than equal to": "<=",
    }

    for term in walk(data):
        col = f"{term['field']['namespace']}.{term['field']['name']}"
        comp = term["comp"].lower().strip()
        val = term["value"]

        if comp == "contains":
            print(f"{col} LIKE '%{val}%'")

        elif comp in ("has all of", "has any of", "in"):
            values = ", ".join(f"'{v}'" for v in val)
            print(f"{col} IN ({values})")

        else:
            value = val if str(val).isdigit() else f"'{val}'"
            print(f"{col} {op_map[comp]} {value}")


sql, params = build_query(
    data=single_rule,
    namespace_config=NAMESPACE_CONFIG_DF,
    mid_value="12345",
)

print(sql)
print(params)
