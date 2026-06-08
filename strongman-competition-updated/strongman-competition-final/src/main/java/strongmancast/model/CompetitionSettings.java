package strongmancast.model;

import jakarta.persistence.*;

@Entity
public class CompetitionSettings {

    @Id
    private Long id;

    private String overallTiebreaker = "EVENT_PLACINGS";

    @OneToOne
    private Competition competition;

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

    public Competition getCompetition() {
        return competition;
    }

    public void setCompetition(Competition competition) {
        this.competition = competition;
    }
}

