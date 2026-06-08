package strongmancast.controller;

import strongmancast.model.Competition;
import strongmancast.repository.CompetitionRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class CompetitionController {

    private final CompetitionRepository competitionRepository;

    public CompetitionController(CompetitionRepository competitionRepository) {
        this.competitionRepository = competitionRepository;
    }

    @GetMapping("/competitions")
    public String competitions(Model model) {
        model.addAttribute("competition", new Competition());
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

    @PostMapping("/competitions/{id}/status/{status}")
    public String updateStatus(@PathVariable Long id, @PathVariable String status) {
        Competition competition = competitionRepository.findById(id).orElseThrow();
        competition.setStatus(status.toUpperCase());
        competitionRepository.save(competition);
        return "redirect:/organizer?competitionId=" + id;
    }

    @GetMapping("/archive")
    public String archive(Model model) {
        model.addAttribute("competitions", competitionRepository.findByStatusOrderByCompetitionDateDescIdDesc("COMPLETE"));
        return "archive";
    }
}

