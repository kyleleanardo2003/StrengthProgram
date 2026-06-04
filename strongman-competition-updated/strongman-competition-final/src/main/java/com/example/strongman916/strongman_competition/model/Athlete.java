package com.example.strongman916.strongman_competition.model;

import jakarta.persistence.*;

@Entity
public class Athlete {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String membership;
    private double bodyweight;

    @Enumerated(EnumType.STRING)
    private Division division;

    // Constructors
    public Athlete() {}

    public Athlete(String name, String membership, double bodyweight, Division division) {
        this.name = name;
        this.membership = membership;
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

    public String getMembership() {
        return membership;
    }

    public void setMembership(String membership) {
        this.membership = membership;
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
