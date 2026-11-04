# VeriMetra Experiment & Benchmarking Guide

## 1. Experimental Objectives

The purpose of VeriMetra's experimental lab is to empirically answer the central research question:
> *Can dependency-aware incremental revalidation identify product-rule evaluations affected by regulatory rule changes, including newly applicable products, while preserving 100% agreement with full revalidation and reducing computational work?*

---

## 2. Experimental Variables

| Variable | Values Supported | Purpose |
| :--- | :--- | :--- |
| **Catalogue Size ($N$)** | 100, 1,000, 10,000, 50,000 | Evaluates scalability across small to enterprise catalogues. |
| **Impact Rate ($\alpha$)** | 5%, 25%, 50%, 90% | Controls the fraction of catalogue items whose categories or declarations are impacted. |
| **Scenario** | `APPLICABILITY_EXPANSION`<br>`APPLICABILITY_NARROWING`<br>`REQUIREMENT_MODIFICATION`<br>`THRESHOLD_MODIFICATION` | Tests candidate discovery when rule scope broadens, shrinks, or changes constraints. |
| **Random Seed** | Fixed integer (e.g. 42) | Guarantees deterministic, reproducible catalogue generation across repeated trials. |

---

## 3. Evaluated Metrics

1. **Canonical Result Agreement %**:
   $$\text{Agreement} = \frac{|\text{Matching Outcomes}|}{|\text{Matching}| + |\text{Differing}| + |\text{Missed}| + |\text{Extra}|} \times 100\%$$
   *Target: 100% across all scenarios.*
2. **Evaluation Work Reduction %**:
   $$\Delta \text{Evals} = \frac{\text{Full Evals} - \text{Incr Evals}}{\text{Full Evals}} \times 100\%$$
3. **Execution Time Reduction %**:
   $$\Delta \text{Time} = \frac{\text{Full Duration} - \text{Incr Duration}}{\text{Full Duration}} \times 100\%$$
4. **Candidate Selection Overhead**:
   Granular timing for Phase A and Phase B candidate identification.
5. **Safe Fallback Rate**:
   Tracks instances where incomplete dependency metadata forced a safe full fallback.

---

## 4. How to Interpret Results

- **When $\alpha$ is Low (5% - 25%)**:
  Incremental revalidation delivers massive computational savings (75% to 95% work reduction), and total runtime is substantially faster than Full Revalidation.
- **When $\alpha$ is Very High (90%)**:
  Incremental candidate selection overhead ($T_{\text{selection}}$) plus re-evaluation may approach Full Revalidation runtime. This is scientifically honest and expected in incremental computing theory.
- **Applicability Expansion Test**:
  Verifies that Phase B successfully discovers newly applicable commodities not present in Phase A without full catalogue scans.
