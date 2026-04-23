package com.example.demo.common.appconfig.repository;

import com.example.demo.common.appconfig.domain.AppConfig;
import com.example.demo.common.appconfig.domain.AppConfig.ConfigType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AppConfigRepository extends JpaRepository<AppConfig, Long> {
    Optional<AppConfig> findByType(ConfigType type);
}
