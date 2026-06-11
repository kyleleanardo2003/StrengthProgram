package strongmancast.model;

import java.util.HashMap;
import java.util.Map;

public class Competitor {
    private Long athleteId;
    private String name;
    private String membership;
    private String gender;
    private String weightClass;
    private int place;
    private double bodyWeight;
    private double totalPoints;
    private String turnStatus = "";

    private Map<String, Double> scores = new HashMap<>();
    private Map<String, Double> eventPoints = new HashMap<>();
    private Map<String, Integer> eventPlacements = new HashMap<>();
    private Map<String, Double> times = new HashMap<>();
    private Map<String, String> displayResults = new HashMap<>();

    // Getters and setters

    public Long getAthleteId() {
        return athleteId;
    }

    public void setAthleteId(Long athleteId) {
        this.athleteId = athleteId;
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

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getWeightClass() {
        return weightClass;
    }

    public void setWeightClass(String weightClass) {
        this.weightClass = weightClass;
    }

    public int getPlace() {
        return place;
    }

    public void setPlace(int place) {
        this.place = place;
    }

    public double getBodyWeight() {
        return bodyWeight;
    }

    public void setBodyWeight(double bodyWeight) {
        this.bodyWeight = bodyWeight;
    }

    public double getTotalPoints() {
        return totalPoints;
    }

    public void setTotalPoints(double totalPoints) {
        this.totalPoints = totalPoints;
    }

    public String getTurnStatus() {
        return turnStatus;
    }

    public void setTurnStatus(String turnStatus) {
        this.turnStatus = turnStatus;
    }

    public boolean isOnStage() {
        return "ON_STAGE".equals(turnStatus);
    }

    public boolean isNextInHole() {
        return "NEXT_IN_HOLE".equals(turnStatus);
    }

    public boolean isNextToHole() {
        return "NEXT_TO_HOLE".equals(turnStatus);
    }

    public Map<String, Double> getScores() {
        return scores;
    }

    public void setScores(Map<String, Double> scores) {
        this.scores = scores;
    }

    public Map<String, Double> getEventPoints() {
        return eventPoints;
    }

    public void setEventPoints(Map<String, Double> eventPoints) {
        this.eventPoints = eventPoints;
    }

    public Map<String, Integer> getEventPlacements() {
        return eventPlacements;
    }

    public void setEventPlacements(Map<String, Integer> eventPlacements) {
        this.eventPlacements = eventPlacements;
    }

    public Map<String, Double> getTimes() {
        return times;
    }

    public void setTimes(Map<String, Double> times) {
        this.times = times;
    }

    public Map<String, String> getDisplayResults() {
        return displayResults;
    }

    public void setDisplayResults(Map<String, String> displayResults) {
        this.displayResults = displayResults;
    }

    // Helper to get full division string
    public String getDivision() {
        return gender + " - " + weightClass;
    }
}

