package com.verimetra.dashboard;

import com.verimetra.evaluation.repository.EvaluationFindingRepository;
import com.verimetra.evaluation.repository.EvaluationRepository;
import com.verimetra.product.repository.ProductRepository;
import com.verimetra.revalidation.repository.RevalidationRunRepository;
import com.verimetra.rule.repository.ComplianceRuleRepository;
import com.verimetra.rule.repository.ComplianceRuleVersionRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

@RestController
@RequestMapping("/api/dashboard")
@Tag(name = "Dashboard", description = "Dashboard summary and charts")
public class DashboardController {

    private final ProductRepository productRepository;
    private final ComplianceRuleRepository ruleRepository;
    private final ComplianceRuleVersionRepository versionRepository;
    private final EvaluationRepository evaluationRepository;
    private final EvaluationFindingRepository findingRepository;
    private final RevalidationRunRepository revalidationRepository;

    public DashboardController(
            ProductRepository productRepository,
            ComplianceRuleRepository ruleRepository,
            ComplianceRuleVersionRepository versionRepository,
            EvaluationRepository evaluationRepository,
            EvaluationFindingRepository findingRepository,
            RevalidationRunRepository revalidationRepository
    ) {
        this.productRepository = productRepository;
        this.ruleRepository = ruleRepository;
        this.versionRepository = versionRepository;
        this.evaluationRepository = evaluationRepository;
        this.findingRepository = findingRepository;
        this.revalidationRepository = revalidationRepository;
    }

    @GetMapping("/summary")
    @Operation(summary = "Get high-level system metrics")
    public ResponseEntity<Map<String, Object>> getSummary() {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalProducts", productRepository.countTotal());
        summary.put("totalRules", ruleRepository.count());
        summary.put("activeRuleVersions", versionRepository.countActive());
        summary.put("totalEvaluations", evaluationRepository.countTotal());
        summary.put("totalFindings", findingRepository.countTotal());
        summary.put("unreviewedFindings", findingRepository.countUnreviewed());
        summary.put("totalRevalidationRuns", revalidationRepository.countTotal());
        return ResponseEntity.ok(summary);
    }

    @GetMapping("/charts")
    @Operation(summary = "Get chart data for dashboard visualization")
    public ResponseEntity<Map<String, Object>> getCharts() {
        Map<String, Object> charts = new LinkedHashMap<>();

        // Products by Category
        List<Map<String, Object>> catList = new ArrayList<>();
        for (Object[] row : productRepository.countByCategory()) {
            catList.add(Map.of("name", row[0], "value", row[1]));
        }
        charts.put("productsByCategory", catList);

        // Evaluations by Status
        List<Map<String, Object>> statusList = new ArrayList<>();
        for (Object[] row : evaluationRepository.countByStatus()) {
            statusList.add(Map.of("status", row[0], "count", row[1]));
        }
        charts.put("evaluationsByStatus", statusList);

        // Findings by Severity
        List<Map<String, Object>> severityList = new ArrayList<>();
        for (Object[] row : findingRepository.countBySeverity()) {
            severityList.add(Map.of("severity", row[0], "count", row[1]));
        }
        charts.put("findingsBySeverity", severityList);

        // Findings by Type
        List<Map<String, Object>> typeList = new ArrayList<>();
        for (Object[] row : findingRepository.countByFindingType()) {
            typeList.add(Map.of("type", row[0], "count", row[1]));
        }
        charts.put("findingsByType", typeList);

        // Recent Revalidations
        charts.put("recentRevalidations", revalidationRepository.findRecentRuns());

        return ResponseEntity.ok(charts);
    }
}
