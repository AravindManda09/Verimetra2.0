# Legal Metrology Rule Sources & Statutory References

## 1. Statutory Context

In India, pre-packaged commodities sold online and offline are governed by:
- **The Legal Metrology Act, 2009** (Act No. 1 of 2010)
- **The Legal Metrology (Packaged Commodities) Rules, 2011** (GSR 202(E) dated 7th March 2011 as amended)
- **Department of Consumer Affairs**, Ministry of Consumer Affairs, Food and Public Distribution, Government of India.
- **Source Portal**: https://consumeraffairs.nic.in/acts-and-rules/legal-metrology
- **Verification Date**: 15th January 2024

---

## 2. Verified Legal Rules Implemented in VeriMetra

| Rule Code | Legal Clause | Mandatory Requirement | Applicability |
| :--- | :--- | :--- | :--- |
| `LM-PC-R6-MRP` | Rule 6(1)(d) | The Maximum Retail Price (MRP) inclusive of all taxes must be declared on every retail package. | Pre-packaged retail commodities |
| `LM-PC-R6-NETQTY` | Rule 6(1)(b) | The net quantity in terms of standard unit of weight or measure (g, kg, ml, L) must be declared. | All pre-packaged commodities |
| `LM-PC-R6-MFR` | Rule 6(1)(a) | The name and complete address of the manufacturer or packer or importer must be declared. | All commodities |
| `LM-PC-R6-COO` | Rule 6(2) | The name of the country of origin or manufacture must be declared on every imported package and e-commerce listing. | All imported commodities & e-commerce |
| `LM-PC-R6-NAME` | Rule 6(1) | The generic or common name of the commodity must be prominently declared. | All commodities |

---

## 3. Illustrative / Analytical Checks (Clearly Distinguished)

The following rules are analytical checks developed by VeriMetra for e-commerce consistency intelligence. They are explicitly marked with `is_illustrative = true`:

| Rule Code | Type | Description |
| :--- | :--- | :--- |
| `VM-CROSS-MRP` | `CROSS_SOURCE_CONSISTENCY` | Compares listing declared MRP against physical packaging OCR MRP. Discrepancy triggers `NEEDS_REVIEW` finding. |
| `VM-CROSS-NETQTY` | `CROSS_SOURCE_CONSISTENCY` | Compares listing declared net quantity against physical packaging OCR net quantity. Normalized comparisons prevent false positives from unit notation differences (e.g. 500 g vs 0.5 kg). |

> **Disclaimer**: VeriMetra is a decision-support and research platform. It does not issue official legal certification or replace qualified legal inspection authorities.
