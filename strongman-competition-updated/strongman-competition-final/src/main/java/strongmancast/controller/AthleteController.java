package strongmancast.controller;

import strongmancast.model.Athlete;
import strongmancast.model.Competition;
import strongmancast.repository.AthleteRepository;
import strongmancast.service.CompetitionContextService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class AthleteController {

    private final AthleteRepository athleteRepository;
    private final CompetitionContextService competitionContextService;

    public AthleteController(AthleteRepository athleteRepository, CompetitionContextService competitionContextService) {
        this.athleteRepository = athleteRepository;
        this.competitionContextService = competitionContextService;
    }

    @GetMapping("/athletes/new")
    public String showForm(@RequestParam(required = false) Long competitionId, Model model) {
        Competition competition = competitionContextService.currentCompetition(competitionId);
        model.addAttribute("athlete", new Athlete());
        model.addAttribute("formTitle", "Add Athlete");
        model.addAttribute("formAction", "/athletes?competitionId=" + competition.getId());
        competitionContextService.addCompetitionModel(model, competition);
        return "athlete_form";
    }

    @GetMapping("/athletes/{id}/edit")
    public String editAthlete(@PathVariable Long id, Model model) {
        Athlete athlete = athleteRepository.findById(id).orElseThrow();
        Competition competition = athlete.getCompetition() == null
                ? competitionContextService.currentCompetition(null)
                : athlete.getCompetition();
        model.addAttribute("athlete", athlete);
        model.addAttribute("formTitle", "Edit Athlete");
        model.addAttribute("formAction", "/athletes/" + id);
        competitionContextService.addCompetitionModel(model, competition);
        return "athlete_form";
    }

    @PostMapping("/athletes")
    public String saveAthlete(@RequestParam(required = false) Long competitionId, @ModelAttribute Athlete athlete) {
        Competition competition = competitionContextService.currentCompetition(competitionId);
        athlete.setCompetition(competition);
        athleteRepository.save(athlete);
        return "redirect:/athletes?competitionId=" + competition.getId();
    }

    @PostMapping("/athletes/{id}")
    public String updateAthlete(@PathVariable Long id, @ModelAttribute Athlete updatedAthlete) {
        Athlete athlete = athleteRepository.findById(id).orElseThrow();
        athlete.setName(updatedAthlete.getName());
        athlete.setMembership(updatedAthlete.getMembership());
        athlete.setBodyweight(updatedAthlete.getBodyweight());
        athlete.setDivision(updatedAthlete.getDivision());
        athleteRepository.save(athlete);
        Long competitionId = athlete.getCompetition() == null ? null : athlete.getCompetition().getId();
        return "redirect:/organizer" + (competitionId == null ? "" : "?competitionId=" + competitionId);
    }

    @GetMapping("/athletes")
    public String listAthletes(@RequestParam(required = false) Long competitionId, Model model) {
        Competition competition = competitionContextService.currentCompetition(competitionId);
        model.addAttribute("athletes", athleteRepository.findByCompetitionOrderByDivisionAscNameAsc(competition));
        competitionContextService.addCompetitionModel(model, competition);
        return "athletes";
    }
}

