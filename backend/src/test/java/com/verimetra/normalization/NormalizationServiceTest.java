package com.verimetra.normalization;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NormalizationServiceTest {

    private NormalizationService normalizationService;

    @BeforeEach
    void setUp() {
        normalizationService = new NormalizationService();
    }

    @Test
    @DisplayName("Normalize weight in grams to kilograms")
    void testWeightGramsToKilograms() {
        NormalizationService.NormalizationResult res = normalizationService.normalize("net_quantity", "500 g");
        assertEquals("0.5", res.normalizedValue());
        assertEquals("kg", res.unit());

        NormalizationService.NormalizationResult res2 = normalizationService.normalize("net_quantity", "1000 gm");
        assertEquals("1", res2.normalizedValue());
        assertEquals("kg", res2.unit());

        NormalizationService.NormalizationResult res3 = normalizationService.normalize("net_quantity", "5 kg");
        assertEquals("5", res3.normalizedValue());
        assertEquals("kg", res3.unit());
    }

    @Test
    @DisplayName("Normalize volume in milliliters to liters")
    void testVolumeMillilitersToLiters() {
        NormalizationService.NormalizationResult res = normalizationService.normalize("net_quantity", "1000 ml");
        assertEquals("1", res.normalizedValue());
        assertEquals("L", res.unit());

        NormalizationService.NormalizationResult res2 = normalizationService.normalize("net_quantity", "250 ml");
        assertEquals("0.25", res2.normalizedValue());
        assertEquals("L", res2.unit());

        NormalizationService.NormalizationResult res3 = normalizationService.normalize("net_quantity", "2 L");
        assertEquals("2", res3.normalizedValue());
        assertEquals("L", res3.unit());
    }

    @Test
    @DisplayName("Normalize MRP with currency symbols")
    void testMrpNormalization() {
        NormalizationService.NormalizationResult res1 = normalizationService.normalize("mrp", "Rs. 185.00");
        assertEquals("185.00", res1.normalizedValue());
        assertEquals("INR", res1.unit());

        NormalizationService.NormalizationResult res2 = normalizationService.normalize("mrp", "₹ 249");
        assertEquals("249.00", res2.normalizedValue());
        assertEquals("INR", res2.unit());

        NormalizationService.NormalizationResult res3 = normalizationService.normalize("mrp", "INR 1499.50");
        assertEquals("1499.50", res3.normalizedValue());
        assertEquals("INR", res3.unit());
    }

    @Test
    @DisplayName("Normalize text with whitespace and case insensitivity")
    void testTextNormalization() {
        NormalizationService.NormalizationResult res = normalizationService.normalize("manufacturer_name", "  Tata   Sampann  Pvt Ltd  ");
        assertEquals("tata sampann pvt ltd", res.normalizedValue());
        assertNull(res.unit());
    }

    @Test
    @DisplayName("Compare normalized values for equality (numeric and text)")
    void testValuesMatch() {
        assertTrue(normalizationService.valuesMatch("0.5", "0.50"));
        assertTrue(normalizationService.valuesMatch("185.00", "185"));
        assertTrue(normalizationService.valuesMatch("india", "INDIA"));
        assertFalse(normalizationService.valuesMatch("100", "150"));
        assertFalse(normalizationService.valuesMatch("india", "china"));
        assertTrue(normalizationService.valuesMatch(null, null));
        assertFalse(normalizationService.valuesMatch("0.5", null));
    }
}
