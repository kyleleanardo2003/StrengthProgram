package com.example.strongman916.strongman_competition.repository;

import com.example.strongman916.strongman_competition.model.CompetitionEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CompetitionEventRepository extends JpaRepository<CompetitionEvent, Long> {
    List<CompetitionEvent> findAllByOrderBySortOrderAscIdAsc();
    Optional<CompetitionEvent> findByEventName(String eventName);
}
