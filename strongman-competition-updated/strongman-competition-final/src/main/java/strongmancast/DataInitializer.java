package strongmancast;

import strongmancast.model.Competition;
import strongmancast.model.CompetitionSettings;
import strongmancast.repository.AthleteRepository;
import strongmancast.repository.CompetitionEventRepository;
import strongmancast.repository.CompetitionRepository;
import strongmancast.repository.CompetitionSettingsRepository;
import strongmancast.repository.EventResultRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class DataInitializer implements CommandLineRunner {

    private final CompetitionRepository competitionRepository;
    private final AthleteRepository athleteRepository;
    private final CompetitionEventRepository competitionEventRepository;
    private final EventResultRepository eventResultRepository;
    private final CompetitionSettingsRepository competitionSettingsRepository;

    public DataInitializer(
            CompetitionRepository competitionRepository,
            AthleteRepository athleteRepository,
            CompetitionEventRepository competitionEventRepository,
            EventResultRepository eventResultRepository,
            CompetitionSettingsRepository competitionSettingsRepository
    ) {
        this.competitionRepository = competitionRepository;
        this.athleteRepository = athleteRepository;
        this.competitionEventRepository = competitionEventRepository;
        this.eventResultRepository = eventResultRepository;
        this.competitionSettingsRepository = competitionSettingsRepository;
    }

    @Override
    public void run(String... args) {
        if (athleteRepository.count() == 0
                && competitionEventRepository.count() == 0
                && eventResultRepository.count() == 0) {
            return;
        }

        Competition competition = competitionRepository.findFirstByStatusInOrderByCompetitionDateDescIdDesc(
                java.util.List.of("SETUP", "LIVE", "COMPLETE")
        ).orElseGet(() -> {
            Competition migratedCompetition = new Competition();
            migratedCompetition.setName("Imported Competition");
            migratedCompetition.setCompetitionDate(LocalDate.now());
            migratedCompetition.setLocation("");
            migratedCompetition.setStatus("SETUP");
            return competitionRepository.save(migratedCompetition);
        });

        athleteRepository.findAll().stream()
                .filter(athlete -> athlete.getCompetition() == null)
                .forEach(athlete -> {
                    athlete.setCompetition(competition);
                    athleteRepository.save(athlete);
                });

        competitionEventRepository.findAll().stream()
                .filter(event -> event.getCompetition() == null)
                .forEach(event -> {
                    event.setCompetition(competition);
                    competitionEventRepository.save(event);
                });

        eventResultRepository.findAll().stream()
                .filter(result -> result.getCompetition() == null)
                .forEach(result -> {
                    result.setCompetition(competition);
                    eventResultRepository.save(result);
                });

        competitionSettingsRepository.findAll().stream()
                .filter(settings -> settings.getCompetition() == null)
                .forEach(settings -> {
                    settings.setCompetition(competition);
                    competitionSettingsRepository.save(settings);
                });

        if (competitionSettingsRepository.findByCompetition(competition).isEmpty()) {
            CompetitionSettings settings = new CompetitionSettings();
            settings.setId(competition.getId());
            settings.setCompetition(competition);
            competitionSettingsRepository.save(settings);
        }
    }
}

