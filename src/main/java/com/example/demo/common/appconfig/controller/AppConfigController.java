package com.example.demo.common.appconfig.controller;

import com.example.demo.common.appconfig.domain.AppConfig;
import com.example.demo.common.appconfig.domain.AppConfig.ConfigType;
import com.example.demo.common.appconfig.service.AppConfigService;
import com.example.demo.common.exception.BusinessException;
import com.example.demo.common.response.ApiResponse;
import com.example.demo.user.domain.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/developer/config")
@RequiredArgsConstructor
public class AppConfigController {

    private final AppConfigService appConfigService;

    @GetMapping("/{type}")
    public ApiResponse<AppConfigResponse> getConfig(
            @PathVariable String type,
            @AuthenticationPrincipal Member member) {
        requireDeveloper(member);
        ConfigType configType = parseType(type);
        AppConfig config = appConfigService.getConfig(configType);
        return ApiResponse.ok(AppConfigResponse.from(config, configType));
    }

    @PutMapping("/{type}")
    public ApiResponse<AppConfigResponse> updateConfig(
            @PathVariable String type,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal Member member) {
        requireDeveloper(member);
        ConfigType configType = parseType(type);
        String contents = body.getOrDefault("contents", "[]");
        AppConfig updated = appConfigService.updateConfig(configType, contents, member.getId());
        return ApiResponse.ok(AppConfigResponse.from(updated, configType));
    }

    private void requireDeveloper(Member member) {
        if (member == null || member.getRole() != Member.Role.DEVELOPER) {
            throw new BusinessException("개발자 권한이 필요합니다.", 403);
        }
    }

    private ConfigType parseType(String type) {
        try {
            return ConfigType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("알 수 없는 config 타입: " + type, 400);
        }
    }

    public record AppConfigResponse(String type, String contents, Long updatedBy, String updatedAt) {
        static AppConfigResponse from(AppConfig config, ConfigType type) {
            if (config == null) return new AppConfigResponse(type.name(), "[]", null, null);
            return new AppConfigResponse(
                config.getType().name(),
                config.getContents(),
                config.getUpdatedBy(),
                config.getUpdatedAt() != null ? config.getUpdatedAt().toString() : null
            );
        }
    }
}
