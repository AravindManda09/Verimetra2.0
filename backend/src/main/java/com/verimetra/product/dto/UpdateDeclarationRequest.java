package com.verimetra.product.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record UpdateDeclarationRequest(
    @NotBlank(message = "Source is required")
    String source,
    List<DeclarationFieldRequest> fields
) {
    public record DeclarationFieldRequest(
        @NotBlank(message = "Field name is required")
        String fieldName,
        String fieldValue
    ) {}
}
