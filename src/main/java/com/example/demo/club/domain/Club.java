package com.example.demo.club.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "clubs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Club {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ClubType type;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    public static Club create(String name, String description, ClubType type) {
        Club club = new Club();
        club.name = name;
        club.description = description;
        club.type = type;
        return club;
    }

    public enum ClubType {
        READING, GENERAL
    }
}
