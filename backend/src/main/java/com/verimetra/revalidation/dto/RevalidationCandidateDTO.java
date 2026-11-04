package com.verimetra.revalidation.dto;

public record RevalidationCandidateDTO(
    Long id,
    Long revalidationRunId,
    Long productId,
    String candidateReason,
    Boolean wasEvaluated,
    Boolean wasReused
) {}
