# VeriMetra Testing Guide

## 1. Test Architecture

The testing suite covers all required validation, normalization, deterministic evaluation, rule versioning, and revalidation logic.

### Test Categories
1. **Normalization Tests** (`NormalizationServiceTest.java`):
   - Grams to kilograms conversion (`500 g` &rarr; `0.5 kg`, `1000 gm` &rarr; `1.0 kg`).
   - Milliliters to liters conversion (`1000 ml` &rarr; `1.0 L`, `250 ml` &rarr; `0.25 L`).
   - MRP normalization (stripping currency symbols `Rs.`, `₹`, `INR`, rounding to 2 decimals).
   - Case-insensitive text normalization and whitespace trimming.
   - Cross-source value matching.
2. **Deterministic Evaluation Tests** (`DeterministicRuleEvaluatorTest.java`):
   - Category applicability enforcement (`NOT_APPLICABLE` for mismatched categories).
   - Mandatory declaration satisfied vs missing (`NON_COMPLIANT` with `REGULATORY_REQUIREMENT` finding).
   - Cross-source discrepancy detection between listing and packaging (`NEEDS_REVIEW` with `CROSS_SOURCE_CONSISTENCY` finding).
   - Unit-normalized consistency matching (e.g. `1 kg` on listing matches `1000 g` on package).
3. **Critical Incremental Revalidation Tests** (`IncrementalRevalidationTest.java`):
   - **Phase A + Phase B Discovery**: Tests rule applicability expansion from FOOD to FOOD + COSMETICS.
   - Verifies Phase A captures previously evaluated FOOD products.
   - Verifies Phase B captures newly applicable COSMETICS products from current catalogue.
   - Verifies candidate deduplication.
   - Applicability narrowing tests (`NOT_APPLICABLE` assignment).
   - Safety fallback trigger test when baseline dependencies are missing.
4. **Product Validation Tests** (`ProductValidationTest.java`):
   - JSR-380 bean validation on product requests, required fields, and category constraints.

---

## 2. Running Automated Tests

### Run Backend Tests (Maven)
```bash
cd backend
mvn test
```

Expected output:
```text
[INFO] Running com.verimetra.evaluation.engine.DeterministicRuleEvaluatorTest
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.verimetra.normalization.NormalizationServiceTest
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.verimetra.revalidation.IncrementalRevalidationTest
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.verimetra.product.ProductValidationTest
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0
[INFO] Results:
[INFO] Tests run: 17, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

### Run Frontend Production Build & TypeScript Typecheck
```bash
cd frontend
pnpm run build
```

Expected output:
```text
$ tsc -b && vite build
vite v5.4.x building for production...
✓ 2239 modules transformed.
dist/index.html
dist/assets/index.js
✓ built in ~10s
```
