package com.inhatc.demp.service;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Component;

@Component
public class ContentSanitizer {

    private static final Safelist ALLOWED_CONTENT = new Safelist()
            .addTags("b", "strong", "i", "em", "u", "p", "br", "ul", "ol", "li", "blockquote", "pre", "code", "a",
                    "h1", "h2", "h3", "h4", "h5", "h6", "hr", "del",
                    "table", "thead", "tbody", "tr", "th", "td")
            .addAttributes("a", "href")
            .addProtocols("a", "href", "http", "https", "mailto")
            .preserveRelativeLinks(true);

    public String sanitize(String html) {
        if (html == null) {
            return "";
        }
        // 상대 URL의 프로토콜 검사에만 사용하며 실제 네트워크 요청은 발생하지 않는다.
        return Jsoup.clean(html, "https://content.invalid/", ALLOWED_CONTENT,
                new Document.OutputSettings().prettyPrint(false));
    }
}
