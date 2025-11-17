import pandas as pd
import matplotlib.pyplot as plt

# ========== Load & clean data ==========
df = pd.read_csv("results.csv")
df.columns = df.columns.str.strip()

print("Columns:", df.columns.tolist())

# Split serial (baseline) and parallel
serial = df[df["WithParallelism"] == 0].copy()
parallel = df[df["WithParallelism"] == 1].copy()

serial = serial.rename(columns={"RunTime": "SerialRunTime"})
parallel = parallel.rename(columns={"RunTime": "ParallelRunTime"})

# Match serial & parallel by BoardSize, MaxDepth ONLY
merged = pd.merge(
    parallel,
    serial[["BoardSize", "MaxDepth", "SerialRunTime"]],
    on=["BoardSize", "MaxDepth"],
    how="left",
)

# Filter for valid timing data (avoid division by zero / missing baseline)
merged = merged[
    (merged["SerialRunTime"] > 0)
    & (merged["ParallelRunTime"] > 0)
    & (~merged["SerialRunTime"].isna())
]

# Compute speedup = T_serial / T_parallel
merged["Speedup"] = merged["SerialRunTime"] / merged["ParallelRunTime"]

# ----------------------------------------------------
# 1) GENERAL SPEEDUP: average over all problems
# ----------------------------------------------------
avg_speedup = (
    merged.groupby(["ThreadCount", "ParallelVersion"])["Speedup"]
    .mean()
    .reset_index()
    .sort_values(["ParallelVersion", "ThreadCount"])
)

plt.figure()
plt.title("Average Speedup vs ThreadCount (all BoardSizes / MaxDepths)")
plt.xlabel("ThreadCount")
plt.ylabel("Speedup (Serial / Parallel)")

for pv in sorted(avg_speedup["ParallelVersion"].unique()):
    sub = avg_speedup[avg_speedup["ParallelVersion"] == pv]
    plt.plot(
        sub["ThreadCount"],
        sub["Speedup"],
        marker="o",
        linestyle="-",
        label=f"Parallel V{pv}",
    )

plt.legend()
plt.grid(True, linestyle="--", alpha=0.3)
plt.tight_layout()
plt.savefig("speedup_overall.png")
print("Saved: speedup_overall.png")

# ----------------------------------------------------
# 2) INDIVIDUAL SPEEDUPS: 6 lines (V1/V2 × 2/4/8 threads)
# ----------------------------------------------------
# Sort by problem size for consistent x-axis
merged_sorted = merged.sort_values(["BoardSize", "MaxDepth"]).copy()
merged_sorted["Label"] = merged_sorted.apply(
    lambda row: f"B{row['BoardSize']}-D{row['MaxDepth']}", axis=1
)

plt.figure()
plt.title("Speedup per Problem (V1/V2 × Threads)")
plt.xlabel("Problem (BoardSize-MaxDepth)")
plt.ylabel("Speedup (Serial / Parallel)")

thread_values = [2, 4, 8]
markers = {2: "o", 4: "s", 8: "^"}

for pv in [1, 2]:
    for t in thread_values:
        sub = merged_sorted[
            (merged_sorted["ParallelVersion"] == pv)
            & (merged_sorted["ThreadCount"] == t)
        ]
        if sub.empty:
            continue
        x = range(len(sub))
        label = f"V{pv}, {t} threads"
        plt.plot(
            x,
            sub["Speedup"],
            marker=markers[t],
            linestyle="-",
            label=label,
        )

# Set x-ticks based on the first subset's labels (they all align by sort)
# Use the union of labels in merged_sorted
labels = merged_sorted.drop_duplicates(["BoardSize", "MaxDepth"])["Label"].tolist()
plt.xticks(range(len(labels)), labels, rotation=45, ha="right")

plt.legend()
plt.grid(True, linestyle="--", alpha=0.3)
plt.tight_layout()
plt.savefig("speedup_individual.png")
print("Saved: speedup_individual.png")

# ----------------------------------------------------
# 3) RUNTIME PLOTS per ThreadCount (Serial vs V1 vs V2)
# ----------------------------------------------------
for t in [2, 4, 8]:
    # Serial baseline: ignore thread count, just one per (BoardSize, MaxDepth)
    s = serial[["BoardSize", "MaxDepth", "SerialRunTime"]].copy()

    # Parallel V1 and V2 for this thread count
    v1 = parallel[
        (parallel["ParallelVersion"] == 1)
        & (parallel["ThreadCount"] == t)
    ][["BoardSize", "MaxDepth", "ParallelRunTime"]].rename(
        columns={"ParallelRunTime": "RunTimeV1"}
    )

    v2 = parallel[
        (parallel["ParallelVersion"] == 2)
        & (parallel["ThreadCount"] == t)
    ][["BoardSize", "MaxDepth", "ParallelRunTime"]].rename(
        columns={"ParallelRunTime": "RunTimeV2"}
    )

    # Merge serial + V1 + V2 on (BoardSize, MaxDepth)
    tmp = pd.merge(
        s,
        v1,
        on=["BoardSize", "MaxDepth"],
        how="inner",
    )
    tmp = pd.merge(
        tmp,
        v2,
        on=["BoardSize", "MaxDepth"],
        how="inner",
    )

    if tmp.empty:
        print(f"No data for ThreadCount={t}, skipping runtime plot.")
        continue

    tmp = tmp.sort_values(["BoardSize", "MaxDepth"])
    tmp["Label"] = tmp.apply(
        lambda row: f"B{row['BoardSize']}-D{row['MaxDepth']}", axis=1
    )

    x = range(len(tmp))

    plt.figure()
    plt.title(f"RunTime vs Problem Size (ThreadCount={t})")
    plt.xlabel("Problem (BoardSize-MaxDepth)")
    plt.ylabel("RunTime (seconds)")

    plt.plot(x, tmp["SerialRunTime"], marker="o", linestyle="-", label="Serial")
    plt.plot(x, tmp["RunTimeV1"], marker="s", linestyle="-", label="Parallel V1")
    plt.plot(x, tmp["RunTimeV2"], marker="^", linestyle="-", label="Parallel V2")

    plt.xticks(x, tmp["Label"], rotation=45, ha="right")
    plt.legend()
    plt.grid(True, linestyle="--", alpha=0.3)
    plt.tight_layout()

    fname = f"runtimes_threads{t}.png"
    plt.savefig(fname)
    print(f"Saved: {fname}")
    # plt.show()
