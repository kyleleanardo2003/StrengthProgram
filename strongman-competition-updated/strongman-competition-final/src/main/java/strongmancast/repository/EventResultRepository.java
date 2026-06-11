package strongmancast.repository;

import strongmancast.model.Athlete;
import strongmancast.model.Competition;
import strongmancast.model.EventResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EventResultRepository extends JpaRepository<EventResult, Long> {
    List<EventResult> findByAthlete(Athlete athlete);
    List<EventResult> findByEventName(String eventName);
    Optional<EventResult> findByAthleteAndEventName(Athlete athlete, String eventName);
    List<EventResult> findByCompetition(Competition competition);
    List<EventResult> findByAthleteAndCompetition(Athlete athlete, Competition competition);
    Optional<EventResult> findByAthleteAndCompetitionAndEventName(Athlete athlete, Competition competition, String eventName);
    void deleteByCompetition(Competition competition);
    void deleteByCompetitionAndEventName(Competition competition, String eventName);
    void deleteByAthlete(Athlete athlete);
}

