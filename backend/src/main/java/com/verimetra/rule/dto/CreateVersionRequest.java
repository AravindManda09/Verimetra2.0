package com.verimetra.rule.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

public record CreateVersionRequest(
    @NotBlank String requirementType,
    @NotBlank String requiredField,
    @NotBlank String validationOperator,
    String expectedValue,
    String severity,
    @NotNull LocalDate effectiveFrom,
    LocalDate effectiveTo,
    String changeSummary,
    @NotEmpty List<String> applicableCategories
) {}
