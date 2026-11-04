package com.verimetra.rule.service;

import com.verimetra.common.exception.BadRequestException;
import com.verimetra.common.exception.ResourceNotFoundException;
import com.verimetra.rule.dto.*;
import com.verimetra.rule.entity.*;
import com.verimetra.rule.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class RuleService {

    private final ComplianceRuleRepository ruleRepository;
    private final ComplianceRuleVersionRepository versionRepository;
    private final RuleApplicabilityRepository applicabilityRepository;

    public RuleService(ComplianceRuleRepository ruleRepository,
                      ComplianceRuleVersionRepository versionRepository,
                      RuleApplicabilityRepository applicabilityRepository) {
        this.ruleRepository = ruleRepository;
        this.versionRepository = versionRepository;
        this.applicabilityRepository = applicabilityRepository;
    }

    @Transactional
    public RuleDTO createRule(CreateRuleRequest request) {
        if (ruleRepository.existsByRuleCode(request.ruleCode())) {
            throw new BadRequestException("Rule with code '" + request.ruleCode() + "' already exists");
        }

        ComplianceRule rule = new ComplianceRule();
        rule.setRuleCode(request.ruleCode());
        rule.setRuleName(request.ruleName());
        rule.setDescription(request.description());
        rule.setLegalReference(request.legalReference());
        rule.setSourceUrl(request.sourceUrl());
        rule.setSourceVerifiedDate(request.sourceVerifiedDate());
        rule.setIsIllustrative(request.isIllustrative() != null ? request.isIllustrative() : false);
        rule = ruleRepository.save(rule);

        // Create version 1
        ComplianceRuleVersion version = new ComplianceRuleVersion();
        version.setRuleId(rule.getId());
        version.setVersionNumber(1);
        version.setRequirementType(request.requirementType());
        version.setRequiredField(request.requiredField());
        version.setValidationOperator(request.validationOperator());
        version.setExpectedValue(request.expectedValue());
        version.setSeverity(request.severity() != null ? request.severity() : "MAJOR");
        version.setEffectiveFrom(request.effectiveFrom());
        version.setEffectiveTo(request.effectiveTo());
        version.setStatus("ACTIVE");
        version.setChangeSummary("Initial version");
        version = versionRepository.save(version);

        // Create applicability
        for (String cat : request.applicableCategories()) {
            RuleApplicability ra = new RuleApplicability();
            ra.setRuleVersionId(version.getId());
            ra.setCategory(cat.toUpperCase());
            applicabilityRepository.save(ra);
        }

        return toDTO(rule);
    }

    @Transactional
    public RuleVersionDTO createNewVersion(Long ruleId, CreateVersionRequest request) {
        ruleRepository.findById(ruleId)
                .orElseThrow(() -> new ResourceNotFoundException("Rule", ruleId));

        int nextVersion = versionRepository.findMaxVersionNumber(ruleId).orElse(0) + 1;

        ComplianceRuleVersion version = new ComplianceRuleVersion();
        version.setRuleId(ruleId);
        version.setVersionNumber(nextVersion);
        version.setRequirementType(request.requirementType());
        version.setRequiredField(request.requiredField());
        version.setValidationOperator(request.validationOperator());
        version.setExpectedValue(request.expectedValue());
        version.setSeverity(request.severity() != null ? request.severity() : "MAJOR");
        version.setEffectiveFrom(request.effectiveFrom());
        version.setEffectiveTo(request.effectiveTo());
        version.setStatus("DRAFT");
        version.setChangeSummary(request.changeSummary());
        version = versionRepository.save(version);

        // Create applicability
        for (String cat : request.applicableCategories()) {
            RuleApplicability ra = new RuleApplicability();
            ra.setRuleVersionId(version.getId());
            ra.setCategory(cat.toUpperCase());
            applicabilityRepository.save(ra);
        }

        return toVersionDTO(version);
    }

    @Transactional
    public RuleVersionDTO activateVersion(Long ruleId, Long versionId) {
        ComplianceRuleVersion version = versionRepository.findById(versionId)
                .orElseThrow(() -> new ResourceNotFoundException("RuleVersion", versionId));

        if (!version.getRuleId().equals(ruleId)) {
            throw new BadRequestException("Version does not belong to the specified rule");
        }

        // Supersede current active version
        versionRepository.findByRuleIdAndStatus(ruleId, "ACTIVE").ifPresent(active -> {
            active.setStatus("SUPERSEDED");
            versionRepository.save(active);
        });

        version.setStatus("ACTIVE");
        version = versionRepository.save(version);

        return toVersionDTO(version);
    }

    public RuleDTO getRule(Long id) {
        ComplianceRule rule = ruleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Rule", id));
        return toDTO(rule);
    }

    public List<RuleDTO> listRules() {
        return ruleRepository.findAll().stream().map(this::toDTO).toList();
    }

    public List<RuleVersionDTO> getVersionHistory(Long ruleId) {
        return versionRepository.findByRuleIdOrderByVersionNumberDesc(ruleId)
                .stream().map(this::toVersionDTO).toList();
    }

    public RuleVersionDTO getVersion(Long ruleId, Long versionId) {
        ComplianceRuleVersion version = versionRepository.findById(versionId)
                .orElseThrow(() -> new ResourceNotFoundException("RuleVersion", versionId));
        if (!version.getRuleId().equals(ruleId)) {
            throw new BadRequestException("Version does not belong to the specified rule");
        }
        return toVersionDTO(version);
    }

    public Map<String, Object> compareVersions(Long ruleId, Long versionId1, Long versionId2) {
        RuleVersionDTO v1 = getVersion(ruleId, versionId1);
        RuleVersionDTO v2 = getVersion(ruleId, versionId2);

        Map<String, Object> comparison = new LinkedHashMap<>();
        comparison.put("version1", v1);
        comparison.put("version2", v2);

        List<Map<String, Object>> changes = new ArrayList<>();
        addChange(changes, "requirementType", v1.requirementType(), v2.requirementType());
        addChange(changes, "requiredField", v1.requiredField(), v2.requiredField());
        addChange(changes, "validationOperator", v1.validationOperator(), v2.validationOperator());
        addChange(changes, "expectedValue", v1.expectedValue(), v2.expectedValue());
        addChange(changes, "severity", v1.severity(), v2.severity());
        
        Set<String> cats1 = new HashSet<>(v1.applicableCategories());
        Set<String> cats2 = new HashSet<>(v2.applicableCategories());
        if (!cats1.equals(cats2)) {
            Set<String> added = new HashSet<>(cats2);
            added.removeAll(cats1);
            Set<String> removed = new HashSet<>(cats1);
            removed.removeAll(cats2);
            Map<String, Object> change = new LinkedHashMap<>();
            change.put("field", "applicableCategories");
            change.put("oldValue", cats1);
            change.put("newValue", cats2);
            change.put("addedCategories", added);
            change.put("removedCategories", removed);
            changes.add(change);
        }

        comparison.put("changes", changes);
        comparison.put("hasChanges", !changes.isEmpty());
        return comparison;
    }

    public List<ComplianceRuleVersion> getActiveVersionsForCategory(String category) {
        List<Long> versionIds = applicabilityRepository.findVersionIdsByCategory(category);
        return versionIds.stream()
                .map(id -> versionRepository.findById(id).orElse(null))
                .filter(v -> v != null && "ACTIVE".equals(v.getStatus()))
                .toList();
    }

    public List<ComplianceRuleVersion> getAllActiveVersions() {
        return versionRepository.findAllActive();
    }

    public List<String> getCategoriesForVersion(Long versionId) {
        return applicabilityRepository.findCategoriesByVersionId(versionId);
    }

    private void addChange(List<Map<String, Object>> changes, String field, Object old, Object newVal) {
        if (!Objects.equals(old, newVal)) {
            Map<String, Object> change = new LinkedHashMap<>();
            change.put("field", field);
            change.put("oldValue", old);
            change.put("newValue", newVal);
            changes.add(change);
        }
    }

    private RuleDTO toDTO(ComplianceRule rule) {
        List<ComplianceRuleVersion> versions = versionRepository.findByRuleIdOrderByVersionNumberDesc(rule.getId());
        RuleVersionDTO activeVersion = versions.stream()
                .filter(v -> "ACTIVE".equals(v.getStatus()))
                .findFirst()
                .map(this::toVersionDTO)
                .orElse(null);
        return new RuleDTO(rule.getId(), rule.getRuleCode(), rule.getRuleName(),
                rule.getDescription(), rule.getLegalReference(), rule.getSourceUrl(),
                rule.getSourceVerifiedDate(), rule.getIsIllustrative(),
                rule.getCreatedAt(), rule.getUpdatedAt(),
                activeVersion, versions.stream().map(this::toVersionDTO).toList());
    }

    public RuleVersionDTO toVersionDTO(ComplianceRuleVersion v) {
        List<String> categories = applicabilityRepository.findCategoriesByVersionId(v.getId());
        return new RuleVersionDTO(v.getId(), v.getRuleId(), v.getVersionNumber(),
                v.getRequirementType(), v.getRequiredField(), v.getValidationOperator(),
                v.getExpectedValue(), v.getSeverity(), v.getEffectiveFrom(), v.getEffectiveTo(),
                v.getStatus(), v.getChangeSummary(), v.getCreatedAt(), categories);
    }
}
