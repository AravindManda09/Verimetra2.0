package com.verimetra.revalidation.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "revalidation_candidates")
public class RevalidationCandidate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "revalidation_run_id", nullable = false)
    private Long revalidationRunId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "candidate_reason", nullable = false, length = 50)
    private String candidateReason; // PREVIOUSLY_ASSOCIATED, NEWLY_APPLICABLE, DECLARATION_CHANGED, FULL_SCAN

    @Column(name = "was_evaluated", nullable = false)
    private Boolean wasEvaluated = false;

    @Column(name = "was_reused", nullable = false)
    private Boolean wasReused = false;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getRevalidationRunId() { return revalidationRunId; }
    public void setRevalidationRunId(Long revalidationRunId) { this.revalidationRunId = revalidationRunId; }
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public String getCandidateReason() { return candidateReason; }
    public void setCandidateReason(String candidateReason) { this.candidateReason = candidateReason; }
    public Boolean getWasEvaluated() { return wasEvaluated; }
    public void setWasEvaluated(Boolean wasEvaluated) { this.wasEvaluated = wasEvaluated; }
    public Boolean getWasReused() { return wasReused; }
    public void setWasReused(Boolean wasReused) { this.wasReused = wasReused; }
}
