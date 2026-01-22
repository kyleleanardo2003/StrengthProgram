package com.example.strongman916.strongman_competition.model;

public enum Division {
    LIGHTWEIGHT_MEN("Lightweight Men"),
    LIGHTWEIGHT_WOMEN("Lightweight Women"),
    MIDDLEWEIGHT_MEN("Middleweight Men"),
    MIDDLEWEIGHT_WOMEN("Middleweight Women"),
    HEAVYWEIGHT_MEN("Heavyweight Men"),
    HEAVYWEIGHT_WOMEN("Heavyweight Women");

    private final String label;

    Division(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
