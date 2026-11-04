package com.verimetra.revalidation.controller;

import com.verimetra.common.exception.ResourceNotFoundException;
import com.verimetra.revalidation.dto.RevalidationCandidateDTO;
import com.verimetra.revalidation.dto.RevalidationRequest;
import com.verimetra.revalidation.dto.RevalidationRunDTO;
import com.verimetra.revalidation.entity.RevalidationCandidate;
import com.verimetra.revalidation.entity.RevalidationRun;
import com.verimetra.revalidation.repository.RevalidationCandidateRepository;
import com.verimetra.revalidation.repository.RevalidationRunRepository;
import com.verimetra.revalidation.service.FullRevalidationService;
import com.verimetra.revalidation.service.IncrementalRevalidationService;
import com.verimetra.rule.entity.ComplianceRule;
import com.verimetra.rule.entity.ComplianceRuleVersion;
import com.verimetra.rule.repository.ComplianceRuleRepository;
import com.verimetra.rule.repository.ComplianceRuleVersionRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/revalidation")
@Tag(name = "Revalidation", description = "Incremental and Full Revalidation operations")
public class RevalidationController {

    private final IncrementalRevalidationService incrementalService;
    private final FullRevalidationService fullService;
    private final RevalidationRunRepository runRepository;
    private final RevalidationCandidateRepository candidateRepository;
    private final ComplianceRuleRepository ruleRepository;
    private final ComplianceRuleVersionRepository versionRepository;

    public RevalidationController(
            IncrementalRevalidationService incrementalService,
            FullRevalidationService fullService,
            RevalidationRunRepository runRepository,
            RevalidationCandidateRepository candidateRepository,
            ComplianceRuleRepository ruleRepository,
            ComplianceRuleVersionRepository versionRepository
    ) {
        this.incrementalService = incrementalService;
        this.fullService = fullService;
        this.runRepository = runRepository;
        this.candidateRepository = candidateRepository;
        this.ruleRepository = ruleRepository;
        this.versionRepository = versionRepository;
    }

    @PostMapping("/incremental")
    @Operation(summary = "Execute dependency-aware incremental revalidation")
    public ResponseEntity<RevalidationRunDTO> runIncremental(@Valid @RequestBody RevalidationRequest request) {
        return ResponseEntity.ok(incrementalService.executeIncrementalRevalidation(request));
    }

    @PostMapping("/full")
    @Operation(summary = "Execute full revalidation baseline")
    public ResponseEntity<RevalidationRunDTO> runFull(@Valid @RequestBody RevalidationRequest request) {
        return ResponseEntity.ok(fullService.executeFullRevalidation(request));
    }

    @GetMapping("/runs")
    @Operation(summary = "List all revalidation runs")
    public ResponseEntity<Page<RevalidationRunDTO>> listRuns(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Page<RevalidationRun> runs = runRepository.findAllByOrderByStartedAtDesc(PageRequest.of(page, size));
        return ResponseEntity.ok(runs.map(this::toDTO));
    }

    @GetMapping("/runs/{id}")
    @Operation(summary = "Get revalidation run details")
    public ResponseEntity<RevalidationRunDTO> getRun(@PathVariable Long id) {
        RevalidationRun run = runRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("RevalidationRun", id));
        return ResponseEntity.ok(toDTO(run));
    }

    @GetMapping("/runs/{id}/candidates")
    @Operation(summary = "Get candidate products discovered during revalidation")
    public ResponseEntity<List<RevalidationCandidateDTO>> getCandidates(@PathVariable Long id) {
        List<RevalidationCandidate> candidates = candidateRepository.findByRevalidationRunId(id);
        List<RevalidationCandidateDTO> dtos = candidates.stream()
                .map(c -> new RevalidationCandidateDTO(
                        c.getId(), c.getRevalidationRunId(), c.getProductId(),
                        c.getCandidateReason(), c.getWasEvaluated(), c.getWasReused()
                ))
                .toList();
        return ResponseEntity.ok(dtos);
    }

    private RevalidationRunDTO toDTO(RevalidationRun r) {
        ComplianceRule rule = r.getTriggerRuleId() != null ? ruleRepository.findById(r.getTriggerRuleId()).orElse(null) : null;
        ComplianceRuleVersion prev = r.getPreviousVersionId() != null ? versionRepository.findById(r.getPreviousVersionId()).orElse(null) : null;
        ComplianceRuleVersion next = r.getNewVersionId() != null ? versionRepository.findById(r.getNewVersionId()).orElse(null) : null;
        return fullService.toRunDTO(r, rule, prev, next);
    }
}
