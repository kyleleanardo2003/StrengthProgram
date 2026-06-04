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
        model.addAttribute("formTitle", "Add Athlete");
        model.addAttribute("formAction", "/athletes");
        return "athlete_form";
    }

    @GetMapping("/athletes/{id}/edit")
    public String editAthlete(@PathVariable Long id, Model model) {
        Athlete athlete = athleteRepository.findById(id).orElseThrow();
        model.addAttribute("athlete", athlete);
        model.addAttribute("formTitle", "Edit Athlete");
        model.addAttribute("formAction", "/athletes/" + id);
        return "athlete_form";
    }

    @PostMapping("/athletes")
    public String saveAthlete(@ModelAttribute Athlete athlete) {
        athleteRepository.save(athlete);
        return "redirect:/athletes";
    }

    @PostMapping("/athletes/{id}")
    public String updateAthlete(@PathVariable Long id, @ModelAttribute Athlete updatedAthlete) {
        Athlete athlete = athleteRepository.findById(id).orElseThrow();
        athlete.setName(updatedAthlete.getName());
        athlete.setMembership(updatedAthlete.getMembership());
        athlete.setBodyweight(updatedAthlete.getBodyweight());
        athlete.setDivision(updatedAthlete.getDivision());
        athleteRepository.save(athlete);
        return "redirect:/organizer";
    }

    @GetMapping("/athletes")
    public String listAthletes(Model model) {
        model.addAttribute("athletes", athleteRepository.findAll());
        return "athletes";
    }
}
