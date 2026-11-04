# VeriMetra

**An Evidence-Based Regulatory Compliance Verification and Incremental Revalidation System for E-Commerce Product Declarations**

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.5-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React-18-blue.svg)](https://react.dev/)
[![TypeScript](https://img.shields.io/badge/TypeScript-5.6-blue.svg)](https://www.typescriptlang.org/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue.svg)](https://www.postgresql.org/)
[![FastAPI](https://img.shields.io/badge/FastAPI-0.115-teal.svg)](https://fastapi.tiangolo.com/)

---

## 1. Project Overview

**VeriMetra** evaluates product declarations published on e-commerce platforms against statutory requirements under India's **Legal Metrology (Packaged Commodities) Rules, 2011**.

The system collects product declaration information, extracts relevant information from package images using local OCR, normalizes extracted values (e.g., `500 g` &rarr; `0.5 kg`, `1000 ml` &rarr; `1.0 L`), evaluates applicable regulatory requirements, checks consistency between online listings and physical packaging, and generates auditable, evidence-backed findings.

### Primary Research Contribution
VeriMetra's central scientific contribution is **dependency-aware incremental revalidation**. When a regulatory rule evolves:
1. **Phase A (Previously Associated Products)**: Identifies products evaluated under prior rule versions via active dependency graphs.
2. **Phase B (Newly Applicable Products)**: Detects existing catalogue products that become newly subject when a rule's category applicability expands (e.g. from `FOOD` only to `FOOD + COSMETICS`).
3. **Safe Reuse & Fallback**: Safely reuses unaffected evaluations while falling back to Full Revalidation if candidate selection completeness cannot be guaranteed.
4. **Canonical Agreement**: Guarantees 100% logical outcome agreement with independent Full Revalidation while drastically reducing computational workload.

---

## 2. Architecture & Technology Stack

- **Backend**: Java 21, Spring Boot 3.3.5, Spring Data JPA, Hibernate, Flyway Migrations, SpringDoc OpenAPI (Swagger), JUnit 5.
- **Frontend**: React 18, TypeScript, Vite, Tailwind CSS, Lucide Icons, Recharts, React Hot Toast, React Router v6.
- **OCR Service**: Python 3.10, FastAPI, PaddleOCR, Pillow, NumPy, Regular Expression Pattern Matcher.
- **Database**: PostgreSQL 16 (16 relational tables with indexes, foreign keys, and JSONB snapshots).
- **Deployment**: Docker & Docker Compose.

---

## 3. Quick Start with Docker Compose

The entire stack (PostgreSQL, OCR service, Spring Boot backend, and React frontend) can be launched with a single command:

```bash
docker compose up --build
```

### Access Endpoints
| Service | URL | Description |
| :--- | :--- | :--- |
| **Frontend Web App** | http://localhost:3000 | Complete Enterprise Compliance SaaS UI |
| **Backend API** | http://localhost:8080/api | Spring Boot RESTful API |
| **Swagger UI** | http://localhost:8080/swagger-ui.html | Interactive OpenAPI 3.0 Documentation |
| **OCR Healthcheck** | http://localhost:8090/health | FastAPI Local OCR Service Status |

---

## 4. Local Development Setup (Without Docker)

### Prerequisites
- JDK 21 (Temurin / OpenJDK)
- Apache Maven 3.9+
- Node.js 20+ & pnpm
- Python 3.9+
- PostgreSQL 15+ running locally on port 5432 with database `verimetra`

### Backend
```bash
cd backend
mvn clean test
mvn spring-boot:run
```

### Frontend
```bash
cd frontend
pnpm install
pnpm run build
pnpm run dev
```

### OCR Service
```bash
cd ocr-service
pip install -r requirements.txt
python main.py
```

---

## 5. Seed Data & Key Entities

On database startup, Flyway automatically executes `V1__initial_schema.sql`, populating:
- **14 Pre-packaged Commodities**: Across `FOOD`, `COSMETICS`, `ELECTRONICS`, and `HOUSEHOLD`.
- **58 Declaration Records**: Both `LISTING` and `PACKAGE` with intentional test mismatches (e.g. MRP discrepancy on hair oil, quantity mismatch on green tea).
- **7 Compliance Rules**:
  - `LM-PC-R6-MRP`: Maximum Retail Price mandatory declaration (Rule 6(1)(d)).
  - `LM-PC-R6-NETQTY`: Net quantity declaration in standard metric units (Rule 6(1)(b)).
  - `LM-PC-R6-MFR`: Manufacturer name and address declaration (Rule 6(1)(a)).
  - `LM-PC-R6-COO`: Country of origin declaration (Rule 6(2)).
  - `LM-PC-R6-NAME`: Commodity generic name declaration (Rule 6(1)).
  - `VM-CROSS-MRP`: Cross-source price consistency analytical check.
  - `VM-CROSS-NETQTY`: Cross-source unit-normalized quantity consistency check.

---

## 6. End-to-End Demonstration Sequence

Follow the 12-step guided demonstration in [docs/DEMONSTRATION_GUIDE.md](docs/DEMONSTRATION_GUIDE.md):
1. **Overview Dashboard**: View seed metrics and charts.
2. **Register Product**: Add a food product (e.g. `Aashirvaad Atta`).
3. **Upload Package Evidence**: Upload an image and run OCR extraction.
4. **Human Review**: Inspect raw OCR text, review provisional fields, and confirm declarations.
5. **Evaluate Compliance**: Run deterministic checks and view `COMPLIANT` / `NON_COMPLIANT` findings.
6. **Inspect Rule v1**: Examine rule `LM-PC-R6-MRP` applicable only to `FOOD`.
7. **Create Rule v2**: Expand applicability to `FOOD + COSMETICS` and activate it.
8. **Incremental Revalidation**: Observe **Phase A** (FOOD products) and **Phase B** (newly applicable COSMETICS products) discovered and re-evaluated with unaffected products reused.
9. **Full Revalidation**: Run full baseline scan and compare execution metrics.
10. **Compare Results**: Verify 100% logical agreement between incremental and full strategies.
11. **Benchmarking**: Run synthetic experiment ($N=1,000$, $\alpha=25\%$) and inspect work reduction.
12. **Export Reports**: Download audit-ready CSV reports.

---

## 7. Documentation Index

- [Architecture Design](docs/ARCHITECTURE.md)
- [Database Schema Design](docs/DATABASE_DESIGN.md)
- [Reference Project & Academic Study](docs/REFERENCE_STUDY.md)
- [Revalidation Algorithm & Mathematical Formulation](docs/REVALIDATION_ALGORITHM.md)
- [Legal Rule Sources & Statutory Citations](docs/LEGAL_RULE_SOURCES.md)
- [REST API Documentation](docs/API_DOCUMENTATION.md)
- [Experiment & Benchmarking Guide](docs/EXPERIMENT_GUIDE.md)
- [Demonstration Guide](docs/DEMONSTRATION_GUIDE.md)
- [Testing Guide](docs/TESTING_GUIDE.md)
- [Project Verification Status](docs/PROJECT_STATUS.md)

---

## 8. Disclaimer
VeriMetra is an academic research and decision-support prototype. It does not issue official legal certifications or replace statutory inspectors under the Legal Metrology Act, 2009.
