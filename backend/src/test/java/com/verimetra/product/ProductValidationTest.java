package com.verimetra.product;

import com.verimetra.product.dto.CreateProductRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ProductValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("Valid product request passes validation")
    void testValidProduct() {
        CreateProductRequest req = new CreateProductRequest(
                "Organic Wheat Flour", "FOOD-001", "FOOD",
                "Nature Fresh Ltd", "India", "Organic stone ground flour"
        );
        Set<ConstraintViolation<CreateProductRequest>> violations = validator.validate(req);
        assertTrue(violations.isEmpty(), "Valid product request should have no validation violations");
    }

    @Test
    @DisplayName("Product with invalid category fails validation")
    void testInvalidCategory() {
        CreateProductRequest req = new CreateProductRequest(
                "Mystery Item", "SKU-999", "AUTOMOBILE",
                "Unknown Corp", "India", "Description"
        );
        Set<ConstraintViolation<CreateProductRequest>> violations = validator.validate(req);
        assertFalse(violations.isEmpty(), "Invalid category should trigger validation constraint");
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("category")));
    }

    @Test
    @DisplayName("Product with blank name fails validation")
    void testBlankProductName() {
        CreateProductRequest req = new CreateProductRequest(
                "", "FOOD-002", "FOOD",
                "Nature Fresh Ltd", "India", "Description"
        );
        Set<ConstraintViolation<CreateProductRequest>> violations = validator.validate(req);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("productName")));
    }
}
