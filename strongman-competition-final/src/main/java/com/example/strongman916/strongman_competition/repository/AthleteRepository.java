package com.example.strongman916.strongman_competition.repository;

import com.example.strongman916.strongman_competition.model.Athlete;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AthleteRepository extends JpaRepository<Athlete, Long> {
}
// This interface extends JpaRepository, which provides methods for CRUD operations on Athlete entities.