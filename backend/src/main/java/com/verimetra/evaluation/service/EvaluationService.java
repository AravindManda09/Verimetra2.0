package com.verimetra.evaluation.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.verimetra.common.exception.ResourceNotFoundException;
import com.verimetra.evaluation.dto.EvaluationDTO;
import com.verimetra.evaluation.dto.EvaluationFindingDTO;
import com.verimetra.evaluation.dto.RunEvaluationRequest;
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
import com.verimetra.revalidation.entity.ProductRuleDependency;
import com.verimetra.revalidation.repository.ProductRuleDependencyRepository;
import com.verimetra.rule.entity.ComplianceRule;
import com.verimetra.rule.entity.ComplianceRuleVersion;
import com.verimetra.rule.repository.ComplianceRuleRepository;
import com.verimetra.rule.repository.ComplianceRuleVersionRepository;
import com.verimetra.rule.repository.RuleApplicabilityRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
public class EvaluationService {

    private final EvaluationRepository evaluationRepository;
    private final EvaluationSnapshotRepository snapshotRepository;
    private final EvaluationFindingRepository findingRepository;
    private final ProductRepository productRepository;
    private final ProductDeclarationRepository declarationRepository;
    private final PackageEvidenceRepository evidenceRepository;
    private final ComplianceRuleRepository ruleRepository;
    private final ComplianceRuleVersionRepository versionRepository;
    private final RuleApplicabilityRepository applicabilityRepository;
    private final ProductRuleDependencyRepository dependencyRepository;
    private final DeterministicRuleEvaluator evaluator;
    private final ObjectMapper objectMapper;

    public EvaluationService(
            EvaluationRepository evaluationRepository,
            EvaluationSnapshotRepository snapshotRepository,
            EvaluationFindingRepository findingRepository,
            ProductRepository productRepository,
            ProductDeclarationRepository declarationRepository,
            PackageEvidenceRepository evidenceRepository,
            ComplianceRuleRepository ruleRepository,
            ComplianceRuleVersionRepository versionRepository,
            RuleApplicabilityRepository applicabilityRepository,
            ProductRuleDependencyRepository dependencyRepository,
            DeterministicRuleEvaluator evaluator,
            ObjectMapper objectMapper
    ) {
        this.evaluationRepository = evaluationRepository;
        this.snapshotRepository = snapshotRepository;
        this.findingRepository = findingRepository;
        this.productRepository = productRepository;
        this.declarationRepository = declarationRepository;
        this.evidenceRepository = evidenceRepository;
        this.ruleRepository = ruleRepository;
        this.versionRepository = versionRepository;
        this.applicabilityRepository = applicabilityRepository;
        this.dependencyRepository = dependencyRepository;
        this.evaluator = evaluator;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public List<EvaluationDTO> runEvaluation(RunEvaluationRequest request) {
        Product product = productRepository.findById(request.productId())
                .orElseThrow(() -> new ResourceNotFoundException("Product", request.productId()));

        List<ComplianceRuleVersion> targetVersions;
        if (request.ruleVersionIds() != null && !request.ruleVersionIds().isEmpty()) {
            targetVersions = versionRepository.findAllById(request.ruleVersionIds());
        } else {
            // Find all active rule versions applicable to product category
            List<Long> versionIds = applicabilityRepository.findVersionIdsByCategory(product.getCategory());
            targetVersions = versionRepository.findAllById(versionIds).stream()
                    .filter(v -> "ACTIVE".equals(v.getStatus()))
                    .toList();
        }

        List<ProductDeclaration> declarations = declarationRepository.findByProductId(product.getId());
        List<PackageEvidence> evidences = evidenceRepository.findByProductIdOrderByCreatedAtDesc(product.getId());
        Long latestEvidenceId = evidences.isEmpty() ? null : evidences.get(0).getId();

        List<EvaluationDTO> results = new ArrayList<>();

        for (ComplianceRuleVersion rv : targetVersions) {
            ComplianceRule rule = ruleRepository.findById(rv.getRuleId()).orElse(null);
            if (rule == null) continue;

            List<String> categories = applicabilityRepository.findCategoriesByVersionId(rv.getId());

            long start = System.currentTimeMillis();
            DeterministicRuleEvaluator.EvaluatorResult evalResult = evaluator.evaluate(
                    product, declarations, rule, rv, categories, latestEvidenceId
            );
            long duration = System.currentTimeMillis() - start;

            // Persist evaluation
            Evaluation eval = new Evaluation();
            eval.setProductId(product.getId());
            eval.setRuleVersionId(rv.getId());
            eval.setEvaluationStatus(evalResult.status());
            eval.setEvaluationType("MANUAL");
            eval.setTriggeredBy("USER");
            eval.setEvaluatedAt(Instant.now());
            eval.setDurationMs(duration);
            eval = evaluationRepository.save(eval);

            // Persist findings
            List<EvaluationFinding> savedFindings = new ArrayList<>();
            for (EvaluationFinding f : evalResult.findings()) {
                f.setEvaluationId(eval.getId());
                savedFindings.add(findingRepository.save(f));
            }

            // Persist snapshot
            try {
                EvaluationSnapshot snapshot = new EvaluationSnapshot();
                snapshot.setEvaluationId(eval.getId());
                snapshot.setProductSnapshot(objectMapper.writeValueAsString(product));
                snapshot.setDeclarationSnapshot(objectMapper.writeValueAsString(declarations));
                snapshot.setRuleVersionSnapshot(objectMapper.writeValueAsString(rv));
                snapshotRepository.save(snapshot);
            } catch (Exception ignored) {}

            // Update dependency
            updateDependency(product.getId(), rule.getId(), rv.getId(), eval.getId());

            results.add(toDTO(eval, product, rule, rv, savedFindings));
        }

        return results;
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

    public List<EvaluationDTO> getEvaluationsForProduct(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", productId));
        return evaluationRepository.findByProductIdOrderByEvaluatedAtDesc(productId).stream()
                .map(e -> toDTO(e, product))
                .toList();
    }

    public Page<EvaluationDTO> listAllEvaluations(Pageable pageable) {
        return evaluationRepository.findAllByOrderByEvaluatedAtDesc(pageable)
                .map(this::toDTOWithoutProductFetch);
    }

    public EvaluationDTO getEvaluation(Long id) {
        Evaluation eval = evaluationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Evaluation", id));
        Product product = productRepository.findById(eval.getProductId()).orElse(null);
        return toDTO(eval, product);
    }

    public List<EvaluationFindingDTO> getFindingsForEvaluation(Long evalId) {
        return findingRepository.findByEvaluationId(evalId).stream()
                .map(this::toFindingDTO)
                .toList();
    }

    @Transactional
    public EvaluationFindingDTO updateFindingReview(Long findingId, String status) {
        EvaluationFinding finding = findingRepository.findById(findingId)
                .orElseThrow(() -> new ResourceNotFoundException("Finding", findingId));
        finding.setReviewStatus(status.toUpperCase());
        return toFindingDTO(findingRepository.save(finding));
    }

    private EvaluationDTO toDTO(Evaluation e, Product product) {
        ComplianceRuleVersion rv = versionRepository.findById(e.getRuleVersionId()).orElse(null);
        ComplianceRule rule = rv != null ? ruleRepository.findById(rv.getRuleId()).orElse(null) : null;
        List<EvaluationFinding> findings = findingRepository.findByEvaluationId(e.getId());
        return toDTO(e, product, rule, rv, findings);
    }

    private EvaluationDTO toDTOWithoutProductFetch(Evaluation e) {
        Product product = productRepository.findById(e.getProductId()).orElse(null);
        return toDTO(e, product);
    }

    private EvaluationDTO toDTO(Evaluation e, Product p, ComplianceRule r, ComplianceRuleVersion rv, List<EvaluationFinding> findings) {
        return new EvaluationDTO(
                e.getId(),
                e.getProductId(),
                p != null ? p.getProductName() : null,
                p != null ? p.getCategory() : null,
                e.getRuleVersionId(),
                r != null ? r.getRuleCode() : null,
                r != null ? r.getRuleName() : null,
                rv != null ? rv.getVersionNumber() : null,
                e.getEvaluationStatus(),
                e.getEvaluationType(),
                e.getTriggeredBy(),
                e.getRevalidationRunId(),
                e.getEvaluatedAt(),
                e.getDurationMs(),
                findings.stream().map(this::toFindingDTO).toList()
        );
    }

    private EvaluationFindingDTO toFindingDTO(EvaluationFinding f) {
        return new EvaluationFindingDTO(
                f.getId(), f.getEvaluationId(), f.getFindingType(), f.getFieldName(),
                f.getExpectedValue(), f.getActualValue(), f.getSeverity(), f.getExplanation(),
                f.getRuleReference(), f.getRuleVersionId(), f.getEvidenceId(), f.getReviewStatus(),
                f.getCreatedAt()
        );
    }
}
