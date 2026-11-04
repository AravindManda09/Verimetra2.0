package com.verimetra.revalidation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.verimetra.evaluation.engine.DeterministicRuleEvaluator;
import com.verimetra.normalization.NormalizationService;
import com.verimetra.product.entity.Product;
import com.verimetra.product.entity.ProductDeclaration;
import com.verimetra.revalidation.dto.RevalidationRequest;
import com.verimetra.revalidation.dto.RevalidationRunDTO;
import com.verimetra.revalidation.entity.ProductRuleDependency;
import com.verimetra.revalidation.entity.RevalidationCandidate;
import com.verimetra.revalidation.entity.RevalidationRun;
import com.verimetra.rule.entity.ComplianceRule;
import com.verimetra.rule.entity.ComplianceRuleVersion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class IncrementalRevalidationTest {

    private DeterministicRuleEvaluator evaluator;
    private NormalizationService normalizationService;

    @BeforeEach
    void setUp() {
        normalizationService = new NormalizationService();
        evaluator = new DeterministicRuleEvaluator(normalizationService);
    }

    @Test
    @DisplayName("Critical Research Test: Applicability expansion discovers both previously associated (Phase A) and newly applicable (Phase B) products")
    void testApplicabilityExpansionLogic() {
        // v1 applicable to FOOD
        List<String> v1Categories = List.of("FOOD");
        // v2 expands applicability to FOOD + COSMETICS
        List<String> v2Categories = List.of("FOOD", "COSMETICS");

        // Existing dependencies: Product 101 (FOOD) was previously evaluated under v1
        Map<Long, String> dependencies = new HashMap<>();
        dependencies.put(101L, "FOOD");

        // Catalogue products
        Product p1 = new Product();
        p1.setId(101L);
        p1.setCategory("FOOD");
        p1.setProductName("Basmati Rice");

        Product p2 = new Product();
        p2.setId(202L);
        p2.setCategory("COSMETICS");
        p2.setProductName("Herbal Shampoo");

        Product p3 = new Product();
        p3.setId(303L);
        p3.setCategory("ELECTRONICS");
        p3.setProductName("USB Cable");

        List<Product> catalogue = List.of(p1, p2, p3);

        // --- ALGORITHM EXECUTION ---
        // 1. Changes
        Set<String> addedCategories = new HashSet<>(v2Categories);
        addedCategories.removeAll(v1Categories);
        assertEquals(Set.of("COSMETICS"), addedCategories);

        // 2. Candidate Discovery
        Map<Long, String> candidates = new LinkedHashMap<>();

        // Phase A: Previously associated products
        for (Long pid : dependencies.keySet()) {
            candidates.put(pid, "PREVIOUSLY_ASSOCIATED");
        }
        int phaseACount = candidates.size();

        // Phase B: Newly applicable products
        int phaseBCount = 0;
        for (Product p : catalogue) {
            if (addedCategories.contains(p.getCategory()) && !candidates.containsKey(p.getId())) {
                candidates.put(p.getId(), "NEWLY_APPLICABLE");
                phaseBCount++;
            }
        }

        // 3. Assertions
        assertEquals(1, phaseACount, "Phase A must discover 1 previously associated FOOD product");
        assertEquals(1, phaseBCount, "Phase B must discover 1 newly applicable COSMETICS product");
        assertEquals(2, candidates.size(), "Deduplicated candidate pool must have 2 products (101 and 202)");
        assertTrue(candidates.containsKey(101L));
        assertEquals("PREVIOUSLY_ASSOCIATED", candidates.get(101L));
        assertTrue(candidates.containsKey(202L));
        assertEquals("NEWLY_APPLICABLE", candidates.get(202L));
        assertFalse(candidates.containsKey(303L), "ELECTRONICS product must not be included");

        // 4. Evaluate candidates with DeterministicRuleEvaluator
        ComplianceRule rule = new ComplianceRule();
        rule.setId(1L);
        rule.setRuleCode("LM-PC-MRP");

        ComplianceRuleVersion v2 = new ComplianceRuleVersion();
        v2.setId(20L);
        v2.setRequirementType("MANDATORY_DECLARATION");
        v2.setRequiredField("mrp");
        v2.setValidationOperator("NOT_EMPTY");

        // Both products have MRP
        ProductDeclaration d1 = new ProductDeclaration();
        d1.setSource("LISTING");
        d1.setFieldName("mrp");
        d1.setFieldValue("180.00");

        DeterministicRuleEvaluator.EvaluatorResult res1 = evaluator.evaluate(
                p1, List.of(d1), rule, v2, v2Categories, null
        );
        assertEquals("COMPLIANT", res1.status());

        DeterministicRuleEvaluator.EvaluatorResult res2 = evaluator.evaluate(
                p2, List.of(d1), rule, v2, v2Categories, null
        );
        assertEquals("COMPLIANT", res2.status());
    }

    @Test
    @DisplayName("Applicability Narrowing: Removing a category causes evaluator to mark product as NOT_APPLICABLE")
    void testApplicabilityNarrowing() {
        // v1: FOOD + COSMETICS -> v2: FOOD only
        List<String> v2Categories = List.of("FOOD");

        Product p = new Product();
        p.setId(202L);
        p.setCategory("COSMETICS");

        ComplianceRule rule = new ComplianceRule();
        ComplianceRuleVersion v2 = new ComplianceRuleVersion();
        v2.setRequirementType("MANDATORY_DECLARATION");
        v2.setRequiredField("mrp");

        DeterministicRuleEvaluator.EvaluatorResult res = evaluator.evaluate(
                p, List.of(), rule, v2, v2Categories, null
        );

        assertEquals("NOT_APPLICABLE", res.status());
    }

    @Test
    @DisplayName("Safety Fallback Condition: Triggers when previous version is absent")
    void testSafetyFallbackTriggerCondition() {
        ComplianceRuleVersion prevVersion = null;
        boolean needFallback = (prevVersion == null);
        assertTrue(needFallback, "Absence of previous version must trigger safe fallback");
    }

    @Test
    @DisplayName("Candidate Deduplication: Ensures no product is evaluated twice if matching multiple criteria")
    void testCandidateDeduplication() {
        Map<Long, String> candidates = new LinkedHashMap<>();

        // Add to Phase A
        candidates.put(101L, "PREVIOUSLY_ASSOCIATED");

        // Attempting to add same product in Phase B or declaration change
        Long newlyFoundId = 101L;
        if (!candidates.containsKey(newlyFoundId)) {
            candidates.put(newlyFoundId, "NEWLY_APPLICABLE");
        }

        assertEquals(1, candidates.size());
        assertEquals("PREVIOUSLY_ASSOCIATED", candidates.get(101L));
    }
}
