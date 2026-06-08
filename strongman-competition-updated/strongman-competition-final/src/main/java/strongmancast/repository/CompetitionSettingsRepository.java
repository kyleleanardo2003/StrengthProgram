package strongmancast.repository;

import strongmancast.model.Competition;
import strongmancast.model.CompetitionSettings;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CompetitionSettingsRepository extends JpaRepository<CompetitionSettings, Long> {
    Optional<CompetitionSettings> findByCompetition(Competition competition);
    void deleteByCompetition(Competition competition);
}

