package com.verimetra.rule.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "compliance_rules")
public class ComplianceRule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "rule_code", nullable = false, unique = true, length = 50)
    private String ruleCode;

    @Column(name = "rule_name", nullable = false, length = 500)
    private String ruleName;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "legal_reference", length = 500)
    private String legalReference;

    @Column(name = "source_url", length = 1000)
    private String sourceUrl;

    @Column(name = "source_verified_date")
    private LocalDate sourceVerifiedDate;

    @Column(name = "is_illustrative", nullable = false)
    private Boolean isIllustrative = false;

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
    public String getRuleCode() { return ruleCode; }
    public void setRuleCode(String ruleCode) { this.ruleCode = ruleCode; }
    public String getRuleName() { return ruleName; }
    public void setRuleName(String ruleName) { this.ruleName = ruleName; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getLegalReference() { return legalReference; }
    public void setLegalReference(String legalReference) { this.legalReference = legalReference; }
    public String getSourceUrl() { return sourceUrl; }
    public void setSourceUrl(String sourceUrl) { this.sourceUrl = sourceUrl; }
    public LocalDate getSourceVerifiedDate() { return sourceVerifiedDate; }
    public void setSourceVerifiedDate(LocalDate sourceVerifiedDate) { this.sourceVerifiedDate = sourceVerifiedDate; }
    public Boolean getIsIllustrative() { return isIllustrative; }
    public void setIsIllustrative(Boolean isIllustrative) { this.isIllustrative = isIllustrative; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
