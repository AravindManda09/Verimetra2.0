package com.verimetra.revalidation.repository;

import com.verimetra.revalidation.entity.ProductRuleDependency;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRuleDependencyRepository extends JpaRepository<ProductRuleDependency, Long> {
    List<ProductRuleDependency> findByProductId(Long productId);
    
    List<ProductRuleDependency> findByRuleId(Long ruleId);
    
    List<ProductRuleDependency> findByRuleVersionId(Long ruleVersionId);
    
    Optional<ProductRuleDependency> findByProductIdAndRuleId(Long productId, Long ruleId);
    
    @Query("SELECT d.productId FROM ProductRuleDependency d WHERE d.ruleId = :ruleId AND d.dependencyStatus = 'ACTIVE'")
    List<Long> findProductIdsByRuleId(@Param("ruleId") Long ruleId);
    
    @Query("SELECT d.productId FROM ProductRuleDependency d WHERE d.ruleVersionId = :ruleVersionId")
    List<Long> findProductIdsByRuleVersionId(@Param("ruleVersionId") Long ruleVersionId);
    
    void deleteByProductId(Long productId);
}
