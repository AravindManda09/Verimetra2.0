package com.verimetra.report;

import com.verimetra.common.exception.ResourceNotFoundException;
import com.verimetra.evaluation.entity.Evaluation;
import com.verimetra.evaluation.entity.EvaluationFinding;
import com.verimetra.evaluation.repository.EvaluationFindingRepository;
import com.verimetra.evaluation.repository.EvaluationRepository;
import com.verimetra.product.entity.Product;
import com.verimetra.product.repository.ProductRepository;
import com.verimetra.revalidation.entity.RevalidationRun;
import com.verimetra.revalidation.repository.RevalidationRunRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/reports")
@Tag(name = "Reports", description = "Report generation and download endpoints")
public class ReportController {

    private final ProductRepository productRepository;
    private final EvaluationRepository evaluationRepository;
    private final EvaluationFindingRepository findingRepository;
    private final RevalidationRunRepository revalidationRepository;

    public ReportController(
            ProductRepository productRepository,
            EvaluationRepository evaluationRepository,
            EvaluationFindingRepository findingRepository,
            RevalidationRunRepository revalidationRepository
    ) {
        this.productRepository = productRepository;
        this.evaluationRepository = evaluationRepository;
        this.findingRepository = findingRepository;
        this.revalidationRepository = revalidationRepository;
    }

    @GetMapping("/compliance/csv")
    @Operation(summary = "Export product compliance report as CSV")
    public ResponseEntity<String> exportComplianceReportCsv() {
        List<Product> products = productRepository.findAll();
        StringBuilder sb = new StringBuilder();
        sb.append("Product ID,SKU,Product Name,Category,Manufacturer,Country of Origin,Status,Findings Count\n");

        for (Product p : products) {
            List<Evaluation> evals = evaluationRepository.findByProductIdOrderByEvaluatedAtDesc(p.getId());
            String overallStatus = "NOT_EVALUATED";
            int findingsCount = 0;

            if (!evals.isEmpty()) {
                boolean hasNonCompliant = evals.stream().anyMatch(e -> "NON_COMPLIANT".equals(e.getEvaluationStatus()));
                boolean hasReview = evals.stream().anyMatch(e -> "NEEDS_REVIEW".equals(e.getEvaluationStatus()));
                if (hasNonCompliant) overallStatus = "NON_COMPLIANT";
                else if (hasReview) overallStatus = "NEEDS_REVIEW";
                else overallStatus = "COMPLIANT";

                for (Evaluation e : evals) {
                    findingsCount += findingRepository.findByEvaluationId(e.getId()).size();
                }
            }

            sb.append(p.getId()).append(",")
              .append(escape(p.getSku())).append(",")
              .append(escape(p.getProductName())).append(",")
              .append(p.getCategory()).append(",")
              .append(escape(p.getManufacturerName())).append(",")
              .append(escape(p.getCountryOfOrigin())).append(",")
              .append(overallStatus).append(",")
              .append(findingsCount).append("\n");
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"verimetra-compliance-report.csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(sb.toString());
    }

    @GetMapping("/revalidation/{id}/csv")
    @Operation(summary = "Export revalidation run report as CSV")
    public ResponseEntity<String> exportRevalidationCsv(@PathVariable Long id) {
        RevalidationRun r = revalidationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("RevalidationRun", id));

        StringBuilder sb = new StringBuilder();
        sb.append("Metric,Value\n");
        sb.append("Run ID,").append(r.getId()).append("\n");
        sb.append("Run Type,").append(r.getRunType()).append("\n");
        sb.append("Status,").append(r.getStatus()).append("\n");
        sb.append("Trigger Rule ID,").append(r.getTriggerRuleId()).append("\n");
        sb.append("Previous Version ID,").append(r.getPreviousVersionId()).append("\n");
        sb.append("New Version ID,").append(r.getNewVersionId()).append("\n");
        sb.append("Total Considered,").append(r.getTotalProductsConsidered()).append("\n");
        sb.append("Previously Associated,").append(r.getPreviouslyAssociatedCount()).append("\n");
        sb.append("Newly Applicable,").append(r.getNewlyApplicableCount()).append("\n");
        sb.append("Candidate Count,").append(r.getCandidateCount()).append("\n");
        sb.append("Evaluations Performed,").append(r.getEvaluationsPerformed()).append("\n");
        sb.append("Evaluations Reused,").append(r.getEvaluationsReused()).append("\n");
        sb.append("Findings Generated,").append(r.getFindingsGenerated()).append("\n");
        sb.append("Candidate Selection (ms),").append(r.getCandidateSelectionMs()).append("\n");
        sb.append("Evaluation Time (ms),").append(r.getEvaluationMs()).append("\n");
        sb.append("Persistence Time (ms),").append(r.getPersistenceMs()).append("\n");
        sb.append("Total Duration (ms),").append(r.getTotalDurationMs()).append("\n");
        sb.append("Used Fallback,").append(r.getUsedFallback()).append("\n");
        sb.append("Fallback Reason,").append(escape(r.getFallbackReason())).append("\n");

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"revalidation-run-" + id + ".csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(sb.toString());
    }

    private String escape(String val) {
        if (val == null) return "";
        if (val.contains(",") || val.contains("\"") || val.contains("\n")) {
            return "\"" + val.replace("\"", "\"\"") + "\"";
        }
        return val;
    }
}
