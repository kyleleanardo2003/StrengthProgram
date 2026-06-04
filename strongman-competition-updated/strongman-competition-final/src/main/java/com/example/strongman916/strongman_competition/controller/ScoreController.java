package com.example.strongman916.strongman_competition.controller;

import com.example.strongman916.strongman_competition.model.Athlete;
import com.example.strongman916.strongman_competition.model.EventResult;
import com.example.strongman916.strongman_competition.repository.AthleteRepository;
import com.example.strongman916.strongman_competition.repository.EventResultRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

import static com.example.strongman916.strongman_competition.model.EventNames.*;

@Controller
public class ScoreController {

    private static final List<String> EVENTS = List.of(
            LOG_AND_KB,
            TRUCK_PULL,
            YOKE_AND_FRAME,
            CAR_DEADLIFT,
            STONE_LOAD
    );

    private final AthleteRepository athleteRepository;
    private final EventResultRepository eventResultRepository;

    public ScoreController(AthleteRepository athleteRepository, EventResultRepository eventResultRepository) {
        this.athleteRepository = athleteRepository;
        this.eventResultRepository = eventResultRepository;
    }

    @GetMapping("/scores")
    public String showScoreForm(Model model) {
        model.addAttribute("athletes", athleteRepository.findAll());
        model.addAttribute("events", EVENTS);
        model.addAttribute("results", eventResultRepository.findAll());
        return "score_form";
    }

    @PostMapping("/scores")
    public String saveScore(
            @RequestParam Long athleteId,
            @RequestParam String eventName,
            @RequestParam(required = false) Double result,
            @RequestParam(required = false) Double time
    ) {
        Athlete athlete = athleteRepository.findById(athleteId).orElseThrow();
        EventResult eventResult = eventResultRepository
                .findByAthleteAndEventName(athlete, eventName)
                .orElseGet(EventResult::new);

        eventResult.setAthlete(athlete);
        eventResult.setEventName(eventName);
        eventResult.setResult(result);
        eventResult.setTime(time);
        eventResultRepository.save(eventResult);

        return "redirect:/scores";
    }
}
