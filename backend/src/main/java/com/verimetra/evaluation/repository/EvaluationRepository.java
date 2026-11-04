package com.verimetra.evaluation.repository;

import com.verimetra.evaluation.entity.Evaluation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EvaluationRepository extends JpaRepository<Evaluation, Long> {
    List<Evaluation> findByProductId(Long productId);
    
    List<Evaluation> findByProductIdOrderByEvaluatedAtDesc(Long productId);
    
    List<Evaluation> findByRevalidationRunId(Long revalidationRunId);
    
    @Query("SELECT e FROM Evaluation e WHERE e.productId = :productId AND e.ruleVersionId = :ruleVersionId ORDER BY e.evaluatedAt DESC LIMIT 1")
    Optional<Evaluation> findLatestByProductAndRuleVersion(@Param("productId") Long productId, @Param("ruleVersionId") Long ruleVersionId);
    
    @Query("SELECT e.evaluationStatus, COUNT(e) FROM Evaluation e GROUP BY e.evaluationStatus")
    List<Object[]> countByStatus();
    
    @Query("SELECT COUNT(e) FROM Evaluation e")
    long countTotal();
    
    Page<Evaluation> findAllByOrderByEvaluatedAtDesc(Pageable pageable);
}
