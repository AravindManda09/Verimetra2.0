package com.verimetra.rule.repository;

import com.verimetra.rule.entity.ComplianceRuleVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ComplianceRuleVersionRepository extends JpaRepository<ComplianceRuleVersion, Long> {
    List<ComplianceRuleVersion> findByRuleIdOrderByVersionNumberDesc(Long ruleId);
    
    Optional<ComplianceRuleVersion> findByRuleIdAndStatus(Long ruleId, String status);
    
    Optional<ComplianceRuleVersion> findByRuleIdAndVersionNumber(Long ruleId, Integer versionNumber);
    
    @Query("SELECT v FROM ComplianceRuleVersion v WHERE v.status = 'ACTIVE'")
    List<ComplianceRuleVersion> findAllActive();
    
    @Query("SELECT v FROM ComplianceRuleVersion v WHERE v.ruleId = :ruleId ORDER BY v.versionNumber DESC LIMIT 1")
    Optional<ComplianceRuleVersion> findLatestVersion(@Param("ruleId") Long ruleId);
    
    @Query("SELECT MAX(v.versionNumber) FROM ComplianceRuleVersion v WHERE v.ruleId = :ruleId")
    Optional<Integer> findMaxVersionNumber(@Param("ruleId") Long ruleId);
    
    @Query("SELECT COUNT(v) FROM ComplianceRuleVersion v WHERE v.status = 'ACTIVE'")
    long countActive();
}
