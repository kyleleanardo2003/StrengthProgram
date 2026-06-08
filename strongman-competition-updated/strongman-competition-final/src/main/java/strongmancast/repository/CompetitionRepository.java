package strongmancast.repository;

import strongmancast.model.Competition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CompetitionRepository extends JpaRepository<Competition, Long> {
    List<Competition> findAllByOrderByCompetitionDateDescIdDesc();
    List<Competition> findByStatusOrderByCompetitionDateDescIdDesc(String status);
    Optional<Competition> findFirstByStatusInOrderByCompetitionDateDescIdDesc(List<String> statuses);
}

