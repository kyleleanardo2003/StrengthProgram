package strongmancast.model;

import jakarta.persistence.*;

@Entity
public class Athlete {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String membership;
    private String gender;
    private double bodyweight;
    private String division;
    private String eventGroup;

    @ManyToOne
    private Competition competition;

    // Constructors
    public Athlete() {}

    public Athlete(String name, String membership, double bodyweight, String division) {
        this.name = name;
        this.membership = membership;
        this.bodyweight = bodyweight;
        this.division = division;
    }

    // Getters and setters
    public Long getId() {
        return id;
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

    public double getBodyweight() {
        return bodyweight;
    }

    public void setBodyweight(double bodyweight) {
        this.bodyweight = bodyweight;
    }

    public String getDivision() {
        return division;
    }

    public void setDivision(String division) {
        this.division = division;
    }

    public String getEventGroup() {
        return eventGroup;
    }

    public void setEventGroup(String eventGroup) {
        this.eventGroup = eventGroup;
    }

    public Competition getCompetition() {
        return competition;
    }

    public void setCompetition(Competition competition) {
        this.competition = competition;
    }
}

