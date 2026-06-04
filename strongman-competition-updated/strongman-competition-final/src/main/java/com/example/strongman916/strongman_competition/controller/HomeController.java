package com.example.strongman916.strongman_competition.controller;

import com.example.strongman916.strongman_competition.model.Competitor;
import com.example.strongman916.strongman_competition.model.EventConfig;
import com.example.strongman916.strongman_competition.sheets.GoogleSheetsService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.*;

@Controller
public class HomeController {

    private final List<EventConfig> events = new ArrayList<>();

    public HomeController() {
        events.add(new EventConfig("Log And KB", true, false, false));
        events.add(new EventConfig("Truck Pull", false, true, false));
        events.add(new EventConfig("Yoke And Frame", false, false, false));
        events.add(new EventConfig("Car Deadlift", true, false, false));
        events.add(new EventConfig("Stone Load", true, false, false));
    }

    @GetMapping("/")
    public String home(Model model) {
        try {
            GoogleSheetsService service = new GoogleSheetsService();
            List<Competitor> competitors = service.readCompetitorSheet();
            // Group by "Gender - WeightClass"
            Map<String, List<Competitor>> competitorsByDivision = new LinkedHashMap<>();
            for (Competitor comp : competitors) {
                String divisionKey = comp.getGender() + " - " + comp.getWeightClass();
                competitorsByDivision
                        .computeIfAbsent(divisionKey, k -> new ArrayList<>())
                        .add(comp);
            }

            calculateScores(competitorsByDivision);

            model.addAttribute("events", events);
            model.addAttribute("divisions", competitorsByDivision);

        } catch (Exception e) {
            model.addAttribute("error", "Failed to load from Excel: " + e.getMessage());
        }

        return "index";
    }

    @PostMapping("/submit")
    public String submit(@RequestParam Map<String, String> params) {
        // Future use: update scores/times
        return "redirect:/";
    }

    private void calculateScores(Map<String, List<Competitor>> competitorsByDivision) {
        for (List<Competitor> divisionCompetitors : competitorsByDivision.values()) {
            calculateDivisionScores(divisionCompetitors);
        }
    }

    private void calculateDivisionScores(List<Competitor> competitors) {
        for (Competitor comp : competitors) {
            comp.getEventPoints().clear();
        }

        for (EventConfig event : events) {
            String eventName = event.getEventName();
            List<Competitor> sorted = new ArrayList<>(competitors);
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
                }

                i = j + 1;
            }
        }

        for (Competitor comp : competitors) {
            double total = comp.getEventPoints().values().stream().mapToDouble(Double::doubleValue).sum();
            comp.setTotalPoints(total);
        }

        competitors.sort(Comparator.comparingDouble(Competitor::getTotalPoints).reversed());
        for (int i = 0; i < competitors.size(); i++) {
            competitors.get(i).setPlace(i + 1);
        }
    }

    private int compareEventResults(Competitor c1, Competitor c2, EventConfig event) {
        String eventName = event.getEventName();

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

    private boolean sameEventResult(Competitor c1, Competitor c2, EventConfig event) {
        String eventName = event.getEventName();
        if (event.isUsesTime() && !event.isTimeIsTieBreaker()) {
            return Objects.equals(c1.getTimes().get(eventName), c2.getTimes().get(eventName));
        }

        boolean sameScore = Objects.equals(c1.getScores().get(eventName), c2.getScores().get(eventName));
        boolean sameTime = !event.isUsesTime() || !event.isTimeIsTieBreaker()
                || Objects.equals(c1.getTimes().get(eventName), c2.getTimes().get(eventName));
        return sameScore && sameTime;
    }
}
