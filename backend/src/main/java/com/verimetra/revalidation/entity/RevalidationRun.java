package com.verimetra.revalidation.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "revalidation_runs")
public class RevalidationRun {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "run_type", nullable = false, length = 30)
    private String runType; // FULL, INCREMENTAL

    @Column(nullable = false, length = 20)
    private String status = "RUNNING"; // RUNNING, COMPLETED, FAILED, FALLBACK

    @Column(name = "trigger_rule_id")
    private Long triggerRuleId;

    @Column(name = "previous_version_id")
    private Long previousVersionId;

    @Column(name = "new_version_id")
    private Long newVersionId;

    @Column(name = "changed_attributes", columnDefinition = "TEXT")
    private String changedAttributes;

    @Column(name = "total_products_considered", nullable = false)
    private Integer totalProductsConsidered = 0;

    @Column(name = "previously_associated_count", nullable = false)
    private Integer previouslyAssociatedCount = 0;

    @Column(name = "newly_applicable_count", nullable = false)
    private Integer newlyApplicableCount = 0;

    @Column(name = "candidate_count", nullable = false)
    private Integer candidateCount = 0;

    @Column(name = "evaluations_performed", nullable = false)
    private Integer evaluationsPerformed = 0;

    @Column(name = "evaluations_reused", nullable = false)
    private Integer evaluationsReused = 0;

    @Column(name = "findings_generated", nullable = false)
    private Integer findingsGenerated = 0;

    @Column(name = "candidate_selection_ms")
    private Long candidateSelectionMs;

    @Column(name = "evaluation_ms")
    private Long evaluationMs;

    @Column(name = "persistence_ms")
    private Long persistenceMs;

    @Column(name = "total_duration_ms")
    private Long totalDurationMs;

    @Column(name = "used_fallback", nullable = false)
    private Boolean usedFallback = false;

    @Column(name = "fallback_reason", columnDefinition = "TEXT")
    private String fallbackReason;

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

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getRunType() { return runType; }
    public void setRunType(String runType) { this.runType = runType; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Long getTriggerRuleId() { return triggerRuleId; }
    public void setTriggerRuleId(Long triggerRuleId) { this.triggerRuleId = triggerRuleId; }
    public Long getPreviousVersionId() { return previousVersionId; }
    public void setPreviousVersionId(Long previousVersionId) { this.previousVersionId = previousVersionId; }
    public Long getNewVersionId() { return newVersionId; }
    public void setNewVersionId(Long newVersionId) { this.newVersionId = newVersionId; }
    public String getChangedAttributes() { return changedAttributes; }
    public void setChangedAttributes(String changedAttributes) { this.changedAttributes = changedAttributes; }
    public Integer getTotalProductsConsidered() { return totalProductsConsidered; }
    public void setTotalProductsConsidered(Integer totalProductsConsidered) { this.totalProductsConsidered = totalProductsConsidered; }
    public Integer getPreviouslyAssociatedCount() { return previouslyAssociatedCount; }
    public void setPreviouslyAssociatedCount(Integer previouslyAssociatedCount) { this.previouslyAssociatedCount = previouslyAssociatedCount; }
    public Integer getNewlyApplicableCount() { return newlyApplicableCount; }
    public void setNewlyApplicableCount(Integer newlyApplicableCount) { this.newlyApplicableCount = newlyApplicableCount; }
    public Integer getCandidateCount() { return candidateCount; }
    public void setCandidateCount(Integer candidateCount) { this.candidateCount = candidateCount; }
    public Integer getEvaluationsPerformed() { return evaluationsPerformed; }
    public void setEvaluationsPerformed(Integer evaluationsPerformed) { this.evaluationsPerformed = evaluationsPerformed; }
    public Integer getEvaluationsReused() { return evaluationsReused; }
    public void setEvaluationsReused(Integer evaluationsReused) { this.evaluationsReused = evaluationsReused; }
    public Integer getFindingsGenerated() { return findingsGenerated; }
    public void setFindingsGenerated(Integer findingsGenerated) { this.findingsGenerated = findingsGenerated; }
    public Long getCandidateSelectionMs() { return candidateSelectionMs; }
    public void setCandidateSelectionMs(Long candidateSelectionMs) { this.candidateSelectionMs = candidateSelectionMs; }
    public Long getEvaluationMs() { return evaluationMs; }
    public void setEvaluationMs(Long evaluationMs) { this.evaluationMs = evaluationMs; }
    public Long getPersistenceMs() { return persistenceMs; }
    public void setPersistenceMs(Long persistenceMs) { this.persistenceMs = persistenceMs; }
    public Long getTotalDurationMs() { return totalDurationMs; }
    public void setTotalDurationMs(Long totalDurationMs) { this.totalDurationMs = totalDurationMs; }
    public Boolean getUsedFallback() { return usedFallback; }
    public void setUsedFallback(Boolean usedFallback) { this.usedFallback = usedFallback; }
    public String getFallbackReason() { return fallbackReason; }
    public void setFallbackReason(String fallbackReason) { this.fallbackReason = fallbackReason; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }
    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
}
