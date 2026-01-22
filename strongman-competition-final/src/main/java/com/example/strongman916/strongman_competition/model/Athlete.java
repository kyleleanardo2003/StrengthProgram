package com.example.strongman916.strongman_competition.model;

import jakarta.persistence.*;

@Entity
public class Athlete {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private double bodyweight;

    @Enumerated(EnumType.STRING)
    private Division division;

    // Constructors
    public Athlete() {}

    public Athlete(String name, double bodyweight, Division division) {
        this.name = name;
        this.bodyweight = bodyweight;
        this.division = division;
    }

    // Getters and setters
    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getBodyweight() {
        return bodyweight;
    }

    public void setBodyweight(double bodyweight) {
        this.bodyweight = bodyweight;
    }

    public Division getDivision() {
        return division;
    }

    public void setDivision(Division division) {
        this.division = division;
    }
}
