package com.verimetra.revalidation.dto;

import jakarta.validation.constraints.NotNull;

public record RevalidationRequest(
    @NotNull Long ruleId,
    @NotNull Long newVersionId
) {}
