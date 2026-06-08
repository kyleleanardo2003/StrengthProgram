package strongmancast.repository;

import strongmancast.model.CompetitionEvent;
import strongmancast.model.Competition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CompetitionEventRepository extends JpaRepository<CompetitionEvent, Long> {
    List<CompetitionEvent> findAllByOrderBySortOrderAscIdAsc();
    Optional<CompetitionEvent> findByEventName(String eventName);
    List<CompetitionEvent> findByCompetitionOrderBySortOrderAscIdAsc(Competition competition);
    Optional<CompetitionEvent> findByCompetitionAndEventName(Competition competition, String eventName);
    void deleteByCompetition(Competition competition);
}

