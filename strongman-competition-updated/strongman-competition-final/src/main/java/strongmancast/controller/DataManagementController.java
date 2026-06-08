package strongmancast.controller;

import strongmancast.model.Athlete;
import strongmancast.model.Competition;
import strongmancast.model.CompetitionEvent;
import strongmancast.model.EventResult;
import strongmancast.repository.AthleteRepository;
import strongmancast.repository.CompetitionEventRepository;
import strongmancast.repository.CompetitionSettingsRepository;
import strongmancast.repository.EventResultRepository;
import strongmancast.service.CompetitionContextService;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

@Controller
public class DataManagementController {

    private static final String WRATH_2025_SEED_FILE = "/base-model/wrath-2025-seed.tsv";

    private final AthleteRepository athleteRepository;
    private final CompetitionEventRepository competitionEventRepository;
    private final CompetitionSettingsRepository competitionSettingsRepository;
    private final EventResultRepository eventResultRepository;
    private final CompetitionContextService competitionContextService;

    public DataManagementController(
            AthleteRepository athleteRepository,
            CompetitionEventRepository competitionEventRepository,
            CompetitionSettingsRepository competitionSettingsRepository,
            EventResultRepository eventResultRepository,
            CompetitionContextService competitionContextService
    ) {
        this.athleteRepository = athleteRepository;
        this.competitionEventRepository = competitionEventRepository;
        this.competitionSettingsRepository = competitionSettingsRepository;
        this.eventResultRepository = eventResultRepository;
        this.competitionContextService = competitionContextService;
    }

    @PostMapping("/data/delete-results")
    @Transactional
    public String deleteResults(@RequestParam(required = false) Long competitionId) {
        Competition competition = competitionContextService.currentCompetition(competitionId);
        eventResultRepository.deleteByCompetition(competition);
        return "redirect:/organizer?competitionId=" + competition.getId();
    }

    @PostMapping("/data/delete-competitors")
    @Transactional
    public String deleteCompetitors(@RequestParam(required = false) Long competitionId) {
        Competition competition = competitionContextService.currentCompetition(competitionId);
        eventResultRepository.deleteByCompetition(competition);
        athleteRepository.deleteByCompetition(competition);
        return "redirect:/athletes?competitionId=" + competition.getId();
    }

    @PostMapping("/data/delete-events")
    @Transactional
    public String deleteEvents(@RequestParam(required = false) Long competitionId) {
        Competition competition = competitionContextService.currentCompetition(competitionId);
        eventResultRepository.deleteByCompetition(competition);
        competitionEventRepository.deleteByCompetition(competition);
        return "redirect:/events?competitionId=" + competition.getId();
    }

    @PostMapping("/data/delete-all")
    @Transactional
    public String deleteAll(@RequestParam(required = false) Long competitionId) {
        Competition competition = competitionContextService.currentCompetition(competitionId);
        deleteCompetitionData(competition);
        return "redirect:/organizer?competitionId=" + competition.getId();
    }

    @PostMapping("/data/autofill-base-model")
    @Transactional
    public String autofillBaseModel(@RequestParam(required = false) Long competitionId) {
        Competition competition = competitionContextService.currentCompetition(competitionId);
        deleteCompetitionData(competition);
        seedBaseEvents(competition);
        seedBaseCompetitorsAndScores(competition);
        return "redirect:/organizer?competitionId=" + competition.getId();
    }

    private void deleteCompetitionData(Competition competition) {
        eventResultRepository.deleteByCompetition(competition);
        athleteRepository.deleteByCompetition(competition);
        competitionEventRepository.deleteByCompetition(competition);
        competitionSettingsRepository.deleteByCompetition(competition);
    }

    private void seedBaseEvents(Competition competition) {
        saveEvent(competition, 1, "Log And KB", "Reps", "reps", true, false, false, false, null, "Record completed reps.");
        saveEvent(competition, 2, "Truck Pull Time", "Time", "sec", false, true, false, false, null, "Record the truck pull time in seconds. Fastest time wins.");
        saveEvent(competition, 3, "Yoke And Frame", "Time", "sec", false, true, false, false, null, "Record the yoke and frame time in seconds. Fastest time wins.");
        saveEvent(competition, 4, "Car Deadlift", "Reps", "reps", true, false, false, false, null, "Record completed reps.");
        saveEvent(competition, 5, "Stone Load", "Reps", "reps", true, false, false, false, null, "Record completed stone loads.");
    }

    private void seedBaseCompetitorsAndScores(Competition competition) {
        Map<String, Athlete> seededAthletes = new LinkedHashMap<>();

        try (InputStream inputStream = getClass().getResourceAsStream(WRATH_2025_SEED_FILE)) {
            if (inputStream == null) {
                throw new IllegalStateException("Missing seed file " + WRATH_2025_SEED_FILE);
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                String line = reader.readLine();
                while ((line = reader.readLine()) != null) {
                    String[] columns = line.split("\t", -1);
                    if (columns.length < 8) {
                        continue;
                    }

                    String athleteName = columns[0].trim();
                    String membership = columns[1].trim();
                    double bodyweight = parseDouble(columns[2]);
                    String division = columns[3].trim();
                    String athleteKey = athleteName + "|" + division;

                    Athlete athlete = seededAthletes.computeIfAbsent(
                            athleteKey,
                            key -> saveAthlete(competition, athleteName, membership, bodyweight, division)
                    );

                    saveResult(
                            competition,
                            athlete,
                            columns[4].trim(),
                            parseOptionalDouble(columns[5]),
                            columns[6].trim(),
                            parseOptionalDouble(columns[7])
                    );
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Could not read seed file " + WRATH_2025_SEED_FILE, e);
        }
    }

    private void saveEvent(
            Competition competition,
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
        CompetitionEvent event = new CompetitionEvent();
        event.setCompetition(competition);
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
    }

    private Athlete saveAthlete(Competition competition, String name, String membership, double bodyweight, String division) {
        Athlete athlete = new Athlete();
        athlete.setCompetition(competition);
        athlete.setName(name);
        athlete.setMembership(membership);
        athlete.setBodyweight(bodyweight);
        athlete.setDivision(division);
        return athleteRepository.save(athlete);
    }

    private void saveResult(Competition competition, Athlete athlete, String eventName, Double result, String unit, Double time) {
        EventResult eventResult = new EventResult();
        eventResult.setCompetition(competition);
        eventResult.setAthlete(athlete);
        eventResult.setEventName(eventName);
        eventResult.setResult(result);
        eventResult.setUnit(unit);
        eventResult.setTime(time);
        eventResultRepository.save(eventResult);
    }

    private double parseDouble(String value) {
        Double parsed = parseOptionalDouble(value);
        return parsed == null ? 0.0 : parsed;
    }

    private Double parseOptionalDouble(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return Double.parseDouble(value.trim());
    }
}

