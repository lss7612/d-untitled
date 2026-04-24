package com.example.demo.common.exception;

import com.example.demo.common.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<?> handleBusinessException(BusinessException e) {
        log.warn("[BusinessException] status={} message={} details={}",
            e.getStatus(), e.getMessage(), e.getDetails());
        // details 가 있으면 응답 body 에 함께 실어 클라이언트가 케이스를 구분할 수 있게 한다.
        // 없으면 기존 ApiResponse.error(...) 형식을 유지.
        if (e.getDetails() == null || e.getDetails().isEmpty()) {
            return ResponseEntity.status(e.getStatus()).body(ApiResponse.error(e.getMessage()));
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", false);
        body.put("data", null);
        body.put("message", e.getMessage());
        body.put("details", e.getDetails());
        return ResponseEntity.status(e.getStatus()).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<?>> handleException(Exception e) {
        log.error("[UnhandledException] {}: {}", e.getClass().getSimpleName(), e.getMessage(), e);
        return ResponseEntity.status(500).body(ApiResponse.error("서버 오류가 발생했습니다."));
    }
}
