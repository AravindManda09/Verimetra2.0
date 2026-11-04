package com.verimetra.revalidation.dto;

import java.time.Instant;

public record RevalidationRunDTO(
    Long id,
    String runType,
    String status,
    Long triggerRuleId,
    String triggerRuleCode,
    String triggerRuleName,
    Long previousVersionId,
    Integer previousVersionNumber,
    Long newVersionId,
    Integer newVersionNumber,
    String changedAttributes,
    Integer totalProductsConsidered,
    Integer previouslyAssociatedCount,
    Integer newlyApplicableCount,
    Integer candidateCount,
    Integer evaluationsPerformed,
    Integer evaluationsReused,
    Integer findingsGenerated,
    Long candidateSelectionMs,
    Long evaluationMs,
    Long persistenceMs,
    Long totalDurationMs,
    Boolean usedFallback,
    String fallbackReason,
    String errorMessage,
    Instant startedAt,
    Instant completedAt
) {}
