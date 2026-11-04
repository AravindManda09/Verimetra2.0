package com.verimetra.evaluation.engine;

import com.verimetra.normalization.NormalizationService;
import com.verimetra.product.entity.Product;
import com.verimetra.product.entity.ProductDeclaration;
import com.verimetra.rule.entity.ComplianceRule;
import com.verimetra.rule.entity.ComplianceRuleVersion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DeterministicRuleEvaluatorTest {

    private DeterministicRuleEvaluator evaluator;
    private NormalizationService normalizationService;

    @BeforeEach
    void setUp() {
        normalizationService = new NormalizationService();
        evaluator = new DeterministicRuleEvaluator(normalizationService);
    }

    @Test
    @DisplayName("Evaluate NOT_APPLICABLE when product category is outside rule applicability")
    void testNotApplicableCategory() {
        Product p = new Product();
        p.setId(1L);
        p.setCategory("ELECTRONICS");
        p.setProductName("Laptop");

        ComplianceRule rule = new ComplianceRule();
        rule.setId(1L);
        rule.setRuleCode("FOOD-ONLY-RULE");

        ComplianceRuleVersion version = new ComplianceRuleVersion();
        version.setId(10L);
        version.setRuleId(1L);
        version.setRequirementType("MANDATORY_DECLARATION");
        version.setRequiredField("mrp");

        List<String> applicableCategories = List.of("FOOD");

        DeterministicRuleEvaluator.EvaluatorResult res = evaluator.evaluate(
                p, List.of(), rule, version, applicableCategories, null
        );

        assertEquals("NOT_APPLICABLE", res.status());
        assertTrue(res.findings().isEmpty());
    }

    @Test
    @DisplayName("Evaluate MANDATORY_DECLARATION passes when field is present on listing")
    void testMandatoryDeclarationPasses() {
        Product p = new Product();
        p.setId(1L);
        p.setCategory("FOOD");
        p.setProductName("Rice");

        ComplianceRule rule = new ComplianceRule();
        rule.setId(1L);

        ComplianceRuleVersion version = new ComplianceRuleVersion();
        version.setId(10L);
        version.setRequirementType("MANDATORY_DECLARATION");
        version.setRequiredField("mrp");
        version.setValidationOperator("NOT_EMPTY");
        version.setSeverity("CRITICAL");

        ProductDeclaration decl = new ProductDeclaration();
        decl.setSource("LISTING");
        decl.setFieldName("mrp");
        decl.setFieldValue("150.00");

        DeterministicRuleEvaluator.EvaluatorResult res = evaluator.evaluate(
                p, List.of(decl), rule, version, List.of("FOOD"), null
        );

        assertEquals("COMPLIANT", res.status());
        assertTrue(res.findings().isEmpty());
    }

    @Test
    @DisplayName("Evaluate MANDATORY_DECLARATION fails and generates REGULATORY_REQUIREMENT finding when missing")
    void testMandatoryDeclarationFailsWhenMissing() {
        Product p = new Product();
        p.setId(1L);
        p.setCategory("FOOD");
        p.setProductName("Rice");

        ComplianceRule rule = new ComplianceRule();
        rule.setId(1L);
        rule.setLegalReference("Rule 6(1)(d)");

        ComplianceRuleVersion version = new ComplianceRuleVersion();
        version.setId(10L);
        version.setRequirementType("MANDATORY_DECLARATION");
        version.setRequiredField("mrp");
        version.setValidationOperator("NOT_EMPTY");
        version.setSeverity("CRITICAL");

        DeterministicRuleEvaluator.EvaluatorResult res = evaluator.evaluate(
                p, List.of(), rule, version, List.of("FOOD"), null
        );

        assertEquals("NON_COMPLIANT", res.status());
        assertEquals(1, res.findings().size());
        assertEquals("REGULATORY_REQUIREMENT", res.findings().get(0).getFindingType());
        assertEquals("CRITICAL", res.findings().get(0).getSeverity());
    }

    @Test
    @DisplayName("Evaluate CROSS_SOURCE_CONSISTENCY detects mismatch between listing and package")
    void testCrossSourceMismatch() {
        Product p = new Product();
        p.setId(1L);
        p.setCategory("FOOD");
        p.setProductName("Tea");

        ComplianceRule rule = new ComplianceRule();
        rule.setId(2L);

        ComplianceRuleVersion version = new ComplianceRuleVersion();
        version.setId(20L);
        version.setRequirementType("CROSS_SOURCE_CONSISTENCY");
        version.setRequiredField("net_quantity");
        version.setValidationOperator("CROSS_SOURCE_MATCH");
        version.setSeverity("MAJOR");

        ProductDeclaration listing = new ProductDeclaration();
        listing.setSource("LISTING");
        listing.setFieldName("net_quantity");
        listing.setFieldValue("100 g");
        listing.setNormalizedValue("0.1");
        listing.setNormalizationUnit("kg");

        ProductDeclaration pkg = new ProductDeclaration();
        pkg.setSource("PACKAGE");
        pkg.setFieldName("net_quantity");
        pkg.setFieldValue("75 g");
        pkg.setNormalizedValue("0.075");
        pkg.setNormalizationUnit("kg");

        DeterministicRuleEvaluator.EvaluatorResult res = evaluator.evaluate(
                p, List.of(listing, pkg), rule, version, List.of("FOOD"), 55L
        );

        assertEquals("NEEDS_REVIEW", res.status());
        assertEquals(1, res.findings().size());
        assertEquals("CROSS_SOURCE_CONSISTENCY", res.findings().get(0).getFindingType());
        assertEquals("MAJOR", res.findings().get(0).getSeverity());
        assertEquals(55L, res.findings().get(0).getEvidenceId());
    }

    @Test
    @DisplayName("Evaluate CROSS_SOURCE_CONSISTENCY passes when normalized values match despite different unit formats")
    void testCrossSourceMatchesNormalized() {
        Product p = new Product();
        p.setId(1L);
        p.setCategory("FOOD");
        p.setProductName("Flour");

        ComplianceRule rule = new ComplianceRule();
        rule.setId(2L);

        ComplianceRuleVersion version = new ComplianceRuleVersion();
        version.setId(20L);
        version.setRequirementType("CROSS_SOURCE_CONSISTENCY");
        version.setRequiredField("net_quantity");
        version.setValidationOperator("CROSS_SOURCE_MATCH");
        version.setSeverity("MAJOR");

        ProductDeclaration listing = new ProductDeclaration();
        listing.setSource("LISTING");
        listing.setFieldName("net_quantity");
        listing.setFieldValue("1 kg");
        listing.setNormalizedValue("1");
        listing.setNormalizationUnit("kg");

        ProductDeclaration pkg = new ProductDeclaration();
        pkg.setSource("PACKAGE");
        pkg.setFieldName("net_quantity");
        pkg.setFieldValue("1000 g");
        pkg.setNormalizedValue("1");
        pkg.setNormalizationUnit("kg");

        DeterministicRuleEvaluator.EvaluatorResult res = evaluator.evaluate(
                p, List.of(listing, pkg), rule, version, List.of("FOOD"), null
        );

        assertEquals("COMPLIANT", res.status());
        assertTrue(res.findings().isEmpty());
    }
}
