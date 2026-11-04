# VeriMetra End-to-End Demonstration Guide

Follow this exact 12-step sequence through the application web interface (`http://localhost:3000`) without writing manual SQL queries.

---

### Step 1 — Open the Overview Dashboard
- Navigate to `http://localhost:3000`.
- Verify the seeded metrics:
  - Total Products (14 seeded products across FOOD, COSMETICS, ELECTRONICS, HOUSEHOLD).
  - Active Rules (7 compliance rules).
  - Category distribution and findings breakdown charts.

---

### Step 2 — Register a Packaged Food Product
- Click on **Products** in the left sidebar.
- Click **Register Product**.
- Enter:
  - **Product Name**: `Aashirvaad Whole Wheat Atta`
  - **SKU**: `FOOD-008`
  - **Category**: `FOOD`
  - **Manufacturer Name**: `ITC Limited`
  - **Country of Origin**: `India`
  - **Description**: `100% whole wheat flour, packaged`
- Click **Register**. The product appears in the table with ID assigned.

---

### Step 3 — Upload Package Evidence & Run OCR
- Click on **Package Evidence** in the left sidebar.
- Under **Upload Package Image**:
  - Select your newly created product (`#15 — Aashirvaad Whole Wheat Atta`).
  - Choose a package image file (or any sample packaging JPG/PNG).
  - Click **Upload Evidence**.
- Click **Run OCR Extraction**.
- View the raw extracted OCR text and provisional suggested fields (MRP, net quantity, manufacturer, origin).

---

### Step 4 — Review & Confirm Extracted Declarations
- Review the Extracted Declaration Review table.
- Verify suggested values. Edit or add any missing declaration (e.g. `mrp` = `240.00`, `net_quantity` = `5 kg`).
- Click **Confirm & Save as Package Declarations**.
- Notice the confirmed values are saved to the product record with normalized units (`5 kg` &rarr; `5.0 kg`).

---

### Step 5 — Run Compliance Evaluation
- Return to **Products** and click on your product.
- Click **Run Compliance Check**.
- Observe the evaluated findings:
  - `LM-PC-R6-MRP`: COMPLIANT.
  - `LM-PC-R6-NETQTY`: COMPLIANT.
  - `VM-CROSS-MRP`: Cross-source consistency evaluated between listing and package.

---

### Step 6 — Inspect Rule Version 1
- Navigate to **Rule Management**.
- Select rule `LM-PC-R6-MRP` (MRP Declaration Required).
- Observe that **Version 1** applicability is restricted to **FOOD** only.

---

### Step 7 — Create Rule Version 2 (Applicability Expansion)
- In Rule Management, click **Create Version**.
- In the version creation modal:
  - Check both **FOOD** and **COSMETICS** under Applicable Categories.
  - Enter Change Summary: `Expanded statutory applicability to COSMETICS as per Legal Metrology amendment`.
  - Click **Create Version**.
- In the version history table, click **Activate &rarr;** next to v2.
- v1 transitions to `SUPERSEDED`, and v2 becomes `ACTIVE`.
- (Optional) Use **Compare Rule Versions** to inspect the visual diff showing `COSMETICS` added.

---

### Step 8 — Run Incremental Revalidation (Research Showcase)
- Navigate to **Revalidation** in the sidebar.
- Notice the **Two-Phase Architecture** visual summary.
- Select Rule `LM-PC-R6-MRP` and Version `v2`.
- Click **Run Incremental Revalidation**.
- Observe the real-time breakdown:
  - **Phase A (Previously Associated)**: Discovered existing FOOD products evaluated under v1.
  - **Phase B (Newly Applicable)**: Discovered existing COSMETICS products (e.g. `Aloe Vera Face Wash`, `Coconut Hair Oil`, `Herbal Shampoo`) that were never evaluated under this rule before!
  - **Deduplication**: Total candidates = Phase A + Phase B.
  - **Evaluations Reused**: Unaffected ELECTRONICS and HOUSEHOLD products safely retain their prior states.
  - **Total Execution Time**: Granular milliseconds recorded.

---

### Step 9 — Run Full Revalidation Baseline
- Click **Run Full (Baseline)** on the same rule and version.
- Observe that Full Revalidation scans the entire catalogue without candidate filtering.
- Review the duration and evaluation count comparison.

---

### Step 10 — Compare Strategies & Canonical Agreement
- Inspect the Candidate Breakdown table:
  - Candidates marked `PREVIOUSLY_ASSOCIATED` vs `NEWLY_APPLICABLE`.
  - Both Full and Incremental strategies achieve identical logical compliance outcomes.

---

### Step 11 — Run Benchmarking Experiment
- Navigate to **Experiments** in the sidebar.
- Select Catalogue Size: `1,000 products`, Scenario: `APPLICABILITY_EXPANSION`, Impact Rate: `25%`.
- Click **Run Experiment**.
- Observe the live results:
  - **Correctness Agreement**: `100.0%` (`PERFECT_AGREEMENT`).
  - **Work Reduction %**: ~75% reduction in evaluations performed.
  - **Comparative BarChart**: Side-by-side Full vs Incremental work and runtime.

---

### Step 12 — Export Compliance and Audit Reports
- Navigate to **Reports** in the sidebar.
- Click **Download CSV** on the Product Compliance Report.
- Click **Download Benchmark CSV** on the Experiment Report.
- Verify that both CSV files contain structured audit records.
