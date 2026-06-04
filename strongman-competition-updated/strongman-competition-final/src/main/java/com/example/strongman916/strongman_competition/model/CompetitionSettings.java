package com.example.strongman916.strongman_competition.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class CompetitionSettings {

    @Id
    private Long id = 1L;

    private String overallTiebreaker = "EVENT_PLACINGS";

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getOverallTiebreaker() {
        return overallTiebreaker;
    }

    public void setOverallTiebreaker(String overallTiebreaker) {
        this.overallTiebreaker = overallTiebreaker;
    }
}
