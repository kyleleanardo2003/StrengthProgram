package com.example.strongman916.strongman_competition.controller;

import com.example.strongman916.strongman_competition.model.CompetitionEvent;
import com.example.strongman916.strongman_competition.model.CompetitionSettings;
import com.example.strongman916.strongman_competition.repository.CompetitionEventRepository;
import com.example.strongman916.strongman_competition.repository.CompetitionSettingsRepository;
import com.example.strongman916.strongman_competition.repository.EventResultRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class EventSetupController {

    private final CompetitionEventRepository competitionEventRepository;
    private final CompetitionSettingsRepository competitionSettingsRepository;
    private final EventResultRepository eventResultRepository;

    public EventSetupController(
            CompetitionEventRepository competitionEventRepository,
            CompetitionSettingsRepository competitionSettingsRepository,
            EventResultRepository eventResultRepository
    ) {
        this.competitionEventRepository = competitionEventRepository;
        this.competitionSettingsRepository = competitionSettingsRepository;
        this.eventResultRepository = eventResultRepository;
    }

    @GetMapping("/events")
    public String events(Model model) {
        model.addAttribute("event", new CompetitionEvent());
        model.addAttribute("events", competitionEventRepository.findAllByOrderBySortOrderAscIdAsc());
        model.addAttribute("settings", loadSettings());
        return "event_setup";
    }

    @PostMapping("/settings/tiebreaker")
    public String updateTiebreaker(@RequestParam String overallTiebreaker) {
        CompetitionSettings settings = loadSettings();
        settings.setOverallTiebreaker(overallTiebreaker);
        competitionSettingsRepository.save(settings);
        return "redirect:/events";
    }

    @PostMapping("/events")
    public String createEvent(@ModelAttribute CompetitionEvent event) {
        competitionEventRepository.save(event);
        return "redirect:/events";
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
        competitionEventRepository.save(event);
        return "redirect:/events";
    }

    @PostMapping("/events/{id}/delete")
    public String deleteEvent(@PathVariable Long id) {
        CompetitionEvent event = competitionEventRepository.findById(id).orElseThrow();
        eventResultRepository.deleteAll(eventResultRepository.findByEventName(event.getEventName()));
        competitionEventRepository.delete(event);
        return "redirect:/events";
    }

    private CompetitionSettings loadSettings() {
        return competitionSettingsRepository.findById(1L).orElseGet(() -> {
            CompetitionSettings settings = new CompetitionSettings();
            settings.setId(1L);
            settings.setOverallTiebreaker("EVENT_PLACINGS");
            return competitionSettingsRepository.save(settings);
        });
    }
}
