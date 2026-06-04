package com.example.strongman916.strongman_competition.controller;

import com.example.strongman916.strongman_competition.model.Athlete;
import com.example.strongman916.strongman_competition.model.CompetitionEvent;
import com.example.strongman916.strongman_competition.model.EventResult;
import com.example.strongman916.strongman_competition.repository.AthleteRepository;
import com.example.strongman916.strongman_competition.repository.CompetitionEventRepository;
import com.example.strongman916.strongman_competition.repository.CompetitionSettingsRepository;
import com.example.strongman916.strongman_competition.repository.EventResultRepository;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;

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

    public DataManagementController(
            AthleteRepository athleteRepository,
            CompetitionEventRepository competitionEventRepository,
            CompetitionSettingsRepository competitionSettingsRepository,
            EventResultRepository eventResultRepository
    ) {
        this.athleteRepository = athleteRepository;
        this.competitionEventRepository = competitionEventRepository;
        this.competitionSettingsRepository = competitionSettingsRepository;
        this.eventResultRepository = eventResultRepository;
    }

    @PostMapping("/data/delete-results")
    public String deleteResults() {
        eventResultRepository.deleteAll();
        return "redirect:/scores";
    }

    @PostMapping("/data/delete-competitors")
    public String deleteCompetitors() {
        eventResultRepository.deleteAll();
        athleteRepository.deleteAll();
        return "redirect:/athletes";
    }

    @PostMapping("/data/delete-events")
    public String deleteEvents() {
        eventResultRepository.deleteAll();
        competitionEventRepository.deleteAll();
        return "redirect:/events";
    }

    @PostMapping("/data/delete-all")
    public String deleteAll() {
        eventResultRepository.deleteAll();
        athleteRepository.deleteAll();
        competitionEventRepository.deleteAll();
        competitionSettingsRepository.deleteAll();
        return "redirect:/organizer";
    }

    @PostMapping("/data/autofill-base-model")
    public String autofillBaseModel() {
        deleteAllData();
        seedBaseEvents();
        seedBaseCompetitorsAndScores();
        return "redirect:/organizer";
    }

    private void deleteAllData() {
        eventResultRepository.deleteAll();
        athleteRepository.deleteAll();
        competitionEventRepository.deleteAll();
        competitionSettingsRepository.deleteAll();
    }

    private void seedBaseEvents() {
        saveEvent(1, "Log And KB", "Reps", "reps", true, false, false, false, null, "Record completed reps.");
        saveEvent(2, "Truck Pull Time", "Time", "sec", false, true, false, false, null, "Record the truck pull time in seconds. Fastest time wins.");
        saveEvent(3, "Yoke And Frame", "Time", "sec", false, true, false, false, null, "Record the yoke and frame time in seconds. Fastest time wins.");
        saveEvent(4, "Car Deadlift", "Reps", "reps", true, false, false, false, null, "Record completed reps.");
        saveEvent(5, "Stone Load", "Reps", "reps", true, false, false, false, null, "Record completed stone loads.");
    }

    private void seedBaseCompetitorsAndScores() {
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
                            key -> saveAthlete(athleteName, membership, bodyweight, division)
                    );

                    saveResult(
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

    private Athlete saveAthlete(String name, String membership, double bodyweight, String division) {
        Athlete athlete = new Athlete();
        athlete.setName(name);
        athlete.setMembership(membership);
        athlete.setBodyweight(bodyweight);
        athlete.setDivision(division);
        return athleteRepository.save(athlete);
    }

    private void saveResult(Athlete athlete, String eventName, Double result, String unit, Double time) {
        EventResult eventResult = new EventResult();
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
