#!/usr/bin/env python3
"""
rule_gen_from_db.py
Generate rule‑JSON **and** the equivalent SQL straight from a live DB.

▪  pip install sqlalchemy psycopg2-binary
▪  python rule_gen_from_db.py postgresql://user:pass@host/db 3
"""

import json, random, sys
from contextlib import closing
from dataclasses import dataclass, field
from typing import Any, Dict, List, Tuple

from sqlalchemy import create_engine, inspect, text
from sqlalchemy.engine import Engine

# ──────────────────────────────────────────────────────────────────────────────
# 1.  CONSTANTS  ──  operators you allow
# ──────────────────────────────────────────────────────────────────────────────

OPS_NUM = [
    "equal to", "not equal to",
    "greater than", "greater than equal to",
    "less than", "less than equal to",
    "exists", "not exists", "is null"
]
OPS_STR = [
    "equal to", "not equal to", "is empty", "is null",
    "exists", "not exists"
]
NO_VAL  = {"exists", "not exists", "is null", "is empty"}

MAX_TERMS  = 4
MAX_DEPTH  = 3
SUBGROUP_P = 0.35


# ──────────────────────────────────────────────────────────────────────────────
# 2.  INTROSPECTION
# ──────────────────────────────────────────────────────────────────────────────

@dataclass
class ColumnInfo:
    table: str
    column: str
    dtype: str                  # "string" | "numeric"
    samples: List[Any] = field(default_factory=list)

def fetch_samples(engine: Engine, table: str, col: str, k=5):
    sql = text(f"SELECT {col} FROM {table} WHERE {col} IS NOT NULL LIMIT :k")
    with engine.begin() as conn:
        return [r[0] for r in conn.execute(sql, {"k": k}).fetchall()]

def load_schema(engine: Engine) -> List[ColumnInfo]:
    insp, out = inspect(engine), []
    for table in insp.get_table_names():
        for col in insp.get_columns(table):
            colname, dtype = col["name"], str(col["type"]).lower()
            is_str = any(t in dtype for t in ("char", "text", "uuid"))
            out.append(ColumnInfo(
                table, colname, "string" if is_str else "numeric",
                fetch_samples(engine, table, colname)))
    return out


# ──────────────────────────────────────────────────────────────────────────────
# 3.  RULE JSON GENERATION
# ──────────────────────────────────────────────────────────────────────────────

def pick_col(cols): return random.choice(cols)
def pick_op(ci):     return random.choice(OPS_STR if ci.dtype=="string" else OPS_NUM)

def rand_val(ci, op):
    if op in NO_VAL: return None
    return random.choice(ci.samples) if ci.samples else ("" if ci.dtype=="string" else 0)

def make_leaf(ci, groups):
    op, val = pick_op(ci), None
    if op not in NO_VAL: val = rand_val(ci, op)

    key = (ci.table, )
    groups.setdefault(key, str(len(groups)+1))
    leaf = {
        "field": {
            "name": ci.column,
            "datasource": "CASGraphQL",
            "namespace": ci.table,
            "evaluation_group": groups[key],
        },
        "comp": op,
    }
    if val is not None: leaf["value"] = val
    return leaf

def subtree(depth, cols, groups):
    if depth>=MAX_DEPTH or random.random()>SUBGROUP_P:
        return make_leaf(pick_col(cols), groups)
    node_op = random.choice(["and","or"])
    kids = [subtree(depth+1, cols, groups)
            for _ in range(random.randint(2, MAX_TERMS))]
    return {"op": node_op, "terms": kids}

def build_rule(cols):
    groups={}
    terms=[subtree(1,cols,groups) for _ in range(random.randint(2,MAX_TERMS))]
    return {"op":"and","terms":terms,"version":"2"}


# ──────────────────────────────────────────────────────────────────────────────
# 4.  JSON → SQL TRANSPILER
# ──────────────────────────────────────────────────────────────────────────────

def quote(v):
    if isinstance(v,str):  return f"'{v}'"
    if isinstance(v,bool): return "TRUE" if v else "FALSE"
    return str(v)

def transpile(rule):
    # 4‑A  collect aliases
    alias_map, joins = {}, []
    def alias(tab, eg):
        key=(tab,eg)
        if key in alias_map: return alias_map[key]
        alias_map[key]=f"t{len(alias_map)+1}"
        return alias_map[key]

    # 4‑B  recursively build predicates
    def pred(node):
        if "op" in node:
            joined=f" {node['op'].upper()} ".join(pred(t) for t in node["terms"])
            return f"({joined})"
        f=node["field"]; a=alias(f["namespace"], f["evaluation_group"])
        col=f"{a}.{f['name']}"
        op=node["comp"]; val=node.get("value")

        mapping={
            "equal to":f"{col} = {quote(val)}",
            "not equal to":f"{col} <> {quote(val)}",
            "greater than":f"{col} > {quote(val)}",
            "greater than equal to":f"{col} >= {quote(val)}",
            "less than":f"{col} < {quote(val)}",
            "less than equal to":f"{col} <= {quote(val)}",
            "is null":f"{col} IS NULL",
            "is empty":f"({col} = '' OR {col} IS NULL)",
            "exists":f"EXISTS (SELECT 1 FROM {f['namespace']} {a}_ex WHERE {a}_ex.cust_id = :cust_id)",
            "not exists":f"NOT EXISTS (SELECT 1 FROM {f['namespace']} {a}_nex WHERE {a}_nex.cust_id = :cust_id)",
        }
        return mapping[op]

    where_clause=pred(rule)
    # 4‑C  FROM / JOIN list
    keys=list(alias_map.keys())
    root_tab, root_alias=keys[0][0], alias_map[keys[0]]
    joins=[f"JOIN {t} {alias_map[(t,eg)]} ON {alias_map[(t,eg)]}.cust_id = {root_alias}.cust_id"
           for (t,eg) in keys[1:]]

    sql=(
        f"SELECT 1\nFROM {root_tab} {root_alias}\n" +
        ("\n".join(joins)) + "\nWHERE " +
        where_clause +
        f"\n  AND {root_alias}.cust_id = :cust_id;"
    )
    return sql


# ──────────────────────────────────────────────────────────────────────────────
# 5.  CLI
# ──────────────────────────────────────────────────────────────────────────────

def main():
    if len(sys.argv)<3:
        print("usage: rule_gen_from_db.py <db‑url> <n>")
        sys.exit(1)
    eng=create_engine(sys.argv[1]); n=int(sys.argv[2])
    cols=load_schema(eng)
    if not cols: sys.exit("no schema!")

    random.seed()
    for _ in range(n):
        rule=build_rule(cols)
        print("‑‑ JSON ‑‑")
        print(json.dumps(rule,indent=2,default=str))
        print("\n‑‑ SQL  ‑‑")
        print(transpile(rule))
        print("\n"+"═"*70+"\n")

if __name__=="__main__":
    main()
