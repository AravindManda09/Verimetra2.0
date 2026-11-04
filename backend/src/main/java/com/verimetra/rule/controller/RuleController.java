package com.verimetra.rule.controller;

import com.verimetra.rule.dto.*;
import com.verimetra.rule.service.RuleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/rules")
@Tag(name = "Rules", description = "Compliance rule management")
public class RuleController {

    private final RuleService ruleService;

    public RuleController(RuleService ruleService) {
        this.ruleService = ruleService;
    }

    @PostMapping
    @Operation(summary = "Create a new compliance rule")
    public ResponseEntity<RuleDTO> createRule(@Valid @RequestBody CreateRuleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ruleService.createRule(request));
    }

    @GetMapping
    @Operation(summary = "List all rules")
    public ResponseEntity<List<RuleDTO>> listRules() {
        return ResponseEntity.ok(ruleService.listRules());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get rule details")
    public ResponseEntity<RuleDTO> getRule(@PathVariable Long id) {
        return ResponseEntity.ok(ruleService.getRule(id));
    }

    @PostMapping("/{id}/versions")
    @Operation(summary = "Create a new rule version")
    public ResponseEntity<RuleVersionDTO> createVersion(
            @PathVariable Long id, @Valid @RequestBody CreateVersionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ruleService.createNewVersion(id, request));
    }

    @GetMapping("/{id}/versions")
    @Operation(summary = "Get version history")
    public ResponseEntity<List<RuleVersionDTO>> getVersions(@PathVariable Long id) {
        return ResponseEntity.ok(ruleService.getVersionHistory(id));
    }

    @GetMapping("/{id}/versions/{versionId}")
    @Operation(summary = "Get specific version")
    public ResponseEntity<RuleVersionDTO> getVersion(@PathVariable Long id, @PathVariable Long versionId) {
        return ResponseEntity.ok(ruleService.getVersion(id, versionId));
    }

    @PostMapping("/{id}/versions/{versionId}/activate")
    @Operation(summary = "Activate a rule version")
    public ResponseEntity<RuleVersionDTO> activateVersion(@PathVariable Long id, @PathVariable Long versionId) {
        return ResponseEntity.ok(ruleService.activateVersion(id, versionId));
    }

    @GetMapping("/{id}/versions/compare")
    @Operation(summary = "Compare two versions")
    public ResponseEntity<Map<String, Object>> compareVersions(
            @PathVariable Long id,
            @RequestParam Long version1,
            @RequestParam Long version2) {
        return ResponseEntity.ok(ruleService.compareVersions(id, version1, version2));
    }
}
