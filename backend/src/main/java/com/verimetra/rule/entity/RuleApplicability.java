package com.verimetra.rule.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "rule_applicability")
public class RuleApplicability {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "rule_version_id", nullable = false)
    private Long ruleVersionId;

    @Column(nullable = false, length = 50)
    private String category;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getRuleVersionId() { return ruleVersionId; }
    public void setRuleVersionId(Long ruleVersionId) { this.ruleVersionId = ruleVersionId; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
}
