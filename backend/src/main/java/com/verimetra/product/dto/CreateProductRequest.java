package com.verimetra.product.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateProductRequest(
    @NotBlank(message = "Product name is required")
    @Size(max = 500, message = "Product name must be less than 500 characters")
    String productName,
    
    @Size(max = 100, message = "SKU must be less than 100 characters")
    String sku,
    
    @NotBlank(message = "Category is required")
    @Pattern(regexp = "FOOD|COSMETICS|ELECTRONICS|HOUSEHOLD", message = "Category must be FOOD, COSMETICS, ELECTRONICS, or HOUSEHOLD")
    String category,
    
    @Size(max = 500, message = "Manufacturer name must be less than 500 characters")
    String manufacturerName,
    
    @Size(max = 200, message = "Country of origin must be less than 200 characters")
    String countryOfOrigin,
    
    String description
) {}
