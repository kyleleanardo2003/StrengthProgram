package strongmancast.model;

import jakarta.persistence.*;

@Entity
public class Athlete {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String membership;
    private double bodyweight;
    private String division;

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

    public Competition getCompetition() {
        return competition;
    }

    public void setCompetition(Competition competition) {
        this.competition = competition;
    }
}

