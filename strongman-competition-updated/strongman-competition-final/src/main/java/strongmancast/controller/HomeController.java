package strongmancast.controller;

import strongmancast.model.Athlete;
import strongmancast.model.Competition;
import strongmancast.model.CompetitionEvent;
import strongmancast.model.Competitor;
import strongmancast.model.EventConfig;
import strongmancast.model.EventResult;
import strongmancast.model.OrganizerScoreRow;
import strongmancast.repository.AthleteRepository;
import strongmancast.repository.CompetitionEventRepository;
import strongmancast.repository.EventResultRepository;
import strongmancast.service.CompetitionContextService;
import strongmancast.service.WeightClassService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.security.Principal;
import java.util.*;

@Controller
public class HomeController {

    private final AthleteRepository athleteRepository;
    private final EventResultRepository eventResultRepository;
    private final CompetitionEventRepository competitionEventRepository;
    private final CompetitionContextService competitionContextService;
    private final WeightClassService weightClassService;

    public HomeController(
            AthleteRepository athleteRepository,
            EventResultRepository eventResultRepository,
            CompetitionEventRepository competitionEventRepository,
            CompetitionContextService competitionContextService,
            WeightClassService weightClassService
    ) {
        this.athleteRepository = athleteRepository;
        this.eventResultRepository = eventResultRepository;
        this.competitionEventRepository = competitionEventRepository;
        this.competitionContextService = competitionContextService;
        this.weightClassService = weightClassService;
    }

    @GetMapping("/")
    public String home() {
        return "redirect:/competitions";
    }

    @GetMapping("/results")
    public String home(@RequestParam(required = false) Long competitionId, Model model, Principal principal) {
        model.addAttribute("canEditCompetitions", principal != null);
        try {
            Competition competition = competitionContextService.currentCompetition(competitionId);
            addResultsModel(competition, model);
            competitionContextService.addCompetitionModel(model, competition);
        } catch (Exception e) {
            model.addAttribute("error", "Failed to load results: " + e.getMessage());
        }

        return "index";
    }

    @GetMapping("/organizer")
    public String organizer(@RequestParam(required = false) Long competitionId, Model model) {
        try {
            Competition competition = competitionContextService.currentCompetition(competitionId);
            Map<String, List<Competitor>> competitorsByDivision = buildCompetitorsByDivision(competition);
            model.addAttribute("events", loadEvents(competition));
            model.addAttribute("divisions", competitorsByDivision);
            model.addAttribute("athletes", athleteRepository.findByCompetitionOrderByDivisionAscNameAsc(competition));
            model.addAttribute("configuredEvents", competitionEventRepository.findByCompetitionOrderBySortOrderAscIdAsc(competition));
            model.addAttribute("scoreRowsByDivision", buildScoreRowsByDivision(competition, competitorsByDivision));
            competitionContextService.addCompetitionModel(model, competition);
        } catch (Exception e) {
            model.addAttribute("error", "Failed to load organizer results: " + e.getMessage());
        }

        return "organizer_results";
    }

    @PostMapping("/submit")
    public String submit(@RequestParam Map<String, String> params) {
        // Future use: update scores/times
        return "redirect:/";
    }

    private void addResultsModel(Competition competition, Model model) {
        Map<String, List<Competitor>> competitorsByDivision = buildCompetitorsByDivision(competition);

        model.addAttribute("events", loadEvents(competition));
        model.addAttribute("configuredEvents", competitionEventRepository.findByCompetitionOrderBySortOrderAscIdAsc(competition));
        model.addAttribute("divisions", competitorsByDivision);
    }

    private Map<String, List<Competitor>> buildCompetitorsByDivision(Competition competition) {
        List<Competitor> competitors = loadCompetitors(competition);

        Map<String, List<Competitor>> competitorsByDivision = new LinkedHashMap<>();
        for (Competitor comp : competitors) {
            String divisionKey = comp.getWeightClass();
            competitorsByDivision
                    .computeIfAbsent(divisionKey, k -> new ArrayList<>())
                    .add(comp);
        }

        calculateScores(competition, competitorsByDivision);
        applyCurrentEventTurnStatuses(competition, competitorsByDivision);
        return competitorsByDivision;
    }

    private List<EventConfig> loadEvents(Competition competition) {
        return competitionEventRepository.findByCompetitionOrderBySortOrderAscIdAsc(competition).stream()
                .map(event -> new EventConfig(
                        event.getEventName(),
                        event.isHigherIsBetter(),
                        event.isUsesTime(),
                        event.isTimeIsTieBreaker(),
                        event.isRequiresCompletionForTimeRanking(),
                        event.getCompletionTarget()
                ))
                .toList();
    }

    private Map<String, List<OrganizerScoreRow>> buildScoreRowsByDivision(Competition competition, Map<String, List<Competitor>> competitorsByDivision) {
        Map<String, List<OrganizerScoreRow>> rowsByDivision = new LinkedHashMap<>();
        Map<String, List<Athlete>> athletesByKey = new HashMap<>();
        for (Athlete athlete : athleteRepository.findByCompetitionOrderByDivisionAscNameAsc(competition)) {
            athletesByKey
                    .computeIfAbsent(athlete.getName() + "|" + displayDivision(athlete), key -> new ArrayList<>())
                    .add(athlete);
        }

        for (Map.Entry<String, List<Competitor>> divisionEntry : competitorsByDivision.entrySet()) {
            for (Competitor competitor : divisionEntry.getValue()) {
                List<Athlete> matchingAthletes = athletesByKey.get(competitor.getName() + "|" + competitor.getWeightClass());
                if (matchingAthletes == null || matchingAthletes.isEmpty()) {
                    continue;
                }

                Athlete athlete = matchingAthletes.remove(0);
                OrganizerScoreRow row = new OrganizerScoreRow(athlete);
                row.setCompetitor(competitor);
                row.setTurnStatus(competitor.getTurnStatus());

                for (EventResult result : eventResultRepository.findByAthleteAndCompetition(athlete, competition)) {
                    row.getResults().put(result.getEventName(), result);
                }

                rowsByDivision.computeIfAbsent(divisionEntry.getKey(), key -> new ArrayList<>()).add(row);
            }
        }

        return rowsByDivision;
    }

    private List<Competitor> loadCompetitors(Competition competition) {
        List<Competitor> competitors = new ArrayList<>();

        for (Athlete athlete : athleteRepository.findByCompetitionOrderByDivisionAscNameAsc(competition)) {
            Competitor competitor = new Competitor();
            competitor.setAthleteId(athlete.getId());
            competitor.setName(athlete.getName());
            competitor.setMembership(athlete.getMembership());
            competitor.setBodyWeight(athlete.getBodyweight());
            competitor.setGender("");
            competitor.setWeightClass(displayDivision(athlete));

            for (EventResult result : eventResultRepository.findByAthleteAndCompetition(athlete, competition)) {
                if (result.getResult() != null) {
                    competitor.getScores().put(result.getEventName(), result.getResult());
                    competitor.getDisplayResults().put(result.getEventName(), formatResult(result.getResult(), result.getUnit()));
                }
                if (result.getTime() != null) {
                    competitor.getTimes().put(result.getEventName(), result.getTime());
                    String timeDisplay = formatResult(result.getTime(), "sec");
                    appendDisplayResult(competitor, result.getEventName(), timeDisplay);
                }
                if (result.getSecondaryTime() != null) {
                    appendDisplayResult(competitor, result.getEventName(), "Secondary " + formatResult(result.getSecondaryTime(), "sec"));
                }
            }

            competitors.add(competitor);
        }

        return competitors;
    }

    private void applyCurrentEventTurnStatuses(Competition competition, Map<String, List<Competitor>> competitorsByDivision) {
        List<CompetitionEvent> events = competitionEventRepository.findByCompetitionOrderBySortOrderAscIdAsc(competition);
        if (events.isEmpty()) {
            return;
        }

        List<Competitor> competitors = competitorsByDivision.values().stream()
                .flatMap(List::stream)
                .toList();
        Map<Long, Competitor> competitorsByAthleteId = competitors.stream()
                .filter(competitor -> competitor.getAthleteId() != null)
                .collect(java.util.stream.Collectors.toMap(Competitor::getAthleteId, competitor -> competitor, (first, second) -> first));
        Map<String, EventResult> resultsByAthleteAndEvent = eventResultRepository.findByCompetition(competition).stream()
                .collect(java.util.stream.Collectors.toMap(
                        result -> result.getAthlete().getId() + "|" + result.getEventName(),
                        result -> result,
                        (first, second) -> first));

        CompetitionEvent currentEvent = currentEvent(events, competitors, resultsByAthleteAndEvent);
        if (currentEvent == null) {
            return;
        }

        List<Competitor> runOrder = runOrderForCurrentEvent(events, currentEvent, competitorsByDivision);
        int activeSlots = Integer.valueOf(2).equals(currentEvent.getActiveSlots()) ? 2 : 1;
        int activeCount = 0;
        int holeCount = 0;
        int nextHoleCount = 0;

        for (Competitor competitor : runOrder) {
            EventResult result = resultsByAthleteAndEvent.get(competitor.getAthleteId() + "|" + currentEvent.getEventName());
            if (result != null && hasScore(result)) {
                continue;
            }

            Competitor highlightedCompetitor = competitorsByAthleteId.get(competitor.getAthleteId());
            if (highlightedCompetitor == null) {
                continue;
            }

            if (activeCount < activeSlots) {
                highlightedCompetitor.setTurnStatus("ON_STAGE");
                activeCount++;
            } else if (holeCount < activeSlots) {
                highlightedCompetitor.setTurnStatus("NEXT_IN_HOLE");
                holeCount++;
            } else if (nextHoleCount < activeSlots) {
                highlightedCompetitor.setTurnStatus("NEXT_TO_HOLE");
                nextHoleCount++;
            } else {
                break;
            }
        }
    }

    private CompetitionEvent currentEvent(
            List<CompetitionEvent> events,
            List<Competitor> competitors,
            Map<String, EventResult> resultsByAthleteAndEvent
    ) {
        for (CompetitionEvent event : events) {
            boolean hasUnscoredCompetitor = competitors.stream().anyMatch(competitor -> {
                EventResult result = resultsByAthleteAndEvent.get(competitor.getAthleteId() + "|" + event.getEventName());
                return result == null || !hasScore(result);
            });
            if (hasUnscoredCompetitor) {
                return event;
            }
        }
        return null;
    }

    private List<Competitor> runOrderForCurrentEvent(
            List<CompetitionEvent> events,
            CompetitionEvent currentEvent,
            Map<String, List<Competitor>> competitorsByDivision
    ) {
        List<Competitor> runOrder = new ArrayList<>();
        int currentIndex = events.indexOf(currentEvent);
        String previousEventName = currentIndex > 0 ? events.get(currentIndex - 1).getEventName() : null;

        for (List<Competitor> divisionCompetitors : competitorsByDivision.values()) {
            List<Competitor> orderedDivision = new ArrayList<>(divisionCompetitors);
            if (previousEventName != null) {
                orderedDivision.sort(Comparator
                        .comparing((Competitor competitor) -> competitor.getEventPlacements().getOrDefault(previousEventName, Integer.MAX_VALUE))
                        .thenComparing(Competitor::getName, Comparator.nullsLast(String::compareToIgnoreCase)));
            } else {
                orderedDivision.sort(Comparator.comparing(Competitor::getName, Comparator.nullsLast(String::compareToIgnoreCase)));
            }
            runOrder.addAll(orderedDivision);
        }

        return runOrder;
    }

    private boolean hasScore(EventResult result) {
        return result.getResult() != null || result.getTime() != null || result.getSecondaryTime() != null;
    }

    private void appendDisplayResult(Competitor competitor, String eventName, String displayValue) {
        String currentDisplay = competitor.getDisplayResults().get(eventName);
        competitor.getDisplayResults().put(
                eventName,
                currentDisplay == null || currentDisplay.isBlank() ? displayValue : currentDisplay + " / " + displayValue
        );
    }

    private String formatResult(Double value, String unit) {
        if (value == null) {
            return "";
        }

        String text = value % 1 == 0 ? String.valueOf(value.intValue()) : String.valueOf(value);
        if (unit == null || unit.isBlank()) {
            return text;
        }

        return text + " " + unit;
    }

    private String displayDivision(Athlete athlete) {
        return weightClassService.resolveDivision(athlete);
    }

    private void calculateScores(Competition competition, Map<String, List<Competitor>> competitorsByDivision) {
        List<EventConfig> configuredEvents = loadEvents(competition);
        String tiebreaker = competitionContextService.settingsFor(competition).getOverallTiebreaker();
        for (List<Competitor> divisionCompetitors : competitorsByDivision.values()) {
            calculateDivisionScores(divisionCompetitors, configuredEvents, tiebreaker);
        }
    }

    private void calculateDivisionScores(List<Competitor> competitors, List<EventConfig> configuredEvents, String tiebreaker) {
        for (Competitor comp : competitors) {
            comp.getEventPoints().clear();
            comp.getEventPlacements().clear();
        }

        for (EventConfig event : configuredEvents) {
            String eventName = event.getEventName();
            List<Competitor> sorted = new ArrayList<>(competitors.stream()
                    .filter(competitor -> hasEventResult(competitor, event))
                    .toList());
            sorted.sort((c1, c2) -> compareEventResults(c1, c2, event));

            for (int i = 0; i < sorted.size(); ) {
                int j = i;
                while (j + 1 < sorted.size() && sameEventResult(sorted.get(i), sorted.get(j + 1), event)) {
                    j++;
                }

                double sumPoints = 0;
                for (int k = i; k <= j; k++) {
                    sumPoints += competitors.size() - k;
                }

                double avgPoints = sumPoints / (j - i + 1);
                for (int k = i; k <= j; k++) {
                    sorted.get(k).getEventPoints().put(eventName, avgPoints);
                    sorted.get(k).getEventPlacements().put(eventName, i + 1);
                }

                i = j + 1;
            }
        }

        for (Competitor comp : competitors) {
            double total = comp.getEventPoints().values().stream().mapToDouble(Double::doubleValue).sum();
            comp.setTotalPoints(total);
        }

        competitors.sort((c1, c2) -> compareOverallStanding(c1, c2, configuredEvents, tiebreaker));
        assignOverallPlaces(competitors, configuredEvents, tiebreaker);
    }

    private void assignOverallPlaces(List<Competitor> competitors, List<EventConfig> configuredEvents, String tiebreaker) {
        for (int i = 0; i < competitors.size(); i++) {
            if (i > 0 && compareOverallStanding(competitors.get(i - 1), competitors.get(i), configuredEvents, tiebreaker) == 0) {
                competitors.get(i).setPlace(competitors.get(i - 1).getPlace());
            } else {
                competitors.get(i).setPlace(i + 1);
            }
        }
    }

    private int compareOverallStanding(Competitor c1, Competitor c2, List<EventConfig> configuredEvents, String tiebreaker) {
        int totalCompare = Double.compare(c2.getTotalPoints(), c1.getTotalPoints());
        if (totalCompare != 0) {
            return totalCompare;
        }

        if ("LAST_EVENT".equals(tiebreaker)) {
            return compareLastEventPlacement(c1, c2, configuredEvents);
        }

        return compareEventPlacementTotals(c1, c2, configuredEvents);
    }

    private int compareEventPlacementTotals(Competitor c1, Competitor c2, List<EventConfig> configuredEvents) {
        int c1PlacementTotal = totalEventPlacements(c1, configuredEvents);
        int c2PlacementTotal = totalEventPlacements(c2, configuredEvents);
        return Integer.compare(c1PlacementTotal, c2PlacementTotal);
    }

    private int totalEventPlacements(Competitor competitor, List<EventConfig> configuredEvents) {
        int total = 0;
        int missingPlacement = configuredEvents.size() + 1;
        for (EventConfig event : configuredEvents) {
            total += competitor.getEventPlacements().getOrDefault(event.getEventName(), missingPlacement);
        }

        return total;
    }

    private int compareLastEventPlacement(Competitor c1, Competitor c2, List<EventConfig> configuredEvents) {
        if (configuredEvents.isEmpty()) {
            return 0;
        }

        String lastEventName = configuredEvents.get(configuredEvents.size() - 1).getEventName();
        int missingPlacement = configuredEvents.size() + 1;
        return Integer.compare(
                c1.getEventPlacements().getOrDefault(lastEventName, missingPlacement),
                c2.getEventPlacements().getOrDefault(lastEventName, missingPlacement)
        );
    }

    private int compareEventResults(Competitor c1, Competitor c2, EventConfig event) {
        String eventName = event.getEventName();

        if (event.isRequiresCompletionForTimeRanking()) {
            boolean c1Completed = completedRequiredResult(c1, event);
            boolean c2Completed = completedRequiredResult(c2, event);

            if (c1Completed && c2Completed) {
                return Double.compare(
                        c1.getTimes().getOrDefault(eventName, Double.MAX_VALUE),
                        c2.getTimes().getOrDefault(eventName, Double.MAX_VALUE)
                );
            }

            if (c1Completed != c2Completed) {
                return c1Completed ? -1 : 1;
            }

            return Double.compare(
                    c2.getScores().getOrDefault(eventName, 0.0),
                    c1.getScores().getOrDefault(eventName, 0.0)
            );
        }

        if (event.isUsesTime() && !event.isTimeIsTieBreaker()) {
            return Double.compare(
                    c1.getTimes().getOrDefault(eventName, 0.0),
                    c2.getTimes().getOrDefault(eventName, 0.0)
            );
        }

        double val1 = c1.getScores().getOrDefault(eventName, 0.0);
        double val2 = c2.getScores().getOrDefault(eventName, 0.0);
        int cmp = event.isHigherIsBetter() ? Double.compare(val2, val1) : Double.compare(val1, val2);
        if (cmp != 0) {
            return cmp;
        }

        if (event.isUsesTime() && event.isTimeIsTieBreaker()) {
            return Double.compare(
                    c1.getTimes().getOrDefault(eventName, 0.0),
                    c2.getTimes().getOrDefault(eventName, 0.0)
            );
        }

        return 0;
    }

    private boolean completedRequiredResult(Competitor competitor, EventConfig event) {
        Double completionTarget = event.getCompletionTarget();
        if (completionTarget == null) {
            return false;
        }

        return competitor.getScores().getOrDefault(event.getEventName(), 0.0) >= completionTarget;
    }

    private boolean hasEventResult(Competitor competitor, EventConfig event) {
        String eventName = event.getEventName();
        if (event.isRequiresCompletionForTimeRanking()) {
            return competitor.getScores().containsKey(eventName);
        }

        if (event.isUsesTime() && !event.isTimeIsTieBreaker()) {
            return competitor.getTimes().containsKey(eventName);
        }

        return competitor.getScores().containsKey(eventName);
    }

    private boolean sameEventResult(Competitor c1, Competitor c2, EventConfig event) {
        String eventName = event.getEventName();
        if (event.isRequiresCompletionForTimeRanking()) {
            boolean c1Completed = completedRequiredResult(c1, event);
            boolean c2Completed = completedRequiredResult(c2, event);
            if (c1Completed && c2Completed) {
                return Objects.equals(c1.getTimes().get(eventName), c2.getTimes().get(eventName));
            }

            return c1Completed == c2Completed
                    && Objects.equals(c1.getScores().get(eventName), c2.getScores().get(eventName));
        }

        if (event.isUsesTime() && !event.isTimeIsTieBreaker()) {
            return Objects.equals(c1.getTimes().get(eventName), c2.getTimes().get(eventName));
        }

        boolean sameScore = Objects.equals(c1.getScores().get(eventName), c2.getScores().get(eventName));
        boolean sameTime = !event.isUsesTime() || !event.isTimeIsTieBreaker()
                || Objects.equals(c1.getTimes().get(eventName), c2.getTimes().get(eventName));
        return sameScore && sameTime;
    }
}

