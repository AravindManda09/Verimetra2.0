# Reference Projects & Academic Literature Study

This document records architectural patterns, design ideas, and adaptations synthesized from reference systems and academic literature.

---

## 1. Validra (https://github.com/dhruvpatel16120/Validra)
- **Analyzed Concepts**:
  - Image acquisition workflow for physical commodity packages.
  - OCR-assisted field extraction targeting mandatory legal fields (MRP, net quantity, manufacturer name).
  - Deterministic compliance evaluation comparing declared attributes against legal requirements.
- **VeriMetra Adaptation**:
  - Validra provided a prototype compliance workflow. VeriMetra formalizes this into an enterprise-grade pipeline separating provisional OCR suggestions from human-confirmed declarations.
  - VeriMetra introduces **dependency-aware incremental revalidation**, which Validra does not possess.

---

## 2. PARAKH (https://github.com/jitendrachoudhary1401-hue/Parakh)
- **Analyzed Concepts**:
  - Clear architectural decoupling between text extraction and compliance verification.
  - Human review of ambiguous package declarations.
  - Auditable inspection history.
- **VeriMetra Adaptation**:
  - VeriMetra adopts the strict separation between provisional extraction suggestions and confirmed product declarations.
  - VeriMetra omits experimental blockchain components present in reference prototypes in favor of an ACID-compliant PostgreSQL audit trail with frozen JSONB snapshots.

---

## 3. LabelIQ (https://github.com/nawangdorjay/labeliq)
- **Analyzed Concepts**:
  - Human-in-the-loop review interface for OCR suggestions.
  - Separation of provisional extraction from confirmed product records.
  - Rule versioning concepts.
- **VeriMetra Adaptation**:
  - Implemented an interactive review table where each extracted field can be individually **Confirmed**, **Edited**, or **Rejected**.
  - Confirmed values automatically undergo deterministic normalization (e.g. `500 g` to `0.5 kg`, `1000 ml` to `1 L`).

---

## 4. Legal-Lens (https://github.com/VarnitAgustya27/LEGAL_LENS)
- **Analyzed Concepts**:
  - Finding traceability linking directly to legislative clauses.
  - Clear visual distinction between mandatory legal requirements and consistency checks.
- **VeriMetra Adaptation**:
  - VeriMetra explicitly distinguishes two finding types:
    - `REGULATORY_REQUIREMENT`: Absolute statutory compliance checks.
    - `CROSS_SOURCE_CONSISTENCY`: Analytical discrepancy checks between listing values and package values. Cross-source mismatch indicates review need, not automatic legal culpability.

---

## 5. Incremental Entity Resolution on Rules and Data (Whang et al., VLDBJ)
- **Academic Citation**: Whang, S. E., Benjelloun, O., & Garcia-Molina, H. (2009). *Generic entity resolution with negative rules and incremental processing*. VLDB Journal.
- **Analyzed Concepts**:
  - When rules or underlying entity attributes evolve, avoid recomputing the entire universe.
  - Model dependencies between rule definitions and previously processed records.
  - Reuse prior verified results when their validity can be formally established.
- **VeriMetra Research Adaptation**:
  - Adapted the incremental entity resolution principles to regulatory compliance checking.
  - Formalized candidate selection into **Phase A** (retrieving previously associated products via dependency graph) and **Phase B** (discovering newly applicable products upon applicability expansion).

---

## 6. MURCIA & GDPR Automated Compliance Checking
- **Analyzed Concepts**:
  - Regulatory change tracking with human-in-the-loop versioning.
  - Requirement traceability matrix and explainable findings.
- **VeriMetra Adaptation**:
  - Versioned declarative rules rather than opaque code execution or arbitrary scripts.
  - Immutable version snapshots ensuring that historical evaluations remain reproducible even after regulations evolve.
