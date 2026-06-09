package strongmancast.controller;

import strongmancast.model.Athlete;
import strongmancast.model.Competition;
import strongmancast.model.CompetitionEvent;
import strongmancast.repository.AthleteRepository;
import strongmancast.repository.CompetitionEventRepository;
import strongmancast.service.CompetitionContextService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Controller
public class EventMonitorController {

    private final CompetitionEventRepository competitionEventRepository;
    private final AthleteRepository athleteRepository;
    private final CompetitionContextService competitionContextService;

    public EventMonitorController(
            CompetitionEventRepository competitionEventRepository,
            AthleteRepository athleteRepository,
            CompetitionContextService competitionContextService
    ) {
        this.competitionEventRepository = competitionEventRepository;
        this.athleteRepository = athleteRepository;
        this.competitionContextService = competitionContextService;
    }

    @GetMapping("/events/{id}/monitor")
    public String monitor(@PathVariable Long id, Model model) {
        CompetitionEvent event = competitionEventRepository.findById(id).orElseThrow();
        addMonitorModel(event, model);
        return "event_monitor";
    }

    @PostMapping("/events/{id}/monitor")
    public String updateMonitor(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") int activeSlots,
            @RequestParam(required = false) Long activeAthleteOneId,
            @RequestParam(required = false) Long activeAthleteTwoId
    ) {
        CompetitionEvent event = competitionEventRepository.findById(id).orElseThrow();
        int slots = activeSlots == 2 ? 2 : 1;
        event.setActiveSlots(slots);
        event.setActiveAthleteOneId(activeAthleteOneId);
        event.setActiveAthleteTwoId(slots == 2 ? activeAthleteTwoId : null);
        competitionEventRepository.save(event);
        return "redirect:/events/" + id + "/monitor";
    }

    @GetMapping("/events/{id}/live")
    public String liveEvent(@PathVariable Long id, Model model) {
        CompetitionEvent event = competitionEventRepository.findById(id).orElseThrow();
        addMonitorModel(event, model);
        return "event_live";
    }

    private void addMonitorModel(CompetitionEvent event, Model model) {
        Competition competition = event.getCompetition();
        List<Athlete> athletes = competition == null
                ? List.of()
                : athleteRepository.findByCompetitionOrderByDivisionAscNameAsc(competition);

        Set<Long> activeAthleteIds = new LinkedHashSet<>();
        if (event.getActiveAthleteOneId() != null) {
            activeAthleteIds.add(event.getActiveAthleteOneId());
        }
        if (Integer.valueOf(2).equals(event.getActiveSlots()) && event.getActiveAthleteTwoId() != null) {
            activeAthleteIds.add(event.getActiveAthleteTwoId());
        }

        List<Athlete> activeAthletes = athletes.stream()
                .filter(athlete -> activeAthleteIds.contains(athlete.getId()))
                .toList();

        model.addAttribute("event", event);
        model.addAttribute("athletes", athletes);
        model.addAttribute("activeAthleteIds", activeAthleteIds);
        model.addAttribute("activeAthletes", activeAthletes);
        model.addAttribute("competitionId", competition == null ? null : competition.getId());
        if (competition != null) {
            competitionContextService.addCompetitionModel(model, competition);
        }
    }
}
