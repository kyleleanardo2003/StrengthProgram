package com.example.strongman916.strongman_competition.repository;

import com.example.strongman916.strongman_competition.model.Athlete;
import com.example.strongman916.strongman_competition.model.EventResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EventResultRepository extends JpaRepository<EventResult, Long> {
    List<EventResult> findByAthlete(Athlete athlete);
    List<EventResult> findByEventName(String eventName);
    Optional<EventResult> findByAthleteAndEventName(Athlete athlete, String eventName);
}
