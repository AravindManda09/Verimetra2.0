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
public class FullRevalidationService {

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
    private final DeterministicRuleEvaluator evaluator;
    private final ObjectMapper objectMapper;

    public FullRevalidationService(
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
        this.evaluator = evaluator;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public RevalidationRunDTO executeFullRevalidation(RevalidationRequest request) {
        long runStart = System.currentTimeMillis();

        ComplianceRule rule = ruleRepository.findById(request.ruleId())
                .orElseThrow(() -> new ResourceNotFoundException("Rule", request.ruleId()));
        ComplianceRuleVersion newVersion = versionRepository.findById(request.newVersionId())
                .orElseThrow(() -> new ResourceNotFoundException("RuleVersion", request.newVersionId()));

        // Find previous version if available
        Optional<ComplianceRuleVersion> prevVersionOpt = versionRepository.findByRuleIdOrderByVersionNumberDesc(rule.getId())
                .stream()
                .filter(v -> !v.getId().equals(newVersion.getId()))
                .findFirst();

        RevalidationRun run = new RevalidationRun();
        run.setRunType("FULL");
        run.setStatus("RUNNING");
        run.setTriggerRuleId(rule.getId());
        run.setPreviousVersionId(prevVersionOpt.map(ComplianceRuleVersion::getId).orElse(null));
        run.setNewVersionId(newVersion.getId());
        run.setChangedAttributes("Full catalogue scan against rule: " + rule.getRuleCode() + " v" + newVersion.getVersionNumber());
        run = runRepository.save(run);

        long selStart = System.currentTimeMillis();
        // Full revalidation considers the ENTIRE catalogue
        List<Product> allProducts = productRepository.findAll();
        List<String> applicableCategories = applicabilityRepository.findCategoriesByVersionId(newVersion.getId());
        long selEnd = System.currentTimeMillis();

        int totalProducts = allProducts.size();
        int candidateCount = 0;
        int evalsPerformed = 0;
        int findingsCount = 0;

        long evalTime = 0;
        long persistTime = 0;

        for (Product product : allProducts) {
            boolean isApplicable = applicableCategories.contains(product.getCategory().toUpperCase());
            if (!isApplicable) {
                continue;
            }

            candidateCount++;
            RevalidationCandidate cand = new RevalidationCandidate();
            cand.setRevalidationRunId(run.getId());
            cand.setProductId(product.getId());
            cand.setCandidateReason("FULL_SCAN");
            cand.setWasEvaluated(true);
            cand.setWasReused(false);
            candidateRepository.save(cand);

            List<ProductDeclaration> decls = declarationRepository.findByProductId(product.getId());
            List<PackageEvidence> evList = evidenceRepository.findByProductIdOrderByCreatedAtDesc(product.getId());
            Long evidenceId = evList.isEmpty() ? null : evList.get(0).getId();

            long eStart = System.currentTimeMillis();
            DeterministicRuleEvaluator.EvaluatorResult result = evaluator.evaluate(
                    product, decls, rule, newVersion, applicableCategories, evidenceId
            );
            long eEnd = System.currentTimeMillis();
            evalTime += (eEnd - eStart);

            long pStart = System.currentTimeMillis();
            Evaluation eval = new Evaluation();
            eval.setProductId(product.getId());
            eval.setRuleVersionId(newVersion.getId());
            eval.setEvaluationStatus(result.status());
            eval.setEvaluationType("FULL_REVALIDATION");
            eval.setTriggeredBy("FULL_REVALIDATION_RUN_" + run.getId());
            eval.setRevalidationRunId(run.getId());
            eval.setEvaluatedAt(Instant.now());
            eval.setDurationMs(eEnd - eStart);
            eval = evaluationRepository.save(eval);

            for (EvaluationFinding f : result.findings()) {
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

            updateDependency(product.getId(), rule.getId(), newVersion.getId(), eval.getId());
            evalsPerformed++;
            long pEnd = System.currentTimeMillis();
            persistTime += (pEnd - pStart);
        }

        long runEnd = System.currentTimeMillis();
        run.setStatus("COMPLETED");
        run.setTotalProductsConsidered(totalProducts);
        run.setPreviouslyAssociatedCount(0);
        run.setNewlyApplicableCount(0);
        run.setCandidateCount(candidateCount);
        run.setEvaluationsPerformed(evalsPerformed);
        run.setEvaluationsReused(0);
        run.setFindingsGenerated(findingsCount);
        run.setCandidateSelectionMs(selEnd - selStart);
        run.setEvaluationMs(evalTime);
        run.setPersistenceMs(persistTime);
        run.setTotalDurationMs(runEnd - runStart);
        run.setCompletedAt(Instant.now());
        run = runRepository.save(run);

        return toRunDTO(run, rule, prevVersionOpt.orElse(null), newVersion);
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

    public RevalidationRunDTO toRunDTO(RevalidationRun r, ComplianceRule rule, ComplianceRuleVersion prev, ComplianceRuleVersion next) {
        return new RevalidationRunDTO(
                r.getId(),
                r.getRunType(),
                r.getStatus(),
                r.getTriggerRuleId(),
                rule != null ? rule.getRuleCode() : null,
                rule != null ? rule.getRuleName() : null,
                r.getPreviousVersionId(),
                prev != null ? prev.getVersionNumber() : null,
                r.getNewVersionId(),
                next != null ? next.getVersionNumber() : null,
                r.getChangedAttributes(),
                r.getTotalProductsConsidered(),
                r.getPreviouslyAssociatedCount(),
                r.getNewlyApplicableCount(),
                r.getCandidateCount(),
                r.getEvaluationsPerformed(),
                r.getEvaluationsReused(),
                r.getFindingsGenerated(),
                r.getCandidateSelectionMs(),
                r.getEvaluationMs(),
                r.getPersistenceMs(),
                r.getTotalDurationMs(),
                r.getUsedFallback(),
                r.getFallbackReason(),
                r.getErrorMessage(),
                r.getStartedAt(),
                r.getCompletedAt()
        );
    }
}
