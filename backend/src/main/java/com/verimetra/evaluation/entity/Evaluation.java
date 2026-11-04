package com.verimetra.evaluation.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "evaluations")
public class Evaluation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "rule_version_id", nullable = false)
    private Long ruleVersionId;

    @Column(name = "evaluation_status", nullable = false, length = 30)
    private String evaluationStatus; // COMPLIANT, NON_COMPLIANT, NEEDS_REVIEW, NOT_APPLICABLE, INSUFFICIENT_DATA

    @Column(name = "evaluation_type", nullable = false, length = 30)
    private String evaluationType = "MANUAL"; // MANUAL, FULL_REVALIDATION, INCREMENTAL_REVALIDATION, EXPERIMENT

    @Column(name = "triggered_by", length = 50)
    private String triggeredBy;

    @Column(name = "revalidation_run_id")
    private Long revalidationRunId;

    @Column(name = "evaluated_at", nullable = false)
    private Instant evaluatedAt;

    @Column(name = "duration_ms")
    private Long durationMs;

    @PrePersist
    protected void onCreate() {
        if (evaluatedAt == null) evaluatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public Long getRuleVersionId() { return ruleVersionId; }
    public void setRuleVersionId(Long ruleVersionId) { this.ruleVersionId = ruleVersionId; }
    public String getEvaluationStatus() { return evaluationStatus; }
    public void setEvaluationStatus(String evaluationStatus) { this.evaluationStatus = evaluationStatus; }
    public String getEvaluationType() { return evaluationType; }
    public void setEvaluationType(String evaluationType) { this.evaluationType = evaluationType; }
    public String getTriggeredBy() { return triggeredBy; }
    public void setTriggeredBy(String triggeredBy) { this.triggeredBy = triggeredBy; }
    public Long getRevalidationRunId() { return revalidationRunId; }
    public void setRevalidationRunId(Long revalidationRunId) { this.revalidationRunId = revalidationRunId; }
    public Instant getEvaluatedAt() { return evaluatedAt; }
    public void setEvaluatedAt(Instant evaluatedAt) { this.evaluatedAt = evaluatedAt; }
    public Long getDurationMs() { return durationMs; }
    public void setDurationMs(Long durationMs) { this.durationMs = durationMs; }
}
