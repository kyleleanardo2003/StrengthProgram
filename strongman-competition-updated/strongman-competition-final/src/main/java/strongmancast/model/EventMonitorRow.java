package strongmancast.model;

public class EventMonitorRow {

    private final Athlete athlete;
    private final EventResult result;
    private final String divisionDisplay;
    private final String scoreDisplay;
    private final Integer divisionPlace;
    private final Double eventPoints;
    private final Integer overallPlace;
    private final String status;

    public EventMonitorRow(Athlete athlete, EventResult result, String divisionDisplay, String scoreDisplay, Integer divisionPlace, Double eventPoints, Integer overallPlace, String status) {
        this.athlete = athlete;
        this.result = result;
        this.divisionDisplay = divisionDisplay;
        this.scoreDisplay = scoreDisplay;
        this.divisionPlace = divisionPlace;
        this.eventPoints = eventPoints;
        this.overallPlace = overallPlace;
        this.status = status;
    }

    public Athlete getAthlete() {
        return athlete;
    }

    public EventResult getResult() {
        return result;
    }

    public String getDivisionDisplay() {
        return divisionDisplay;
    }

    public Double getResultValue() {
        return result == null ? null : result.getResult();
    }

    public String getUnit() {
        return result == null ? null : result.getUnit();
    }

    public Double getTimeMinutes() {
        if (result == null || result.getTime() == null) {
            return null;
        }
        return Math.floor(result.getTime() / 60);
    }

    public Double getTimeSeconds() {
        if (result == null || result.getTime() == null) {
            return null;
        }
        return result.getTime() % 60;
    }

    public Double getSecondaryTimeMinutes() {
        if (result == null || result.getSecondaryTime() == null) {
            return null;
        }
        return Math.floor(result.getSecondaryTime() / 60);
    }

    public Double getSecondaryTimeSeconds() {
        if (result == null || result.getSecondaryTime() == null) {
            return null;
        }
        return result.getSecondaryTime() % 60;
    }

    public String getScoreDisplay() {
        return scoreDisplay;
    }

    public Integer getDivisionPlace() {
        return divisionPlace;
    }

    public Double getEventPoints() {
        return eventPoints;
    }

    public Integer getOverallPlace() {
        return overallPlace;
    }

    public String getStatus() {
        return status;
    }

    public boolean isOnStage() {
        return "ON_STAGE".equals(status);
    }

    public boolean isNextInHole() {
        return "NEXT_IN_HOLE".equals(status);
    }

    public boolean isNextToHole() {
        return "NEXT_TO_HOLE".equals(status);
    }
}
