package com.verimetra.revalidation.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "product_rule_dependencies")
public class ProductRuleDependency {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "rule_id", nullable = false)
    private Long ruleId;

    @Column(name = "rule_version_id", nullable = false)
    private Long ruleVersionId;

    @Column(name = "last_evaluation_id")
    private Long lastEvaluationId;

    @Column(name = "dependency_status", nullable = false, length = 20)
    private String dependencyStatus = "ACTIVE"; // ACTIVE, STALE, REMOVED

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public Long getRuleId() { return ruleId; }
    public void setRuleId(Long ruleId) { this.ruleId = ruleId; }
    public Long getRuleVersionId() { return ruleVersionId; }
    public void setRuleVersionId(Long ruleVersionId) { this.ruleVersionId = ruleVersionId; }
    public Long getLastEvaluationId() { return lastEvaluationId; }
    public void setLastEvaluationId(Long lastEvaluationId) { this.lastEvaluationId = lastEvaluationId; }
    public String getDependencyStatus() { return dependencyStatus; }
    public void setDependencyStatus(String dependencyStatus) { this.dependencyStatus = dependencyStatus; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
