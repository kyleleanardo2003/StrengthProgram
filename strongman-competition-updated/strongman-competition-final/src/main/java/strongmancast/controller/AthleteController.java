package strongmancast.controller;

import strongmancast.model.Athlete;
import strongmancast.model.Competition;
import strongmancast.repository.AthleteRepository;
import strongmancast.repository.EventResultRepository;
import strongmancast.service.CompetitionContextService;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class AthleteController {

    private final AthleteRepository athleteRepository;
    private final EventResultRepository eventResultRepository;
    private final CompetitionContextService competitionContextService;

    public AthleteController(
            AthleteRepository athleteRepository,
            EventResultRepository eventResultRepository,
            CompetitionContextService competitionContextService
    ) {
        this.athleteRepository = athleteRepository;
        this.eventResultRepository = eventResultRepository;
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
    public String saveAthlete(
            @RequestParam(required = false) Long competitionId,
            @RequestParam(required = false) String divisionChoice,
            @RequestParam(required = false) String customDivision,
            @ModelAttribute Athlete athlete
    ) {
        Competition competition = competitionContextService.currentCompetition(competitionId);
        athlete.setCompetition(competition);
        applyDivisionChoice(athlete, divisionChoice, customDivision);
        athleteRepository.save(athlete);
        return "redirect:/athletes?competitionId=" + competition.getId();
    }

    @PostMapping("/athletes/{id}")
    public String updateAthlete(
            @PathVariable Long id,
            @RequestParam(required = false) String divisionChoice,
            @RequestParam(required = false) String customDivision,
            @ModelAttribute Athlete updatedAthlete
    ) {
        Athlete athlete = athleteRepository.findById(id).orElseThrow();
        athlete.setName(updatedAthlete.getName());
        athlete.setMembership(updatedAthlete.getMembership());
        athlete.setBodyweight(updatedAthlete.getBodyweight());
        applyDivisionChoice(athlete, divisionChoice, customDivision);
        athleteRepository.save(athlete);
        Long competitionId = athlete.getCompetition() == null ? null : athlete.getCompetition().getId();
        return "redirect:/organizer" + (competitionId == null ? "" : "?competitionId=" + competitionId);
    }

    @PostMapping("/athletes/{id}/delete")
    @Transactional
    public String deleteAthlete(@PathVariable Long id) {
        Athlete athlete = athleteRepository.findById(id).orElseThrow();
        Long competitionId = athlete.getCompetition() == null ? null : athlete.getCompetition().getId();
        eventResultRepository.deleteByAthlete(athlete);
        athleteRepository.delete(athlete);
        return "redirect:/athletes" + (competitionId == null ? "" : "?competitionId=" + competitionId);
    }

    @GetMapping("/athletes")
    public String listAthletes(@RequestParam(required = false) Long competitionId, Model model) {
        Competition competition = competitionContextService.currentCompetition(competitionId);
        model.addAttribute("athletes", athleteRepository.findByCompetitionOrderByDivisionAscNameAsc(competition));
        competitionContextService.addCompetitionModel(model, competition);
        return "athletes";
    }

    private void applyDivisionChoice(Athlete athlete, String divisionChoice, String customDivision) {
        String division = "";
        if ("OTHER".equals(divisionChoice)) {
            division = customDivision == null ? "" : customDivision.trim();
        } else if (divisionChoice != null && !divisionChoice.isBlank() && !"AUTO".equals(divisionChoice)) {
            division = divisionChoice.trim();
        }

        athlete.setDivision(division);
        athlete.setGender(genderForDivision(division));
        athlete.setEventGroup("");
    }

    private String genderForDivision(String division) {
        if (division == null || division.isBlank()) {
            return "";
        }
        if (division.startsWith("Women")) {
            return "Women";
        }
        if (division.startsWith("Men")) {
            return "Men";
        }
        if (division.contains("Adaptive")) {
            return "Adaptive";
        }
        return "";
    }
}

