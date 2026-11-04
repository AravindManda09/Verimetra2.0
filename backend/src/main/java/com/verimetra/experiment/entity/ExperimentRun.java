package com.verimetra.experiment.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "experiment_runs")
public class ExperimentRun {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "experiment_name", length = 200)
    private String experimentName;

    @Column(name = "catalogue_size", nullable = false)
    private Integer catalogueSize;

    @Column(name = "impact_rate", nullable = false)
    private Double impactRate;

    @Column(nullable = false, length = 50)
    private String scenario;

    @Column(name = "random_seed", nullable = false)
    private Long randomSeed;

    @Column(nullable = false, length = 20)
    private String status = "RUNNING";

    // Full metrics
    @Column(name = "full_total_products")
    private Integer fullTotalProducts;

    @Column(name = "full_applicable_pairs")
    private Integer fullApplicablePairs;

    @Column(name = "full_evaluations")
    private Integer fullEvaluations;

    @Column(name = "full_findings")
    private Integer fullFindings;

    @Column(name = "full_duration_ms")
    private Long fullDurationMs;

    // Incremental metrics
    @Column(name = "incr_candidates")
    private Integer incrCandidates;

    @Column(name = "incr_previously_associated")
    private Integer incrPreviouslyAssociated;

    @Column(name = "incr_newly_applicable")
    private Integer incrNewlyApplicable;

    @Column(name = "incr_evaluations")
    private Integer incrEvaluations;

    @Column(name = "incr_reused")
    private Integer incrReused;

    @Column(name = "incr_findings")
    private Integer incrFindings;

    @Column(name = "incr_candidate_selection_ms")
    private Long incrCandidateSelectionMs;

    @Column(name = "incr_evaluation_ms")
    private Long incrEvaluationMs;

    @Column(name = "incr_persistence_ms")
    private Long incrPersistenceMs;

    @Column(name = "incr_total_duration_ms")
    private Long incrTotalDurationMs;

    // Comparison metrics
    @Column(name = "evaluation_reduction_pct")
    private Double evaluationReductionPct;

    @Column(name = "time_reduction_pct")
    private Double timeReductionPct;

    @Column(name = "result_agreement_pct")
    private Double resultAgreementPct;

    @Column(name = "missed_affected_count")
    private Integer missedAffectedCount;

    @Column(name = "unnecessary_reevals")
    private Integer unnecessaryReevals;

    @Column(name = "matching_outcomes")
    private Integer matchingOutcomes;

    @Column(name = "differing_outcomes")
    private Integer differingOutcomes;

    @Column(name = "missing_outcomes")
    private Integer missingOutcomes;

    @Column(name = "extra_outcomes")
    private Integer extraOutcomes;

    @Column(name = "correctness_status", length = 30)
    private String correctnessStatus;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @PrePersist
    protected void onCreate() {
        if (startedAt == null) startedAt = Instant.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getExperimentName() { return experimentName; }
    public void setExperimentName(String experimentName) { this.experimentName = experimentName; }
    public Integer getCatalogueSize() { return catalogueSize; }
    public void setCatalogueSize(Integer catalogueSize) { this.catalogueSize = catalogueSize; }
    public Double getImpactRate() { return impactRate; }
    public void setImpactRate(Double impactRate) { this.impactRate = impactRate; }
    public String getScenario() { return scenario; }
    public void setScenario(String scenario) { this.scenario = scenario; }
    public Long getRandomSeed() { return randomSeed; }
    public void setRandomSeed(Long randomSeed) { this.randomSeed = randomSeed; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getFullTotalProducts() { return fullTotalProducts; }
    public void setFullTotalProducts(Integer fullTotalProducts) { this.fullTotalProducts = fullTotalProducts; }
    public Integer getFullApplicablePairs() { return fullApplicablePairs; }
    public void setFullApplicablePairs(Integer fullApplicablePairs) { this.fullApplicablePairs = fullApplicablePairs; }
    public Integer getFullEvaluations() { return fullEvaluations; }
    public void setFullEvaluations(Integer fullEvaluations) { this.fullEvaluations = fullEvaluations; }
    public Integer getFullFindings() { return fullFindings; }
    public void setFullFindings(Integer fullFindings) { this.fullFindings = fullFindings; }
    public Long getFullDurationMs() { return fullDurationMs; }
    public void setFullDurationMs(Long fullDurationMs) { this.fullDurationMs = fullDurationMs; }
    public Integer getIncrCandidates() { return incrCandidates; }
    public void setIncrCandidates(Integer incrCandidates) { this.incrCandidates = incrCandidates; }
    public Integer getIncrPreviouslyAssociated() { return incrPreviouslyAssociated; }
    public void setIncrPreviouslyAssociated(Integer incrPreviouslyAssociated) { this.incrPreviouslyAssociated = incrPreviouslyAssociated; }
    public Integer getIncrNewlyApplicable() { return incrNewlyApplicable; }
    public void setIncrNewlyApplicable(Integer incrNewlyApplicable) { this.incrNewlyApplicable = incrNewlyApplicable; }
    public Integer getIncrEvaluations() { return incrEvaluations; }
    public void setIncrEvaluations(Integer incrEvaluations) { this.incrEvaluations = incrEvaluations; }
    public Integer getIncrReused() { return incrReused; }
    public void setIncrReused(Integer incrReused) { this.incrReused = incrReused; }
    public Integer getIncrFindings() { return incrFindings; }
    public void setIncrFindings(Integer incrFindings) { this.incrFindings = incrFindings; }
    public Long getIncrCandidateSelectionMs() { return incrCandidateSelectionMs; }
    public void setIncrCandidateSelectionMs(Long incrCandidateSelectionMs) { this.incrCandidateSelectionMs = incrCandidateSelectionMs; }
    public Long getIncrEvaluationMs() { return incrEvaluationMs; }
    public void setIncrEvaluationMs(Long incrEvaluationMs) { this.incrEvaluationMs = incrEvaluationMs; }
    public Long getIncrPersistenceMs() { return incrPersistenceMs; }
    public void setIncrPersistenceMs(Long incrPersistenceMs) { this.incrPersistenceMs = incrPersistenceMs; }
    public Long getIncrTotalDurationMs() { return incrTotalDurationMs; }
    public void setIncrTotalDurationMs(Long incrTotalDurationMs) { this.incrTotalDurationMs = incrTotalDurationMs; }
    public Double getEvaluationReductionPct() { return evaluationReductionPct; }
    public void setEvaluationReductionPct(Double evaluationReductionPct) { this.evaluationReductionPct = evaluationReductionPct; }
    public Double getTimeReductionPct() { return timeReductionPct; }
    public void setTimeReductionPct(Double timeReductionPct) { this.timeReductionPct = timeReductionPct; }
    public Double getResultAgreementPct() { return resultAgreementPct; }
    public void setResultAgreementPct(Double resultAgreementPct) { this.resultAgreementPct = resultAgreementPct; }
    public Integer getMissedAffectedCount() { return missedAffectedCount; }
    public void setMissedAffectedCount(Integer missedAffectedCount) { this.missedAffectedCount = missedAffectedCount; }
    public Integer getUnnecessaryReevals() { return unnecessaryReevals; }
    public void setUnnecessaryReevals(Integer unnecessaryReevals) { this.unnecessaryReevals = unnecessaryReevals; }
    public Integer getMatchingOutcomes() { return matchingOutcomes; }
    public void setMatchingOutcomes(Integer matchingOutcomes) { this.matchingOutcomes = matchingOutcomes; }
    public Integer getDifferingOutcomes() { return differingOutcomes; }
    public void setDifferingOutcomes(Integer differingOutcomes) { this.differingOutcomes = differingOutcomes; }
    public Integer getMissingOutcomes() { return missingOutcomes; }
    public void setMissingOutcomes(Integer missingOutcomes) { this.missingOutcomes = missingOutcomes; }
    public Integer getExtraOutcomes() { return extraOutcomes; }
    public void setExtraOutcomes(Integer extraOutcomes) { this.extraOutcomes = extraOutcomes; }
    public String getCorrectnessStatus() { return correctnessStatus; }
    public void setCorrectnessStatus(String correctnessStatus) { this.correctnessStatus = correctnessStatus; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }
    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
}
