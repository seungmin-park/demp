package com.inhatc.demp.domain.announcement;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.text.Normalizer;
import java.util.*;

/** Identity of the external posting; its display title is deliberately excluded. */
public final class AnnouncementSourceKey {
    private AnnouncementSourceKey() {}
    public static String of(String url, String institution, String cohort) {
        if (url == null || institution == null) return null;
        URI uri = URI.create(url.trim()).normalize();
        String scheme = uri.getScheme().toLowerCase(Locale.ROOT);
        int port = uri.getPort();
        String path = Optional.ofNullable(uri.getRawPath()).orElse("").replaceAll("/+$", "");
        String query = Arrays.stream(Optional.ofNullable(uri.getRawQuery()).orElse("").split("&"))
                .filter(p -> !p.isBlank() && !p.toLowerCase(Locale.ROOT).startsWith("utm_") && !p.startsWith("fbclid=") && !p.startsWith("gclid="))
                .sorted().collect(java.util.stream.Collectors.joining("&"));
        String normalized = scheme + "://" + uri.getHost().toLowerCase(Locale.ROOT)
                + (port < 0 || scheme.equals("https") && port == 443 || scheme.equals("http") && port == 80 ? "" : ":" + port)
                + path + (query.isEmpty() ? "" : "?" + query);
        String value = normalized + "\n" + text(institution) + "\n" + text(cohort);
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
    private static String text(String value) { return Normalizer.normalize(value == null ? "" : value.trim(), Normalizer.Form.NFKC).toLowerCase(Locale.ROOT); }
}
