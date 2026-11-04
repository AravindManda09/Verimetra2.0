package com.verimetra.experiment.repository;

import com.verimetra.experiment.entity.ExperimentRun;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExperimentRunRepository extends JpaRepository<ExperimentRun, Long> {
    List<ExperimentRun> findByOrderByStartedAtDesc();
    Page<ExperimentRun> findAllByOrderByStartedAtDesc(Pageable pageable);
}
