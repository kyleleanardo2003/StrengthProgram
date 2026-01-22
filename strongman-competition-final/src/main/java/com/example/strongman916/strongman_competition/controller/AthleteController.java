package com.example.strongman916.strongman_competition.controller;

import com.example.strongman916.strongman_competition.model.Athlete;
import com.example.strongman916.strongman_competition.repository.AthleteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class AthleteController {

    @Autowired
    private AthleteRepository athleteRepository;

    @GetMapping("/athletes/new")
    public String showForm(Model model) {
        model.addAttribute("athlete", new Athlete());
        return "athlete_form";
    }

    @PostMapping("/athletes")
    public String saveAthlete(@ModelAttribute Athlete athlete) {
        athleteRepository.save(athlete);
        return "redirect:/athletes";
    }
}
