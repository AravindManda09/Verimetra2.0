# Dependency-Aware Incremental Revalidation Algorithm

## 1. Problem Formulation

Let $\mathcal{C}$ be the e-commerce product catalogue:
$$\mathcal{C} = \{p_1, p_2, \dots, p_N\}$$
Let $\mathcal{R}$ be the set of regulatory compliance rules. Each rule $r \in \mathcal{R}$ has an active version $v_r$, defined by:
$$v_r = \langle \text{reqType}, \text{reqField}, \text{op}, \text{threshold}, \mathcal{A}_r \rangle$$
where $\mathcal{A}_r \subseteq \{\text{FOOD}, \text{COSMETICS}, \text{ELECTRONICS}, \text{HOUSEHOLD}\}$ denotes the applicable commodity categories.

When an administrator updates a rule from version $v_r^{\text{old}}$ to $v_r^{\text{new}}$, the naive approach (**Full Revalidation**) evaluates all products in $\mathcal{C}$ against $v_r^{\text{new}}$.

The central research objective of **VeriMetra** is:
> To identify the minimal complete candidate set $\mathcal{K} \subseteq \mathcal{C}$ affected by the change, re-evaluate only $\mathcal{K}$ using the identical deterministic evaluator, and safely reuse unaffected evaluations while guaranteeing 100% logical agreement with Full Revalidation.

---

## 2. The Two-Phase Incremental Algorithm

### Phase A: Previously Associated Products
Identifies all products that were evaluated under the previous version $v_r^{\text{old}}$ or have an active entry in the dependency graph:
$$\mathcal{K}_A = \{ p \in \mathcal{C} \mid (p, r) \in \text{Dependencies} \}$$

### Phase B: Newly Applicable Products (Applicability Expansion)
If the updated rule version expands applicability:
$$\Delta \mathcal{A} = \mathcal{A}_r^{\text{new}} \setminus \mathcal{A}_r^{\text{old}}$$
If $\Delta \mathcal{A} \neq \emptyset$, any existing catalogue product whose category falls in $\Delta \mathcal{A}$ becomes newly subject to rule $r$:
$$\mathcal{K}_B = \{ p \in \mathcal{C} \mid \text{category}(p) \in \Delta \mathcal{A} \land p \notin \mathcal{K}_A \}$$

### Deduplicated Candidate Pool
$$\mathcal{K} = \mathcal{K}_A \cup \mathcal{K}_B \quad (\mathcal{K}_A \cap \mathcal{K}_B = \emptyset)$$

---

## 3. Algorithm Pseudocode

```python
def incremental_revalidation(rule_id, new_version_id):
    start_time = now()
    rule = get_rule(rule_id)
    new_version = get_version(new_version_id)
    prev_version = get_previous_version(rule_id, new_version_id)

    # 1. Safety Fallback Check
    if prev_version is None or dependencies_corrupted():
        log("Safety fallback triggered: missing baseline version.")
        return full_revalidation(rule_id, new_version_id, fallback=True)

    # 2. Compute Category Delta
    old_cats = get_categories(prev_version.id)
    new_cats = get_categories(new_version.id)
    added_cats = new_cats - old_cats
    removed_cats = old_cats - new_cats

    # 3. Phase A Discovery
    candidates = {}
    for pid in get_dependencies(rule.id):
        candidates[pid] = "PREVIOUSLY_ASSOCIATED"

    # 4. Phase B Discovery (Applicability Expansion)
    if added_cats:
        for product in find_products_by_categories(added_cats):
            if product.id not in candidates:
                candidates[product.id] = "NEWLY_APPLICABLE"

    selection_duration = now() - start_time

    # 5. Deterministic Evaluation of Candidates
    eval_start = now()
    evaluations_performed = 0
    findings_generated = 0

    for pid, reason in candidates.items():
        product = get_product(pid)
        declarations = get_confirmed_declarations(pid)
        evidence = get_latest_evidence(pid)

        # EXACT SAME DETERMINISTIC EVALUATOR AS FULL REVALIDATION
        result = DeterministicRuleEvaluator.evaluate(
            product, declarations, rule, new_version, new_cats, evidence
        )

        persist_evaluation_and_snapshot(product, new_version, result)
        update_dependency(pid, rule.id, new_version.id)
        evaluations_performed += 1
        findings_generated += len(result.findings)

    # 6. Compute Reusable Unaffected Evaluations
    total_catalogue = count_catalogue()
    evaluations_reused = max(0, total_catalogue - len(candidates))

    total_duration = now() - start_time

    return RevalidationRunReport(
        candidate_count=len(candidates),
        phase_a_count=len(candidates) - len(added_cats),
        phase_b_count=len(added_cats),
        evaluations_performed=evaluations_performed,
        evaluations_reused=evaluations_reused,
        candidate_selection_ms=selection_duration,
        total_duration_ms=total_duration,
        used_fallback=False
    )
```

---

## 4. Correctness Guarantee & Canonical Comparison

Both strategies evaluate candidate pairs using the exact same function:
$$\text{Eval}(p, \text{decls}, r, v_r, \mathcal{A})$$

Logical equivalence is established by comparing canonicalized tuples:
$$\text{Tuple}(p) = \langle \text{status}, \{ (\text{type}_i, \text{field}_i, \text{severity}_i) \}_{i=1}^m \rangle$$

- **Agreement %**:
$$\text{Agreement} = \frac{|\text{Matches}|}{|\text{Matches}| + |\text{Differences}| + |\text{Missed}| + |\text{Extra}|} \times 100\%$$
- **Correctness Status**:
  - `PERFECT_AGREEMENT`: $100\%$ match, zero missed or differing outcomes.
  - `DISCREPANCY_DETECTED`: Indicates unsafe selection or dependency divergence.
