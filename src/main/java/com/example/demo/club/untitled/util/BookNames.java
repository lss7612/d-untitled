package com.example.demo.club.untitled.util;

import java.text.Normalizer;
import java.util.Locale;

/**
 * 책 제목/저자 정규화 유틸.
 * - NFKC (전각→반각, 호환 문자 통일)
 * - 연속 공백 1개로 축소 + 양끝 trim
 * - 소문자화 (영문만 영향)
 *
 * 중복 판정에 사용되므로 모든 레이어 (서비스/엔티티 PrePersist/시드 SQL) 가
 * 동일 규칙을 따라야 한다.
 */
public final class BookNames {

    private BookNames() {}

    public static String normalize(String raw) {
        if (raw == null) return "";
        String nfkc = Normalizer.normalize(raw, Normalizer.Form.NFKC);
        String collapsed = nfkc.replaceAll("\\s+", " ").trim();
        return collapsed.toLowerCase(Locale.ROOT);
    }
}
