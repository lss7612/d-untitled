package com.example.demo.common.exception;

import lombok.Getter;

import java.util.Collections;
import java.util.Map;

@Getter
public class BusinessException extends RuntimeException {

    private final int status;

    /**
     * 에러 응답에 함께 실릴 추가 데이터. 클라이언트가 특정 케이스
     * (예: 중복 책 에러에서 duplicateBookId) 를 구분·후속 동작하는 데 사용.
     */
    private final Map<String, Object> details;

    public BusinessException(String message, int status) {
        this(message, status, Collections.emptyMap());
    }

    public BusinessException(String message, int status, Map<String, Object> details) {
        super(message);
        this.status = status;
        this.details = details == null ? Collections.emptyMap() : details;
    }
}
