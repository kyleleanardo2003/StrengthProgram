package strongmancast.controller;

import strongmancast.model.Competition;
import strongmancast.model.CompetitionEvent;
import strongmancast.model.CompetitionSettings;
import strongmancast.repository.CompetitionEventRepository;
import strongmancast.repository.CompetitionSettingsRepository;
import strongmancast.repository.EventResultRepository;
import strongmancast.service.CompetitionContextService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class EventSetupController {

    private final CompetitionEventRepository competitionEventRepository;
    private final CompetitionSettingsRepository competitionSettingsRepository;
    private final EventResultRepository eventResultRepository;
    private final CompetitionContextService competitionContextService;

    public EventSetupController(
            CompetitionEventRepository competitionEventRepository,
            CompetitionSettingsRepository competitionSettingsRepository,
            EventResultRepository eventResultRepository,
            CompetitionContextService competitionContextService
    ) {
        this.competitionEventRepository = competitionEventRepository;
        this.competitionSettingsRepository = competitionSettingsRepository;
        this.eventResultRepository = eventResultRepository;
        this.competitionContextService = competitionContextService;
    }

    @GetMapping("/events")
    public String events(@RequestParam(required = false) Long competitionId, Model model) {
        Competition competition = competitionContextService.currentCompetition(competitionId);
        model.addAttribute("event", new CompetitionEvent());
        model.addAttribute("events", competitionEventRepository.findByCompetitionOrderBySortOrderAscIdAsc(competition));
        model.addAttribute("settings", competitionContextService.settingsFor(competition));
        competitionContextService.addCompetitionModel(model, competition);
        return "event_setup";
    }

    @PostMapping("/settings/tiebreaker")
    public String updateTiebreaker(
            @RequestParam(required = false) Long competitionId,
            @RequestParam String overallTiebreaker
    ) {
        Competition competition = competitionContextService.currentCompetition(competitionId);
        CompetitionSettings settings = competitionContextService.settingsFor(competition);
        settings.setOverallTiebreaker(overallTiebreaker);
        competitionSettingsRepository.save(settings);
        return "redirect:/events?competitionId=" + competition.getId();
    }

    @PostMapping("/events")
    public String createEvent(@RequestParam(required = false) Long competitionId, @ModelAttribute CompetitionEvent event) {
        Competition competition = competitionContextService.currentCompetition(competitionId);
        event.setCompetition(competition);
        competitionEventRepository.save(event);
        return "redirect:/events?competitionId=" + competition.getId();
    }

    @PostMapping("/events/{id}")
    public String updateEvent(
            @PathVariable Long id,
            @RequestParam int sortOrder,
            @RequestParam String eventName,
            @RequestParam String resultLabel,
            @RequestParam(required = false) String defaultUnit,
            @RequestParam(defaultValue = "false") boolean higherIsBetter,
            @RequestParam(defaultValue = "false") boolean usesTime,
            @RequestParam(defaultValue = "false") boolean timeIsTieBreaker,
            @RequestParam(defaultValue = "false") boolean requiresCompletionForTimeRanking,
            @RequestParam(required = false) Double completionTarget,
            @RequestParam(required = false) String extraFields
    ) {
        CompetitionEvent event = saveEventFields(
                id,
                sortOrder,
                eventName,
                resultLabel,
                defaultUnit,
                higherIsBetter,
                usesTime,
                timeIsTieBreaker,
                requiresCompletionForTimeRanking,
                completionTarget,
                extraFields
        );
        Long competitionId = event.getCompetition() == null ? null : event.getCompetition().getId();
        return "redirect:/events" + (competitionId == null ? "" : "?competitionId=" + competitionId);
    }

    @PostMapping("/events/{id}/autosave")
    @ResponseBody
    public ResponseEntity<Void> autosaveEvent(
            @PathVariable Long id,
            @RequestParam int sortOrder,
            @RequestParam String eventName,
            @RequestParam String resultLabel,
            @RequestParam(required = false) String defaultUnit,
            @RequestParam(defaultValue = "false") boolean higherIsBetter,
            @RequestParam(defaultValue = "false") boolean usesTime,
            @RequestParam(defaultValue = "false") boolean timeIsTieBreaker,
            @RequestParam(defaultValue = "false") boolean requiresCompletionForTimeRanking,
            @RequestParam(required = false) Double completionTarget,
            @RequestParam(required = false) String extraFields
    ) {
        saveEventFields(
                id,
                sortOrder,
                eventName,
                resultLabel,
                defaultUnit,
                higherIsBetter,
                usesTime,
                timeIsTieBreaker,
                requiresCompletionForTimeRanking,
                completionTarget,
                extraFields
        );
        return ResponseEntity.noContent().build();
    }

    private CompetitionEvent saveEventFields(
            Long id,
            int sortOrder,
            String eventName,
            String resultLabel,
            String defaultUnit,
            boolean higherIsBetter,
            boolean usesTime,
            boolean timeIsTieBreaker,
            boolean requiresCompletionForTimeRanking,
            Double completionTarget,
            String extraFields
    ) {
        CompetitionEvent event = competitionEventRepository.findById(id).orElseThrow();
        event.setSortOrder(sortOrder);
        event.setEventName(eventName);
        event.setResultLabel(resultLabel);
        event.setDefaultUnit(defaultUnit);
        event.setHigherIsBetter(higherIsBetter);
        event.setUsesTime(usesTime);
        event.setTimeIsTieBreaker(timeIsTieBreaker);
        event.setRequiresCompletionForTimeRanking(requiresCompletionForTimeRanking);
        event.setCompletionTarget(completionTarget);
        event.setExtraFields(extraFields);
        return competitionEventRepository.save(event);
    }

    @PostMapping("/events/{id}/delete")
    @Transactional
    public String deleteEvent(@PathVariable Long id) {
        CompetitionEvent event = competitionEventRepository.findById(id).orElseThrow();
        Competition competition = event.getCompetition();
        if (competition != null) {
            eventResultRepository.deleteAll(eventResultRepository.findByCompetition(competition).stream()
                    .filter(result -> event.getEventName().equals(result.getEventName()))
                    .toList());
        }
        competitionEventRepository.delete(event);
        return "redirect:/events" + (competition == null ? "" : "?competitionId=" + competition.getId());
    }
}

