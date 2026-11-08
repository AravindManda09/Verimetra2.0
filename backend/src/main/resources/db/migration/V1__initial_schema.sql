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
-- ============================================
-- SEED DATA: 20 Curated Demonstration Products
-- ============================================
INSERT INTO products (id, product_name, sku, category, manufacturer_name, country_of_origin, description) VALUES
-- 5 FOOD PRODUCTS (IDs 1-5)
(1, 'Tata Salt Vacuum Evaporated Iodised Salt 1kg', 'FOOD-001', 'FOOD', 'Tata Consumer Products Ltd', 'India', 'India’s trusted vacuum evaporated iodised table salt, enriched with essential iodine'),
(2, 'Amul Pasteurized Salted Butter 500g', 'FOOD-002', 'FOOD', 'Gujarat Co-operative Milk Marketing Federation Ltd', 'India', 'Delicious creamy pasteurized table butter made from pure milk fat'),
(3, 'Maggi 2-Minute Masala Instant Noodles 420g', 'FOOD-003', 'FOOD', 'Nestle India Limited', 'India', 'Popular 2-minute instant noodles with signature blend of roasted Indian spices'),
(4, 'Aashirvaad Superior MP Whole Wheat Atta 5kg', 'FOOD-004', 'FOOD', 'ITC Limited', 'India', '100% pure whole wheat stone-ground flour from Madhya Pradesh golden wheat grains'),
(5, 'Fortune Sunlite Refined Sunflower Oil 1L', 'FOOD-005', 'FOOD', 'Adani Wilmar Limited', 'India', 'Light and healthy refined sunflower cooking oil enriched with Vitamins A and D'),

-- 5 COSMETICS PRODUCTS (IDs 6-10)
(6, 'Himalaya Purifying Neem Face Wash 150ml', 'COSM-001', 'COSMETICS', 'Himalaya Wellness Company', 'India', 'Herbal soap-free facial formulation that gently cleanses impurities and pimples'),
(7, 'Parachute 100% Pure Coconut Hair Oil 200ml', 'COSM-002', 'COSMETICS', 'Marico Limited', 'India', 'Pure naturally filtered coconut hair oil made from hand-picked sun-dried coconuts'),
(8, 'Nivea Soft Light Moisturizing Cream 100ml', 'COSM-003', 'COSMETICS', 'Nivea India Pvt Ltd', 'India', 'Refreshing soft moisturizing cream with Jojoba Oil and Vitamin E for face, body, and hands'),
(9, 'Lakme Sun Expert SPF 50 Ultra Matte Sunscreen 50g', 'COSM-004', 'COSMETICS', 'Hindustan Unilever Limited', 'India', 'Broad spectrum UV protection matte gel lotion blocking up to 97% harmful rays'),
(10, 'Dettol Skincare Liquid Handwash Refill 675ml', 'COSM-005', 'COSMETICS', 'Reckitt Benckiser India Pvt Ltd', 'India', 'Moisturizing antibacterial liquid soap refill pouch enriched with skin moisturizers'),

-- 5 ELECTRONICS PRODUCTS (IDs 11-15)
(11, 'boAt Rockerz 450 Bluetooth On-Ear Headphones', 'ELEC-001', 'ELECTRONICS', 'Imagine Marketing Limited', 'China', 'Wireless over-ear headset with 40mm dynamic drivers and up to 15 hours playback'),
(12, 'Noise ColorFit Pulse 2 Max Bluetooth Smartwatch', 'ELEC-002', 'ELECTRONICS', 'Nexxbase Marketing Pvt Ltd', 'India', '1.85-inch TFT display smartwatch with 550 nits brightness and 100 sports modes'),
(13, 'Philips Multi-Grooming Cordless Trimmer Series 3000', 'ELEC-003', 'ELECTRONICS', 'Philips India Limited', 'Indonesia', '9-in-1 all-in-one face and hair trimmer with self-sharpening skin-friendly blades'),
(14, 'Samsung 25W USB-C Super Fast Power Adapter', 'ELEC-004', 'ELECTRONICS', 'Samsung Electronics Co Ltd', 'Vietnam', 'Super Fast Charging power delivery adapter with Type-C output for mobile devices'),
(15, 'SanDisk Ultra Dual Drive Go Type-C 64GB', 'ELEC-005', 'ELECTRONICS', 'Western Digital Technologies Inc', 'Malaysia', '2-in-1 reversible USB Type-C and Type-A flash drive for smartphones, tablets, and computers'),

-- 5 HOUSEHOLD PRODUCTS (IDs 16-20)
(16, 'Surf Excel Matic Top Load Liquid Detergent 1L', 'HOUS-001', 'HOUSEHOLD', 'Hindustan Unilever Limited', 'India', 'Advanced concentrated liquid wash formula designed for automated washing machines'),
(17, 'Lizol Disinfectant Surface Floor Cleaner Citrus 1L', 'HOUS-002', 'HOUSEHOLD', 'Reckitt Benckiser India Pvt Ltd', 'India', 'Hospital-grade surface disinfectant floor cleaner proven to eliminate 99.9% germs'),
(18, 'Godrej Aer Pocket Bathroom Fragrance Lavender 10g', 'HOUS-003', 'HOUSEHOLD', 'Godrej Consumer Products Limited', 'India', 'Power gel fragrance pocket designed to keep bathrooms fresh for up to 30 days'),
(19, 'Vim Dishwash Liquid Gel Lemon 750ml', 'HOUS-004', 'HOUSEHOLD', 'Hindustan Unilever Limited', 'India', 'Powerful degreasing dishwash gel infused with the cleaning power of 100 lemons'),
(20, 'Harpic Power Plus Disinfectant Toilet Cleaner 1L', 'HOUS-005', 'HOUSEHOLD', 'Reckitt Benckiser India Pvt Ltd', 'India', '10x better stain removal disinfectant toilet cleaner formula for absolute hygiene');

-- ============================================
-- E-COMMERCE LISTING DECLARATIONS (All 20 Products)
-- ============================================
INSERT INTO product_declarations (product_id, source, field_name, field_value, normalized_value, normalization_unit, confirmed) VALUES
-- Product 1: Tata Salt (FOOD) - Fully Compliant
(1, 'LISTING', 'mrp', '28.00', '28.00', 'INR', true),
(1, 'LISTING', 'net_quantity', '1 kg', '1.0', 'kg', true),
(1, 'LISTING', 'manufacturer_name', 'Tata Consumer Products Ltd', 'tata consumer products ltd', NULL, true),
(1, 'LISTING', 'country_of_origin', 'India', 'india', NULL, true),
(1, 'LISTING', 'fssai_license', '10014022002653', '10014022002653', NULL, true),
(1, 'LISTING', 'expiry_date', '24 months from packaging', '24 months', NULL, true),

-- Product 2: Amul Butter (FOOD) - Fully Compliant
(2, 'LISTING', 'mrp', '275.00', '275.00', 'INR', true),
(2, 'LISTING', 'net_quantity', '500 g', '0.5', 'kg', true),
(2, 'LISTING', 'manufacturer_name', 'Gujarat Co-operative Milk Marketing Federation Ltd', 'gujarat co-operative milk marketing federation ltd', NULL, true),
(2, 'LISTING', 'country_of_origin', 'India', 'india', NULL, true),
(2, 'LISTING', 'fssai_license', '10012021000071', '10012021000071', NULL, true),
(2, 'LISTING', 'expiry_date', '9 months from mfg', '9 months', NULL, true),

-- Product 3: Maggi Noodles (FOOD) - Fully Compliant
(3, 'LISTING', 'mrp', '96.00', '96.00', 'INR', true),
(3, 'LISTING', 'net_quantity', '420 g', '0.42', 'kg', true),
(3, 'LISTING', 'manufacturer_name', 'Nestle India Limited', 'nestle india limited', NULL, true),
(3, 'LISTING', 'country_of_origin', 'India', 'india', NULL, true),
(3, 'LISTING', 'fssai_license', '10012011000168', '10012011000168', NULL, true),
(3, 'LISTING', 'expiry_date', '9 months from packaging', '9 months', NULL, true),

-- Product 4: Aashirvaad Atta (FOOD) - Non-Compliant: Missing FSSAI license on listing
(4, 'LISTING', 'mrp', '265.00', '265.00', 'INR', true),
(4, 'LISTING', 'net_quantity', '5 kg', '5.0', 'kg', true),
(4, 'LISTING', 'manufacturer_name', 'ITC Limited', 'itc limited', NULL, true),
(4, 'LISTING', 'country_of_origin', 'India', 'india', NULL, true),
(4, 'LISTING', 'expiry_date', '3 months from packaging', '3 months', NULL, true),

-- Product 5: Fortune Sunflower Oil (FOOD) - Cross-source mismatch (MRP ₹165 on listing vs ₹145 on package)
(5, 'LISTING', 'mrp', '165.00', '165.00', 'INR', true),
(5, 'LISTING', 'net_quantity', '1 L', '1.0', 'L', true),
(5, 'LISTING', 'manufacturer_name', 'Adani Wilmar Limited', 'adani wilmar limited', NULL, true),
(5, 'LISTING', 'country_of_origin', 'India', 'india', NULL, true),
(5, 'LISTING', 'fssai_license', '10013021000817', '10013021000817', NULL, true),
(5, 'LISTING', 'expiry_date', '9 months from packaging', '9 months', NULL, true),

-- Product 6: Himalaya Face Wash (COSMETICS) - Fully Compliant
(6, 'LISTING', 'mrp', '199.00', '199.00', 'INR', true),
(6, 'LISTING', 'net_quantity', '150 ml', '0.15', 'L', true),
(6, 'LISTING', 'manufacturer_name', 'Himalaya Wellness Company', 'himalaya wellness company', NULL, true),
(6, 'LISTING', 'country_of_origin', 'India', 'india', NULL, true),
(6, 'LISTING', 'manufacture_date', '01/2026', '2026-01-01', NULL, true),
(6, 'LISTING', 'expiry_date', '12/2028', '2028-12-01', NULL, true),

-- Product 7: Parachute Hair Oil (COSMETICS) - Fully Compliant
(7, 'LISTING', 'mrp', '115.00', '115.00', 'INR', true),
(7, 'LISTING', 'net_quantity', '200 ml', '0.2', 'L', true),
(7, 'LISTING', 'manufacturer_name', 'Marico Limited', 'marico limited', NULL, true),
(7, 'LISTING', 'country_of_origin', 'India', 'india', NULL, true),
(7, 'LISTING', 'manufacture_date', '02/2026', '2026-02-01', NULL, true),
(7, 'LISTING', 'expiry_date', '01/2028', '2028-01-01', NULL, true),

-- Product 8: Nivea Cream (COSMETICS) - Fully Compliant
(8, 'LISTING', 'mrp', '185.00', '185.00', 'INR', true),
(8, 'LISTING', 'net_quantity', '100 ml', '0.1', 'L', true),
(8, 'LISTING', 'manufacturer_name', 'Nivea India Pvt Ltd', 'nivea india pvt ltd', NULL, true),
(8, 'LISTING', 'country_of_origin', 'India', 'india', NULL, true),
(8, 'LISTING', 'manufacture_date', '01/2026', '2026-01-01', NULL, true),
(8, 'LISTING', 'expiry_date', '01/2029', '2029-01-01', NULL, true),

-- Product 9: Lakme Sunscreen (COSMETICS) - Non-Compliant: Missing Country of Origin
(9, 'LISTING', 'mrp', '299.00', '299.00', 'INR', true),
(9, 'LISTING', 'net_quantity', '50 g', '0.05', 'kg', true),
(9, 'LISTING', 'manufacturer_name', 'Hindustan Unilever Limited', 'hindustan unilever limited', NULL, true),
(9, 'LISTING', 'manufacture_date', '03/2026', '2026-03-01', NULL, true),
(9, 'LISTING', 'expiry_date', '02/2028', '2028-02-01', NULL, true),

-- Product 10: Dettol Handwash (COSMETICS) - Cross-source mismatch (Listing 750ml vs Package 675ml)
(10, 'LISTING', 'mrp', '109.00', '109.00', 'INR', true),
(10, 'LISTING', 'net_quantity', '750 ml', '0.75', 'L', true),
(10, 'LISTING', 'manufacturer_name', 'Reckitt Benckiser India Pvt Ltd', 'reckitt benckiser india pvt ltd', NULL, true),
(10, 'LISTING', 'country_of_origin', 'India', 'india', NULL, true),
(10, 'LISTING', 'manufacture_date', '01/2026', '2026-01-01', NULL, true),
(10, 'LISTING', 'expiry_date', '12/2027', '2027-12-01', NULL, true),

-- Product 11: boAt Headphones (ELECTRONICS) - Fully Compliant
(11, 'LISTING', 'mrp', '1499.00', '1499.00', 'INR', true),
(11, 'LISTING', 'net_quantity', '1 Unit', '1', 'Unit', true),
(11, 'LISTING', 'manufacturer_name', 'Imagine Marketing Limited', 'imagine marketing limited', NULL, true),
(11, 'LISTING', 'country_of_origin', 'China', 'china', NULL, true),
(11, 'LISTING', 'model_number', 'Rockerz-450', 'rockerz-450', NULL, true),
(11, 'LISTING', 'warranty_period', '1 Year', '1 Year', NULL, true),

-- Product 12: Noise Smartwatch (ELECTRONICS) - Fully Compliant
(12, 'LISTING', 'mrp', '1999.00', '1999.00', 'INR', true),
(12, 'LISTING', 'net_quantity', '1 Unit', '1', 'Unit', true),
(12, 'LISTING', 'manufacturer_name', 'Nexxbase Marketing Pvt Ltd', 'nexxbase marketing pvt ltd', NULL, true),
(12, 'LISTING', 'country_of_origin', 'India', 'india', NULL, true),
(12, 'LISTING', 'model_number', 'Pulse-2-Max', 'pulse-2-max', NULL, true),
(12, 'LISTING', 'warranty_period', '1 Year', '1 Year', NULL, true),

-- Product 13: Philips Trimmer (ELECTRONICS) - Fully Compliant
(13, 'LISTING', 'mrp', '1695.00', '1695.00', 'INR', true),
(13, 'LISTING', 'net_quantity', '1 Unit', '1', 'Unit', true),
(13, 'LISTING', 'manufacturer_name', 'Philips India Limited', 'philips india limited', NULL, true),
(13, 'LISTING', 'country_of_origin', 'Indonesia', 'indonesia', NULL, true),
(13, 'LISTING', 'model_number', 'MG3710', 'mg3710', NULL, true),
(13, 'LISTING', 'warranty_period', '2 Years', '2 Years', NULL, true),

-- Product 14: Samsung Charger (ELECTRONICS) - Non-Compliant: Missing Manufacturer on listing
(14, 'LISTING', 'mrp', '1299.00', '1299.00', 'INR', true),
(14, 'LISTING', 'net_quantity', '1 Unit', '1', 'Unit', true),
(14, 'LISTING', 'country_of_origin', 'Vietnam', 'vietnam', NULL, true),
(14, 'LISTING', 'model_number', 'EP-TA800', 'ep-ta800', NULL, true),
(14, 'LISTING', 'warranty_period', '6 Months', '6 Months', NULL, true),

-- Product 15: SanDisk Flash Drive (ELECTRONICS) - Fully Compliant
(15, 'LISTING', 'mrp', '789.00', '789.00', 'INR', true),
(15, 'LISTING', 'net_quantity', '1 Unit', '1', 'Unit', true),
(15, 'LISTING', 'manufacturer_name', 'Western Digital Technologies Inc', 'western digital technologies inc', NULL, true),
(15, 'LISTING', 'country_of_origin', 'Malaysia', 'malaysia', NULL, true),
(15, 'LISTING', 'model_number', 'SDDDC3-064G', 'sdddc3-064g', NULL, true),
(15, 'LISTING', 'warranty_period', '5 Years', '5 Years', NULL, true),

-- Product 16: Surf Excel (HOUSEHOLD) - Fully Compliant
(16, 'LISTING', 'mrp', '230.00', '230.00', 'INR', true),
(16, 'LISTING', 'net_quantity', '1 L', '1.0', 'L', true),
(16, 'LISTING', 'manufacturer_name', 'Hindustan Unilever Limited', 'hindustan unilever limited', NULL, true),
(16, 'LISTING', 'country_of_origin', 'India', 'india', NULL, true),
(16, 'LISTING', 'manufacture_date', '01/2026', '2026-01-01', NULL, true),
(16, 'LISTING', 'expiry_date', '24 months from mfg', '24 months', NULL, true),

-- Product 17: Lizol Floor Cleaner (HOUSEHOLD) - Fully Compliant
(17, 'LISTING', 'mrp', '219.00', '219.00', 'INR', true),
(17, 'LISTING', 'net_quantity', '1 L', '1.0', 'L', true),
(17, 'LISTING', 'manufacturer_name', 'Reckitt Benckiser India Pvt Ltd', 'reckitt benckiser india pvt ltd', NULL, true),
(17, 'LISTING', 'country_of_origin', 'India', 'india', NULL, true),
(17, 'LISTING', 'manufacture_date', '02/2026', '2026-02-01', NULL, true),
(17, 'LISTING', 'expiry_date', '24 months from mfg', '24 months', NULL, true),

-- Product 18: Godrej Aer Pocket (HOUSEHOLD) - Fully Compliant
(18, 'LISTING', 'mrp', '60.00', '60.00', 'INR', true),
(18, 'LISTING', 'net_quantity', '10 g', '0.01', 'kg', true),
(18, 'LISTING', 'manufacturer_name', 'Godrej Consumer Products Limited', 'godrej consumer products limited', NULL, true),
(18, 'LISTING', 'country_of_origin', 'India', 'india', NULL, true),
(18, 'LISTING', 'manufacture_date', '01/2026', '2026-01-01', NULL, true),
(18, 'LISTING', 'expiry_date', '18 months from mfg', '18 months', NULL, true),

-- Product 19: Vim Dishwash Gel (HOUSEHOLD) - Non-Compliant: Missing MRP on listing
(19, 'LISTING', 'net_quantity', '750 ml', '0.75', 'L', true),
(19, 'LISTING', 'manufacturer_name', 'Hindustan Unilever Limited', 'hindustan unilever limited', NULL, true),
(19, 'LISTING', 'country_of_origin', 'India', 'india', NULL, true),
(19, 'LISTING', 'manufacture_date', '02/2026', '2026-02-01', NULL, true),
(19, 'LISTING', 'expiry_date', '24 months from mfg', '24 months', NULL, true),

-- Product 20: Harpic Toilet Cleaner (HOUSEHOLD) - Cross-source mismatch (Listing ₹195 vs Package ₹180)
(20, 'LISTING', 'mrp', '195.00', '195.00', 'INR', true),
(20, 'LISTING', 'net_quantity', '1 L', '1.0', 'L', true),
(20, 'LISTING', 'manufacturer_name', 'Reckitt Benckiser India Pvt Ltd', 'reckitt benckiser india pvt ltd', NULL, true),
(20, 'LISTING', 'country_of_origin', 'India', 'india', NULL, true),
(20, 'LISTING', 'manufacture_date', '01/2026', '2026-01-01', NULL, true),
(20, 'LISTING', 'expiry_date', '24 months from mfg', '24 months', NULL, true);

-- ============================================
-- PHYSICAL PACKAGE DECLARATIONS (Verified from Label Evidence)
-- ============================================
INSERT INTO product_declarations (product_id, source, field_name, field_value, normalized_value, normalization_unit, confirmed) VALUES
-- Product 1: Tata Salt Package (Matches listing)
(1, 'PACKAGE', 'mrp', '28.00', '28.00', 'INR', true),
(1, 'PACKAGE', 'net_quantity', '1 kg', '1.0', 'kg', true),
(1, 'PACKAGE', 'manufacturer_name', 'Tata Consumer Products Ltd', 'tata consumer products ltd', NULL, true),
(1, 'PACKAGE', 'country_of_origin', 'India', 'india', NULL, true),

-- Product 2: Amul Butter Package (Matches listing)
(2, 'PACKAGE', 'mrp', '275.00', '275.00', 'INR', true),
(2, 'PACKAGE', 'net_quantity', '500 g', '0.5', 'kg', true),
(2, 'PACKAGE', 'manufacturer_name', 'Gujarat Co-operative Milk Marketing Federation Ltd', 'gujarat co-operative milk marketing federation ltd', NULL, true),
(2, 'PACKAGE', 'country_of_origin', 'India', 'india', NULL, true),

-- Product 5: Fortune Sunflower Oil Package (MRP Discrepancy: ₹145 package vs ₹165 online)
(5, 'PACKAGE', 'mrp', '145.00', '145.00', 'INR', true),
(5, 'PACKAGE', 'net_quantity', '1 L', '1.0', 'L', true),
(5, 'PACKAGE', 'manufacturer_name', 'Adani Wilmar Limited', 'adani wilmar limited', NULL, true),
(5, 'PACKAGE', 'country_of_origin', 'India', 'india', NULL, true),

-- Product 7: Parachute Hair Oil Package (Matches listing)
(7, 'PACKAGE', 'mrp', '115.00', '115.00', 'INR', true),
(7, 'PACKAGE', 'net_quantity', '200 ml', '0.2', 'L', true),
(7, 'PACKAGE', 'manufacturer_name', 'Marico Limited', 'marico limited', NULL, true),
(7, 'PACKAGE', 'country_of_origin', 'India', 'india', NULL, true),

-- Product 10: Dettol Handwash Package (Quantity Discrepancy: 675ml package vs 750ml online)
(10, 'PACKAGE', 'mrp', '109.00', '109.00', 'INR', true),
(10, 'PACKAGE', 'net_quantity', '675 ml', '0.675', 'L', true),
(10, 'PACKAGE', 'manufacturer_name', 'Reckitt Benckiser India Pvt Ltd', 'reckitt benckiser india pvt ltd', NULL, true),
(10, 'PACKAGE', 'country_of_origin', 'India', 'india', NULL, true),

-- Product 15: SanDisk Flash Drive Package (Matches listing)
(15, 'PACKAGE', 'mrp', '789.00', '789.00', 'INR', true),
(15, 'PACKAGE', 'net_quantity', '1 Unit', '1', 'Unit', true),
(15, 'PACKAGE', 'manufacturer_name', 'Western Digital Technologies Inc', 'western digital technologies inc', NULL, true),
(15, 'PACKAGE', 'country_of_origin', 'Malaysia', 'malaysia', NULL, true),

-- Product 20: Harpic Toilet Cleaner Package (MRP Discrepancy: ₹180 package vs ₹195 online)
(20, 'PACKAGE', 'mrp', '180.00', '180.00', 'INR', true),
(20, 'PACKAGE', 'net_quantity', '1 L', '1.0', 'L', true),
(20, 'PACKAGE', 'manufacturer_name', 'Reckitt Benckiser India Pvt Ltd', 'reckitt benckiser india pvt ltd', NULL, true),
(20, 'PACKAGE', 'country_of_origin', 'India', 'india', NULL, true);

-- ============================================
-- COMPLIANCE RULES (Legal Metrology Packaged Commodities)
-- ============================================
INSERT INTO compliance_rules (id, rule_code, rule_name, description, legal_reference, source_url, source_verified_date, is_illustrative) VALUES
(1, 'LM-PC-R6-MRP', 'MRP Declaration Required', 
 'Every retail package of a commodity must conspicuously declare the maximum retail price inclusive of all taxes.',
 'Legal Metrology (Packaged Commodities) Rules, 2011 - Rule 6(1)(d)',
 'https://consumeraffairs.nic.in/acts-and-rules/legal-metrology', '2024-01-15', false),

(2, 'LM-PC-R6-NETQTY', 'Net Quantity Declaration Required',
 'Every package must declare the net quantity of the commodity in standard metric units of weight, volume, or number.',
 'Legal Metrology (Packaged Commodities) Rules, 2011 - Rule 6(1)(b)',
 'https://consumeraffairs.nic.in/acts-and-rules/legal-metrology', '2024-01-15', false),

(3, 'LM-PC-R6-MFR', 'Manufacturer / Packer Name Required',
 'The name and complete registered address of the manufacturer, packer, or importer must be clearly declared.',
 'Legal Metrology (Packaged Commodities) Rules, 2011 - Rule 6(1)(a)',
 'https://consumeraffairs.nic.in/acts-and-rules/legal-metrology', '2024-01-15', false),

(4, 'LM-PC-R6-COO', 'Country of Origin Declaration Required',
 'The country of manufacture or assembly must be declared on packages and e-commerce product listings.',
 'Legal Metrology (Packaged Commodities) Rules, 2011 - Rule 6(2)',
 'https://consumeraffairs.nic.in/acts-and-rules/legal-metrology', '2024-01-15', false),

(5, 'LM-PC-R6-NAME', 'Product Name / Description Required',
 'The generic name or description of the commodity contained inside the package must be explicitly declared.',
 'Legal Metrology (Packaged Commodities) Rules, 2011 - Rule 6(1)',
 'https://consumeraffairs.nic.in/acts-and-rules/legal-metrology', '2024-01-15', false),

(6, 'VM-CROSS-MRP', 'MRP Cross-Source Consistency Check',
 'Verifies whether the MRP declared on the online listing matches the physical package label to prevent consumer overcharging.',
 'VeriMetra Analytical Consistency Standard', NULL, '2024-01-15', true),

(7, 'VM-CROSS-NETQTY', 'Net Quantity Cross-Source Consistency Check',
 'Verifies whether the net weight or volume declared online matches the physical package to prevent misleading weight claims.',
 'VeriMetra Analytical Consistency Standard', NULL, '2024-01-15', true);

-- ============================================
-- RULE VERSIONS (Version 1 & Version 2 for Key Demonstration Rules)
-- ============================================

-- Rule 1: MRP Declaration
-- Version 1: Applies to FOOD only (SUPERSEDED)
INSERT INTO compliance_rule_versions (id, rule_id, version_number, requirement_type, required_field, validation_operator, expected_value, severity, effective_from, effective_to, status, change_summary) VALUES
(1, 1, 1, 'MANDATORY_DECLARATION', 'mrp', 'NOT_EMPTY', NULL, 'CRITICAL', '2024-01-01', '2025-12-31', 'SUPERSEDED', 'Initial version: MRP declaration mandatory for food products');

-- Version 2: Expanded to FOOD and COSMETICS (ACTIVE)
INSERT INTO compliance_rule_versions (id, rule_id, version_number, requirement_type, required_field, validation_operator, expected_value, severity, effective_from, effective_to, status, change_summary) VALUES
(8, 1, 2, 'MANDATORY_DECLARATION', 'mrp', 'NOT_EMPTY', NULL, 'CRITICAL', '2026-01-01', NULL, 'ACTIVE', 'Legal Metrology Amendment: Mandatory MRP declaration expanded to include Cosmetics commodities');

-- Rule 2: Net Quantity
-- Version 1: Applies to FOOD (ACTIVE)
INSERT INTO compliance_rule_versions (id, rule_id, version_number, requirement_type, required_field, validation_operator, expected_value, severity, effective_from, status, change_summary) VALUES
(2, 2, 1, 'MANDATORY_DECLARATION', 'net_quantity', 'NOT_EMPTY', NULL, 'CRITICAL', '2024-01-01', 'ACTIVE', 'Initial version: Net quantity declaration mandatory for food products');

-- Version 2: Expanded to FOOD, COSMETICS, HOUSEHOLD (DRAFT)
INSERT INTO compliance_rule_versions (id, rule_id, version_number, requirement_type, required_field, validation_operator, expected_value, severity, effective_from, status, change_summary) VALUES
(9, 2, 2, 'MANDATORY_DECLARATION', 'net_quantity', 'NOT_EMPTY', NULL, 'CRITICAL', '2026-06-01', 'DRAFT', 'Applicability expanded to Cosmetics and Household commodities');

-- Rules 3 to 7 (Version 1)
INSERT INTO compliance_rule_versions (id, rule_id, version_number, requirement_type, required_field, validation_operator, expected_value, severity, effective_from, status, change_summary) VALUES
(3, 3, 1, 'MANDATORY_DECLARATION', 'manufacturer_name', 'NOT_EMPTY', NULL, 'MAJOR', '2024-01-01', 'ACTIVE', 'Manufacturer name mandatory for all commodities'),
(4, 4, 1, 'MANDATORY_DECLARATION', 'country_of_origin', 'NOT_EMPTY', NULL, 'MAJOR', '2024-01-01', 'ACTIVE', 'Country of origin mandatory for all commodities'),
(5, 5, 1, 'MANDATORY_DECLARATION', 'product_name', 'NOT_EMPTY', NULL, 'CRITICAL', '2024-01-01', 'ACTIVE', 'Product generic name mandatory for all commodities'),
(6, 6, 1, 'CROSS_SOURCE_CONSISTENCY', 'mrp', 'CROSS_SOURCE_MATCH', NULL, 'MAJOR', '2024-01-01', 'ACTIVE', 'MRP consistency cross-check between listing and physical label'),
(7, 7, 1, 'CROSS_SOURCE_CONSISTENCY', 'net_quantity', 'CROSS_SOURCE_MATCH', NULL, 'MAJOR', '2024-01-01', 'ACTIVE', 'Net quantity consistency cross-check between listing and physical label');

-- ============================================
-- RULE APPLICABILITY MAPPINGS
-- ============================================
-- Rule 1 Version 1: FOOD only
INSERT INTO rule_applicability (rule_version_id, category) VALUES
(1, 'FOOD');

-- Rule 1 Version 2: FOOD + COSMETICS (Demonstrates Phase B Applicability Expansion)
INSERT INTO rule_applicability (rule_version_id, category) VALUES
(8, 'FOOD'), (8, 'COSMETICS');

-- Rule 2 Version 1: FOOD only
INSERT INTO rule_applicability (rule_version_id, category) VALUES
(2, 'FOOD');

-- Rule 2 Version 2: FOOD + COSMETICS + HOUSEHOLD
INSERT INTO rule_applicability (rule_version_id, category) VALUES
(9, 'FOOD'), (9, 'COSMETICS'), (9, 'HOUSEHOLD');

-- Rules 3, 4, 5: All 4 categories
INSERT INTO rule_applicability (rule_version_id, category) VALUES
(3, 'FOOD'), (3, 'COSMETICS'), (3, 'ELECTRONICS'), (3, 'HOUSEHOLD'),
(4, 'FOOD'), (4, 'COSMETICS'), (4, 'ELECTRONICS'), (4, 'HOUSEHOLD'),
(5, 'FOOD'), (5, 'COSMETICS'), (5, 'ELECTRONICS'), (5, 'HOUSEHOLD');

-- Rules 6, 7: Cross-source rules apply to categories with package evidence
INSERT INTO rule_applicability (rule_version_id, category) VALUES
(6, 'FOOD'), (6, 'COSMETICS'), (6, 'HOUSEHOLD'),
(7, 'FOOD'), (7, 'COSMETICS'), (7, 'HOUSEHOLD');

-- ============================================
-- PRE-SEEDED BASELINE EVALUATION & DEPENDENCY GRAPH
-- Rule 1 Version 1 Baseline for 5 Food Products
-- ============================================
INSERT INTO evaluations (id, product_id, rule_version_id, evaluation_status, evaluation_type, triggered_by, evaluated_at, duration_ms) VALUES
(1, 1, 1, 'COMPLIANT', 'MANUAL', 'SYSTEM_BASELINE', NOW(), 1),
(2, 2, 1, 'COMPLIANT', 'MANUAL', 'SYSTEM_BASELINE', NOW(), 1),
(3, 3, 1, 'COMPLIANT', 'MANUAL', 'SYSTEM_BASELINE', NOW(), 1),
(4, 4, 1, 'COMPLIANT', 'MANUAL', 'SYSTEM_BASELINE', NOW(), 1),
(5, 5, 1, 'COMPLIANT', 'MANUAL', 'SYSTEM_BASELINE', NOW(), 1);

INSERT INTO product_rule_dependencies (product_id, rule_id, rule_version_id, last_evaluation_id, dependency_status) VALUES
(1, 1, 1, 1, 'ACTIVE'),
(2, 1, 1, 2, 'ACTIVE'),
(3, 1, 1, 3, 'ACTIVE'),
(4, 1, 1, 4, 'ACTIVE'),
(5, 1, 1, 5, 'ACTIVE');

-- Reset database sequences
SELECT setval('products_id_seq', 20);
SELECT setval('compliance_rules_id_seq', 7);
SELECT setval('compliance_rule_versions_id_seq', 9);
SELECT setval('evaluations_id_seq', 5);
SELECT setval('product_rule_dependencies_id_seq', 5);
SELECT setval('product_declarations_id_seq', (SELECT MAX(id) FROM product_declarations));

