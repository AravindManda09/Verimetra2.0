package com.verimetra.rule.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record RuleVersionDTO(
    Long id, Long ruleId, Integer versionNumber, String requirementType,
    String requiredField, String validationOperator, String expectedValue,
    String severity, LocalDate effectiveFrom, LocalDate effectiveTo,
    String status, String changeSummary, Instant createdAt,
    List<String> applicableCategories
) {}
