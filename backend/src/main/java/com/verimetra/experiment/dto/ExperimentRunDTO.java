package com.verimetra.experiment.dto;

import java.time.Instant;

public record ExperimentRunDTO(
    Long id,
    String experimentName,
    Integer catalogueSize,
    Double impactRate,
    String scenario,
    Long randomSeed,
    String status,
    
    // Full
    Integer fullTotalProducts,
    Integer fullApplicablePairs,
    Integer fullEvaluations,
    Integer fullFindings,
    Long fullDurationMs,
    
    // Incremental
    Integer incrCandidates,
    Integer incrPreviouslyAssociated,
    Integer incrNewlyApplicable,
    Integer incrEvaluations,
    Integer incrReused,
    Integer incrFindings,
    Long incrCandidateSelectionMs,
    Long incrEvaluationMs,
    Long incrPersistenceMs,
    Long incrTotalDurationMs,
    
    // Comparison
    Double evaluationReductionPct,
    Double timeReductionPct,
    Double resultAgreementPct,
    Integer missedAffectedCount,
    Integer unnecessaryReevals,
    Integer matchingOutcomes,
    Integer differingOutcomes,
    Integer missingOutcomes,
    Integer extraOutcomes,
    String correctnessStatus,
    String errorMessage,
    Instant startedAt,
    Instant completedAt
) {}
