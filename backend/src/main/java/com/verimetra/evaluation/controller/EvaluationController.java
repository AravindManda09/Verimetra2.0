package com.verimetra.evaluation.controller;

import com.verimetra.evaluation.dto.EvaluationDTO;
import com.verimetra.evaluation.dto.EvaluationFindingDTO;
import com.verimetra.evaluation.dto.RunEvaluationRequest;
import com.verimetra.evaluation.service.EvaluationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/evaluations")
@Tag(name = "Evaluations", description = "Product compliance evaluation endpoints")
public class EvaluationController {

    private final EvaluationService evaluationService;

    public EvaluationController(EvaluationService evaluationService) {
        this.evaluationService = evaluationService;
    }

    @PostMapping("/run")
    @Operation(summary = "Run compliance evaluation on a product")
    public ResponseEntity<List<EvaluationDTO>> runEvaluation(@Valid @RequestBody RunEvaluationRequest request) {
        return ResponseEntity.ok(evaluationService.runEvaluation(request));
    }

    @GetMapping("/product/{productId}")
    @Operation(summary = "Get evaluations for a specific product")
    public ResponseEntity<List<EvaluationDTO>> getProductEvaluations(@PathVariable Long productId) {
        return ResponseEntity.ok(evaluationService.getEvaluationsForProduct(productId));
    }

    @GetMapping
    @Operation(summary = "List all evaluations paginated")
    public ResponseEntity<Page<EvaluationDTO>> listEvaluations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(evaluationService.listAllEvaluations(PageRequest.of(page, size)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get evaluation details")
    public ResponseEntity<EvaluationDTO> getEvaluation(@PathVariable Long id) {
        return ResponseEntity.ok(evaluationService.getEvaluation(id));
    }

    @GetMapping("/{id}/findings")
    @Operation(summary = "Get findings for an evaluation")
    public ResponseEntity<List<EvaluationFindingDTO>> getFindings(@PathVariable Long id) {
        return ResponseEntity.ok(evaluationService.getFindingsForEvaluation(id));
    }

    @PatchMapping("/findings/{findingId}/review")
    @Operation(summary = "Update finding review status")
    public ResponseEntity<EvaluationFindingDTO> updateFindingReview(
            @PathVariable Long findingId,
            @RequestParam String status
    ) {
        return ResponseEntity.ok(evaluationService.updateFindingReview(findingId, status));
    }
}
