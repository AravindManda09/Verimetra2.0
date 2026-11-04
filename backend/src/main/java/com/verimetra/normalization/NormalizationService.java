package com.verimetra.normalization;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class NormalizationService {

    public record NormalizationResult(String normalizedValue, String unit) {}

    private static final Pattern QUANTITY_PATTERN = Pattern.compile(
            "^\\s*(\\d+(?:\\.\\d+)?)\\s*(g|gm|gms|gram|grams|kg|kgs|kilogram|kilograms|ml|millilitre|milliliter|l|ltr|litre|liter|litres|liters|oz|lb|lbs)\\s*$",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern MRP_PATTERN = Pattern.compile(
            "^\\s*(?:Rs\\.?|₹|INR)?\\s*(\\d+(?:\\.\\d{1,2})?)\\s*$",
            Pattern.CASE_INSENSITIVE
    );

    private static final Map<String, BigDecimal> WEIGHT_TO_KG = Map.ofEntries(
            Map.entry("g", new BigDecimal("0.001")),
            Map.entry("gm", new BigDecimal("0.001")),
            Map.entry("gms", new BigDecimal("0.001")),
            Map.entry("gram", new BigDecimal("0.001")),
            Map.entry("grams", new BigDecimal("0.001")),
            Map.entry("kg", BigDecimal.ONE),
            Map.entry("kgs", BigDecimal.ONE),
            Map.entry("kilogram", BigDecimal.ONE),
            Map.entry("kilograms", BigDecimal.ONE)
    );

    private static final Map<String, BigDecimal> VOLUME_TO_L = Map.ofEntries(
            Map.entry("ml", new BigDecimal("0.001")),
            Map.entry("millilitre", new BigDecimal("0.001")),
            Map.entry("milliliter", new BigDecimal("0.001")),
            Map.entry("l", BigDecimal.ONE),
            Map.entry("ltr", BigDecimal.ONE),
            Map.entry("litre", BigDecimal.ONE),
            Map.entry("liter", BigDecimal.ONE),
            Map.entry("litres", BigDecimal.ONE),
            Map.entry("liters", BigDecimal.ONE)
    );

    public NormalizationResult normalize(String fieldName, String value) {
        if (value == null || value.isBlank()) {
            return new NormalizationResult(null, null);
        }
        
        return switch (fieldName.toLowerCase()) {
            case "mrp" -> normalizeMrp(value);
            case "net_quantity" -> normalizeQuantity(value);
            default -> normalizeText(value);
        };
    }

    public NormalizationResult normalizeMrp(String value) {
        if (value == null || value.isBlank()) return new NormalizationResult(null, null);
        
        Matcher matcher = MRP_PATTERN.matcher(value.trim());
        if (matcher.matches()) {
            BigDecimal amount = new BigDecimal(matcher.group(1)).setScale(2, RoundingMode.HALF_UP);
            return new NormalizationResult(amount.toPlainString(), "INR");
        }
        // Try stripping non-numeric prefix
        String cleaned = value.replaceAll("[^\\d.]", "").trim();
        if (!cleaned.isEmpty()) {
            try {
                BigDecimal amount = new BigDecimal(cleaned).setScale(2, RoundingMode.HALF_UP);
                return new NormalizationResult(amount.toPlainString(), "INR");
            } catch (NumberFormatException e) {
                // fall through
            }
        }
        return new NormalizationResult(value.trim().toLowerCase(), null);
    }

    public NormalizationResult normalizeQuantity(String value) {
        if (value == null || value.isBlank()) return new NormalizationResult(null, null);
        
        Matcher matcher = QUANTITY_PATTERN.matcher(value.trim());
        if (!matcher.matches()) {
            return new NormalizationResult(value.trim().toLowerCase(), null);
        }
        
        BigDecimal numericValue = new BigDecimal(matcher.group(1));
        String unit = matcher.group(2).toLowerCase();
        
        // Normalize weight to kg
        if (WEIGHT_TO_KG.containsKey(unit)) {
            BigDecimal factor = WEIGHT_TO_KG.get(unit);
            BigDecimal normalizedVal = numericValue.multiply(factor).stripTrailingZeros();
            return new NormalizationResult(normalizedVal.toPlainString(), "kg");
        }
        
        // Normalize volume to L
        if (VOLUME_TO_L.containsKey(unit)) {
            BigDecimal factor = VOLUME_TO_L.get(unit);
            BigDecimal normalizedVal = numericValue.multiply(factor).stripTrailingZeros();
            return new NormalizationResult(normalizedVal.toPlainString(), "L");
        }
        
        return new NormalizationResult(value.trim().toLowerCase(), null);
    }

    public NormalizationResult normalizeText(String value) {
        if (value == null || value.isBlank()) return new NormalizationResult(null, null);
        String normalized = value.trim().replaceAll("\\s+", " ").toLowerCase();
        return new NormalizationResult(normalized, null);
    }

    /**
     * Compare two normalized values for equality.
     */
    public boolean valuesMatch(String normalizedA, String normalizedB) {
        if (normalizedA == null && normalizedB == null) return true;
        if (normalizedA == null || normalizedB == null) return false;
        
        // Try numeric comparison first
        try {
            BigDecimal a = new BigDecimal(normalizedA);
            BigDecimal b = new BigDecimal(normalizedB);
            return a.compareTo(b) == 0;
        } catch (NumberFormatException e) {
            // Fall back to string comparison
            return normalizedA.equalsIgnoreCase(normalizedB);
        }
    }
}
