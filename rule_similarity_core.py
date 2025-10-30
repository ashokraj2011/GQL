import json
import numpy as np
import pandas as pd
from sentence_transformers import SentenceTransformer
from sklearn.metrics.pairwise import cosine_similarity
from sklearn.cluster import DBSCAN
from sklearn.feature_extraction.text import CountVectorizer
import umap

def flatten_rule(rule):
    if isinstance(rule, str):
        try:
            rule = json.loads(rule)
        except:
            return rule
    def rec(x):
        if isinstance(x, dict):
            return " ".join(f"{k}:{rec(v)}" for k, v in x.items())
        elif isinstance(x, list):
            return " ".join(rec(i) for i in x)
        else:
            return str(x)
    return rec(rule)

def compute_embeddings(df):
    model = SentenceTransformer("all-MiniLM-L6-v2")
    df["text_rule"] = df["rule_json"].apply(flatten_rule)
    embeddings = model.encode(df["text_rule"].tolist(), normalize_embeddings=True)
    return embeddings

def compute_similarity_report(df, embeddings, top_k=3):
    sim_matrix = cosine_similarity(embeddings)
    sim_df = pd.DataFrame(sim_matrix, index=df["rulename"], columns=df["rulename"])
    rows = []
    for rule in sim_df.index:
        sims = sim_df.loc[rule].drop(rule).sort_values(ascending=False).head(top_k)
        for other, score in sims.items():
            rows.append({"rule": rule, "similar_rule": other, "similarity": round(score, 3)})
    return sim_df, pd.DataFrame(rows)

def cluster_rules(df, embeddings):
    cluster_model = DBSCAN(eps=0.25, min_samples=1, metric="cosine")
    labels = cluster_model.fit_predict(embeddings)
    df["cluster_id"] = labels

    vectorizer = CountVectorizer(stop_words="english")
    X = vectorizer.fit_transform(df["text_rule"])
    feature_names = np.array(vectorizer.get_feature_names_out())

    def top_tokens(cid, top_n=3):
        subset = df[df["cluster_id"] == cid]["text_rule"].tolist()
        if not subset:
            return ""
        mat = vectorizer.transform(subset).sum(axis=0)
        counts = np.asarray(mat).flatten()
        top_idx = counts.argsort()[::-1][:top_n]
        return ", ".join(feature_names[top_idx])

    cluster_labels = {cid: top_tokens(cid) for cid in sorted(df["cluster_id"].unique())}
    df["cluster_label"] = df["cluster_id"].map(cluster_labels)
    return df, cluster_labels

def project_umap(embeddings):
    reducer = umap.UMAP(n_neighbors=3, min_dist=0.1, metric="cosine", random_state=42)
    proj = reducer.fit_transform(embeddings)
    return proj
