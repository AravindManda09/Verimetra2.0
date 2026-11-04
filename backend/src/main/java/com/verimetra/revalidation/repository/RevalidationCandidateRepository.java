package com.verimetra.revalidation.repository;

import com.verimetra.revalidation.entity.RevalidationCandidate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RevalidationCandidateRepository extends JpaRepository<RevalidationCandidate, Long> {
    List<RevalidationCandidate> findByRevalidationRunId(Long revalidationRunId);
}
