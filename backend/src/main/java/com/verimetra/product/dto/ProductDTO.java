package com.verimetra.product.dto;

import java.time.Instant;

public record ProductDTO(
    Long id,
    String productName,
    String sku,
    String category,
    String manufacturerName,
    String countryOfOrigin,
    String description,
    Instant createdAt,
    Instant updatedAt
) {}
