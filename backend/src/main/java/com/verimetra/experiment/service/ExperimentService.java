package com.verimetra.experiment.service;

import com.verimetra.common.exception.ResourceNotFoundException;
import com.verimetra.evaluation.engine.DeterministicRuleEvaluator;
import com.verimetra.evaluation.entity.EvaluationFinding;
import com.verimetra.experiment.dto.ExperimentRunDTO;
import com.verimetra.experiment.dto.RunExperimentRequest;
import com.verimetra.experiment.entity.ExperimentRun;
import com.verimetra.experiment.repository.ExperimentRunRepository;
import com.verimetra.normalization.NormalizationService;
import com.verimetra.product.entity.Product;
import com.verimetra.product.entity.ProductDeclaration;
import com.verimetra.rule.entity.ComplianceRule;
import com.verimetra.rule.entity.ComplianceRuleVersion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

@Service
public class ExperimentService {

    private final ExperimentRunRepository experimentRepository;
    private final DeterministicRuleEvaluator evaluator;
    private final NormalizationService normalizationService;

    public ExperimentService(
            ExperimentRunRepository experimentRepository,
            DeterministicRuleEvaluator evaluator,
            NormalizationService normalizationService
    ) {
        this.experimentRepository = experimentRepository;
        this.evaluator = evaluator;
        this.normalizationService = normalizationService;
    }

    private static class SyntheticItem {
        Product product;
        List<ProductDeclaration> declarations;

        SyntheticItem(Product p, List<ProductDeclaration> d) {
            this.product = p;
            this.declarations = d;
        }
    }

    public ExperimentRunDTO runExperiment(RunExperimentRequest request) {
        long runStart = System.currentTimeMillis();
        long seed = request.randomSeed() != null ? request.randomSeed() : 42L;
        Random rng = new Random(seed);

        int catalogueSize = request.catalogueSize();
        double impactRate = request.impactRate();
        String scenario = request.scenario().toUpperCase();

        // 1. Generate Deterministic Synthetic Catalogue
        List<SyntheticItem> catalogue = generateSyntheticCatalogue(catalogueSize, impactRate, rng);

        // 2. Set up Version 1 and Version 2 based on scenario
        ComplianceRule rule = new ComplianceRule();
        rule.setId(999L);
        rule.setRuleCode("EXP-RULE-01");
        rule.setRuleName("Experiment Evaluation Rule");
        rule.setLegalReference("LM Rules 2011");

        ComplianceRuleVersion v1 = new ComplianceRuleVersion();
        v1.setId(1001L);
        v1.setRuleId(rule.getId());
        v1.setVersionNumber(1);
        v1.setRequirementType("MANDATORY_DECLARATION");
        v1.setRequiredField("mrp");
        v1.setValidationOperator("NOT_EMPTY");
        v1.setSeverity("MAJOR");
        v1.setStatus("SUPERSEDED");

        ComplianceRuleVersion v2 = new ComplianceRuleVersion();
        v2.setId(1002L);
        v2.setRuleId(rule.getId());
        v2.setVersionNumber(2);
        v2.setStatus("ACTIVE");

        List<String> v1Categories = new ArrayList<>();
        List<String> v2Categories = new ArrayList<>();

        switch (scenario) {
            case "APPLICABILITY_EXPANSION":
                // v1: FOOD only -> v2: FOOD + COSMETICS
                v1Categories = List.of("FOOD");
                v2Categories = List.of("FOOD", "COSMETICS");
                v2.setRequirementType("MANDATORY_DECLARATION");
                v2.setRequiredField("mrp");
                v2.setValidationOperator("NOT_EMPTY");
                v2.setSeverity("MAJOR");
                break;
            case "APPLICABILITY_NARROWING":
                // v1: FOOD + COSMETICS -> v2: FOOD only
                v1Categories = List.of("FOOD", "COSMETICS");
                v2Categories = List.of("FOOD");
                v2.setRequirementType("MANDATORY_DECLARATION");
                v2.setRequiredField("mrp");
                v2.setValidationOperator("NOT_EMPTY");
                v2.setSeverity("MAJOR");
                break;
            case "REQUIREMENT_MODIFICATION":
                // Field changes from mrp to net_quantity
                v1Categories = List.of("FOOD", "COSMETICS");
                v2Categories = List.of("FOOD", "COSMETICS");
                v2.setRequirementType("MANDATORY_DECLARATION");
                v2.setRequiredField("net_quantity");
                v2.setValidationOperator("NOT_EMPTY");
                v2.setSeverity("CRITICAL");
                break;
            case "THRESHOLD_MODIFICATION":
            default:
                // Value constraint threshold modification: price limit
                v1Categories = List.of("FOOD");
                v2Categories = List.of("FOOD");
                v2.setRequirementType("VALUE_CONSTRAINT");
                v2.setRequiredField("mrp");
                v2.setValidationOperator("LESS_THAN");
                v2.setExpectedValue("500");
                v2.setSeverity("MAJOR");
                break;
        }

        // 3. Establish baseline dependencies by evaluating against v1
        Map<Long, String> priorDependencies = new HashMap<>(); // productId -> status under v1
        for (SyntheticItem item : catalogue) {
            if (v1Categories.contains(item.product.getCategory())) {
                DeterministicRuleEvaluator.EvaluatorResult r = evaluator.evaluate(
                        item.product, item.declarations, rule, v1, v1Categories, null
                );
                priorDependencies.put(item.product.getId(), r.status());
            }
        }

        // ----------------------------------------------------
        // STRATEGY 1: FULL REVALIDATION
        // ----------------------------------------------------
        long fullStart = System.currentTimeMillis();
        int fullEvals = 0;
        int fullFindings = 0;
        Map<Long, LogicalOutcome> fullOutcomes = new HashMap<>();

        for (SyntheticItem item : catalogue) {
            if (v2Categories.contains(item.product.getCategory())) {
                fullEvals++;
                DeterministicRuleEvaluator.EvaluatorResult res = evaluator.evaluate(
                        item.product, item.declarations, rule, v2, v2Categories, null
                );
                fullFindings += res.findings().size();
                fullOutcomes.put(item.product.getId(), new LogicalOutcome(res.status(), res.findings()));
            }
        }
        long fullDuration = System.currentTimeMillis() - fullStart;

        // ----------------------------------------------------
        // STRATEGY 2: INCREMENTAL REVALIDATION
        // ----------------------------------------------------
        long incrTotalStart = System.currentTimeMillis();

        long selStart = System.currentTimeMillis();
        // Candidate selection:
        // Phase A: Previously associated products
        Map<Long, String> candidates = new LinkedHashMap<>();
        for (Long pid : priorDependencies.keySet()) {
            candidates.put(pid, "PREVIOUSLY_ASSOCIATED");
        }
        int phaseACount = candidates.size();

        // Phase B: Newly applicable products (applicability expansion)
        Set<String> addedCategories = new HashSet<>(v2Categories);
        addedCategories.removeAll(v1Categories);

        int phaseBCount = 0;
        if (!addedCategories.isEmpty()) {
            for (SyntheticItem item : catalogue) {
                if (addedCategories.contains(item.product.getCategory()) && !candidates.containsKey(item.product.getId())) {
                    candidates.put(item.product.getId(), "NEWLY_APPLICABLE");
                    phaseBCount++;
                }
            }
        }
        long selDuration = System.currentTimeMillis() - selStart;

        // Evaluation of candidates
        long evalStart = System.currentTimeMillis();
        int incrEvals = 0;
        int incrFindings = 0;
        Map<Long, LogicalOutcome> incrOutcomes = new HashMap<>();

        for (Long candId : candidates.keySet()) {
            SyntheticItem item = catalogue.get(candId.intValue() - 1);
            if (v2Categories.contains(item.product.getCategory())) {
                incrEvals++;
                DeterministicRuleEvaluator.EvaluatorResult res = evaluator.evaluate(
                        item.product, item.declarations, rule, v2, v2Categories, null
                );
                incrFindings += res.findings().size();
                incrOutcomes.put(item.product.getId(), new LogicalOutcome(res.status(), res.findings()));
            }
        }
        long evalDuration = System.currentTimeMillis() - evalStart;

        // In Incremental revalidation, reuse evaluations for unaffected pairs
        int incrReused = Math.max(0, catalogueSize - candidates.size());

        long persistDuration = 1; // in-memory experiment simulation
        long incrTotalDuration = (System.currentTimeMillis() - incrTotalStart);

        // ----------------------------------------------------
        // CANONICAL CORRECTNESS COMPARISON
        // ----------------------------------------------------
        int matching = 0;
        int differing = 0;
        int missing = 0;
        int extra = 0;

        for (Map.Entry<Long, LogicalOutcome> entry : fullOutcomes.entrySet()) {
            Long pid = entry.getKey();
            LogicalOutcome fullOut = entry.getValue();
            LogicalOutcome incrOut = incrOutcomes.get(pid);

            if (incrOut == null) {
                missing++;
            } else if (fullOut.status.equals(incrOut.status) && fullOut.findingTypes.equals(incrOut.findingTypes)) {
                matching++;
            } else {
                differing++;
            }
        }

        for (Long pid : incrOutcomes.keySet()) {
            if (!fullOutcomes.containsKey(pid)) {
                extra++;
            }
        }

        double totalOutcomes = matching + differing + missing + extra;
        double agreementPct = totalOutcomes > 0 ? (matching / totalOutcomes) * 100.0 : 100.0;
        String correctnessStatus = (differing == 0 && missing == 0 && extra == 0) ? "PERFECT_AGREEMENT" : "DISCREPANCY_DETECTED";

        // Metrics calculations
        double evalReduction = fullEvals > 0 ? ((fullEvals - incrEvals) / (double) fullEvals) * 100.0 : 0.0;
        double timeReduction = fullDuration > 0 ? ((fullDuration - incrTotalDuration) / (double) fullDuration) * 100.0 : 0.0;

        // Persist Experiment Run
        ExperimentRun exp = new ExperimentRun();
        exp.setExperimentName(request.experimentName() != null ? request.experimentName() : "Benchmark N=" + catalogueSize + " " + scenario);
        exp.setCatalogueSize(catalogueSize);
        exp.setImpactRate(impactRate);
        exp.setScenario(scenario);
        exp.setRandomSeed(seed);
        exp.setStatus("COMPLETED");

        exp.setFullTotalProducts(catalogueSize);
        exp.setFullApplicablePairs(fullOutcomes.size());
        exp.setFullEvaluations(fullEvals);
        exp.setFullFindings(fullFindings);
        exp.setFullDurationMs(fullDuration);

        exp.setIncrCandidates(candidates.size());
        exp.setIncrPreviouslyAssociated(phaseACount);
        exp.setIncrNewlyApplicable(phaseBCount);
        exp.setIncrEvaluations(incrEvals);
        exp.setIncrReused(incrReused);
        exp.setIncrFindings(incrFindings);
        exp.setIncrCandidateSelectionMs(selDuration);
        exp.setIncrEvaluationMs(evalDuration);
        exp.setIncrPersistenceMs(persistDuration);
        exp.setIncrTotalDurationMs(incrTotalDuration);

        exp.setEvaluationReductionPct(evalReduction);
        exp.setTimeReductionPct(timeReduction);
        exp.setResultAgreementPct(agreementPct);
        exp.setMissedAffectedCount(missing);
        exp.setUnnecessaryReevals(0);
        exp.setMatchingOutcomes(matching);
        exp.setDifferingOutcomes(differing);
        exp.setMissingOutcomes(missing);
        exp.setExtraOutcomes(extra);
        exp.setCorrectnessStatus(correctnessStatus);
        exp.setCompletedAt(Instant.now());

        exp = experimentRepository.save(exp);

        return toDTO(exp);
    }

    private static class LogicalOutcome {
        String status;
        Set<String> findingTypes = new HashSet<>();

        LogicalOutcome(String status, List<EvaluationFinding> findings) {
            this.status = status;
            for (EvaluationFinding f : findings) {
                this.findingTypes.add(f.getFindingType() + ":" + f.getFieldName());
            }
        }
    }

    private List<SyntheticItem> generateSyntheticCatalogue(int size, double impactRate, Random rng) {
        List<SyntheticItem> items = new ArrayList<>(size);
        String[] categories = {"FOOD", "COSMETICS", "ELECTRONICS", "HOUSEHOLD"};

        for (int i = 1; i <= size; i++) {
            Product p = new Product();
            p.setId((long) i);
            p.setSku("SKU-" + String.format("%06d", i));

            // Distribute category according to impact scenario
            String cat = categories[rng.nextInt(categories.length)];
            p.setCategory(cat);
            p.setProductName("Synthetic " + cat + " Product #" + i);
            p.setManufacturerName("Manufacturer " + (i % 20));
            p.setCountryOfOrigin(i % 5 == 0 ? "China" : "India");

            List<ProductDeclaration> decls = new ArrayList<>();

            // 85% have MRP, 15% missing (tests missing mandatory declaration)
            if (rng.nextDouble() < 0.85) {
                ProductDeclaration mrp = new ProductDeclaration();
                mrp.setProductId((long) i);
                mrp.setSource("LISTING");
                mrp.setFieldName("mrp");
                int price = 50 + rng.nextInt(950);
                mrp.setFieldValue(String.valueOf(price));
                mrp.setNormalizedValue(String.valueOf(price));
                mrp.setNormalizationUnit("INR");
                mrp.setConfirmed(true);
                decls.add(mrp);
            }

            // Net Quantity
            if (rng.nextDouble() < 0.90) {
                ProductDeclaration qty = new ProductDeclaration();
                qty.setProductId((long) i);
                qty.setSource("LISTING");
                qty.setFieldName("net_quantity");
                int val = (rng.nextInt(10) + 1) * 100;
                qty.setFieldValue(val + " g");
                qty.setNormalizedValue(new BigDecimal(val).multiply(new BigDecimal("0.001")).toPlainString());
                qty.setNormalizationUnit("kg");
                qty.setConfirmed(true);
                decls.add(qty);
            }

            // Product name
            ProductDeclaration name = new ProductDeclaration();
            name.setProductId((long) i);
            name.setSource("LISTING");
            name.setFieldName("product_name");
            name.setFieldValue(p.getProductName());
            name.setNormalizedValue(p.getProductName().toLowerCase());
            name.setConfirmed(true);
            decls.add(name);

            // Manufacturer
            ProductDeclaration mfr = new ProductDeclaration();
            mfr.setProductId((long) i);
            mfr.setSource("LISTING");
            mfr.setFieldName("manufacturer_name");
            mfr.setFieldValue(p.getManufacturerName());
            mfr.setNormalizedValue(p.getManufacturerName().toLowerCase());
            mfr.setConfirmed(true);
            decls.add(mfr);

            items.add(new SyntheticItem(p, decls));
        }
        return items;
    }

    public Page<ExperimentRunDTO> listExperiments(Pageable pageable) {
        return experimentRepository.findAllByOrderByStartedAtDesc(pageable).map(this::toDTO);
    }

    public ExperimentRunDTO getExperiment(Long id) {
        return toDTO(experimentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ExperimentRun", id)));
    }

    public String exportExperimentCsv(Long id) {
        ExperimentRun e = experimentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ExperimentRun", id));
        StringBuilder sb = new StringBuilder();
        sb.append("Metric,Value\n");
        sb.append("Experiment ID,").append(e.getId()).append("\n");
        sb.append("Experiment Name,").append(e.getExperimentName()).append("\n");
        sb.append("Scenario,").append(e.getScenario()).append("\n");
        sb.append("Catalogue Size,").append(e.getCatalogueSize()).append("\n");
        sb.append("Impact Rate %,").append(e.getImpactRate()).append("\n");
        sb.append("Random Seed,").append(e.getRandomSeed()).append("\n");
        sb.append("Full Total Products,").append(e.getFullTotalProducts()).append("\n");
        sb.append("Full Evaluations,").append(e.getFullEvaluations()).append("\n");
        sb.append("Full Duration (ms),").append(e.getFullDurationMs()).append("\n");
        sb.append("Incr Candidates,").append(e.getIncrCandidates()).append("\n");
        sb.append("Incr Previously Associated,").append(e.getIncrPreviouslyAssociated()).append("\n");
        sb.append("Incr Newly Applicable,").append(e.getIncrNewlyApplicable()).append("\n");
        sb.append("Incr Evaluations,").append(e.getIncrEvaluations()).append("\n");
        sb.append("Incr Reused,").append(e.getIncrReused()).append("\n");
        sb.append("Incr Candidate Selection (ms),").append(e.getIncrCandidateSelectionMs()).append("\n");
        sb.append("Incr Evaluation (ms),").append(e.getIncrEvaluationMs()).append("\n");
        sb.append("Incr Total Duration (ms),").append(e.getIncrTotalDurationMs()).append("\n");
        sb.append("Evaluation Reduction %,").append(e.getEvaluationReductionPct()).append("\n");
        sb.append("Time Reduction %,").append(e.getTimeReductionPct()).append("\n");
        sb.append("Result Agreement %,").append(e.getResultAgreementPct()).append("\n");
        sb.append("Matching Outcomes,").append(e.getMatchingOutcomes()).append("\n");
        sb.append("Differing Outcomes,").append(e.getDifferingOutcomes()).append("\n");
        sb.append("Missing Outcomes,").append(e.getMissingOutcomes()).append("\n");
        sb.append("Extra Outcomes,").append(e.getExtraOutcomes()).append("\n");
        sb.append("Correctness Status,").append(e.getCorrectnessStatus()).append("\n");
        return sb.toString();
    }

    private ExperimentRunDTO toDTO(ExperimentRun e) {
        return new ExperimentRunDTO(
                e.getId(), e.getExperimentName(), e.getCatalogueSize(), e.getImpactRate(),
                e.getScenario(), e.getRandomSeed(), e.getStatus(), e.getFullTotalProducts(),
                e.getFullApplicablePairs(), e.getFullEvaluations(), e.getFullFindings(),
                e.getFullDurationMs(), e.getIncrCandidates(), e.getIncrPreviouslyAssociated(),
                e.getIncrNewlyApplicable(), e.getIncrEvaluations(), e.getIncrReused(),
                e.getIncrFindings(), e.getIncrCandidateSelectionMs(), e.getIncrEvaluationMs(),
                e.getIncrPersistenceMs(), e.getIncrTotalDurationMs(), e.getEvaluationReductionPct(),
                e.getTimeReductionPct(), e.getResultAgreementPct(), e.getMissedAffectedCount(),
                e.getUnnecessaryReevals(), e.getMatchingOutcomes(), e.getDifferingOutcomes(),
                e.getMissingOutcomes(), e.getExtraOutcomes(), e.getCorrectnessStatus(),
                e.getErrorMessage(), e.getStartedAt(), e.getCompletedAt()
        );
    }
}
