package com.verimetra.experiment.controller;

import com.verimetra.experiment.dto.ExperimentRunDTO;
import com.verimetra.experiment.dto.RunExperimentRequest;
import com.verimetra.experiment.service.ExperimentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/experiments")
@Tag(name = "Experiments", description = "Benchmarking and evaluation comparison experiments")
public class ExperimentController {

    private final ExperimentService experimentService;

    public ExperimentController(ExperimentService experimentService) {
        this.experimentService = experimentService;
    }

    @PostMapping("/run")
    @Operation(summary = "Run a benchmark experiment comparing full vs incremental revalidation")
    public ResponseEntity<ExperimentRunDTO> runExperiment(@Valid @RequestBody RunExperimentRequest request) {
        return ResponseEntity.ok(experimentService.runExperiment(request));
    }

    @GetMapping
    @Operation(summary = "List all experiment runs")
    public ResponseEntity<Page<ExperimentRunDTO>> listExperiments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(experimentService.listExperiments(PageRequest.of(page, size)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get experiment run details")
    public ResponseEntity<ExperimentRunDTO> getExperiment(@PathVariable Long id) {
        return ResponseEntity.ok(experimentService.getExperiment(id));
    }

    @GetMapping("/{id}/export/csv")
    @Operation(summary = "Export experiment results as CSV")
    public ResponseEntity<String> exportCsv(@PathVariable Long id) {
        String csv = experimentService.exportExperimentCsv(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"experiment-" + id + ".csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv);
    }

    @GetMapping("/{id}/export/json")
    @Operation(summary = "Export experiment results as JSON")
    public ResponseEntity<ExperimentRunDTO> exportJson(@PathVariable Long id) {
        return ResponseEntity.ok(experimentService.getExperiment(id));
    }
}
