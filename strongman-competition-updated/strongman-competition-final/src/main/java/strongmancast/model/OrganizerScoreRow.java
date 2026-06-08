package strongmancast.model;

import java.util.HashMap;
import java.util.Map;

public class OrganizerScoreRow {
    private Athlete athlete;
    private Competitor competitor;
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

    public Competitor getCompetitor() {
        return competitor;
    }

    public void setCompetitor(Competitor competitor) {
        this.competitor = competitor;
    }

    public Map<String, EventResult> getResults() {
        return results;
    }

    public void setResults(Map<String, EventResult> results) {
        this.results = results;
    }
}

