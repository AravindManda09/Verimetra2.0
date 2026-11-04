package com.verimetra.revalidation.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.verimetra.common.exception.ResourceNotFoundException;
import com.verimetra.evaluation.engine.DeterministicRuleEvaluator;
import com.verimetra.evaluation.entity.Evaluation;
import com.verimetra.evaluation.entity.EvaluationFinding;
import com.verimetra.evaluation.entity.EvaluationSnapshot;
import com.verimetra.evaluation.repository.EvaluationFindingRepository;
import com.verimetra.evaluation.repository.EvaluationRepository;
import com.verimetra.evaluation.repository.EvaluationSnapshotRepository;
import com.verimetra.evidence.entity.PackageEvidence;
import com.verimetra.evidence.repository.PackageEvidenceRepository;
import com.verimetra.product.entity.Product;
import com.verimetra.product.entity.ProductDeclaration;
import com.verimetra.product.repository.ProductDeclarationRepository;
import com.verimetra.product.repository.ProductRepository;
import com.verimetra.revalidation.dto.RevalidationRequest;
import com.verimetra.revalidation.dto.RevalidationRunDTO;
import com.verimetra.revalidation.entity.ProductRuleDependency;
import com.verimetra.revalidation.entity.RevalidationCandidate;
import com.verimetra.revalidation.entity.RevalidationRun;
import com.verimetra.revalidation.repository.ProductRuleDependencyRepository;
import com.verimetra.revalidation.repository.RevalidationCandidateRepository;
import com.verimetra.revalidation.repository.RevalidationRunRepository;
import com.verimetra.rule.entity.ComplianceRule;
import com.verimetra.rule.entity.ComplianceRuleVersion;
import com.verimetra.rule.repository.ComplianceRuleRepository;
import com.verimetra.rule.repository.ComplianceRuleVersionRepository;
import com.verimetra.rule.repository.RuleApplicabilityRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
public class IncrementalRevalidationService {

    private final RevalidationRunRepository runRepository;
    private final RevalidationCandidateRepository candidateRepository;
    private final ProductRepository productRepository;
    private final ProductDeclarationRepository declarationRepository;
    private final PackageEvidenceRepository evidenceRepository;
    private final ComplianceRuleRepository ruleRepository;
    private final ComplianceRuleVersionRepository versionRepository;
    private final RuleApplicabilityRepository applicabilityRepository;
    private final ProductRuleDependencyRepository dependencyRepository;
    private final EvaluationRepository evaluationRepository;
    private final EvaluationFindingRepository findingRepository;
    private final EvaluationSnapshotRepository snapshotRepository;
    private final FullRevalidationService fullRevalidationService;
    private final DeterministicRuleEvaluator evaluator;
    private final ObjectMapper objectMapper;

    public IncrementalRevalidationService(
            RevalidationRunRepository runRepository,
            RevalidationCandidateRepository candidateRepository,
            ProductRepository productRepository,
            ProductDeclarationRepository declarationRepository,
            PackageEvidenceRepository evidenceRepository,
            ComplianceRuleRepository ruleRepository,
            ComplianceRuleVersionRepository versionRepository,
            RuleApplicabilityRepository applicabilityRepository,
            ProductRuleDependencyRepository dependencyRepository,
            EvaluationRepository evaluationRepository,
            EvaluationFindingRepository findingRepository,
            EvaluationSnapshotRepository snapshotRepository,
            FullRevalidationService fullRevalidationService,
            DeterministicRuleEvaluator evaluator,
            ObjectMapper objectMapper
    ) {
        this.runRepository = runRepository;
        this.candidateRepository = candidateRepository;
        this.productRepository = productRepository;
        this.declarationRepository = declarationRepository;
        this.evidenceRepository = evidenceRepository;
        this.ruleRepository = ruleRepository;
        this.versionRepository = versionRepository;
        this.applicabilityRepository = applicabilityRepository;
        this.dependencyRepository = dependencyRepository;
        this.evaluationRepository = evaluationRepository;
        this.findingRepository = findingRepository;
        this.snapshotRepository = snapshotRepository;
        this.fullRevalidationService = fullRevalidationService;
        this.evaluator = evaluator;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public RevalidationRunDTO executeIncrementalRevalidation(RevalidationRequest request) {
        long runStart = System.currentTimeMillis();

        ComplianceRule rule = ruleRepository.findById(request.ruleId())
                .orElseThrow(() -> new ResourceNotFoundException("Rule", request.ruleId()));
        ComplianceRuleVersion newVersion = versionRepository.findById(request.newVersionId())
                .orElseThrow(() -> new ResourceNotFoundException("RuleVersion", request.newVersionId()));

        // Retrieve prior version(s)
        List<ComplianceRuleVersion> allVersions = versionRepository.findByRuleIdOrderByVersionNumberDesc(rule.getId());
        Optional<ComplianceRuleVersion> prevVersionOpt = allVersions.stream()
                .filter(v -> !v.getId().equals(newVersion.getId()))
                .findFirst();

        // Safety fallback check: if no previous version exists or dependency metadata is unavailable
        boolean needFallback = false;
        String fallbackReason = null;

        if (prevVersionOpt.isEmpty()) {
            needFallback = true;
            fallbackReason = "No previous rule version exists to establish baseline dependencies. Falling back to full revalidation.";
        }

        if (needFallback) {
            RevalidationRunDTO fullRun = fullRevalidationService.executeFullRevalidation(request);
            // Record that fallback occurred
            RevalidationRun run = runRepository.findById(fullRun.id()).orElse(null);
            if (run != null) {
                run.setUsedFallback(true);
                run.setFallbackReason(fallbackReason);
                runRepository.save(run);
            }
            return fullRevalidationService.toRunDTO(run, rule, null, newVersion);
        }

        ComplianceRuleVersion prevVersion = prevVersionOpt.get();

        long selStart = System.currentTimeMillis();

        // 1. Analyze changes between previous and new version
        List<String> prevCategories = applicabilityRepository.findCategoriesByVersionId(prevVersion.getId());
        List<String> newCategories = applicabilityRepository.findCategoriesByVersionId(newVersion.getId());

        Set<String> addedCategories = new HashSet<>(newCategories);
        addedCategories.removeAll(prevCategories);

        Set<String> removedCategories = new HashSet<>(prevCategories);
        removedCategories.removeAll(newCategories);

        StringBuilder changeBuilder = new StringBuilder();
        if (!Objects.equals(prevVersion.getValidationOperator(), newVersion.getValidationOperator())) {
            changeBuilder.append("Operator changed: ").append(prevVersion.getValidationOperator())
                    .append(" -> ").append(newVersion.getValidationOperator()).append("; ");
        }
        if (!Objects.equals(prevVersion.getExpectedValue(), newVersion.getExpectedValue())) {
            changeBuilder.append("Expected value changed: ").append(prevVersion.getExpectedValue())
                    .append(" -> ").append(newVersion.getExpectedValue()).append("; ");
        }
        if (!Objects.equals(prevVersion.getRequiredField(), newVersion.getRequiredField())) {
            changeBuilder.append("Required field changed: ").append(prevVersion.getRequiredField())
                    .append(" -> ").append(newVersion.getRequiredField()).append("; ");
        }
        if (!addedCategories.isEmpty()) {
            changeBuilder.append("Applicability expanded to: ").append(addedCategories).append("; ");
        }
        if (!removedCategories.isEmpty()) {
            changeBuilder.append("Applicability narrowed, removed: ").append(removedCategories).append("; ");
        }

        // 2. Candidate Discovery
        // Map of productId -> candidate reason (guarantees deduplication)
        Map<Long, String> candidates = new LinkedHashMap<>();

        // Phase A: Previously associated products
        // Products that have an active dependency on this rule or evaluated under previous version
        List<Long> previouslyAssociatedIds = dependencyRepository.findProductIdsByRuleId(rule.getId());
        for (Long pid : previouslyAssociatedIds) {
            candidates.put(pid, "PREVIOUSLY_ASSOCIATED");
        }

        int phaseACount = candidates.size();

        // Phase B: Newly applicable products (Applicability Expansion)
        // If new categories were added, search current catalogue for products in those categories
        int phaseBCount = 0;
        if (!addedCategories.isEmpty()) {
            List<Product> newlyApplicableProducts = productRepository.findByCategoryIn(new ArrayList<>(addedCategories));
            for (Product nap : newlyApplicableProducts) {
                if (!candidates.containsKey(nap.getId())) {
                    candidates.put(nap.getId(), "NEWLY_APPLICABLE");
                    phaseBCount++;
                }
            }
        }

        long selEnd = System.currentTimeMillis();

        // Total products in catalogue for comparison
        long totalProductsInCatalogue = productRepository.countTotal();

        // Record revalidation run
        RevalidationRun run = new RevalidationRun();
        run.setRunType("INCREMENTAL");
        run.setStatus("RUNNING");
        run.setTriggerRuleId(rule.getId());
        run.setPreviousVersionId(prevVersion.getId());
        run.setNewVersionId(newVersion.getId());
        run.setChangedAttributes(changeBuilder.length() > 0 ? changeBuilder.toString() : "Version update");
        run.setTotalProductsConsidered((int) totalProductsInCatalogue);
        run.setPreviouslyAssociatedCount(phaseACount);
        run.setNewlyApplicableCount(phaseBCount);
        run.setCandidateCount(candidates.size());
        run = runRepository.save(run);

        // 3. Re-evaluate candidates deterministically
        int evalsPerformed = 0;
        int findingsCount = 0;
        long evalTime = 0;
        long persistTime = 0;

        for (Map.Entry<Long, String> entry : candidates.entrySet()) {
            Long productId = entry.getKey();
            String reason = entry.getValue();

            Optional<Product> prodOpt = productRepository.findById(productId);
            if (prodOpt.isEmpty()) continue;
            Product product = prodOpt.get();

            // Record candidate entry
            RevalidationCandidate cand = new RevalidationCandidate();
            cand.setRevalidationRunId(run.getId());
            cand.setProductId(productId);
            cand.setCandidateReason(reason);
            cand.setWasEvaluated(true);
            cand.setWasReused(false);
            candidateRepository.save(cand);

            List<ProductDeclaration> decls = declarationRepository.findByProductId(productId);
            List<PackageEvidence> evList = evidenceRepository.findByProductIdOrderByCreatedAtDesc(productId);
            Long evidenceId = evList.isEmpty() ? null : evList.get(0).getId();

            long eStart = System.currentTimeMillis();
            // USE THE EXACT SAME DETERMINISTIC EVALUATOR!
            DeterministicRuleEvaluator.EvaluatorResult evalResult = evaluator.evaluate(
                    product, decls, rule, newVersion, newCategories, evidenceId
            );
            long eEnd = System.currentTimeMillis();
            evalTime += (eEnd - eStart);

            long pStart = System.currentTimeMillis();
            Evaluation eval = new Evaluation();
            eval.setProductId(productId);
            eval.setRuleVersionId(newVersion.getId());
            eval.setEvaluationStatus(evalResult.status());
            eval.setEvaluationType("INCREMENTAL_REVALIDATION");
            eval.setTriggeredBy("INCREMENTAL_REVALIDATION_RUN_" + run.getId());
            eval.setRevalidationRunId(run.getId());
            eval.setEvaluatedAt(Instant.now());
            eval.setDurationMs(eEnd - eStart);
            eval = evaluationRepository.save(eval);

            for (EvaluationFinding f : evalResult.findings()) {
                f.setEvaluationId(eval.getId());
                findingRepository.save(f);
                findingsCount++;
            }

            try {
                EvaluationSnapshot snapshot = new EvaluationSnapshot();
                snapshot.setEvaluationId(eval.getId());
                snapshot.setProductSnapshot(objectMapper.writeValueAsString(product));
                snapshot.setDeclarationSnapshot(objectMapper.writeValueAsString(decls));
                snapshot.setRuleVersionSnapshot(objectMapper.writeValueAsString(newVersion));
                snapshotRepository.save(snapshot);
            } catch (Exception ignored) {}

            updateDependency(productId, rule.getId(), newVersion.getId(), eval.getId());
            evalsPerformed++;
            long pEnd = System.currentTimeMillis();
            persistTime += (pEnd - pStart);
        }

        // Calculate reusable unaffected evaluations
        // Products that are NOT in candidate set and not affected by this rule version change retain their prior status
        int reusedCount = Math.max(0, (int) totalProductsInCatalogue - candidates.size());

        long runEnd = System.currentTimeMillis();

        run.setStatus("COMPLETED");
        run.setEvaluationsPerformed(evalsPerformed);
        run.setEvaluationsReused(reusedCount);
        run.setFindingsGenerated(findingsCount);
        run.setCandidateSelectionMs(selEnd - selStart);
        run.setEvaluationMs(evalTime);
        run.setPersistenceMs(persistTime);
        run.setTotalDurationMs(runEnd - runStart);
        run.setUsedFallback(false);
        run.setCompletedAt(Instant.now());
        run = runRepository.save(run);

        return fullRevalidationService.toRunDTO(run, rule, prevVersion, newVersion);
    }

    private void updateDependency(Long productId, Long ruleId, Long ruleVersionId, Long evalId) {
        ProductRuleDependency dep = dependencyRepository.findByProductIdAndRuleId(productId, ruleId)
                .orElseGet(() -> {
                    ProductRuleDependency d = new ProductRuleDependency();
                    d.setProductId(productId);
                    d.setRuleId(ruleId);
                    return d;
                });
        dep.setRuleVersionId(ruleVersionId);
        dep.setLastEvaluationId(evalId);
        dep.setDependencyStatus("ACTIVE");
        dependencyRepository.save(dep);
    }
}
