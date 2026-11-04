# VeriMetra Architecture & Technical Design

## 1. System Overview

**VeriMetra** is an evidence-based regulatory compliance verification and incremental revalidation platform for e-commerce product declarations under India's Legal Metrology (Packaged Commodities) Rules, 2011.

The system is designed as a **clean modular monolith** on the backend with an independent local OCR microservice and a modern React/TypeScript single-page application on the frontend.

```
+-------------------------------------------------------------------------+
|                        Frontend (React 18 + TS)                         |
|  - Overview Dashboard   - Product Management   - Package Evidence & OCR |
|  - Rule Management      - Compliance Eval      - Incremental Revalidation|
|  - Benchmarking Lab     - Audit Reports Center                          |
+------------------------------------+------------------------------------+
                                     | HTTP REST
                                     v
+-------------------------------------------------------------------------+
|                      Backend (Spring Boot 3 + Java 21)                  |
|  +--------------------+  +---------------------+  +-------------------+ |
|  |   Product Module   |  |   Evidence Module   |  |   Rule Module     | |
|  +--------------------+  +---------------------+  +-------------------+ |
|  +--------------------+  +---------------------+  +-------------------+ |
|  | Normalization Svc  |  | Deterministic Engine|  | Dependency Graph  | |
|  +--------------------+  +---------------------+  +-------------------+ |
|  +---------------------------------------------+  +-------------------+ |
|  | Incremental Revalidation Service (Phase A+B)|  | Benchmarking Lab  | |
|  +---------------------------------------------+  +-------------------+ |
+------------------+------------------------------------+-----------------+
                   |                                    |
          Internal HTTP /extract               JDBC PostgreSQL 16
                   v                                    v
+-----------------------------------+   +---------------------------------+
|       OCR Service (FastAPI)       |   |       PostgreSQL Database       |
|  - PaddleOCR Engine               |   |  - 16 Relational Tables         |
|  - Regex Field Matcher            |   |  - Flyway Versioned Migrations  |
|  - Image Format Validation        |   |  - Foreign Keys & Indexes       |
+-----------------------------------+   +---------------------------------+
```

---

## 2. Module Boundaries & Responsibilities

| Package | Responsibility |
| :--- | :--- |
| `com.verimetra.product` | Product catalogue CRUD, listing declaration management, category filtering (FOOD, COSMETICS, ELECTRONICS, HOUSEHOLD). |
| `com.verimetra.evidence` | Image upload, secure file storage with path traversal protection, OCR service dispatch, human-in-the-loop extraction review. |
| `com.verimetra.normalization` | Deterministic unit normalization (grams to kg, ml to L, currency symbol stripping, case-folding). |
| `com.verimetra.rule` | Rule repository, immutable rule versioning, category applicability, version comparison diff. |
| `com.verimetra.evaluation` | Core `DeterministicRuleEvaluator`, evaluation persistence, frozen evaluation snapshots (`JSONB`), findings generation. |
| `com.verimetra.revalidation` | Research Core: Phase A (previously associated) & Phase B (newly applicable) candidate discovery, safe fallback, run metrics. |
| `com.verimetra.experiment` | Benchmarking lab: synthetic catalogue generator (N=100..50000), canonical correctness agreement comparison, export. |
| `com.verimetra.dashboard` | Real database metric aggregation (KPIs, category distributions, severity breakdown). |
| `com.verimetra.report` | Audit-ready CSV export of compliance records, revalidation runs, and benchmark outcomes. |

---

## 3. Communication Patterns

1. **Frontend to Backend**: RESTful JSON endpoints over HTTP/HTTPS with CORS configuration for standard development and production ports.
2. **Backend to OCR Service**: Multipart form data POST to `/extract`. The backend reads uploaded package bytes and receives raw OCR text + pattern-matched field suggestions.
3. **Backend to Database**: Spring Data JPA with HikariCP connection pool, Flyway migration runner, and Hibernate validating against schema V1.
4. **Data Isolation**: Raw OCR is preserved as provisional in `ocr_extractions` and `extracted_fields`. It only transitions to `product_declarations` (source: `PACKAGE`) when explicitly reviewed and confirmed by a human operator.
