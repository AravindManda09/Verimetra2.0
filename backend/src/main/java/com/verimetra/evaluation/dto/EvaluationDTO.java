package com.verimetra.evaluation.dto;

import java.time.Instant;
import java.util.List;

public record EvaluationDTO(
    Long id,
    Long productId,
    String productName,
    String category,
    Long ruleVersionId,
    String ruleCode,
    String ruleName,
    Integer versionNumber,
    String evaluationStatus,
    String evaluationType,
    String triggeredBy,
    Long revalidationRunId,
    Instant evaluatedAt,
    Long durationMs,
    List<EvaluationFindingDTO> findings
) {}
