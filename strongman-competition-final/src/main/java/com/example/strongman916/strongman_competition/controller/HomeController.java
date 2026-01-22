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
        events.add(new EventConfig("Truck Pull", false, true, true));
        events.add(new EventConfig("Yoke And Frame", false, false, false));
        events.add(new EventConfig("Car Deadlift", true, false, false));
        events.add(new EventConfig("Stone Load", true, false, false));
    }

    @GetMapping("/")
    public String home(Model model) {
        try {
            GoogleSheetsService service = new GoogleSheetsService();
            List<Competitor> competitors = service.readCompetitorSheet();
            calculateScores(competitors);

            // Group by "Gender - WeightClass"
            Map<String, List<Competitor>> competitorsByDivision = new LinkedHashMap<>();
            for (Competitor comp : competitors) {
                String divisionKey = comp.getGender() + " - " + comp.getWeightClass();
                competitorsByDivision
                        .computeIfAbsent(divisionKey, k -> new ArrayList<>())
                        .add(comp);
            }

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

    private void calculateScores(List<Competitor> competitors) {
        for (EventConfig event : events) {
            String eventName = event.getEventName();
            boolean higherIsBetter = event.isHigherIsBetter();
            boolean usesTime = event.isUsesTime();
            boolean timeIsTieBreaker = event.isTimeIsTieBreaker();

            List<Competitor> sorted = new ArrayList<>(competitors);
            sorted.sort((c1, c2) -> {
                double val1 = c1.getScores().getOrDefault(eventName, 0.0);
                double val2 = c2.getScores().getOrDefault(eventName, 0.0);
                int cmp = higherIsBetter ? Double.compare(val2, val1) : Double.compare(val1, val2);
                if (cmp != 0) return cmp;

                if (usesTime && timeIsTieBreaker) {
                    double time1 = c1.getTimes().getOrDefault(eventName, 0.0);
                    double time2 = c2.getTimes().getOrDefault(eventName, 0.0);
                    return Double.compare(time1, time2);
                }
                return 0;
            });

            for (int i = 0; i < sorted.size(); ) {
                int j = i;
                while (j + 1 < sorted.size()) {
                    Competitor a = sorted.get(i);
                    Competitor b = sorted.get(j + 1);
                    boolean sameScore = Objects.equals(a.getScores().get(eventName), b.getScores().get(eventName));
                    boolean sameTime = !usesTime || !timeIsTieBreaker ||
                            Objects.equals(a.getTimes().get(eventName), b.getTimes().get(eventName));
                    if (sameScore && sameTime) j++;
                    else break;
                }

                double sumPoints = 0;
                for (int k = i; k <= j; k++) sumPoints += (competitors.size() - k);
                double avgPoints = sumPoints / (j - i + 1);
                for (int k = i; k <= j; k++) sorted.get(k).getEventPoints().put(eventName, avgPoints);
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
}
