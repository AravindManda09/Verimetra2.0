package com.verimetra.rule.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record RuleDTO(
    Long id, String ruleCode, String ruleName, String description,
    String legalReference, String sourceUrl, LocalDate sourceVerifiedDate,
    Boolean isIllustrative, Instant createdAt, Instant updatedAt,
    RuleVersionDTO activeVersion, List<RuleVersionDTO> versions
) {}
