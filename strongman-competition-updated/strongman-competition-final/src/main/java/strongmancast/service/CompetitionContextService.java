package strongmancast.service;

import strongmancast.model.Competition;
import strongmancast.model.CompetitionSettings;
import strongmancast.repository.CompetitionRepository;
import strongmancast.repository.CompetitionSettingsRepository;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;

import java.time.LocalDate;
import java.util.List;

@Service
public class CompetitionContextService {

    private final CompetitionRepository competitionRepository;
    private final CompetitionSettingsRepository competitionSettingsRepository;

    public CompetitionContextService(
            CompetitionRepository competitionRepository,
            CompetitionSettingsRepository competitionSettingsRepository
    ) {
        this.competitionRepository = competitionRepository;
        this.competitionSettingsRepository = competitionSettingsRepository;
    }

    public Competition currentCompetition(Long competitionId) {
        if (competitionId != null) {
            return competitionRepository.findById(competitionId).orElseGet(this::ensureDefaultCompetition);
        }

        return competitionRepository
                .findFirstByStatusInOrderByCompetitionDateDescIdDesc(List.of("SETUP", "LIVE"))
                .orElseGet(this::ensureDefaultCompetition);
    }

    public CompetitionSettings settingsFor(Competition competition) {
        return competitionSettingsRepository.findByCompetition(competition)
                .orElseGet(() -> {
                    CompetitionSettings settings = new CompetitionSettings();
                    settings.setId(competition.getId());
                    settings.setCompetition(competition);
                    return competitionSettingsRepository.save(settings);
                });
    }

    public void addCompetitionModel(Model model, Competition currentCompetition) {
        model.addAttribute("currentCompetition", currentCompetition);
        model.addAttribute("competitions", competitionRepository.findAllByOrderByCompetitionDateDescIdDesc());
        model.addAttribute("competitionId", currentCompetition.getId());
    }

    private Competition ensureDefaultCompetition() {
        Competition competition = new Competition();
        competition.setName("Current Competition");
        competition.setCompetitionDate(LocalDate.now());
        competition.setLocation("");
        competition.setStatus("SETUP");
        return competitionRepository.save(competition);
    }
}

