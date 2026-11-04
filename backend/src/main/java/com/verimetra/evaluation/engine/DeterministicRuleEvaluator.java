package com.verimetra.evaluation.engine;

import com.verimetra.evaluation.entity.EvaluationFinding;
import com.verimetra.normalization.NormalizationService;
import com.verimetra.product.entity.Product;
import com.verimetra.product.entity.ProductDeclaration;
import com.verimetra.rule.entity.ComplianceRule;
import com.verimetra.rule.entity.ComplianceRuleVersion;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;

@Component
public class DeterministicRuleEvaluator {

    private final NormalizationService normalizationService;

    public DeterministicRuleEvaluator(NormalizationService normalizationService) {
        this.normalizationService = normalizationService;
    }

    public record EvaluatorResult(
            String status, // COMPLIANT, NON_COMPLIANT, NEEDS_REVIEW, NOT_APPLICABLE, INSUFFICIENT_DATA
            List<EvaluationFinding> findings
    ) {}

    /**
     * Evaluates a single product against a specific rule version deterministically.
     * This EXACT same method must be invoked by manual evaluation, full revalidation, and incremental revalidation.
     */
    public EvaluatorResult evaluate(
            Product product,
            List<ProductDeclaration> declarations,
            ComplianceRule rule,
            ComplianceRuleVersion ruleVersion,
            List<String> applicableCategories,
            Long evidenceId
    ) {
        // 1. Applicability Check
        if (applicableCategories == null || !applicableCategories.contains(product.getCategory().toUpperCase())) {
            return new EvaluatorResult("NOT_APPLICABLE", Collections.emptyList());
        }

        List<EvaluationFinding> findings = new ArrayList<>();
        String status = "COMPLIANT";

        String reqType = ruleVersion.getRequirementType();
        String reqField = ruleVersion.getRequiredField();
        String op = ruleVersion.getValidationOperator();
        String expectedVal = ruleVersion.getExpectedValue();

        // Separate listing vs package declarations
        Map<String, ProductDeclaration> listingMap = new HashMap<>();
        Map<String, ProductDeclaration> packageMap = new HashMap<>();

        for (ProductDeclaration d : declarations) {
            if ("LISTING".equalsIgnoreCase(d.getSource())) {
                listingMap.put(d.getFieldName().toLowerCase(), d);
            } else if ("PACKAGE".equalsIgnoreCase(d.getSource())) {
                packageMap.put(d.getFieldName().toLowerCase(), d);
            }
        }

        ProductDeclaration listingDecl = listingMap.get(reqField.toLowerCase());
        ProductDeclaration packageDecl = packageMap.get(reqField.toLowerCase());

        // Evaluation based on requirement type
        if ("CROSS_SOURCE_CONSISTENCY".equalsIgnoreCase(reqType) || "CROSS_SOURCE_MATCH".equalsIgnoreCase(op)) {
            // Check cross-source consistency between listing and package
            if (listingDecl == null && packageDecl == null) {
                status = "INSUFFICIENT_DATA";
                findings.add(createFinding(
                        "CROSS_SOURCE_CONSISTENCY", reqField, "Matching declaration on listing and package",
                        "None found", "INFO",
                        "Neither listing nor package declaration found for field: " + reqField,
                        rule.getLegalReference(), ruleVersion.getId(), evidenceId
                ));
            } else if (listingDecl == null) {
                status = "NEEDS_REVIEW";
                findings.add(createFinding(
                        "CROSS_SOURCE_CONSISTENCY", reqField, "Listing declaration present",
                        "Package value present (" + packageDecl.getFieldValue() + "), but listing value is missing",
                        "MINOR",
                        "Field " + reqField + " is present on package but missing from e-commerce listing.",
                        rule.getLegalReference(), ruleVersion.getId(), evidenceId
                ));
            } else if (packageDecl == null) {
                status = "NEEDS_REVIEW";
                findings.add(createFinding(
                        "CROSS_SOURCE_CONSISTENCY", reqField, "Package declaration present",
                        "Listing value present (" + listingDecl.getFieldValue() + "), but package value is missing",
                        "MINOR",
                        "Field " + reqField + " is present on e-commerce listing but physical package declaration was not confirmed.",
                        rule.getLegalReference(), ruleVersion.getId(), evidenceId
                ));
            } else {
                // Both are present, compare normalized values
                String normListing = listingDecl.getNormalizedValue();
                String normPackage = packageDecl.getNormalizedValue();

                boolean match = normalizationService.valuesMatch(normListing, normPackage);
                if (!match) {
                    status = "NEEDS_REVIEW";
                    findings.add(createFinding(
                            "CROSS_SOURCE_CONSISTENCY", reqField,
                            listingDecl.getFieldValue() + " (normalized: " + normListing + (listingDecl.getNormalizationUnit() != null ? " " + listingDecl.getNormalizationUnit() : "") + ")",
                            packageDecl.getFieldValue() + " (normalized: " + normPackage + (packageDecl.getNormalizationUnit() != null ? " " + packageDecl.getNormalizationUnit() : "") + ")",
                            ruleVersion.getSeverity(),
                            "Cross-source discrepancy detected for field '" + reqField + "': listing says '" + listingDecl.getFieldValue() + "' but package says '" + packageDecl.getFieldValue() + "'. Requires human inspection.",
                            rule.getLegalReference(), ruleVersion.getId(), evidenceId
                    ));
                }
            }
        } else if ("MANDATORY_DECLARATION".equalsIgnoreCase(reqType)) {
            // Declaration must exist and not be empty on at least one confirmed source (listing or package)
            boolean listingHas = (listingDecl != null && listingDecl.getFieldValue() != null && !listingDecl.getFieldValue().trim().isEmpty());
            boolean packageHas = (packageDecl != null && packageDecl.getFieldValue() != null && !packageDecl.getFieldValue().trim().isEmpty());

            if (!listingHas && !packageHas) {
                status = "NON_COMPLIANT";
                findings.add(createFinding(
                        "REGULATORY_REQUIREMENT", reqField, "Mandatory declaration must be present",
                        "Missing on both listing and package",
                        ruleVersion.getSeverity(),
                        "Mandatory declaration '" + reqField + "' is missing. According to " + (rule.getLegalReference() != null ? rule.getLegalReference() : "applicable rules") + ", this declaration is mandatory for " + product.getCategory() + " commodities.",
                        rule.getLegalReference(), ruleVersion.getId(), evidenceId
                ));
            } else {
                // If present on listing but not package, verify if package evidence was provided
                if (listingHas && !packageHas && packageMap.isEmpty()) {
                    // Only listing verified so far
                    status = "COMPLIANT";
                }
            }
        } else if ("VALUE_CONSTRAINT".equalsIgnoreCase(reqType)) {
            // Evaluate operator against expectedValue
            ProductDeclaration targetDecl = packageDecl != null ? packageDecl : listingDecl;
            if (targetDecl == null || targetDecl.getFieldValue() == null) {
                status = "INSUFFICIENT_DATA";
                findings.add(createFinding(
                        "REGULATORY_REQUIREMENT", reqField, expectedVal, "No value available",
                        "MAJOR", "Cannot evaluate constraint because field " + reqField + " is missing.",
                        rule.getLegalReference(), ruleVersion.getId(), evidenceId
                ));
            } else {
                boolean passes = evaluateOperator(targetDecl.getFieldValue(), targetDecl.getNormalizedValue(), op, expectedVal);
                if (!passes) {
                    status = "NON_COMPLIANT";
                    findings.add(createFinding(
                            "REGULATORY_REQUIREMENT", reqField, expectedVal, targetDecl.getFieldValue(),
                            ruleVersion.getSeverity(),
                            "Field '" + reqField + "' with value '" + targetDecl.getFieldValue() + "' failed validation constraint [" + op + " " + expectedVal + "].",
                            rule.getLegalReference(), ruleVersion.getId(), evidenceId
                    ));
                }
            }
        } else if ("FORMAT_REQUIREMENT".equalsIgnoreCase(reqType)) {
            ProductDeclaration targetDecl = packageDecl != null ? packageDecl : listingDecl;
            if (targetDecl == null || targetDecl.getFieldValue() == null) {
                status = "INSUFFICIENT_DATA";
            } else {
                boolean passes = evaluateOperator(targetDecl.getFieldValue(), targetDecl.getNormalizedValue(), op, expectedVal);
                if (!passes) {
                    status = "NON_COMPLIANT";
                    findings.add(createFinding(
                            "REGULATORY_REQUIREMENT", reqField, expectedVal, targetDecl.getFieldValue(),
                            ruleVersion.getSeverity(),
                            "Field '" + reqField + "' format failed requirement [" + op + "].",
                            rule.getLegalReference(), ruleVersion.getId(), evidenceId
                    ));
                }
            }
        }

        return new EvaluatorResult(status, findings);
    }

    private boolean evaluateOperator(String rawVal, String normVal, String operator, String expected) {
        if (operator == null) return true;
        String val = normVal != null ? normVal : (rawVal != null ? rawVal : "");

        switch (operator.toUpperCase()) {
            case "EXISTS":
            case "NOT_EMPTY":
                return !val.trim().isEmpty();
            case "EQUALS":
                return normalizationService.valuesMatch(val, expected);
            case "NOT_EQUALS":
                return !normalizationService.valuesMatch(val, expected);
            case "CONTAINS":
                return val.toLowerCase().contains(expected != null ? expected.toLowerCase() : "");
            case "MATCHES_REGEX":
                if (expected == null) return true;
                return val.matches(expected);
            case "IN_LIST":
                if (expected == null) return false;
                List<String> allowed = Arrays.stream(expected.split(","))
                        .map(String::trim)
                        .map(String::toLowerCase)
                        .toList();
                return allowed.contains(val.trim().toLowerCase());
            case "GREATER_THAN":
            case "LESS_THAN":
            case "GREATER_EQUAL":
            case "LESS_EQUAL":
                try {
                    BigDecimal num = new BigDecimal(val);
                    BigDecimal expNum = new BigDecimal(expected);
                    int cmp = num.compareTo(expNum);
                    return switch (operator.toUpperCase()) {
                        case "GREATER_THAN" -> cmp > 0;
                        case "LESS_THAN" -> cmp < 0;
                        case "GREATER_EQUAL" -> cmp >= 0;
                        case "LESS_EQUAL" -> cmp <= 0;
                        default -> false;
                    };
                } catch (Exception e) {
                    return false;
                }
            default:
                return true;
        }
    }

    private EvaluationFinding createFinding(
            String type, String field, String expected, String actual, String severity,
            String explanation, String ruleRef, Long ruleVersionId, Long evidenceId
    ) {
        EvaluationFinding f = new EvaluationFinding();
        f.setFindingType(type);
        f.setFieldName(field);
        f.setExpectedValue(expected);
        f.setActualValue(actual);
        f.setSeverity(severity);
        f.setExplanation(explanation);
        f.setRuleReference(ruleRef);
        f.setRuleVersionId(ruleVersionId);
        f.setEvidenceId(evidenceId);
        f.setReviewStatus("UNREVIEWED");
        return f;
    }
}
