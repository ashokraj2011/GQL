from collections import OrderedDict
import re


# Datasources to skip while creating SQL.
# Example:
# SKIPPED_DATASOURCES = ["SomeDatasourceToSkip"]
SKIPPED_DATASOURCES = []


# Change this according to your real table names and primary keys.
NAMESPACE_CONFIG = {
    "moneyMovementEnriched": {
        "table": "moneyMovementEnriched",
        "primary_keys": ["mid"],
        "mid_column": "mid"
    },
    "aoFundingAccounts": {
        "table": "aoFundingAccounts",
        "primary_keys": ["mid"],
        "mid_column": "mid"
    },
    "accountRestrictions": {
        "table": "accountRestrictions",
        "primary_keys": ["mid"],
        "mid_column": "mid"
    }
}


def normalize_operator(op):
    """
    Converts rule operators to SQL operators.

    all -> AND
    any -> OR
    and -> AND
    or  -> OR
    """
    op = (op or "and").upper().strip()

    op_map = {
        "AND": "AND",
        "OR": "OR",
        "ALL": "AND",
        "ANY": "OR"
    }

    if op not in op_map:
        raise ValueError(f"Unsupported operator: {op}")

    return op_map[op]


def normalize_datasource(value):
    if value is None:
        return None

    return str(value).strip().lower()


def should_skip_term(term, skipped_datasources=None):
    """
    Returns True when the term's datasource is in skipped_datasources.
    """
    if not skipped_datasources:
        return False

    skipped = {
        normalize_datasource(datasource)
        for datasource in skipped_datasources
    }

    datasource = term.get("field", {}).get("datasource")

    return normalize_datasource(datasource) in skipped


def safe_identifier(value):
    """
    Allows only safe SQL identifiers like:
    mid
    amount
    movement_status
    """
    if not isinstance(value, str):
        raise ValueError(f"Invalid SQL identifier: {value}")

    if not re.match(r"^[A-Za-z_][A-Za-z0-9_]*$", value):
        raise ValueError(f"Invalid SQL identifier: {value}")

    return value


def safe_table_name(table_name):
    """
    Allows:
    table_name
    schema.table_name
    """
    if not isinstance(table_name, str):
        raise ValueError(f"Invalid table name: {table_name}")

    return ".".join(
        safe_identifier(part)
        for part in table_name.split(".")
    )


def get_primary_keys(config):
    """
    Supports either:

    primary_keys: ["mid"]

    or:

    primary_key: "mid"
    """
    primary_keys = config.get("primary_keys", config.get("primary_key"))

    if primary_keys is None:
        raise ValueError(f"Missing primary_keys in config: {config}")

    if isinstance(primary_keys, str):
        return [primary_keys]

    if isinstance(primary_keys, list) and primary_keys:
        return primary_keys

    raise ValueError(f"Invalid primary_keys: {primary_keys}")


def relative_date_to_sql(value):
    """
    Converts:

    CURRENT_DATE:ago:8:day(s)

    into:

    CURRENT_DATE - INTERVAL '8 day'
    """
    if not isinstance(value, str):
        return None

    match = re.match(
        r"^CURRENT_DATE:(ago|from_now):(\d+):(day|week|month|year)\(s\)$",
        value
    )

    if not match:
        return None

    direction, amount, unit = match.groups()
    operator = "-" if direction == "ago" else "+"

    return f"CURRENT_DATE {operator} INTERVAL '{amount} {unit}'"


def sql_literal(value):
    """
    Converts rule values directly into SQL literals.

    Important:
    Only mid uses ?
    Rule values are written directly into SQL.
    """
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
        # Numeric string should not be quoted.
        if re.match(r"^-?\d+(\.\d+)?$", value):
            return value

        # Escape single quotes.
        escaped = value.replace("'", "''")
        return f"'{escaped}'"

    raise ValueError(f"Unsupported SQL literal value: {value}")


def collect_leaf_terms(node, leaf_terms=None, skipped_datasources=None):
    """
    Collects only non-skipped leaf conditions.
    """
    if leaf_terms is None:
        leaf_terms = []

    for term in node.get("terms", []):
        if "terms" in term:
            collect_leaf_terms(
                node=term,
                leaf_terms=leaf_terms,
                skipped_datasources=skipped_datasources
            )
        else:
            if should_skip_term(term, skipped_datasources):
                continue

            leaf_terms.append(term)

    return leaf_terms


def collect_namespaces(node, namespaces=None, skipped_datasources=None):
    """
    Finds all namespaces used by non-skipped terms.
    Keeps first-seen order.
    """
    if namespaces is None:
        namespaces = []

    for term in node.get("terms", []):
        if "terms" in term:
            collect_namespaces(
                node=term,
                namespaces=namespaces,
                skipped_datasources=skipped_datasources
            )
        else:
            if should_skip_term(term, skipped_datasources):
                continue

            namespace = term["field"]["namespace"]

            if namespace not in namespaces:
                namespaces.append(namespace)

    return namespaces


def choose_base_namespace(namespaces, namespace_config):
    """
    Prefer a namespace/table that has mid_column configured.
    """
    for namespace in namespaces:
        if namespace_config[namespace].get("mid_column"):
            return namespace

    return namespaces[0]


def build_from_and_joins(namespaces, namespace_config):
    """
    Builds FROM and JOIN clauses.

    Example:

    FROM moneyMovementEnriched t1
    JOIN aoFundingAccounts t2 ON t1.mid = t2.mid
    JOIN accountRestrictions t3 ON t1.mid = t3.mid
    """
    base_namespace = choose_base_namespace(
        namespaces,
        namespace_config
    )

    ordered_namespaces = [base_namespace] + [
        namespace
        for namespace in namespaces
        if namespace != base_namespace
    ]

    aliases = {
        namespace: f"t{index + 1}"
        for index, namespace in enumerate(ordered_namespaces)
    }

    base_config = namespace_config[base_namespace]
    base_table = safe_table_name(
        base_config.get("table", base_namespace)
    )
    base_alias = aliases[base_namespace]
    base_primary_keys = get_primary_keys(base_config)

    sql_parts = [
        f"FROM {base_table} {base_alias}"
    ]

    for namespace in ordered_namespaces[1:]:
        config = namespace_config[namespace]
        table = safe_table_name(
            config.get("table", namespace)
        )
        alias = aliases[namespace]
        join_primary_keys = get_primary_keys(config)

        if len(base_primary_keys) != len(join_primary_keys):
            raise ValueError(
                f"Primary key count mismatch between "
                f"{base_namespace} and {namespace}"
            )

        join_conditions = []

        for base_pk, join_pk in zip(base_primary_keys, join_primary_keys):
            join_conditions.append(
                f"{base_alias}.{safe_identifier(base_pk)} = "
                f"{alias}.{safe_identifier(join_pk)}"
            )

        sql_parts.append(
            f"JOIN {table} {alias} ON " + " AND ".join(join_conditions)
        )

    return "\n".join(sql_parts), aliases, ordered_namespaces


def build_condition(term, aliases):
    """
    Converts one non-skipped leaf rule into one SQL condition.
    """
    field = term["field"]
    namespace = field["namespace"]

    alias = aliases[namespace]
    column_name = safe_identifier(field["name"])
    column = f"{alias}.{column_name}"

    comp = term["comp"].lower().strip()
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
def is_not_equal_term(term):
    return term.get("comp", "").lower().strip() == "not equal to"


def build_positive_equal_condition(term, aliases):
    """
    Used only for special NOT EQUAL TO grouping.

    Converts:
    field not equal to value

    Into positive form:
    field = value

    So multiple NOT EQUAL TO terms can become:
    NOT (field1 = value1 AND field2 = value2)
    """
    field = term["field"]
    namespace = field["namespace"]

    alias = aliases[namespace]
    column_name = safe_identifier(field["name"])
    column = f"{alias}.{column_name}"

    value = term.get("value")

    if value is None:
        return f"{column} IS NULL"

    return f"{column} = {sql_literal(value)}"


def build_eval_group_condition(group_data, aliases):
    """
    Special rule:

    If two or more NOT EQUAL TO terms exist in the same evaluation_group,
    convert them into:

    NOT (
        condition1 = value1
        AND condition2 = value2
    )

    Other conditions in the same evaluation_group are kept normally.
    """
    inner_op = group_data["op"]
    terms = group_data["terms"]

    not_equal_terms = [
        term for term in terms
        if is_not_equal_term(term)
    ]

    # Normal behavior if there are not at least 2 NOT EQUAL TO terms.
    if len(not_equal_terms) < 2:
        conditions = [
            build_condition(term, aliases)
            for term in terms
        ]

        return "(" + f" {inner_op} ".join(conditions) + ")"

    positive_equal_conditions = [
        build_positive_equal_condition(term, aliases)
        for term in not_equal_terms
    ]

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

        final_conditions.append(
            build_condition(term, aliases)
        )

    return "(" + f" {inner_op} ".join(final_conditions) + ")"

def collect_conditions_by_eval_group(
    node,
    groups,
    aliases,
    parent_op,
    skipped_datasources=None
):
    """
    Same evaluation_group goes into one bracket.
    Skipped datasource terms are ignored.

    This stores raw terms instead of already-built condition strings,
    because we need to detect multiple NOT EQUAL TO conditions
    inside the same evaluation_group.
    """
    current_op = normalize_operator(
        node.get("op", parent_op)
    )

    for term in node.get("terms", []):
        if "terms" in term:
            collect_conditions_by_eval_group(
                node=term,
                groups=groups,
                aliases=aliases,
                parent_op=current_op,
                skipped_datasources=skipped_datasources
            )
        else:
            if should_skip_term(term, skipped_datasources):
                continue

            evaluation_group = term["field"].get(
                "evaluation_group",
                "default"
            )

            if evaluation_group not in groups:
                groups[evaluation_group] = {
                    "op": current_op,
                    "terms": []
                }

            group_op = groups[evaluation_group]["op"]

            if group_op != current_op:
                raise ValueError(
                    f"Conflicting operators for evaluation_group "
                    f"{evaluation_group}: {group_op} and {current_op}"
                )

            groups[evaluation_group]["terms"].append(term)

def build_eval_group_where(data, aliases, skipped_datasources=None):
    """
    Builds WHERE rule condition for normal multi-condition rules.

    Same evaluation_group is bracketed together.
    Top-level operator joins the evaluation_group brackets.
    Skipped datasource terms are ignored.

    Special case:
    Multiple NOT EQUAL TO terms in the same evaluation_group become:

    NOT (condition1 = value1 AND condition2 = value2)
    """
    top_level_op = normalize_operator(
        data.get("op", "and")
    )

    groups = OrderedDict()

    collect_conditions_by_eval_group(
        node=data,
        groups=groups,
        aliases=aliases,
        parent_op=top_level_op,
        skipped_datasources=skipped_datasources
    )

    grouped_conditions = []

    for evaluation_group, group_data in groups.items():
        terms = group_data["terms"]

        if not terms:
            continue

        grouped_conditions.append(
            build_eval_group_condition(group_data, aliases)
        )

    if not grouped_conditions:
        return None

    return f" {top_level_op} ".join(grouped_conditions)


def build_mid_condition(ordered_namespaces, namespace_config, aliases):
    """
    Builds only this parameterized condition:

    t1.mid = ?
    """
    for namespace in ordered_namespaces:
        mid_column = namespace_config[namespace].get("mid_column")

        if mid_column:
            alias = aliases[namespace]

            return f"{alias}.{safe_identifier(mid_column)} = ?"

    raise ValueError(
        "No referenced namespace has a mid_column configured"
    )


def build_single_term_query(
    term,
    namespace_config,
    mid_value=None,
    select_clause="*",
    include_mid=True
):
    """
    Special case:

    One non-skipped rule condition only.
    Generates a simple SELECT on one table.
    """
    namespace = term["field"]["namespace"]

    if namespace not in namespace_config:
        raise ValueError(f"Missing namespace config for: {namespace}")

    config = namespace_config[namespace]
    table = safe_table_name(
        config.get("table", namespace)
    )

    aliases = {
        namespace: "t1"
    }

    rule_condition = build_condition(term, aliases)

    where_conditions = []
    params = []

    if include_mid:
        mid_column = config.get("mid_column")

        if not mid_column:
            raise ValueError(
                f"No mid_column configured for namespace: {namespace}"
            )

        where_conditions.append(
            f"t1.{safe_identifier(mid_column)} = ?"
        )
        params.append(mid_value)

    where_conditions.append(rule_condition)

    sql = f"""SELECT {select_clause}
FROM {table} t1
WHERE """ + "\n  AND ".join(where_conditions)

    return sql, params


def build_mid_only_query(
    namespace,
    namespace_config,
    mid_value=None,
    select_clause="*"
):
    """
    Optional fallback when every rule condition is skipped.

    Generates:

    SELECT *
    FROM table t1
    WHERE t1.mid = ?
    """
    if namespace not in namespace_config:
        raise ValueError(f"Missing namespace config for: {namespace}")

    config = namespace_config[namespace]
    table = safe_table_name(
        config.get("table", namespace)
    )

    mid_column = config.get("mid_column")

    if not mid_column:
        raise ValueError(
            f"No mid_column configured for namespace: {namespace}"
        )

    sql = f"""SELECT {select_clause}
FROM {table} t1
WHERE t1.{safe_identifier(mid_column)} = ?"""

    return sql, [mid_value]


def build_query(
    data,
    namespace_config,
    mid_value=None,
    select_clause="*",
    include_mid=True,
    skipped_datasources=None,
    fallback_namespace=None
):
    """
    Final SQL builder.

    - Uses global SKIPPED_DATASOURCES if skipped_datasources is not passed.
    - Skips any term whose field.datasource is in skipped_datasources.
    - If all terms are skipped, returns "Nothing to generate".
    - If one non-skipped condition remains, creates simple one-table SELECT.
    - If multiple non-skipped conditions remain, creates JOIN SQL.
    - Only mid is parameterized with ?.
    - Rule values are written directly into SQL.
    """

    # Use global skipped datasource list by default
    if skipped_datasources is None:
        skipped_datasources = SKIPPED_DATASOURCES

    leaf_terms = collect_leaf_terms(
        node=data,
        skipped_datasources=skipped_datasources
    )

    # Nothing left after skipping datasources
    if not leaf_terms:
        return "Nothing to generate", []

    # Special case:
    # Only one non-skipped condition.
    if len(leaf_terms) == 1:
        return build_single_term_query(
            term=leaf_terms[0],
            namespace_config=namespace_config,
            mid_value=mid_value,
            select_clause=select_clause,
            include_mid=include_mid
        )

    namespaces = collect_namespaces(
        node=data,
        skipped_datasources=skipped_datasources
    )

    for namespace in namespaces:
        if namespace not in namespace_config:
            raise ValueError(
                f"Missing namespace config for: {namespace}"
            )

    from_and_joins, aliases, ordered_namespaces = build_from_and_joins(
        namespaces=namespaces,
        namespace_config=namespace_config
    )

    where_conditions = []
    params = []

    if include_mid:
        mid_condition = build_mid_condition(
            ordered_namespaces=ordered_namespaces,
            namespace_config=namespace_config,
            aliases=aliases
        )

        where_conditions.append(mid_condition)
        params.append(mid_value)

    rule_where = build_eval_group_where(
        data=data,
        aliases=aliases,
        skipped_datasources=skipped_datasources
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
    def walk(node):
        for t in node.get("terms", []):
            if "terms" in t:
                yield from walk(t)
            elif t["field"]["namespace"] == namespace:
                yield t

    op_map = {
        "equal to": "=",
        "not equal to": "<>",
        "greater than": ">",
        "greater than equal to": ">=",
        "less than": "<",
        "less than equal to": "<=",
    }

    for t in walk(data):
        col = f"{t['field']['namespace']}.{t['field']['name']}"
        comp = t["comp"].lower().strip()
        val = t["value"]

        if comp == "contains":
            print(f"{col} LIKE '%{val}%'")
        elif comp in ("has all of", "has any of", "in"):
            values = ", ".join(f"'{v}'" for v in val)
            print(f"{col} IN ({values})")
        else:
            value = val if str(val).isdigit() else f"'{val}'"
            print(f"{col} {op_map[comp]} {value}")








single_rule = {
    "op": "all",
    "terms": [
        {
            "comp": "greater than equal to",
            "field": {
                "name": "amount",
                "namespace": "moneyMovementEnriched",
                "datasource": "S",
                "evaluation_group": "1"
            },
            "value": "800"
        }
    ],
    "version": "2"
}
import json

# Open and read the JSON file
with open('rule.json', 'r', encoding='utf-8') as file:
    single_rule = json.load(file)

# Access data just like a standard Python dictionary
print(data)
sql, params = build_query(
    data=single_rule,
    namespace_config=NAMESPACE_CONFIG,
    mid_value="12345"
)

print(sql)
print(params)
