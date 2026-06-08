package strongmancast.controller;

import strongmancast.model.Competition;
import strongmancast.repository.AthleteRepository;
import strongmancast.repository.CompetitionEventRepository;
import strongmancast.repository.CompetitionRepository;
import strongmancast.repository.CompetitionSettingsRepository;
import strongmancast.repository.EventResultRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.security.Principal;

@Controller
public class CompetitionController {

    private final CompetitionRepository competitionRepository;
    private final AthleteRepository athleteRepository;
    private final CompetitionEventRepository competitionEventRepository;
    private final CompetitionSettingsRepository competitionSettingsRepository;
    private final EventResultRepository eventResultRepository;

    public CompetitionController(
            CompetitionRepository competitionRepository,
            AthleteRepository athleteRepository,
            CompetitionEventRepository competitionEventRepository,
            CompetitionSettingsRepository competitionSettingsRepository,
            EventResultRepository eventResultRepository
    ) {
        this.competitionRepository = competitionRepository;
        this.athleteRepository = athleteRepository;
        this.competitionEventRepository = competitionEventRepository;
        this.competitionSettingsRepository = competitionSettingsRepository;
        this.eventResultRepository = eventResultRepository;
    }

    @GetMapping("/competitions")
    public String competitions(Model model, Principal principal) {
        model.addAttribute("competition", new Competition());
        model.addAttribute("canEditCompetitions", principal != null);
        model.addAttribute("liveCompetitions", competitionRepository.findByStatusOrderByCompetitionDateDescIdDesc("LIVE"));
        model.addAttribute("upcomingCompetitions", competitionRepository.findByStatusOrderByCompetitionDateDescIdDesc("SETUP"));
        model.addAttribute("pastCompetitions", competitionRepository.findByStatusOrderByCompetitionDateDescIdDesc("COMPLETE"));
        return "competitions";
    }

    @PostMapping("/competitions")
    public String createCompetition(@ModelAttribute Competition competition) {
        if (competition.getStatus() == null || competition.getStatus().isBlank()) {
            competition.setStatus("SETUP");
        }

        Competition savedCompetition = competitionRepository.save(competition);
        return "redirect:/organizer?competitionId=" + savedCompetition.getId();
    }

    @PostMapping("/competitions/{id}")
    public String updateCompetition(@PathVariable Long id, @ModelAttribute Competition competition) {
        Competition existingCompetition = competitionRepository.findById(id).orElseThrow();
        existingCompetition.setName(competition.getName());
        existingCompetition.setCompetitionDate(competition.getCompetitionDate());
        existingCompetition.setLocation(competition.getLocation());
        existingCompetition.setStatus(competition.getStatus());
        competitionRepository.save(existingCompetition);
        return "redirect:/competitions";
    }

    @PostMapping("/competitions/{id}/name")
    @ResponseBody
    public String updateCompetitionName(@PathVariable Long id, @RequestParam String name) {
        Competition competition = competitionRepository.findById(id).orElseThrow();
        competition.setName(name);
        competitionRepository.save(competition);
        return "ok";
    }

    @PostMapping("/competitions/{id}/status/{status}")
    public String updateStatus(@PathVariable Long id, @PathVariable String status) {
        Competition competition = competitionRepository.findById(id).orElseThrow();
        competition.setStatus(status.toUpperCase());
        competitionRepository.save(competition);
        return "redirect:/organizer?competitionId=" + id;
    }

    @PostMapping("/competitions/{id}/delete")
    @Transactional
    public String deleteCompetition(@PathVariable Long id) {
        Competition competition = competitionRepository.findById(id).orElseThrow();
        eventResultRepository.deleteByCompetition(competition);
        athleteRepository.deleteByCompetition(competition);
        competitionEventRepository.deleteByCompetition(competition);
        competitionSettingsRepository.deleteByCompetition(competition);
        competitionRepository.delete(competition);
        return "redirect:/competitions";
    }

    @GetMapping("/archive")
    public String archive(Model model) {
        model.addAttribute("competitions", competitionRepository.findByStatusOrderByCompetitionDateDescIdDesc("COMPLETE"));
        return "archive";
    }
}

