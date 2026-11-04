# VeriMetra REST API Documentation

Base URL: `http://localhost:8080/api`
Swagger OpenAPI UI: `http://localhost:8080/swagger-ui.html`
OpenAPI JSON: `http://localhost:8080/api-docs`

---

## 1. Product Management

### `GET /api/products`
Lists products with optional filtering.
- **Query Params**:
  - `category` (optional, e.g. `FOOD`, `COSMETICS`)
  - `search` (optional string search)
  - `page` (default 0), `size` (default 20), `sortBy`, `sortDir`
- **Response**: `200 OK` Page of `ProductDTO`.

### `POST /api/products`
Registers a new product.
- **Request Body**:
  ```json
  {
    "productName": "Basmati Rice Premium",
    "sku": "FOOD-002",
    "category": "FOOD",
    "manufacturerName": "India Gate Foods",
    "countryOfOrigin": "India",
    "description": "Aged basmati rice"
  }
  ```
- **Response**: `201 Created` `ProductDTO`.

### `GET /api/products/{id}`
Returns details for a single product.

### `GET /api/products/{id}/declarations`
Returns confirmed and provisional declarations for a product (LISTING vs PACKAGE).

### `POST /api/products/{id}/declarations`
Updates declarations for a product with automatic normalization.

---

## 2. Package Evidence & OCR

### `POST /api/evidence/upload/{productId}`
Uploads a package image (`multipart/form-data`).
- **File Types**: JPG, PNG, WEBP, BMP, TIFF. Max size 10MB.
- **Response**: `201 Created` `EvidenceDTO`.

### `POST /api/evidence/{id}/extract`
Invokes the OCR extraction service.
- **Response**: `200 OK` `OcrExtractionDTO` containing `rawText` and structured `extractedFields`.

### `POST /api/evidence/{id}/confirm`
Submits human-in-the-loop review of extracted fields.
- **Request Body**:
  ```json
  {
    "productId": 1,
    "fields": [
      {
        "extractedFieldId": 10,
        "fieldName": "mrp",
        "confirmedValue": "185.00",
        "action": "CONFIRM"
      }
    ]
  }
  ```
- **Response**: `200 OK` List of confirmed fields, saved as `PACKAGE` declarations.

---

## 3. Compliance Rules

### `GET /api/rules`
Returns all rules with their active versions.

### `POST /api/rules`
Creates a new compliance rule definition and initial version v1.

### `POST /api/rules/{id}/versions`
Creates a new immutable version for an existing rule.

### `POST /api/rules/{id}/versions/{versionId}/activate`
Activates a version and supersedes the prior active version.

### `GET /api/rules/{id}/versions/compare?version1=1&version2=2`
Returns visual attribute diff and category delta between two versions.

---

## 4. Compliance Evaluations

### `POST /api/evaluations/run`
Executes deterministic rule evaluation for a product.
- **Request Body**:
  ```json
  {
    "productId": 1
  }
  ```
- **Response**: `200 OK` List of `EvaluationDTO` with status, findings, and evidence references.

---

## 5. Revalidation (Research Core)

### `POST /api/revalidation/incremental`
Executes dependency-aware incremental revalidation using Phase A (previously associated) and Phase B (newly applicable).
- **Request Body**:
  ```json
  {
    "ruleId": 1,
    "newVersionId": 2
  }
  ```
- **Response**: `200 OK` `RevalidationRunDTO` with candidate counts, evaluation times, and reuse count.

### `POST /api/revalidation/full`
Executes Full Revalidation baseline across the entire catalogue.

### `GET /api/revalidation/runs`
Lists historical revalidation runs.

### `GET /api/revalidation/runs/{id}/candidates`
Returns candidate products discovered during revalidation with their discovery reason.

---

## 6. Benchmarking & Experiments

### `POST /api/experiments/run`
Executes a comparative benchmark between Full and Incremental Revalidation.
- **Request Body**:
  ```json
  {
    "catalogueSize": 1000,
    "impactRate": 25,
    "scenario": "APPLICABILITY_EXPANSION",
    "randomSeed": 42
  }
  ```
- **Response**: `200 OK` `ExperimentRunDTO` with canonical result agreement %, work reduction %, and time reduction %.

### `GET /api/experiments/{id}/export/csv`
Downloads experiment metrics as a CSV file.

---

## 7. Dashboard & Reports

- `GET /api/dashboard/summary`: High-level counts.
- `GET /api/dashboard/charts`: Category distributions, findings breakdown.
- `GET /api/reports/compliance/csv`: Complete compliance report CSV.
- `GET /api/reports/revalidation/{id}/csv`: Revalidation run CSV.
