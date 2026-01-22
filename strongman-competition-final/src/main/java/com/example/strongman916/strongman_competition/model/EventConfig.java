package com.example.strongman916.strongman_competition.model;

public class EventConfig {
    private String eventName;
    private boolean higherIsBetter;
    private boolean usesTime;
    private boolean timeIsTieBreaker;

    public EventConfig(String eventName, boolean higherIsBetter, boolean usesTime, boolean timeIsTieBreaker) {
        this.eventName = eventName;
        this.higherIsBetter = higherIsBetter;
        this.usesTime = usesTime;
        this.timeIsTieBreaker = timeIsTieBreaker;
    }

    public String getEventName() { return eventName; }
    public boolean isHigherIsBetter() { return higherIsBetter; }
    public boolean isUsesTime() { return usesTime; }
    public boolean isTimeIsTieBreaker() { return timeIsTieBreaker; }
}
