package com.example.strongman916.strongman_competition.controller;

import com.example.strongman916.strongman_competition.model.CompetitionEvent;
import com.example.strongman916.strongman_competition.model.Athlete;
import com.example.strongman916.strongman_competition.model.EventResult;
import com.example.strongman916.strongman_competition.repository.AthleteRepository;
import com.example.strongman916.strongman_competition.repository.CompetitionEventRepository;
import com.example.strongman916.strongman_competition.repository.EventResultRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@Controller
public class ScoreController {

    private final AthleteRepository athleteRepository;
    private final EventResultRepository eventResultRepository;
    private final CompetitionEventRepository competitionEventRepository;

    public ScoreController(
            AthleteRepository athleteRepository,
            EventResultRepository eventResultRepository,
            CompetitionEventRepository competitionEventRepository
    ) {
        this.athleteRepository = athleteRepository;
        this.eventResultRepository = eventResultRepository;
        this.competitionEventRepository = competitionEventRepository;
    }

    @GetMapping("/scores")
    public String showScoreForm(Model model) {
        model.addAttribute("athletes", athleteRepository.findAll());
        model.addAttribute("events", competitionEventRepository.findAllByOrderBySortOrderAscIdAsc());
        model.addAttribute("results", eventResultRepository.findAll());
        return "score_form";
    }

    @PostMapping("/scores")
    public String saveScore(
            @RequestParam Long athleteId,
            @RequestParam String eventName,
            @RequestParam(required = false) Double result,
            @RequestParam(required = false) String unit,
            @RequestParam(required = false) Double time,
            @RequestParam(required = false) String returnTo
    ) {
        Athlete athlete = athleteRepository.findById(athleteId).orElseThrow();
        EventResult eventResult = eventResultRepository
                .findByAthleteAndEventName(athlete, eventName)
                .orElseGet(EventResult::new);

        eventResult.setAthlete(athlete);
        eventResult.setEventName(eventName);
        eventResult.setResult(result);
        eventResult.setUnit(resolveUnit(eventName, unit));
        eventResult.setTime(time);
        eventResultRepository.save(eventResult);

        return redirectTo(returnTo, "/scores");
    }

    @PostMapping("/scores/bulk")
    public String saveScoresBulk(@RequestParam Map<String, String> params) {
        for (Map.Entry<String, String> entry : params.entrySet()) {
            String key = entry.getKey();
            if (!key.startsWith("result__")) {
                continue;
            }

            String[] parts = key.split("__");
            if (parts.length != 3) {
                continue;
            }

            Long athleteId = parseLong(parts[1]);
            Long eventId = parseLong(parts[2]);
            if (athleteId == null || eventId == null) {
                continue;
            }

            Optional<Athlete> athlete = athleteRepository.findById(athleteId);
            Optional<CompetitionEvent> event = competitionEventRepository.findById(eventId);
            if (athlete.isEmpty() || event.isEmpty()) {
                continue;
            }

            String suffix = "__" + athleteId + "__" + eventId;
            Double result = parseOptionalDouble(params.get("result" + suffix));
            String unit = params.get("unit" + suffix);
            Double time = parseOptionalDouble(params.get("time" + suffix));

            Optional<EventResult> existingResult = eventResultRepository
                    .findByAthleteAndEventName(athlete.get(), event.get().getEventName());
            if (existingResult.isEmpty() && result == null && time == null) {
                continue;
            }

            EventResult eventResult = existingResult.orElseGet(EventResult::new);
            eventResult.setAthlete(athlete.get());
            eventResult.setEventName(event.get().getEventName());
            eventResult.setResult(result);
            eventResult.setUnit(resolveUnit(event.get().getEventName(), unit));
            eventResult.setTime(time);
            eventResultRepository.save(eventResult);
        }

        return "redirect:/organizer";
    }

    @PostMapping("/scores/{id}")
    public String updateScore(
            @PathVariable Long id,
            @RequestParam(required = false) Double result,
            @RequestParam(required = false) String unit,
            @RequestParam(required = false) Double time,
            @RequestParam(required = false) String returnTo
    ) {
        EventResult eventResult = eventResultRepository.findById(id).orElseThrow();
        eventResult.setResult(result);
        eventResult.setUnit(unit);
        eventResult.setTime(time);
        eventResultRepository.save(eventResult);
        return redirectTo(returnTo, "/organizer");
    }

    private Long parseLong(String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Double parseOptionalDouble(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return Double.parseDouble(value.trim());
    }

    private String resolveUnit(String eventName, String requestedUnit) {
        if (requestedUnit != null && !requestedUnit.isBlank()) {
            return requestedUnit;
        }

        return competitionEventRepository.findByEventName(eventName)
                .map(CompetitionEvent::getDefaultUnit)
                .orElse("");
    }

    private String redirectTo(String returnTo, String fallback) {
        if (returnTo == null || returnTo.isBlank() || !returnTo.startsWith("/")) {
            return "redirect:" + fallback;
        }

        return "redirect:" + returnTo;
    }
}
