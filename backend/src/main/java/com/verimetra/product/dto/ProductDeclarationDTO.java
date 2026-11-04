package com.verimetra.product.dto;

import java.time.Instant;

public record ProductDeclarationDTO(
    Long id,
    Long productId,
    String source,
    String fieldName,
    String fieldValue,
    String normalizedValue,
    String normalizationUnit,
    Boolean confirmed,
    Instant confirmedAt,
    Instant createdAt
) {}
