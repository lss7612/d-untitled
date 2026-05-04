package com.example.demo.club.untitled.external;

import com.example.demo.common.exception.BusinessException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 알라딘 상품 페이지 HTML을 받아 JSON-LD를 파싱한다.
 * Open API key 없이 동작. Phase 2 후반에 공식 API로 교체 검토.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AladinApiClient {

    private static final Pattern JSON_LD_PATTERN = Pattern.compile(
        "<script type=\"application/ld\\+json\">(.*?)</script>", Pattern.DOTALL);
    private static final Pattern ISBN_META_PATTERN = Pattern.compile(
        "<meta property=\"books:isbn\" content=\"([0-9Xx]+)\"\\s*/>");
    /**
     * 알라딘 카트 식별 코드. 장바구니 URL `wbasket.aspx?AddBook=<code>` 에서 추출.
     * 책마다 형식이 다르다 — 관찰된 패턴:
     *   - {@code K292830716} (K-prefix, 한국 일부 도서)
     *   - {@code 8925569574} (prefix 없는 10자리, ISBN-10. 한국 책에서 흔함)
     *   - {@code 0718197038} (prefix 없는 10자리, 외서)
     *   - {@code U604737277} (U-prefix, 일부 외서)
     * 셋 다 동일한 카트 API (`BasketAjax.aspx?method=basketaddwithexistcheck&isbn=<code>`)
     * 의 isbn 파라미터로 그대로 동작 — 검증 완료.
     */
    private static final Pattern ALADIN_ITEM_CODE_PATTERN = Pattern.compile(
        "wbasket\\.aspx\\?AddBook=([A-Z]?[0-9]+)");
    private static final Pattern ALLOWED_HOST = Pattern.compile("^https?://(www\\.)?aladin\\.co\\.kr/.+");

    private final HttpClient httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(5))
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ParsedBook parse(String url) {
        if (url == null || !ALLOWED_HOST.matcher(url.trim()).matches()) {
            throw new BusinessException("알라딘 URL만 지원합니다.", 400);
        }

        String html = fetch(url);

        Matcher jsonMatcher = JSON_LD_PATTERN.matcher(html);
        if (!jsonMatcher.find()) {
            throw new BusinessException("도서 정보를 찾을 수 없습니다.", 422);
        }

        JsonNode book;
        try {
            book = objectMapper.readTree(jsonMatcher.group(1));
        } catch (Exception e) {
            throw new BusinessException("도서 정보 파싱에 실패했습니다.", 422);
        }

        String title = textOrNull(book.path("name"));
        String author = textOrNull(book.path("author").path("name"));
        String publisher = textOrNull(book.path("publisher").path("name"));
        String thumbnailUrl = textOrNull(book.path("image"));

        JsonNode offers = book.path("offers");
        BigDecimal price = offers.has("price") ? new BigDecimal(offers.get("price").asText()) : null;
        String currency = textOrNull(offers.path("priceCurrency"));
        if (currency == null) currency = "KRW";

        Matcher isbnMatcher = ISBN_META_PATTERN.matcher(html);
        String isbn = isbnMatcher.find() ? isbnMatcher.group(1) : null;

        Matcher codeMatcher = ALADIN_ITEM_CODE_PATTERN.matcher(html);
        String aladinItemCode = codeMatcher.find() ? codeMatcher.group(1) : null;

        if (title == null || isbn == null || price == null) {
            throw new BusinessException("도서 필수 정보(제목/ISBN/가격)를 찾을 수 없습니다.", 422);
        }

        return new ParsedBook(title, author, publisher, isbn, price, currency, thumbnailUrl, url, aladinItemCode);
    }

    private String fetch(String url) {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(10))
                .header("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36")
                .GET()
                .build();
            HttpResponse<String> res = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() / 100 != 2) {
                throw new BusinessException("알라딘 페이지 요청 실패 (status=" + res.statusCode() + ")", 502);
            }
            return res.body();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("[AladinApiClient] fetch 실패 url={} reason={}", url, e.getMessage());
            throw new BusinessException("알라딘 페이지를 가져올 수 없습니다.", 502);
        }
    }

    private String textOrNull(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) return null;
        String text = node.asText();
        return (text == null || text.isBlank()) ? null : text.trim();
    }
}
