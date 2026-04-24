package com.example.demo.common.appconfig.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "app_config")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AppConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(unique = true, nullable = false, columnDefinition = "VARCHAR(50)")
    private ConfigType type;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String contents;

    private Long updatedBy;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public static AppConfig of(ConfigType type, String contents, Long updatedBy) {
        AppConfig c = new AppConfig();
        c.type = type;
        c.contents = contents;
        c.updatedBy = updatedBy;
        return c;
    }

    public void update(String contents, Long updatedBy) {
        this.contents = contents;
        this.updatedBy = updatedBy;
    }

    public enum ConfigType {
        WHITE_LIST
    }
}
