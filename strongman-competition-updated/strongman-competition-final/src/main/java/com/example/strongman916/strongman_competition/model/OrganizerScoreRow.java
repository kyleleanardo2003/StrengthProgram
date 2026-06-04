package com.example.strongman916.strongman_competition.model;

import java.util.HashMap;
import java.util.Map;

public class OrganizerScoreRow {
    private Athlete athlete;
    private Map<String, EventResult> results = new HashMap<>();

    public OrganizerScoreRow(Athlete athlete) {
        this.athlete = athlete;
    }

    public Athlete getAthlete() {
        return athlete;
    }

    public void setAthlete(Athlete athlete) {
        this.athlete = athlete;
    }

    public Map<String, EventResult> getResults() {
        return results;
    }

    public void setResults(Map<String, EventResult> results) {
        this.results = results;
    }
}
