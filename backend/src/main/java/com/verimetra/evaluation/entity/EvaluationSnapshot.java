package com.verimetra.evaluation.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;

@Entity
@Table(name = "evaluation_snapshots")
public class EvaluationSnapshot {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "evaluation_id", nullable = false, unique = true)
    private Long evaluationId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "product_snapshot", columnDefinition = "JSONB", nullable = false)
    private String productSnapshot;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "declaration_snapshot", columnDefinition = "JSONB", nullable = false)
    private String declarationSnapshot;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "rule_version_snapshot", columnDefinition = "JSONB", nullable = false)
    private String ruleVersionSnapshot;

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
    public String getProductSnapshot() { return productSnapshot; }
    public void setProductSnapshot(String productSnapshot) { this.productSnapshot = productSnapshot; }
    public String getDeclarationSnapshot() { return declarationSnapshot; }
    public void setDeclarationSnapshot(String declarationSnapshot) { this.declarationSnapshot = declarationSnapshot; }
    public String getRuleVersionSnapshot() { return ruleVersionSnapshot; }
    public void setRuleVersionSnapshot(String ruleVersionSnapshot) { this.ruleVersionSnapshot = ruleVersionSnapshot; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
