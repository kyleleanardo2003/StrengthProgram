package strongmancast.controller;

import strongmancast.model.CompetitionEvent;
import strongmancast.model.Athlete;
import strongmancast.model.Competition;
import strongmancast.model.EventResult;
import strongmancast.repository.AthleteRepository;
import strongmancast.repository.CompetitionEventRepository;
import strongmancast.repository.EventResultRepository;
import strongmancast.service.CompetitionContextService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@Controller
public class ScoreController {

    private final AthleteRepository athleteRepository;
    private final EventResultRepository eventResultRepository;
    private final CompetitionEventRepository competitionEventRepository;
    private final CompetitionContextService competitionContextService;

    public ScoreController(
            AthleteRepository athleteRepository,
            EventResultRepository eventResultRepository,
            CompetitionEventRepository competitionEventRepository,
            CompetitionContextService competitionContextService
    ) {
        this.athleteRepository = athleteRepository;
        this.eventResultRepository = eventResultRepository;
        this.competitionEventRepository = competitionEventRepository;
        this.competitionContextService = competitionContextService;
    }

    @GetMapping("/scores")
    public String showScoreForm() {
        return "redirect:/organizer";
    }

    @PostMapping("/scores")
    public String saveScore(
            @RequestParam Long athleteId,
            @RequestParam String eventName,
            @RequestParam(required = false) Double result,
            @RequestParam(required = false) String unit,
            @RequestParam(required = false) Double time,
            @RequestParam(required = false) String returnTo,
            @RequestParam(required = false) Long competitionId
    ) {
        Competition competition = competitionContextService.currentCompetition(competitionId);
        Athlete athlete = athleteRepository.findById(athleteId).orElseThrow();
        EventResult eventResult = eventResultRepository
                .findByAthleteAndCompetitionAndEventName(athlete, competition, eventName)
                .orElseGet(EventResult::new);

        eventResult.setAthlete(athlete);
        eventResult.setCompetition(competition);
        eventResult.setEventName(eventName);
        eventResult.setResult(result);
        eventResult.setUnit(resolveUnit(competition, eventName, unit));
        eventResult.setTime(time);
        eventResultRepository.save(eventResult);

        return redirectTo(returnTo, "/organizer");
    }

    @PostMapping("/scores/bulk")
    public String saveScoresBulk(
            @RequestParam(required = false) Long competitionId,
            @RequestParam Map<String, String> params
    ) {
        Competition competition = competitionContextService.currentCompetition(competitionId);
        saveScoresBulkForCompetition(competition, params);
        return "redirect:/organizer?competitionId=" + competition.getId();
    }

    @PostMapping("/scores/bulk/autosave")
    @ResponseBody
    public ResponseEntity<Void> autosaveScoresBulk(
            @RequestParam(required = false) Long competitionId,
            @RequestParam Map<String, String> params
    ) {
        Competition competition = competitionContextService.currentCompetition(competitionId);
        saveScoresBulkForCompetition(competition, params);
        return ResponseEntity.noContent().build();
    }

    private void saveScoresBulkForCompetition(Competition competition, Map<String, String> params) {
        saveBulkAthletes(competition, params);

        for (Map.Entry<String, String> entry : params.entrySet()) {
            String key = entry.getKey();
            ScoreFieldKey scoreFieldKey = parseScoreFieldKey(key);
            if (scoreFieldKey == null) {
                continue;
            }

            Long athleteId = scoreFieldKey.athleteId();
            Long eventId = scoreFieldKey.eventId();
            if (athleteId == null || eventId == null) {
                continue;
            }

            Optional<Athlete> athlete = athleteRepository.findById(athleteId);
            Optional<CompetitionEvent> event = competitionEventRepository.findById(eventId)
                    .filter(competitionEvent -> sameCompetition(competition, competitionEvent.getCompetition()));
            if (athlete.isEmpty() || event.isEmpty()) {
                continue;
            }

            String suffix = scoreFieldKey.separator() + athleteId + scoreFieldKey.separator() + eventId;
            Double result = parseOptionalDouble(params.get("result" + suffix));
            String unit = params.get("unit" + suffix);
            Double time = parseTime(params, suffix);

            Optional<EventResult> existingResult = eventResultRepository
                    .findByAthleteAndCompetitionAndEventName(athlete.get(), competition, event.get().getEventName());
            if (existingResult.isEmpty() && result == null && time == null) {
                continue;
            }

            EventResult eventResult = existingResult.orElseGet(EventResult::new);
            eventResult.setAthlete(athlete.get());
            eventResult.setCompetition(competition);
            eventResult.setEventName(event.get().getEventName());
            eventResult.setResult(result);
            eventResult.setUnit(resolveUnit(competition, event.get().getEventName(), unit));
            eventResult.setTime(time);
            eventResultRepository.save(eventResult);
        }
    }

    private ScoreFieldKey parseScoreFieldKey(String key) {
        if (key.startsWith("result__")) {
            String[] parts = key.split("__");
            if (parts.length == 3) {
                return new ScoreFieldKey(parseLong(parts[1]), parseLong(parts[2]), "__");
            }
        }

        if (key.startsWith("result_")) {
            String[] parts = key.split("_");
            if (parts.length == 3) {
                return new ScoreFieldKey(parseLong(parts[1]), parseLong(parts[2]), "_");
            }
        }

        return null;
    }

    private void saveBulkAthletes(Competition competition, Map<String, String> params) {
        for (Map.Entry<String, String> entry : params.entrySet()) {
            String key = entry.getKey();
            if (!key.startsWith("athleteName__")) {
                continue;
            }

            Long athleteId = parseLong(key.substring("athleteName__".length()));
            if (athleteId == null) {
                continue;
            }

            Optional<Athlete> athlete = athleteRepository.findById(athleteId)
                    .filter(foundAthlete -> sameCompetition(competition, foundAthlete.getCompetition()));
            if (athlete.isEmpty()) {
                continue;
            }

            String suffix = "__" + athleteId;
            Athlete updatedAthlete = athlete.get();
            updatedAthlete.setName(entry.getValue());
            updatedAthlete.setMembership(params.getOrDefault("membership" + suffix, ""));
            updatedAthlete.setBodyweight(parseDouble(params.get("bodyweight" + suffix)));
            updatedAthlete.setDivision(params.getOrDefault("division" + suffix, ""));
            athleteRepository.save(updatedAthlete);
        }
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
        String fallback = eventResult.getCompetition() == null
                ? "/organizer"
                : "/organizer?competitionId=" + eventResult.getCompetition().getId();
        return redirectTo(returnTo, fallback);
    }

    private Long parseLong(String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private boolean sameCompetition(Competition expected, Competition actual) {
        return expected != null
                && actual != null
                && expected.getId() != null
                && expected.getId().equals(actual.getId());
    }

    private Double parseOptionalDouble(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private double parseDouble(String value) {
        Double parsed = parseOptionalDouble(value);
        return parsed == null ? 0.0 : parsed;
    }

    private Double parseTime(Map<String, String> params, String suffix) {
        Double directTime = parseOptionalDouble(params.get("time" + suffix));
        if (directTime != null) {
            return directTime;
        }

        Double minutes = parseOptionalDouble(params.get("timeMinutes" + suffix));
        Double seconds = parseOptionalDouble(params.get("timeSeconds" + suffix));
        if (minutes == null && seconds == null) {
            return null;
        }

        return (minutes == null ? 0.0 : minutes * 60.0) + (seconds == null ? 0.0 : seconds);
    }

    private String resolveUnit(Competition competition, String eventName, String requestedUnit) {
        if (requestedUnit != null && !requestedUnit.isBlank()) {
            return requestedUnit;
        }

        return competitionEventRepository.findByCompetitionAndEventName(competition, eventName)
                .map(CompetitionEvent::getDefaultUnit)
                .orElse("");
    }

    private String redirectTo(String returnTo, String fallback) {
        if (returnTo == null || returnTo.isBlank() || !returnTo.startsWith("/")) {
            return "redirect:" + fallback;
        }

        return "redirect:" + returnTo;
    }

    private record ScoreFieldKey(Long athleteId, Long eventId, String separator) {
    }
}

