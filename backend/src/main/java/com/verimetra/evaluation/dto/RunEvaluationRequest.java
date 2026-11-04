package com.verimetra.evaluation.dto;

import java.util.List;

public record RunEvaluationRequest(
    Long productId,
    List<Long> ruleVersionIds // if null/empty, evaluate all active applicable rules
) {}
