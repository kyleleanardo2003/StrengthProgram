package strongmancast.repository;

import strongmancast.model.Athlete;
import strongmancast.model.Competition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AthleteRepository extends JpaRepository<Athlete, Long> {
    List<Athlete> findByCompetitionOrderByDivisionAscNameAsc(Competition competition);
    void deleteByCompetition(Competition competition);
}
// This interface extends JpaRepository, which provides methods for CRUD operations on Athlete entities.

