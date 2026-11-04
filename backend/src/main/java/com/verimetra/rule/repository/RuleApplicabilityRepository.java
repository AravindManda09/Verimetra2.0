package com.verimetra.rule.repository;

import com.verimetra.rule.entity.RuleApplicability;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface RuleApplicabilityRepository extends JpaRepository<RuleApplicability, Long> {
    List<RuleApplicability> findByRuleVersionId(Long ruleVersionId);
    
    @Query("SELECT ra.category FROM RuleApplicability ra WHERE ra.ruleVersionId = :versionId")
    List<String> findCategoriesByVersionId(@Param("versionId") Long versionId);
    
    void deleteByRuleVersionId(Long ruleVersionId);
    
    @Query("SELECT DISTINCT ra.ruleVersionId FROM RuleApplicability ra WHERE ra.category = :category")
    List<Long> findVersionIdsByCategory(@Param("category") String category);
}
