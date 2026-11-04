# VeriMetra Database Design

## 1. Schema Overview

The database is built on **PostgreSQL 16** with 16 normalized relational tables, explicit foreign keys, composite unique constraints, and 28 query-optimizing indexes. Schema versioning is managed via Flyway (`V1__initial_schema.sql`).

```
+------------------+         +--------------------------+
|     products     |1 ----- *|   product_declarations   |
+------------------+         +--------------------------+
        | 1                                 |
        |                                   | (Confirmed)
        *                                   v
+------------------+         +--------------------------+
| package_evidence |1 ----- *|       evaluations        |
+------------------+         +--------------------------+
        | 1                     | 1            | 1
        *                       *              *
+------------------+     +------------+ +---------------+
|  ocr_extractions |     | evaluation | |  evaluation   |
+------------------+     |  findings  | |   snapshots   |
        | 1              +------------+ +---------------+
        *                       ^
+------------------+            |
| extracted_fields |            |
+------------------+            |
                                |
+------------------+ 1          |
| compliance_rules |---* +---------------------------+
+------------------+     | compliance_rule_versions  |
                         +---------------------------+
                                | 1             | 1
                                *               *
                         +--------------+ +-----------------------+
                         |     rule     | | product_rule_         |
                         | applicability| | dependencies          |
                         +--------------+ +-----------------------+
```

---

## 2. Table Catalog

### `products`
- `id` (BIGSERIAL PRIMARY KEY)
- `product_name` (VARCHAR(500) NOT NULL)
- `sku` (VARCHAR(100))
- `category` (VARCHAR(50) NOT NULL, CHECK: 'FOOD', 'COSMETICS', 'ELECTRONICS', 'HOUSEHOLD')
- `manufacturer_name` (VARCHAR(500))
- `country_of_origin` (VARCHAR(200))
- `description` (TEXT)
- `created_at`, `updated_at` (TIMESTAMP WITH TIME ZONE)

### `product_declarations`
- `id` (BIGSERIAL PRIMARY KEY)
- `product_id` (BIGINT REFERENCES products)
- `source` (VARCHAR(20), CHECK: 'LISTING', 'PACKAGE')
- `field_name` (VARCHAR(100) NOT NULL)
- `field_value` (VARCHAR(1000))
- `normalized_value` (VARCHAR(1000))
- `normalization_unit` (VARCHAR(50))
- `confirmed` (BOOLEAN DEFAULT FALSE)
- `confirmed_at` (TIMESTAMP WITH TIME ZONE)
- `UNIQUE(product_id, source, field_name)`

### `package_evidence`
- `id` (BIGSERIAL PRIMARY KEY)
- `product_id` (BIGINT REFERENCES products)
- `original_filename` (VARCHAR(500))
- `stored_filename` (VARCHAR(500))
- `file_path` (VARCHAR(1000))
- `content_type` (VARCHAR(100))
- `file_size_bytes` (BIGINT)
- `upload_status` (VARCHAR(20), CHECK: 'UPLOADED', 'PROCESSING', 'EXTRACTED', 'FAILED')

### `ocr_extractions`
- `id` (BIGSERIAL PRIMARY KEY)
- `evidence_id` (BIGINT REFERENCES package_evidence)
- `raw_text` (TEXT)
- `extraction_status` (VARCHAR(30))
- `error_message` (TEXT)
- `processing_time_ms` (BIGINT)

### `extracted_fields`
- `id` (BIGSERIAL PRIMARY KEY)
- `extraction_id` (BIGINT REFERENCES ocr_extractions)
- `field_name` (VARCHAR(100))
- `suggested_value` (VARCHAR(1000))
- `confidence` (DOUBLE PRECISION)
- `review_status` (VARCHAR(20), CHECK: 'PENDING', 'CONFIRMED', 'REJECTED', 'EDITED')
- `confirmed_value` (VARCHAR(1000))
- `reviewed_at` (TIMESTAMP WITH TIME ZONE)

### `compliance_rules`
- `id` (BIGSERIAL PRIMARY KEY)
- `rule_code` (VARCHAR(50) UNIQUE NOT NULL)
- `rule_name` (VARCHAR(500) NOT NULL)
- `description` (TEXT)
- `legal_reference` (VARCHAR(500))
- `source_url` (VARCHAR(1000))
- `source_verified_date` (DATE)
- `is_illustrative` (BOOLEAN DEFAULT FALSE)

### `compliance_rule_versions`
- `id` (BIGSERIAL PRIMARY KEY)
- `rule_id` (BIGINT REFERENCES compliance_rules)
- `version_number` (INTEGER NOT NULL)
- `requirement_type` (VARCHAR(50), CHECK: 'MANDATORY_DECLARATION', 'VALUE_CONSTRAINT', 'FORMAT_REQUIREMENT', 'CROSS_SOURCE_CONSISTENCY')
- `required_field` (VARCHAR(100) NOT NULL)
- `validation_operator` (VARCHAR(30) NOT NULL)
- `expected_value` (VARCHAR(500))
- `severity` (VARCHAR(20), CHECK: 'CRITICAL', 'MAJOR', 'MINOR', 'INFO')
- `effective_from` (DATE NOT NULL)
- `status` (VARCHAR(20), CHECK: 'DRAFT', 'ACTIVE', 'SUPERSEDED', 'DEPRECATED')
- `change_summary` (TEXT)
- `UNIQUE(rule_id, version_number)`

### `rule_applicability`
- `id` (BIGSERIAL PRIMARY KEY)
- `rule_version_id` (BIGINT REFERENCES compliance_rule_versions)
- `category` (VARCHAR(50), CHECK: 'FOOD', 'COSMETICS', 'ELECTRONICS', 'HOUSEHOLD')
- `UNIQUE(rule_version_id, category)`

### `evaluations`
- `id` (BIGSERIAL PRIMARY KEY)
- `product_id` (BIGINT REFERENCES products)
- `rule_version_id` (BIGINT REFERENCES compliance_rule_versions)
- `evaluation_status` (VARCHAR(30), CHECK: 'COMPLIANT', 'NON_COMPLIANT', 'NEEDS_REVIEW', 'NOT_APPLICABLE', 'INSUFFICIENT_DATA')
- `evaluation_type` (VARCHAR(30))
- `revalidation_run_id` (BIGINT)
- `duration_ms` (BIGINT)

### `evaluation_snapshots`
- `id` (BIGSERIAL PRIMARY KEY)
- `evaluation_id` (BIGINT UNIQUE REFERENCES evaluations)
- `product_snapshot` (JSONB NOT NULL)
- `declaration_snapshot` (JSONB NOT NULL)
- `rule_version_snapshot` (JSONB NOT NULL)

### `evaluation_findings`
- `id` (BIGSERIAL PRIMARY KEY)
- `evaluation_id` (BIGINT REFERENCES evaluations)
- `finding_type` (VARCHAR(40), CHECK: 'REGULATORY_REQUIREMENT', 'CROSS_SOURCE_CONSISTENCY')
- `field_name` (VARCHAR(100))
- `expected_value` (VARCHAR(1000))
- `actual_value` (VARCHAR(1000))
- `severity` (VARCHAR(20))
- `explanation` (TEXT NOT NULL)
- `review_status` (VARCHAR(20) DEFAULT 'UNREVIEWED')

### `product_rule_dependencies`
- `id` (BIGSERIAL PRIMARY KEY)
- `product_id` (BIGINT REFERENCES products)
- `rule_id` (BIGINT REFERENCES compliance_rules)
- `rule_version_id` (BIGINT REFERENCES compliance_rule_versions)
- `last_evaluation_id` (BIGINT REFERENCES evaluations)
- `dependency_status` (VARCHAR(20) DEFAULT 'ACTIVE')
- `UNIQUE(product_id, rule_id)`

### `revalidation_runs` & `revalidation_candidates`
- Stores execution type (`FULL`, `INCREMENTAL`), candidate discovery breakdown (Phase A vs Phase B), duration metrics, and candidates table.

### `experiment_runs`
- Stores synthetic benchmark runs, catalogue sizes (100 to 50,000), impact rates (5% to 90%), and canonical result agreement percentages.
