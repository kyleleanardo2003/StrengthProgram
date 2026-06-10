package strongmancast.model;

public class EventMonitorRow {

    private final Athlete athlete;
    private final EventResult result;
    private final String scoreDisplay;
    private final Integer divisionPlace;
    private final String status;

    public EventMonitorRow(Athlete athlete, EventResult result, String scoreDisplay, Integer divisionPlace, String status) {
        this.athlete = athlete;
        this.result = result;
        this.scoreDisplay = scoreDisplay;
        this.divisionPlace = divisionPlace;
        this.status = status;
    }

    public Athlete getAthlete() {
        return athlete;
    }

    public EventResult getResult() {
        return result;
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

    public String getScoreDisplay() {
        return scoreDisplay;
    }

    public Integer getDivisionPlace() {
        return divisionPlace;
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
}
