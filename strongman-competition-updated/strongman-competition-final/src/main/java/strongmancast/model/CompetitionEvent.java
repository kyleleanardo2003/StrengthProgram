package strongmancast.model;

import jakarta.persistence.*;

@Entity
public class CompetitionEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private int sortOrder;
    private String eventName;
    private String resultLabel;
    private String defaultUnit;
    private boolean higherIsBetter;
    private boolean usesTime;
    private boolean timeIsTieBreaker;
    private boolean requiresCompletionForTimeRanking;
    private Double completionTarget;
    private String extraFields;
    private Integer activeSlots = 1;
    private Long activeAthleteOneId;
    private Long activeAthleteTwoId;

    @ManyToOne
    private Competition competition;

    public Long getId() {
        return id;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }

    public String getEventName() {
        return eventName;
    }

    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

    public String getResultLabel() {
        return resultLabel;
    }

    public void setResultLabel(String resultLabel) {
        this.resultLabel = resultLabel;
    }

    public String getDefaultUnit() {
        return defaultUnit;
    }

    public void setDefaultUnit(String defaultUnit) {
        this.defaultUnit = defaultUnit;
    }

    public boolean isHigherIsBetter() {
        return higherIsBetter;
    }

    public void setHigherIsBetter(boolean higherIsBetter) {
        this.higherIsBetter = higherIsBetter;
    }

    public boolean isUsesTime() {
        return usesTime;
    }

    public void setUsesTime(boolean usesTime) {
        this.usesTime = usesTime;
    }

    public boolean isTimeIsTieBreaker() {
        return timeIsTieBreaker;
    }

    public void setTimeIsTieBreaker(boolean timeIsTieBreaker) {
        this.timeIsTieBreaker = timeIsTieBreaker;
    }

    public boolean isRequiresCompletionForTimeRanking() {
        return requiresCompletionForTimeRanking;
    }

    public void setRequiresCompletionForTimeRanking(boolean requiresCompletionForTimeRanking) {
        this.requiresCompletionForTimeRanking = requiresCompletionForTimeRanking;
    }

    public Double getCompletionTarget() {
        return completionTarget;
    }

    public void setCompletionTarget(Double completionTarget) {
        this.completionTarget = completionTarget;
    }

    public String getExtraFields() {
        return extraFields;
    }

    public void setExtraFields(String extraFields) {
        this.extraFields = extraFields;
    }

    public Integer getActiveSlots() {
        return activeSlots;
    }

    public void setActiveSlots(Integer activeSlots) {
        this.activeSlots = activeSlots;
    }

    public Long getActiveAthleteOneId() {
        return activeAthleteOneId;
    }

    public void setActiveAthleteOneId(Long activeAthleteOneId) {
        this.activeAthleteOneId = activeAthleteOneId;
    }

    public Long getActiveAthleteTwoId() {
        return activeAthleteTwoId;
    }

    public void setActiveAthleteTwoId(Long activeAthleteTwoId) {
        this.activeAthleteTwoId = activeAthleteTwoId;
    }

    public Competition getCompetition() {
        return competition;
    }

    public void setCompetition(Competition competition) {
        this.competition = competition;
    }
}

