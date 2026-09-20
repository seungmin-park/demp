package com.inhatc.demp.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ContentSanitizerTest {

    private final ContentSanitizer sanitizer = new ContentSanitizer();

    @Test
    @DisplayName("스크립트와 이벤트 및 스타일 속성과 이미지를 제거한다")
    void removeActiveContent() {
        assertThat(sanitizer.sanitize("<p style=\"color:red\" onclick=\"alert(1)\">본문<img src=x onerror=alert(1)></p><script>alert(1)</script>"))
                .isEqualTo("<p>본문</p>");
    }

    @Test
    @DisplayName("허용한 서식 태그와 안전한 링크를 보존한다")
    void preserveAllowedMarkup() {
        String html = "<p><b>굵게</b><strong>강조</strong><i>기울임</i><em>강조</em><u>밑줄</u><br></p>"
                + "<ul><li>목록</li></ul><ol><li>순서</li></ol><blockquote>인용</blockquote><pre><code>code</code></pre>"
                + "<a href=\"https://example.com\">HTTPS</a><a href=\"http://example.com\">HTTP</a>"
                + "<a href=\"mailto:test@example.com\">메일</a><a href=\"/questions/1\">상대</a>"
                + "<a href=\"../jobs\">상위</a><a href=\"#section\">앵커</a>";
        assertThat(sanitizer.sanitize(html)).isEqualTo(html);
    }

    @Test
    @DisplayName("난독화한 자바스크립트와 데이터 및 FTP 링크를 제거한다")
    void removeUnsafeProtocols() {
        assertThat(sanitizer.sanitize("<a href=\"jav&#x61;script:alert(1)\">첫째</a>"
                + "<a href=\"JaVaScRiPt:alert(1)\">둘째</a><a href=\"data:text/html,test\">셋째</a>"
                + "<a href=\"ftp://example.com\">넷째</a>"))
                .isEqualTo("<a>첫째</a><a>둘째</a><a>셋째</a><a>넷째</a>");
    }

    @Test
    @DisplayName("허용 목록 밖의 SVG와 iframe 및 임의 태그를 제거한다")
    void removeUnlistedTags() {
        assertThat(sanitizer.sanitize("<svg onload=alert(1)></svg><iframe src=\"https://example.com\"></iframe>"
                + "<span class=\"x\">텍스트</span>"))
                .isEqualTo("텍스트");
    }

    @Test
    @DisplayName("없는 본문은 빈 문자열로 정규화한다")
    void normalizeMissingContent() {
        assertThat(sanitizer.sanitize(null)).isEmpty();
        assertThat(sanitizer.sanitize("")).isEmpty();
    }

    @Test
    @DisplayName("다시 정제해도 결과가 변하지 않는다")
    void sanitizeIdempotently() {
        String sanitized = sanitizer.sanitize("<p><strong>본문 &amp; 코드</strong></p><img src=x>");
        assertThat(sanitizer.sanitize(sanitized)).isEqualTo(sanitized);
    }
}
