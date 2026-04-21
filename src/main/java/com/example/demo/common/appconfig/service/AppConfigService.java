package com.example.demo.common.appconfig.service;

import com.example.demo.common.appconfig.domain.AppConfig;
import com.example.demo.common.appconfig.domain.AppConfig.ConfigType;
import com.example.demo.common.appconfig.repository.AppConfigRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class AppConfigService {

    private final AppConfigRepository appConfigRepository;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public AppConfig getConfig(ConfigType type) {
        return appConfigRepository.findByType(type).orElse(null);
    }

    @Transactional(readOnly = true)
    public Set<String> getWhitelistedEmails() {
        AppConfig config = appConfigRepository.findByType(ConfigType.WHITE_LIST).orElse(null);
        if (config == null) return Set.of();
        try {
            List<String> emails = objectMapper.readValue(config.getContents(), new TypeReference<>() {});
            return Set.copyOf(emails.stream().map(String::toLowerCase).toList());
        } catch (Exception e) {
            log.warn("[AppConfig] WHITE_LIST JSON 파싱 실패: {}", e.getMessage());
            return Set.of();
        }
    }

    @Transactional
    public AppConfig updateConfig(ConfigType type, String contentsJson, Long updatedBy) {
        AppConfig config = appConfigRepository.findByType(type)
            .orElseGet(() -> appConfigRepository.save(AppConfig.of(type, contentsJson, updatedBy)));
        config.update(contentsJson, updatedBy);
        return config;
    }
}
