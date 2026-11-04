package com.verimetra.evaluation.repository;

import com.verimetra.evaluation.entity.EvaluationSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EvaluationSnapshotRepository extends JpaRepository<EvaluationSnapshot, Long> {
    Optional<EvaluationSnapshot> findByEvaluationId(Long evaluationId);
}
