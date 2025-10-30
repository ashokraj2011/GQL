import streamlit as st
import pandas as pd
import json
import seaborn as sns
import matplotlib.pyplot as plt
from rule_similarity_core import (
    compute_embeddings, compute_similarity_report, cluster_rules, project_umap
)

st.set_page_config(page_title="CCRE Rule Similarity & Clustering", layout="wide")

st.title("🧠 CCRE Rule Similarity & Clustering Analyzer")

st.sidebar.header("🔧 Configuration")

# -------------------------------
# Upload rules
# -------------------------------
uploaded = st.sidebar.file_uploader("Upload Rule JSON/CSV", type=["json", "csv"])

if uploaded is not None:
    if uploaded.name.endswith(".json"):
        rules = json.load(uploaded)
        df = pd.DataFrame(rules)
    else:
        df = pd.read_csv(uploaded)
else:
    st.info("Upload a file with columns `rulename` and `rule_json` to begin.")
    st.stop()

st.write("### Raw Rules")
st.dataframe(df.head(), use_container_width=True)

# -------------------------------
# Compute everything
# -------------------------------
with st.spinner("Computing embeddings, similarities, and clusters..."):
    embeddings = compute_embeddings(df)
    sim_df, report_df = compute_similarity_report(df, embeddings, top_k=3)
    df, cluster_labels = cluster_rules(df, embeddings)
    proj = project_umap(embeddings)

st.success("Analysis complete ✅")

# -------------------------------
# Sidebar filters
# -------------------------------
selected_cluster = st.sidebar.selectbox(
    "Filter by Cluster",
    options=["All"] + [f"{cid} - {label}" for cid, label in cluster_labels.items()]
)

similarity_threshold = st.sidebar.slider(
    "Duplicate Detection Threshold",
    min_value=0.5,
    max_value=1.0,
    value=0.9,
    step=0.01,
    help="Rules with similarity above this value are considered duplicates."
)

# -------------------------------
# Filter & duplicate detection
# -------------------------------
if selected_cluster != "All":
    cid = int(selected_cluster.split(" - ")[0])
    df = df[df["cluster_id"] == cid]

# Identify duplicates
duplicates_df = report_df[report_df["similarity"] >= similarity_threshold]
duplicates_df = duplicates_df.sort_values(by="similarity", ascending=False)

st.sidebar.markdown("---")
st.sidebar.metric("🧩 Possible Duplicates", len(duplicates_df))

# -------------------------------
# Visuals
# -------------------------------
st.subheader("🔥 Similarity Heatmap")
fig, ax = plt.subplots(figsize=(6, 5))
sns.heatmap(sim_df, annot=True, cmap="YlGnBu", ax=ax)
st.pyplot(fig)

st.subheader("🎯 UMAP Rule Projection")
fig, ax = plt.subplots(figsize=(6, 5))
palette = sns.color_palette("tab10", len(set(df["cluster_id"])))
for i, row in df.iterrows():
    ax.scatter(proj[i, 0], proj[i, 1],
               color=palette[row["cluster_id"] % len(palette)], s=60)
    ax.text(proj[i, 0] + 0.01, proj[i, 1], row["rulename"], fontsize=8)
ax.set_title("UMAP Rule Clusters")
st.pyplot(fig)

# -------------------------------
# Results & Reports
# -------------------------------
st.subheader("📄 Similarity Report (Top Matches)")
st.dataframe(report_df, use_container_width=True)

st.subheader(f"🧬 Duplicate Rules (Threshold ≥ {similarity_threshold})")
if duplicates_df.empty:
    st.info("No duplicates found above this threshold.")
else:
    st.dataframe(duplicates_df, use_container_width=True)

# -------------------------------
# Download Buttons
# -------------------------------
st.markdown("### 📥 Download Reports")

col1, col2, col3 = st.columns(3)
with col1:
    st.download_button(
        "⬇️ Download Similarity Report",
        report_df.to_csv(index=False).encode("utf-8"),
        "rule_similarity_report.csv",
    )
with col2:
    st.download_button(
        "⬇️ Download Clustered Rules",
        df.to_csv(index=False).encode("utf-8"),
        "rule_clusters.csv",
    )
with col3:
    st.download_button(
        "⬇️ Download Duplicates",
        duplicates_df.to_csv(index=False).encode("utf-8"),
        "rule_duplicates.csv",
    )
