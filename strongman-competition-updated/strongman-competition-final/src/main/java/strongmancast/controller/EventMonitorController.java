package strongmancast.controller;

import strongmancast.model.Athlete;
import strongmancast.model.Competition;
import strongmancast.model.CompetitionEvent;
import strongmancast.model.EventMonitorRow;
import strongmancast.model.EventResult;
import strongmancast.repository.AthleteRepository;
import strongmancast.repository.CompetitionEventRepository;
import strongmancast.repository.EventResultRepository;
import strongmancast.service.CompetitionContextService;
import strongmancast.service.WeightClassService;
import org.springframework.stereotype.Controller;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Controller
public class EventMonitorController {

    private final CompetitionEventRepository competitionEventRepository;
    private final AthleteRepository athleteRepository;
    private final EventResultRepository eventResultRepository;
    private final CompetitionContextService competitionContextService;
    private final WeightClassService weightClassService;

    public EventMonitorController(
            CompetitionEventRepository competitionEventRepository,
            AthleteRepository athleteRepository,
            EventResultRepository eventResultRepository,
            CompetitionContextService competitionContextService,
            WeightClassService weightClassService
    ) {
        this.competitionEventRepository = competitionEventRepository;
        this.athleteRepository = athleteRepository;
        this.eventResultRepository = eventResultRepository;
        this.competitionContextService = competitionContextService;
        this.weightClassService = weightClassService;
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
            @RequestParam(defaultValue = "1") int activeSlots
    ) {
        CompetitionEvent event = competitionEventRepository.findById(id).orElseThrow();
        int slots = activeSlots == 2 ? 2 : 1;
        event.setActiveSlots(slots);
        competitionEventRepository.save(event);
        return "redirect:/events/" + id + "/monitor";
    }

    @PostMapping("/events/{id}/monitor/scores")
    public String saveMonitorScores(@PathVariable Long id, @RequestParam Map<String, String> params) {
        CompetitionEvent event = competitionEventRepository.findById(id).orElseThrow();
        saveScoresForEvent(event, params);
        return "redirect:/events/" + id + "/monitor";
    }

    @PostMapping("/events/{id}/monitor/scores/autosave")
    @ResponseBody
    public ResponseEntity<Void> autosaveMonitorScores(@PathVariable Long id, @RequestParam Map<String, String> params) {
        CompetitionEvent event = competitionEventRepository.findById(id).orElseThrow();
        saveScoresForEvent(event, params);
        return ResponseEntity.noContent().build();
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

        List<EventResult> eventResults = competition == null
                ? List.of()
                : eventResultRepository.findByCompetition(competition).stream()
                        .filter(result -> event.getEventName().equals(result.getEventName()))
                        .toList();
        Map<Long, EventResult> resultsByAthleteId = eventResults.stream()
                .collect(Collectors.toMap(result -> result.getAthlete().getId(), Function.identity(), (first, second) -> first));

        Set<Long> scoredAthleteIds = new LinkedHashSet<>();
        for (EventResult result : eventResults) {
            if (hasScore(result)) {
                scoredAthleteIds.add(result.getAthlete().getId());
            }
        }

        int activeSlots = Integer.valueOf(2).equals(event.getActiveSlots()) ? 2 : 1;
        Set<Long> activeAthleteIds = new LinkedHashSet<>();
        Set<Long> nextInHoleAthleteIds = new LinkedHashSet<>();
        Set<Long> nextToHoleAthleteIds = new LinkedHashSet<>();
        List<Athlete> runOrderAthletes = orderAthletesForEvent(event, competition, athletes);

        for (Athlete athlete : runOrderAthletes) {
            if (scoredAthleteIds.contains(athlete.getId())) {
                continue;
            }

            if (activeAthleteIds.size() < activeSlots) {
                activeAthleteIds.add(athlete.getId());
            } else if (nextInHoleAthleteIds.size() < activeSlots) {
                nextInHoleAthleteIds.add(athlete.getId());
            } else if (nextToHoleAthleteIds.size() < activeSlots) {
                nextToHoleAthleteIds.add(athlete.getId());
            } else {
                break;
            }
        }

        Map<Long, Integer> divisionPlaces = calculateDivisionPlaces(event, athletes, resultsByAthleteId);
        Map<Long, Integer> overallPlaces = calculateOverallPlaces(competition, athletes);
        Map<String, List<EventMonitorRow>> rowsByGroup = new LinkedHashMap<>();
        for (Athlete athlete : runOrderAthletes) {
            String status = "";
            if (activeAthleteIds.contains(athlete.getId())) {
                status = "ON_STAGE";
            } else if (nextInHoleAthleteIds.contains(athlete.getId())) {
                status = "NEXT_IN_HOLE";
            } else if (nextToHoleAthleteIds.contains(athlete.getId())) {
                status = "NEXT_TO_HOLE";
            }

            EventResult result = resultsByAthleteId.get(athlete.getId());
            EventMonitorRow row = new EventMonitorRow(
                    athlete,
                    result,
                    divisionFor(athlete),
                    formatScore(result),
                    divisionPlaces.get(athlete.getId()),
                    overallPlaces.get(athlete.getId()),
                    status
            );
            rowsByGroup.computeIfAbsent(eventGroupFor(athlete), key -> new ArrayList<>()).add(row);
        }

        rowsByGroup.values().forEach(rows -> rows.sort(this::compareRunOrderRows));
        rowsByGroup = rowsByGroup.entrySet().stream()
                .sorted((first, second) -> Integer.compare(groupRunOrderRank(first.getValue()), groupRunOrderRank(second.getValue())))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (first, second) -> first,
                        LinkedHashMap::new
                ));

        model.addAttribute("event", event);
        model.addAttribute("athletes", athletes);
        model.addAttribute("rowsByGroup", rowsByGroup);
        model.addAttribute("activeAthleteIds", activeAthleteIds);
        model.addAttribute("nextInHoleAthleteIds", nextInHoleAthleteIds);
        model.addAttribute("nextToHoleAthleteIds", nextToHoleAthleteIds);
        model.addAttribute("scoredAthleteIds", scoredAthleteIds);
        model.addAttribute("competitionId", competition == null ? null : competition.getId());
        if (competition != null) {
            competitionContextService.addCompetitionModel(model, competition);
        }
    }

    private boolean hasScore(EventResult result) {
        return result.getResult() != null || result.getTime() != null || result.getSecondaryTime() != null;
    }

    private void saveScoresForEvent(CompetitionEvent event, Map<String, String> params) {
        Competition competition = event.getCompetition();
        if (competition == null) {
            return;
        }

        for (Athlete athlete : athleteRepository.findByCompetitionOrderByDivisionAscNameAsc(competition)) {
            String suffix = "__" + athlete.getId();
            Double resultValue = parseOptionalDouble(params.get("result" + suffix));
            String unit = params.get("unit" + suffix);
            Double time = parseTime(params, suffix, "time");
            Double secondaryTime = parseTime(params, suffix, "secondaryTime");

            EventResult eventResult = eventResultRepository
                    .findByAthleteAndCompetitionAndEventName(athlete, competition, event.getEventName())
                    .orElseGet(EventResult::new);
            if (eventResult.getId() == null && resultValue == null && time == null && secondaryTime == null) {
                continue;
            }

            eventResult.setAthlete(athlete);
            eventResult.setCompetition(competition);
            eventResult.setEventName(event.getEventName());
            eventResult.setResult(resultValue);
            eventResult.setUnit(resolveUnit(event, unit));
            eventResult.setTime(time);
            eventResult.setSecondaryTime(secondaryTime);
            eventResultRepository.save(eventResult);
        }
    }

    private Double parseTime(Map<String, String> params, String suffix, String prefix) {
        Double directTime = parseOptionalDouble(params.get(prefix + suffix));
        if (directTime != null) {
            return directTime;
        }

        Double minutes = parseOptionalDouble(params.get(prefix + "Minutes" + suffix));
        Double seconds = parseOptionalDouble(params.get(prefix + "Seconds" + suffix));

        if (minutes == null && seconds == null) {
            return null;
        }

        return (minutes == null ? 0.0 : minutes * 60) + (seconds == null ? 0.0 : seconds);
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

    private String resolveUnit(CompetitionEvent event, String unit) {
        if (unit != null && !unit.isBlank()) {
            return unit;
        }
        return event.getDefaultUnit();
    }

    private int compareRunOrderRows(EventMonitorRow first, EventMonitorRow second) {
        return Integer.compare(rowRunOrderRank(first), rowRunOrderRank(second));
    }

    private int groupRunOrderRank(List<EventMonitorRow> rows) {
        return rows.stream()
                .mapToInt(this::rowRunOrderRank)
                .min()
                .orElse(99);
    }

    private int rowRunOrderRank(EventMonitorRow row) {
        if (row.isOnStage()) {
            return 0;
        }
        if (row.isNextInHole()) {
            return 1;
        }
        if (row.isNextToHole()) {
            return 2;
        }
        if (row.getScoreDisplay() == null || row.getScoreDisplay().isBlank()) {
            return 3;
        }
        return 4;
    }

    private List<Athlete> orderAthletesForEvent(CompetitionEvent event, Competition competition, List<Athlete> athletes) {
        if (competition == null) {
            return athletes;
        }

        List<CompetitionEvent> configuredEvents = competitionEventRepository.findByCompetitionOrderBySortOrderAscIdAsc(competition);
        CompetitionEvent previousEvent = null;
        for (int index = 0; index < configuredEvents.size(); index++) {
            if (configuredEvents.get(index).getId().equals(event.getId())) {
                if (index > 0) {
                    previousEvent = configuredEvents.get(index - 1);
                }
                break;
            }
        }

        if (previousEvent == null) {
            return athletes;
        }

        CompetitionEvent rankingEvent = previousEvent;
        Map<Long, EventResult> previousResultsByAthleteId = eventResultRepository.findByCompetition(competition).stream()
                .filter(result -> rankingEvent.getEventName().equals(result.getEventName()))
                .collect(Collectors.toMap(result -> result.getAthlete().getId(), Function.identity(), (first, second) -> first));
        Map<Long, Integer> previousPlaces = calculateDivisionPlaces(rankingEvent, athletes, previousResultsByAthleteId);

        return athletes.stream()
                .sorted(Comparator
                        .comparing(this::divisionFor)
                        .thenComparing(athlete -> previousPlaces.getOrDefault(athlete.getId(), Integer.MAX_VALUE))
                        .thenComparing(Athlete::getName, Comparator.nullsLast(String::compareToIgnoreCase)))
                .toList();
    }

    private Map<Long, Integer> calculateOverallPlaces(Competition competition, List<Athlete> athletes) {
        Map<Long, Integer> places = new LinkedHashMap<>();
        if (competition == null) {
            return places;
        }

        List<CompetitionEvent> configuredEvents = competitionEventRepository.findByCompetitionOrderBySortOrderAscIdAsc(competition);
        Map<String, List<Athlete>> athletesByDivision = new LinkedHashMap<>();
        for (Athlete athlete : athletes) {
            athletesByDivision.computeIfAbsent(divisionFor(athlete), key -> new ArrayList<>()).add(athlete);
        }

        Map<String, Map<Long, EventResult>> resultsByEventName = new LinkedHashMap<>();
        for (EventResult result : eventResultRepository.findByCompetition(competition)) {
            resultsByEventName
                    .computeIfAbsent(result.getEventName(), key -> new LinkedHashMap<>())
                    .put(result.getAthlete().getId(), result);
        }

        for (List<Athlete> divisionAthletes : athletesByDivision.values()) {
            Map<Long, Double> totals = new LinkedHashMap<>();
            for (Athlete athlete : divisionAthletes) {
                totals.put(athlete.getId(), 0.0);
            }

            for (CompetitionEvent configuredEvent : configuredEvents) {
                Map<Long, EventResult> eventResults = resultsByEventName.getOrDefault(configuredEvent.getEventName(), Map.of());
                List<Athlete> scoredAthletes = divisionAthletes.stream()
                        .filter(athlete -> {
                            EventResult result = eventResults.get(athlete.getId());
                            return result != null && hasScore(result);
                        })
                        .sorted((first, second) -> compareResults(configuredEvent, eventResults.get(first.getId()), eventResults.get(second.getId())))
                        .toList();

                for (int index = 0; index < scoredAthletes.size(); ) {
                    int tieEnd = index;
                    while (tieEnd + 1 < scoredAthletes.size()
                            && sameResult(configuredEvent, eventResults.get(scoredAthletes.get(index).getId()), eventResults.get(scoredAthletes.get(tieEnd + 1).getId()))) {
                        tieEnd++;
                    }

                    double sumPoints = 0.0;
                    for (int placeIndex = index; placeIndex <= tieEnd; placeIndex++) {
                        sumPoints += divisionAthletes.size() - placeIndex;
                    }
                    double averagePoints = sumPoints / (tieEnd - index + 1);

                    for (int placeIndex = index; placeIndex <= tieEnd; placeIndex++) {
                        Athlete athlete = scoredAthletes.get(placeIndex);
                        totals.put(athlete.getId(), totals.get(athlete.getId()) + averagePoints);
                    }

                    index = tieEnd + 1;
                }
            }

            List<Athlete> sortedByTotal = divisionAthletes.stream()
                    .sorted((first, second) -> Double.compare(totals.get(second.getId()), totals.get(first.getId())))
                    .toList();
            for (int index = 0; index < sortedByTotal.size(); index++) {
                if (index > 0 && Double.compare(totals.get(sortedByTotal.get(index - 1).getId()), totals.get(sortedByTotal.get(index).getId())) == 0) {
                    places.put(sortedByTotal.get(index).getId(), places.get(sortedByTotal.get(index - 1).getId()));
                } else {
                    places.put(sortedByTotal.get(index).getId(), index + 1);
                }
            }
        }

        return places;
    }

    private Map<Long, Integer> calculateDivisionPlaces(
            CompetitionEvent event,
            List<Athlete> athletes,
            Map<Long, EventResult> resultsByAthleteId
    ) {
        Map<String, List<Athlete>> athletesByDivision = new LinkedHashMap<>();
        for (Athlete athlete : athletes) {
            athletesByDivision.computeIfAbsent(divisionFor(athlete), key -> new ArrayList<>()).add(athlete);
        }

        Map<Long, Integer> places = new LinkedHashMap<>();
        for (List<Athlete> divisionAthletes : athletesByDivision.values()) {
            List<Athlete> scoredAthletes = divisionAthletes.stream()
                    .filter(athlete -> {
                        EventResult result = resultsByAthleteId.get(athlete.getId());
                        return result != null && hasScore(result);
                    })
                    .sorted((first, second) -> compareResults(event, resultsByAthleteId.get(first.getId()), resultsByAthleteId.get(second.getId())))
                    .toList();

            for (int index = 0; index < scoredAthletes.size(); ) {
                int tieEnd = index;
                while (tieEnd + 1 < scoredAthletes.size()
                        && sameResult(event, resultsByAthleteId.get(scoredAthletes.get(index).getId()), resultsByAthleteId.get(scoredAthletes.get(tieEnd + 1).getId()))) {
                    tieEnd++;
                }

                int place = index + 1;
                for (int placeIndex = index; placeIndex <= tieEnd; placeIndex++) {
                    places.put(scoredAthletes.get(placeIndex).getId(), place);
                }

                index = tieEnd + 1;
            }
        }

        return places;
    }

    private int compareResults(CompetitionEvent event, EventResult first, EventResult second) {
        if (first == null || second == null) {
            return Comparator.nullsLast(Comparator.comparing(EventResult::getId)).compare(first, second);
        }

        if (event.isRequiresCompletionForTimeRanking()) {
            boolean firstCompleted = completedTarget(event, first);
            boolean secondCompleted = completedTarget(event, second);
            if (firstCompleted && secondCompleted) {
                return Double.compare(valueOrMax(first.getTime()), valueOrMax(second.getTime()));
            }
            if (firstCompleted != secondCompleted) {
                return firstCompleted ? -1 : 1;
            }
        }

        if (event.isUsesTime() && !event.isTimeIsTieBreaker()) {
            return Double.compare(valueOrMax(first.getTime()), valueOrMax(second.getTime()));
        }

        double firstResult = valueOrZero(first.getResult());
        double secondResult = valueOrZero(second.getResult());
        int resultCompare = event.isHigherIsBetter()
                ? Double.compare(secondResult, firstResult)
                : Double.compare(firstResult, secondResult);
        if (resultCompare != 0) {
            return resultCompare;
        }

        if (event.isUsesTime() && event.isTimeIsTieBreaker()) {
            return Double.compare(valueOrMax(first.getTime()), valueOrMax(second.getTime()));
        }

        return 0;
    }

    private boolean sameResult(CompetitionEvent event, EventResult first, EventResult second) {
        return compareResults(event, first, second) == 0;
    }

    private boolean completedTarget(CompetitionEvent event, EventResult result) {
        return event.getCompletionTarget() != null
                && result.getResult() != null
                && result.getResult() >= event.getCompletionTarget();
    }

    private double valueOrZero(Double value) {
        return value == null ? 0.0 : value;
    }

    private double valueOrMax(Double value) {
        return value == null ? Double.MAX_VALUE : value;
    }

    private String formatScore(EventResult result) {
        if (result == null || !hasScore(result)) {
            return "";
        }

        List<String> parts = new ArrayList<>();
        if (result.getResult() != null) {
            String score = formatNumber(result.getResult());
            if (result.getUnit() != null && !result.getUnit().isBlank()) {
                score += " " + result.getUnit();
            }
            parts.add(score);
        }
        if (result.getTime() != null) {
            parts.add(formatNumber(result.getTime()) + " sec");
        }
        if (result.getSecondaryTime() != null) {
            parts.add("Secondary " + formatNumber(result.getSecondaryTime()) + " sec");
        }
        return String.join(" / ", parts);
    }

    private String formatNumber(Double value) {
        return value % 1 == 0 ? String.valueOf(value.intValue()) : String.valueOf(value);
    }

    private String eventGroupFor(Athlete athlete) {
        if (athlete.getEventGroup() != null && !athlete.getEventGroup().isBlank()) {
            return athlete.getEventGroup();
        }
        return divisionFor(athlete);
    }

    private String divisionFor(Athlete athlete) {
        return weightClassService.resolveDivision(athlete);
    }
}
