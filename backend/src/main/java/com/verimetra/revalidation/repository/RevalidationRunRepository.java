package com.verimetra.revalidation.repository;

import com.verimetra.revalidation.entity.RevalidationRun;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RevalidationRunRepository extends JpaRepository<RevalidationRun, Long> {
    List<RevalidationRun> findByOrderByStartedAtDesc();
    
    Page<RevalidationRun> findAllByOrderByStartedAtDesc(Pageable pageable);
    
    @Query("SELECT r FROM RevalidationRun r ORDER BY r.startedAt DESC LIMIT 5")
    List<RevalidationRun> findRecentRuns();
    
    @Query("SELECT COUNT(r) FROM RevalidationRun r")
    long countTotal();
}
