package com.example.strongman916.strongman_competition.model;

public class EventConfig {
    private String eventName;
    private boolean higherIsBetter;
    private boolean usesTime;
    private boolean timeIsTieBreaker;
    private boolean requiresCompletionForTimeRanking;
    private Double completionTarget;

    public EventConfig(String eventName, boolean higherIsBetter, boolean usesTime, boolean timeIsTieBreaker) {
        this(eventName, higherIsBetter, usesTime, timeIsTieBreaker, false, null);
    }

    public EventConfig(
            String eventName,
            boolean higherIsBetter,
            boolean usesTime,
            boolean timeIsTieBreaker,
            boolean requiresCompletionForTimeRanking,
            Double completionTarget
    ) {
        this.eventName = eventName;
        this.higherIsBetter = higherIsBetter;
        this.usesTime = usesTime;
        this.timeIsTieBreaker = timeIsTieBreaker;
        this.requiresCompletionForTimeRanking = requiresCompletionForTimeRanking;
        this.completionTarget = completionTarget;
    }

    public String getEventName() { return eventName; }
    public boolean isHigherIsBetter() { return higherIsBetter; }
    public boolean isUsesTime() { return usesTime; }
    public boolean isTimeIsTieBreaker() { return timeIsTieBreaker; }
    public boolean isRequiresCompletionForTimeRanking() { return requiresCompletionForTimeRanking; }
    public Double getCompletionTarget() { return completionTarget; }
}
