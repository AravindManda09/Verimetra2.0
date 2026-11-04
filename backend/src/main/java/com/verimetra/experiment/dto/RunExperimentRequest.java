package com.verimetra.experiment.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RunExperimentRequest(
    String experimentName,
    @NotNull @Min(10) @Max(50000) Integer catalogueSize,
    @NotNull @Min(1) @Max(100) Double impactRate, // percentage, e.g. 5, 25, 50, 90
    @NotBlank String scenario, // APPLICABILITY_EXPANSION, APPLICABILITY_NARROWING, REQUIREMENT_MODIFICATION, THRESHOLD_MODIFICATION
    Long randomSeed
) {}
