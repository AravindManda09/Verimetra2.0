package com.verimetra.evaluation.repository;

import com.verimetra.evaluation.entity.EvaluationFinding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EvaluationFindingRepository extends JpaRepository<EvaluationFinding, Long> {
    List<EvaluationFinding> findByEvaluationId(Long evaluationId);
    
    @Query("SELECT ef.severity, COUNT(ef) FROM EvaluationFinding ef GROUP BY ef.severity")
    List<Object[]> countBySeverity();
    
    @Query("SELECT ef.findingType, COUNT(ef) FROM EvaluationFinding ef GROUP BY ef.findingType")
    List<Object[]> countByFindingType();
    
    @Query("SELECT COUNT(ef) FROM EvaluationFinding ef")
    long countTotal();
    
    @Query("SELECT COUNT(ef) FROM EvaluationFinding ef WHERE ef.reviewStatus = 'UNREVIEWED'")
    long countUnreviewed();
}
