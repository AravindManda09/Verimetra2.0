-- VeriMetra Database Schema v1.0
-- Evidence-Based Regulatory Compliance Verification System

-- ============================================
-- PRODUCTS
-- ============================================
CREATE TABLE products (
    id BIGSERIAL PRIMARY KEY,
    product_name VARCHAR(500) NOT NULL,
    sku VARCHAR(100),
    category VARCHAR(50) NOT NULL,
    manufacturer_name VARCHAR(500),
    country_of_origin VARCHAR(200),
    description TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_product_category CHECK (category IN ('FOOD', 'COSMETICS', 'ELECTRONICS', 'HOUSEHOLD'))
);

CREATE INDEX idx_products_category ON products(category);
CREATE INDEX idx_products_name ON products(product_name);
CREATE INDEX idx_products_sku ON products(sku);

-- ============================================
-- PRODUCT DECLARATIONS (listing vs package)
-- ============================================
CREATE TABLE product_declarations (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    source VARCHAR(20) NOT NULL, -- LISTING or PACKAGE
    field_name VARCHAR(100) NOT NULL,
    field_value VARCHAR(1000),
    normalized_value VARCHAR(1000),
    normalization_unit VARCHAR(50),
    confirmed BOOLEAN NOT NULL DEFAULT FALSE,
    confirmed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_declaration_source CHECK (source IN ('LISTING', 'PACKAGE')),
    CONSTRAINT uq_product_declaration UNIQUE (product_id, source, field_name)
);

CREATE INDEX idx_declarations_product ON product_declarations(product_id);
CREATE INDEX idx_declarations_field ON product_declarations(field_name);

-- ============================================
-- PACKAGE EVIDENCE (uploaded images)
-- ============================================
CREATE TABLE package_evidence (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    original_filename VARCHAR(500) NOT NULL,
    stored_filename VARCHAR(500) NOT NULL,
    file_path VARCHAR(1000) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    file_size_bytes BIGINT NOT NULL,
    upload_status VARCHAR(20) NOT NULL DEFAULT 'UPLOADED',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_upload_status CHECK (upload_status IN ('UPLOADED', 'PROCESSING', 'EXTRACTED', 'FAILED'))
);

CREATE INDEX idx_evidence_product ON package_evidence(product_id);

-- ============================================
-- OCR EXTRACTIONS
-- ============================================
CREATE TABLE ocr_extractions (
    id BIGSERIAL PRIMARY KEY,
    evidence_id BIGINT NOT NULL REFERENCES package_evidence(id) ON DELETE CASCADE,
    raw_text TEXT,
    extraction_status VARCHAR(30) NOT NULL,
    error_message TEXT,
    processing_time_ms BIGINT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_extraction_status CHECK (extraction_status IN ('SUCCESS', 'NO_TEXT_FOUND', 'EXTRACTION_FAILED', 'OCR_UNAVAILABLE', 'PENDING'))
);

CREATE INDEX idx_extractions_evidence ON ocr_extractions(evidence_id);

-- ============================================
-- EXTRACTED FIELDS (from OCR)
-- ============================================
CREATE TABLE extracted_fields (
    id BIGSERIAL PRIMARY KEY,
    extraction_id BIGINT NOT NULL REFERENCES ocr_extractions(id) ON DELETE CASCADE,
    field_name VARCHAR(100) NOT NULL,
    suggested_value VARCHAR(1000),
    confidence DOUBLE PRECISION,
    review_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    confirmed_value VARCHAR(1000),
    reviewed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_review_status CHECK (review_status IN ('PENDING', 'CONFIRMED', 'REJECTED', 'EDITED'))
);

CREATE INDEX idx_extracted_fields_extraction ON extracted_fields(extraction_id);

-- ============================================
-- COMPLIANCE RULES
-- ============================================
CREATE TABLE compliance_rules (
    id BIGSERIAL PRIMARY KEY,
    rule_code VARCHAR(50) NOT NULL UNIQUE,
    rule_name VARCHAR(500) NOT NULL,
    description TEXT,
    legal_reference VARCHAR(500),
    source_url VARCHAR(1000),
    source_verified_date DATE,
    is_illustrative BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_rules_code ON compliance_rules(rule_code);

-- ============================================
-- COMPLIANCE RULE VERSIONS (immutable)
-- ============================================
CREATE TABLE compliance_rule_versions (
    id BIGSERIAL PRIMARY KEY,
    rule_id BIGINT NOT NULL REFERENCES compliance_rules(id) ON DELETE CASCADE,
    version_number INTEGER NOT NULL,
    requirement_type VARCHAR(50) NOT NULL,
    required_field VARCHAR(100) NOT NULL,
    validation_operator VARCHAR(30) NOT NULL,
    expected_value VARCHAR(500),
    severity VARCHAR(20) NOT NULL DEFAULT 'MAJOR',
    effective_from DATE NOT NULL,
    effective_to DATE,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    change_summary TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_requirement_type CHECK (requirement_type IN ('MANDATORY_DECLARATION', 'VALUE_CONSTRAINT', 'FORMAT_REQUIREMENT', 'CROSS_SOURCE_CONSISTENCY')),
    CONSTRAINT chk_validation_operator CHECK (validation_operator IN ('EXISTS', 'NOT_EMPTY', 'EQUALS', 'NOT_EQUALS', 'GREATER_THAN', 'LESS_THAN', 'GREATER_EQUAL', 'LESS_EQUAL', 'CONTAINS', 'MATCHES_REGEX', 'IN_LIST', 'CROSS_SOURCE_MATCH')),
    CONSTRAINT chk_severity CHECK (severity IN ('CRITICAL', 'MAJOR', 'MINOR', 'INFO')),
    CONSTRAINT chk_version_status CHECK (status IN ('DRAFT', 'ACTIVE', 'SUPERSEDED', 'DEPRECATED')),
    CONSTRAINT uq_rule_version UNIQUE (rule_id, version_number)
);

CREATE INDEX idx_rule_versions_rule ON compliance_rule_versions(rule_id);
CREATE INDEX idx_rule_versions_status ON compliance_rule_versions(status);

-- ============================================
-- RULE APPLICABILITY (per version)
-- ============================================
CREATE TABLE rule_applicability (
    id BIGSERIAL PRIMARY KEY,
    rule_version_id BIGINT NOT NULL REFERENCES compliance_rule_versions(id) ON DELETE CASCADE,
    category VARCHAR(50) NOT NULL,
    CONSTRAINT chk_applicability_category CHECK (category IN ('FOOD', 'COSMETICS', 'ELECTRONICS', 'HOUSEHOLD')),
    CONSTRAINT uq_rule_applicability UNIQUE (rule_version_id, category)
);

CREATE INDEX idx_rule_applicability_version ON rule_applicability(rule_version_id);
CREATE INDEX idx_rule_applicability_category ON rule_applicability(category);

-- ============================================
-- EVALUATIONS
-- ============================================
CREATE TABLE evaluations (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    rule_version_id BIGINT NOT NULL REFERENCES compliance_rule_versions(id),
    evaluation_status VARCHAR(30) NOT NULL,
    evaluation_type VARCHAR(30) NOT NULL DEFAULT 'MANUAL',
    triggered_by VARCHAR(50),
    revalidation_run_id BIGINT,
    evaluated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    duration_ms BIGINT,
    CONSTRAINT chk_eval_status CHECK (evaluation_status IN ('COMPLIANT', 'NON_COMPLIANT', 'NEEDS_REVIEW', 'NOT_APPLICABLE', 'INSUFFICIENT_DATA')),
    CONSTRAINT chk_eval_type CHECK (evaluation_type IN ('MANUAL', 'FULL_REVALIDATION', 'INCREMENTAL_REVALIDATION', 'EXPERIMENT'))
);

CREATE INDEX idx_evaluations_product ON evaluations(product_id);
CREATE INDEX idx_evaluations_rule_version ON evaluations(rule_version_id);
CREATE INDEX idx_evaluations_status ON evaluations(evaluation_status);
CREATE INDEX idx_evaluations_revalidation ON evaluations(revalidation_run_id);

-- ============================================
-- EVALUATION SNAPSHOTS (frozen data at eval time)
-- ============================================
CREATE TABLE evaluation_snapshots (
    id BIGSERIAL PRIMARY KEY,
    evaluation_id BIGINT NOT NULL REFERENCES evaluations(id) ON DELETE CASCADE,
    product_snapshot JSONB NOT NULL,
    declaration_snapshot JSONB NOT NULL,
    rule_version_snapshot JSONB NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_eval_snapshot UNIQUE (evaluation_id)
);

-- ============================================
-- EVALUATION FINDINGS
-- ============================================
CREATE TABLE evaluation_findings (
    id BIGSERIAL PRIMARY KEY,
    evaluation_id BIGINT NOT NULL REFERENCES evaluations(id) ON DELETE CASCADE,
    finding_type VARCHAR(40) NOT NULL,
    field_name VARCHAR(100) NOT NULL,
    expected_value VARCHAR(1000),
    actual_value VARCHAR(1000),
    severity VARCHAR(20) NOT NULL,
    explanation TEXT NOT NULL,
    rule_reference VARCHAR(500),
    rule_version_id BIGINT REFERENCES compliance_rule_versions(id),
    evidence_id BIGINT REFERENCES package_evidence(id),
    review_status VARCHAR(20) NOT NULL DEFAULT 'UNREVIEWED',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_finding_type CHECK (finding_type IN ('REGULATORY_REQUIREMENT', 'CROSS_SOURCE_CONSISTENCY')),
    CONSTRAINT chk_finding_severity CHECK (severity IN ('CRITICAL', 'MAJOR', 'MINOR', 'INFO')),
    CONSTRAINT chk_finding_review CHECK (review_status IN ('UNREVIEWED', 'ACKNOWLEDGED', 'DISPUTED', 'RESOLVED'))
);

CREATE INDEX idx_findings_evaluation ON evaluation_findings(evaluation_id);
CREATE INDEX idx_findings_type ON evaluation_findings(finding_type);
CREATE INDEX idx_findings_severity ON evaluation_findings(severity);

-- ============================================
-- PRODUCT-RULE DEPENDENCIES (for incremental revalidation)
-- ============================================
CREATE TABLE product_rule_dependencies (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    rule_id BIGINT NOT NULL REFERENCES compliance_rules(id) ON DELETE CASCADE,
    rule_version_id BIGINT NOT NULL REFERENCES compliance_rule_versions(id),
    last_evaluation_id BIGINT REFERENCES evaluations(id),
    dependency_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_dep_status CHECK (dependency_status IN ('ACTIVE', 'STALE', 'REMOVED')),
    CONSTRAINT uq_product_rule_dep UNIQUE (product_id, rule_id)
);

CREATE INDEX idx_dependencies_product ON product_rule_dependencies(product_id);
CREATE INDEX idx_dependencies_rule ON product_rule_dependencies(rule_id);
CREATE INDEX idx_dependencies_rule_version ON product_rule_dependencies(rule_version_id);

-- ============================================
-- REVALIDATION RUNS
-- ============================================
CREATE TABLE revalidation_runs (
    id BIGSERIAL PRIMARY KEY,
    run_type VARCHAR(30) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'RUNNING',
    trigger_rule_id BIGINT REFERENCES compliance_rules(id),
    previous_version_id BIGINT REFERENCES compliance_rule_versions(id),
    new_version_id BIGINT REFERENCES compliance_rule_versions(id),
    changed_attributes TEXT,
    total_products_considered INTEGER NOT NULL DEFAULT 0,
    previously_associated_count INTEGER NOT NULL DEFAULT 0,
    newly_applicable_count INTEGER NOT NULL DEFAULT 0,
    candidate_count INTEGER NOT NULL DEFAULT 0,
    evaluations_performed INTEGER NOT NULL DEFAULT 0,
    evaluations_reused INTEGER NOT NULL DEFAULT 0,
    findings_generated INTEGER NOT NULL DEFAULT 0,
    candidate_selection_ms BIGINT,
    evaluation_ms BIGINT,
    persistence_ms BIGINT,
    total_duration_ms BIGINT,
    used_fallback BOOLEAN NOT NULL DEFAULT FALSE,
    fallback_reason TEXT,
    error_message TEXT,
    started_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    completed_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT chk_run_type CHECK (run_type IN ('FULL', 'INCREMENTAL')),
    CONSTRAINT chk_run_status CHECK (status IN ('RUNNING', 'COMPLETED', 'FAILED', 'FALLBACK'))
);

CREATE INDEX idx_revalidation_type ON revalidation_runs(run_type);
CREATE INDEX idx_revalidation_status ON revalidation_runs(status);

-- ============================================
-- REVALIDATION CANDIDATES
-- ============================================
CREATE TABLE revalidation_candidates (
    id BIGSERIAL PRIMARY KEY,
    revalidation_run_id BIGINT NOT NULL REFERENCES revalidation_runs(id) ON DELETE CASCADE,
    product_id BIGINT NOT NULL REFERENCES products(id),
    candidate_reason VARCHAR(50) NOT NULL,
    was_evaluated BOOLEAN NOT NULL DEFAULT FALSE,
    was_reused BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT chk_candidate_reason CHECK (candidate_reason IN ('PREVIOUSLY_ASSOCIATED', 'NEWLY_APPLICABLE', 'DECLARATION_CHANGED', 'FULL_SCAN'))
);

CREATE INDEX idx_candidates_run ON revalidation_candidates(revalidation_run_id);

-- ============================================
-- EXPERIMENT RUNS
-- ============================================
CREATE TABLE experiment_runs (
    id BIGSERIAL PRIMARY KEY,
    experiment_name VARCHAR(200),
    catalogue_size INTEGER NOT NULL,
    impact_rate DOUBLE PRECISION NOT NULL,
    scenario VARCHAR(50) NOT NULL,
    random_seed BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'RUNNING',
    
    -- Full revalidation metrics
    full_total_products INTEGER,
    full_applicable_pairs INTEGER,
    full_evaluations INTEGER,
    full_findings INTEGER,
    full_duration_ms BIGINT,
    
    -- Incremental revalidation metrics
    incr_candidates INTEGER,
    incr_previously_associated INTEGER,
    incr_newly_applicable INTEGER,
    incr_evaluations INTEGER,
    incr_reused INTEGER,
    incr_findings INTEGER,
    incr_candidate_selection_ms BIGINT,
    incr_evaluation_ms BIGINT,
    incr_persistence_ms BIGINT,
    incr_total_duration_ms BIGINT,
    
    -- Comparison metrics
    evaluation_reduction_pct DOUBLE PRECISION,
    time_reduction_pct DOUBLE PRECISION,
    result_agreement_pct DOUBLE PRECISION,
    missed_affected_count INTEGER,
    unnecessary_reevals INTEGER,
    matching_outcomes INTEGER,
    differing_outcomes INTEGER,
    missing_outcomes INTEGER,
    extra_outcomes INTEGER,
    
    correctness_status VARCHAR(30),
    error_message TEXT,
    started_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    completed_at TIMESTAMP WITH TIME ZONE,
    
    CONSTRAINT chk_experiment_scenario CHECK (scenario IN ('APPLICABILITY_EXPANSION', 'APPLICABILITY_NARROWING', 'REQUIREMENT_MODIFICATION', 'THRESHOLD_MODIFICATION', 'MIXED')),
    CONSTRAINT chk_experiment_status CHECK (status IN ('RUNNING', 'COMPLETED', 'FAILED'))
);

-- ============================================
-- SEED DATA: Sample Products
-- ============================================
INSERT INTO products (product_name, sku, category, manufacturer_name, country_of_origin, description) VALUES
('Organic Wheat Flour', 'FOOD-001', 'FOOD', 'Nature Fresh Foods Pvt Ltd', 'India', 'Premium organic whole wheat flour, stone ground'),
('Basmati Rice Premium', 'FOOD-002', 'FOOD', 'India Gate Foods', 'India', 'Aged basmati rice, extra long grain'),
('Toor Dal', 'FOOD-003', 'FOOD', 'Tata Sampann', 'India', 'Unpolished toor dal, rich in protein'),
('Mixed Fruit Jam', 'FOOD-004', 'FOOD', 'Kissan Foods', 'India', 'Mixed fruit jam with real fruit pieces'),
('Green Tea Leaves', 'FOOD-005', 'FOOD', 'Organic India', 'India', 'Premium green tea from Darjeeling gardens'),
('Aloe Vera Face Wash', 'COSM-001', 'COSMETICS', 'Himalaya Wellness', 'India', 'Gentle daily face wash with natural aloe vera'),
('Coconut Hair Oil', 'COSM-002', 'COSMETICS', 'Parachute Advanced', 'India', 'Pure coconut oil for hair care'),
('Herbal Shampoo', 'COSM-003', 'COSMETICS', 'Biotique Bio', 'India', 'Herbal shampoo with green apple extract'),
('LED Desk Lamp', 'ELEC-001', 'ELECTRONICS', 'Philips India Ltd', 'China', '10W LED desk lamp with adjustable brightness'),
('USB-C Charger', 'ELEC-002', 'ELECTRONICS', 'Samsung Electronics', 'Vietnam', '25W USB-C fast charger with PPS support'),
('Bluetooth Speaker', 'ELEC-003', 'ELECTRONICS', 'JBL India', 'China', 'Portable waterproof bluetooth speaker'),
('All-Purpose Cleaner', 'HOUS-001', 'HOUSEHOLD', 'Lizol India', 'India', 'Disinfectant surface cleaner, citrus fresh'),
('Liquid Detergent', 'HOUS-002', 'HOUSEHOLD', 'Surf Excel', 'India', 'Matic liquid detergent for front load washing machines'),
('Air Freshener', 'HOUS-003', 'HOUSEHOLD', 'Godrej Aer', 'India', 'Room air freshener spray, lavender bloom');

-- Listing declarations for products
INSERT INTO product_declarations (product_id, source, field_name, field_value, normalized_value, normalization_unit, confirmed) VALUES
-- Organic Wheat Flour (FOOD-001)
(1, 'LISTING', 'product_name', 'Organic Wheat Flour', 'organic wheat flour', NULL, true),
(1, 'LISTING', 'mrp', '185.00', '185.00', 'INR', true),
(1, 'LISTING', 'net_quantity', '5 kg', '5.0', 'kg', true),
(1, 'LISTING', 'manufacturer_name', 'Nature Fresh Foods Pvt Ltd', 'nature fresh foods pvt ltd', NULL, true),
(1, 'LISTING', 'country_of_origin', 'India', 'india', NULL, true),
-- Basmati Rice (FOOD-002)
(2, 'LISTING', 'product_name', 'Basmati Rice Premium', 'basmati rice premium', NULL, true),
(2, 'LISTING', 'mrp', '450.00', '450.00', 'INR', true),
(2, 'LISTING', 'net_quantity', '1 kg', '1.0', 'kg', true),
(2, 'LISTING', 'manufacturer_name', 'India Gate Foods', 'india gate foods', NULL, true),
(2, 'LISTING', 'country_of_origin', 'India', 'india', NULL, true),
-- Toor Dal (FOOD-003)
(3, 'LISTING', 'product_name', 'Toor Dal', 'toor dal', NULL, true),
(3, 'LISTING', 'mrp', '160.00', '160.00', 'INR', true),
(3, 'LISTING', 'net_quantity', '500 g', '0.5', 'kg', true),
(3, 'LISTING', 'manufacturer_name', 'Tata Sampann', 'tata sampann', NULL, true),
(3, 'LISTING', 'country_of_origin', 'India', 'india', NULL, true),
-- Mixed Fruit Jam (FOOD-004) — missing MRP intentionally
(4, 'LISTING', 'product_name', 'Mixed Fruit Jam', 'mixed fruit jam', NULL, true),
(4, 'LISTING', 'net_quantity', '200 g', '0.2', 'kg', true),
(4, 'LISTING', 'manufacturer_name', 'Kissan Foods', 'kissan foods', NULL, true),
(4, 'LISTING', 'country_of_origin', 'India', 'india', NULL, true),
-- Green Tea (FOOD-005) — mismatched quantity between listing and package
(5, 'LISTING', 'product_name', 'Green Tea Leaves', 'green tea leaves', NULL, true),
(5, 'LISTING', 'mrp', '350.00', '350.00', 'INR', true),
(5, 'LISTING', 'net_quantity', '100 g', '0.1', 'kg', true),
(5, 'LISTING', 'manufacturer_name', 'Organic India', 'organic india', NULL, true),
(5, 'LISTING', 'country_of_origin', 'India', 'india', NULL, true),
-- Aloe Vera Face Wash (COSM-001)
(6, 'LISTING', 'product_name', 'Aloe Vera Face Wash', 'aloe vera face wash', NULL, true),
(6, 'LISTING', 'mrp', '199.00', '199.00', 'INR', true),
(6, 'LISTING', 'net_quantity', '150 ml', '0.15', 'L', true),
(6, 'LISTING', 'manufacturer_name', 'Himalaya Wellness', 'himalaya wellness', NULL, true),
(6, 'LISTING', 'country_of_origin', 'India', 'india', NULL, true),
-- Coconut Hair Oil (COSM-002) — mismatched MRP
(7, 'LISTING', 'product_name', 'Coconut Hair Oil', 'coconut hair oil', NULL, true),
(7, 'LISTING', 'mrp', '125.00', '125.00', 'INR', true),
(7, 'LISTING', 'net_quantity', '200 ml', '0.2', 'L', true),
(7, 'LISTING', 'manufacturer_name', 'Parachute Advanced', 'parachute advanced', NULL, true),
(7, 'LISTING', 'country_of_origin', 'India', 'india', NULL, true),
-- Herbal Shampoo (COSM-003) — no manufacturer listed
(8, 'LISTING', 'product_name', 'Herbal Shampoo', 'herbal shampoo', NULL, true),
(8, 'LISTING', 'mrp', '245.00', '245.00', 'INR', true),
(8, 'LISTING', 'net_quantity', '340 ml', '0.34', 'L', true),
(8, 'LISTING', 'country_of_origin', 'India', 'india', NULL, true),
-- LED Desk Lamp (ELEC-001)
(9, 'LISTING', 'product_name', 'LED Desk Lamp', 'led desk lamp', NULL, true),
(9, 'LISTING', 'mrp', '1299.00', '1299.00', 'INR', true),
(9, 'LISTING', 'manufacturer_name', 'Philips India Ltd', 'philips india ltd', NULL, true),
(9, 'LISTING', 'country_of_origin', 'China', 'china', NULL, true),
-- USB-C Charger (ELEC-002)
(10, 'LISTING', 'product_name', 'USB-C Charger', 'usb-c charger', NULL, true),
(10, 'LISTING', 'mrp', '1499.00', '1499.00', 'INR', true),
(10, 'LISTING', 'manufacturer_name', 'Samsung Electronics', 'samsung electronics', NULL, true),
(10, 'LISTING', 'country_of_origin', 'Vietnam', 'vietnam', NULL, true),
-- All-Purpose Cleaner (HOUS-001)
(12, 'LISTING', 'product_name', 'All-Purpose Cleaner', 'all-purpose cleaner', NULL, true),
(12, 'LISTING', 'mrp', '159.00', '159.00', 'INR', true),
(12, 'LISTING', 'net_quantity', '975 ml', '0.975', 'L', true),
(12, 'LISTING', 'manufacturer_name', 'Lizol India', 'lizol india', NULL, true),
(12, 'LISTING', 'country_of_origin', 'India', 'india', NULL, true),
-- Liquid Detergent (HOUS-002)
(13, 'LISTING', 'product_name', 'Liquid Detergent', 'liquid detergent', NULL, true),
(13, 'LISTING', 'mrp', '399.00', '399.00', 'INR', true),
(13, 'LISTING', 'net_quantity', '1 L', '1.0', 'L', true),
(13, 'LISTING', 'manufacturer_name', 'Surf Excel', 'surf excel', NULL, true),
(13, 'LISTING', 'country_of_origin', 'India', 'india', NULL, true);

-- Package declarations (some matching, some different for cross-source testing)
INSERT INTO product_declarations (product_id, source, field_name, field_value, normalized_value, normalization_unit, confirmed) VALUES
-- Organic Wheat Flour (FOOD-001) package — matches listing
(1, 'PACKAGE', 'product_name', 'Organic Wheat Flour', 'organic wheat flour', NULL, true),
(1, 'PACKAGE', 'mrp', '185.00', '185.00', 'INR', true),
(1, 'PACKAGE', 'net_quantity', '5 kg', '5.0', 'kg', true),
(1, 'PACKAGE', 'manufacturer_name', 'Nature Fresh Foods Pvt Ltd', 'nature fresh foods pvt ltd', NULL, true),
(1, 'PACKAGE', 'country_of_origin', 'India', 'india', NULL, true),
-- Green Tea (FOOD-005) package — quantity mismatch (75g vs 100g listing)
(5, 'PACKAGE', 'product_name', 'Green Tea Leaves', 'green tea leaves', NULL, true),
(5, 'PACKAGE', 'mrp', '350.00', '350.00', 'INR', true),
(5, 'PACKAGE', 'net_quantity', '75 g', '0.075', 'kg', true),
(5, 'PACKAGE', 'manufacturer_name', 'Organic India', 'organic india', NULL, true),
(5, 'PACKAGE', 'country_of_origin', 'India', 'india', NULL, true),
-- Coconut Hair Oil (COSM-002) package — MRP mismatch (115 vs 125 listing)
(7, 'PACKAGE', 'product_name', 'Coconut Hair Oil', 'coconut hair oil', NULL, true),
(7, 'PACKAGE', 'mrp', '115.00', '115.00', 'INR', true),
(7, 'PACKAGE', 'net_quantity', '200 ml', '0.2', 'L', true),
(7, 'PACKAGE', 'manufacturer_name', 'Parachute Advanced', 'parachute advanced', NULL, true),
(7, 'PACKAGE', 'country_of_origin', 'India', 'india', NULL, true);

-- ============================================
-- SEED DATA: Compliance Rules
-- ============================================

-- Rule 1: MRP Declaration Required
INSERT INTO compliance_rules (id, rule_code, rule_name, description, legal_reference, source_url, source_verified_date, is_illustrative) VALUES
(1, 'LM-PC-R6-MRP', 'MRP Declaration Required', 
 'Every package of a commodity intended for retail sale must bear the maximum retail price (inclusive of all taxes). This is a mandatory declaration under Legal Metrology rules.',
 'Legal Metrology (Packaged Commodities) Rules, 2011 - Rule 6(1)(d)',
 'https://consumeraffairs.nic.in/acts-and-rules/legal-metrology',
 '2024-01-15', false);

-- Rule 2: Net Quantity Declaration Required
INSERT INTO compliance_rules (id, rule_code, rule_name, description, legal_reference, source_url, source_verified_date, is_illustrative) VALUES
(2, 'LM-PC-R6-NETQTY', 'Net Quantity Declaration Required',
 'Every package must bear the net quantity of the commodity in standard units of weight or measure.',
 'Legal Metrology (Packaged Commodities) Rules, 2011 - Rule 6(1)(b)',
 'https://consumeraffairs.nic.in/acts-and-rules/legal-metrology',
 '2024-01-15', false);

-- Rule 3: Manufacturer Name Required
INSERT INTO compliance_rules (id, rule_code, rule_name, description, legal_reference, source_url, source_verified_date, is_illustrative) VALUES
(3, 'LM-PC-R6-MFR', 'Manufacturer Name Required',
 'The name and complete address of the manufacturer or packer or importer must be declared on the package.',
 'Legal Metrology (Packaged Commodities) Rules, 2011 - Rule 6(1)(a)',
 'https://consumeraffairs.nic.in/acts-and-rules/legal-metrology',
 '2024-01-15', false);

-- Rule 4: Country of Origin Required
INSERT INTO compliance_rules (id, rule_code, rule_name, description, legal_reference, source_url, source_verified_date, is_illustrative) VALUES
(4, 'LM-PC-R6-COO', 'Country of Origin Declaration Required',
 'For imported packages, the country of origin must be declared.',
 'Legal Metrology (Packaged Commodities) Rules, 2011 - Rule 6(2)',
 'https://consumeraffairs.nic.in/acts-and-rules/legal-metrology',
 '2024-01-15', false);

-- Rule 5: Product Name Required
INSERT INTO compliance_rules (id, rule_code, rule_name, description, legal_reference, source_url, source_verified_date, is_illustrative) VALUES
(5, 'LM-PC-R6-NAME', 'Product Name Declaration Required',
 'The name or description of the commodity contained in the package must be declared.',
 'Legal Metrology (Packaged Commodities) Rules, 2011 - Rule 6(1)',
 'https://consumeraffairs.nic.in/acts-and-rules/legal-metrology',
 '2024-01-15', false);

-- Rule 6: MRP Cross-Source Consistency
INSERT INTO compliance_rules (id, rule_code, rule_name, description, legal_reference, is_illustrative) VALUES
(6, 'VM-CROSS-MRP', 'MRP Cross-Source Consistency Check',
 'Checks whether the MRP declared on the e-commerce listing matches the MRP on the physical package. A mismatch may indicate a discrepancy requiring review. This is a VeriMetra analytical check, not a direct legal requirement.',
 'VeriMetra Cross-Source Analysis', true);

-- Rule 7: Net Quantity Cross-Source Consistency
INSERT INTO compliance_rules (id, rule_code, rule_name, description, legal_reference, is_illustrative) VALUES
(7, 'VM-CROSS-NETQTY', 'Net Quantity Cross-Source Consistency Check',
 'Checks whether the net quantity declared on the e-commerce listing matches the net quantity on the physical package.',
 'VeriMetra Cross-Source Analysis', true);

-- Rule Versions (version 1 for all rules — applicability: FOOD only for first demonstration)

-- MRP Required v1 — applies to FOOD
INSERT INTO compliance_rule_versions (id, rule_id, version_number, requirement_type, required_field, validation_operator, expected_value, severity, effective_from, status, change_summary) VALUES
(1, 1, 1, 'MANDATORY_DECLARATION', 'mrp', 'NOT_EMPTY', NULL, 'CRITICAL', '2024-01-01', 'ACTIVE', 'Initial version: MRP declaration mandatory for food products');

-- Net Quantity Required v1 — applies to FOOD
INSERT INTO compliance_rule_versions (id, rule_id, version_number, requirement_type, required_field, validation_operator, expected_value, severity, effective_from, status, change_summary) VALUES
(2, 2, 1, 'MANDATORY_DECLARATION', 'net_quantity', 'NOT_EMPTY', NULL, 'CRITICAL', '2024-01-01', 'ACTIVE', 'Initial version: Net quantity declaration mandatory for food products');

-- Manufacturer Required v1 — applies to FOOD
INSERT INTO compliance_rule_versions (id, rule_id, version_number, requirement_type, required_field, validation_operator, expected_value, severity, effective_from, status, change_summary) VALUES
(3, 3, 1, 'MANDATORY_DECLARATION', 'manufacturer_name', 'NOT_EMPTY', NULL, 'MAJOR', '2024-01-01', 'ACTIVE', 'Initial version: Manufacturer name mandatory for food products');

-- Country of Origin v1 — applies to all categories
INSERT INTO compliance_rule_versions (id, rule_id, version_number, requirement_type, required_field, validation_operator, expected_value, severity, effective_from, status, change_summary) VALUES
(4, 4, 1, 'MANDATORY_DECLARATION', 'country_of_origin', 'NOT_EMPTY', NULL, 'MAJOR', '2024-01-01', 'ACTIVE', 'Initial version: Country of origin mandatory for all products');

-- Product Name v1 — applies to all categories
INSERT INTO compliance_rule_versions (id, rule_id, version_number, requirement_type, required_field, validation_operator, expected_value, severity, effective_from, status, change_summary) VALUES
(5, 5, 1, 'MANDATORY_DECLARATION', 'product_name', 'NOT_EMPTY', NULL, 'CRITICAL', '2024-01-01', 'ACTIVE', 'Initial version: Product name mandatory for all products');

-- MRP Cross-Source v1 — applies to FOOD
INSERT INTO compliance_rule_versions (id, rule_id, version_number, requirement_type, required_field, validation_operator, expected_value, severity, effective_from, status, change_summary) VALUES
(6, 6, 1, 'CROSS_SOURCE_CONSISTENCY', 'mrp', 'CROSS_SOURCE_MATCH', NULL, 'MAJOR', '2024-01-01', 'ACTIVE', 'Initial version: MRP consistency check for food products');

-- Net Qty Cross-Source v1 — applies to FOOD
INSERT INTO compliance_rule_versions (id, rule_id, version_number, requirement_type, required_field, validation_operator, expected_value, severity, effective_from, status, change_summary) VALUES
(7, 7, 1, 'CROSS_SOURCE_CONSISTENCY', 'net_quantity', 'CROSS_SOURCE_MATCH', NULL, 'MAJOR', '2024-01-01', 'ACTIVE', 'Initial version: Net quantity consistency check for food products');

-- Rule Applicability
-- Rules 1-3 and 6-7: FOOD only (for demonstrating applicability expansion later)
INSERT INTO rule_applicability (rule_version_id, category) VALUES
(1, 'FOOD'), (2, 'FOOD'), (3, 'FOOD'),
(6, 'FOOD'), (7, 'FOOD');

-- Rules 4-5: All categories
INSERT INTO rule_applicability (rule_version_id, category) VALUES
(4, 'FOOD'), (4, 'COSMETICS'), (4, 'ELECTRONICS'), (4, 'HOUSEHOLD'),
(5, 'FOOD'), (5, 'COSMETICS'), (5, 'ELECTRONICS'), (5, 'HOUSEHOLD');

-- Reset sequences
SELECT setval('products_id_seq', (SELECT MAX(id) FROM products));
SELECT setval('compliance_rules_id_seq', (SELECT MAX(id) FROM compliance_rules));
SELECT setval('compliance_rule_versions_id_seq', (SELECT MAX(id) FROM compliance_rule_versions));
