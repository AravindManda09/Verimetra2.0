package com.verimetra.evaluation.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "evaluation_findings")
public class EvaluationFinding {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "evaluation_id", nullable = false)
    private Long evaluationId;

    @Column(name = "finding_type", nullable = false, length = 40)
    private String findingType; // REGULATORY_REQUIREMENT, CROSS_SOURCE_CONSISTENCY

    @Column(name = "field_name", nullable = false, length = 100)
    private String fieldName;

    @Column(name = "expected_value", length = 1000)
    private String expectedValue;

    @Column(name = "actual_value", length = 1000)
    private String actualValue;

    @Column(nullable = false, length = 20)
    private String severity; // CRITICAL, MAJOR, MINOR, INFO

    @Column(columnDefinition = "TEXT", nullable = false)
    private String explanation;

    @Column(name = "rule_reference", length = 500)
    private String ruleReference;

    @Column(name = "rule_version_id")
    private Long ruleVersionId;

    @Column(name = "evidence_id")
    private Long evidenceId;

    @Column(name = "review_status", nullable = false, length = 20)
    private String reviewStatus = "UNREVIEWED"; // UNREVIEWED, ACKNOWLEDGED, DISPUTED, RESOLVED

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getEvaluationId() { return evaluationId; }
    public void setEvaluationId(Long evaluationId) { this.evaluationId = evaluationId; }
    public String getFindingType() { return findingType; }
    public void setFindingType(String findingType) { this.findingType = findingType; }
    public String getFieldName() { return fieldName; }
    public void setFieldName(String fieldName) { this.fieldName = fieldName; }
    public String getExpectedValue() { return expectedValue; }
    public void setExpectedValue(String expectedValue) { this.expectedValue = expectedValue; }
    public String getActualValue() { return actualValue; }
    public void setActualValue(String actualValue) { this.actualValue = actualValue; }
    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }
    public String getExplanation() { return explanation; }
    public void setExplanation(String explanation) { this.explanation = explanation; }
    public String getRuleReference() { return ruleReference; }
    public void setRuleReference(String ruleReference) { this.ruleReference = ruleReference; }
    public Long getRuleVersionId() { return ruleVersionId; }
    public void setRuleVersionId(Long ruleVersionId) { this.ruleVersionId = ruleVersionId; }
    public Long getEvidenceId() { return evidenceId; }
    public void setEvidenceId(Long evidenceId) { this.evidenceId = evidenceId; }
    public String getReviewStatus() { return reviewStatus; }
    public void setReviewStatus(String reviewStatus) { this.reviewStatus = reviewStatus; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
