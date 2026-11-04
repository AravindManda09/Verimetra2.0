package com.verimetra.evaluation.dto;

import java.time.Instant;
import java.util.List;

public record EvaluationFindingDTO(
    Long id,
    Long evaluationId,
    String findingType,
    String fieldName,
    String expectedValue,
    String actualValue,
    String severity,
    String explanation,
    String ruleReference,
    Long ruleVersionId,
    Long evidenceId,
    String reviewStatus,
    Instant createdAt
) {}
