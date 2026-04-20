package com.example.demo.club.repository;

import com.example.demo.club.domain.Club;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ClubRepository extends JpaRepository<Club, Long> {
    Optional<Club> findByName(String name);
}
