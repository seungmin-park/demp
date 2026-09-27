package com.inhatc.demp.service;

import com.inhatc.demp.domain.announcement.UploadFile;
import com.inhatc.demp.error.ApiException;
import java.io.IOException;
import java.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/** Owns the mapping between submitted body images and files belonging to this announcement. */
@Component
@RequiredArgsConstructor
@Slf4j
public class AnnouncementBodyImages {
    private final FileStorage storage;
    private final AnnouncementImageUrl urls;
    private final ContentSanitizer sanitizer;

    public List<UploadFile> upload(List<MultipartFile> images) throws IOException {
        if (images == null || images.size() > 10) throw new ApiException(HttpStatus.BAD_REQUEST);
        List<UploadFile> uploaded = new ArrayList<>();
        try {
            for (MultipartFile image : images) uploaded.add(storage.save(image));
            return uploaded;
        } catch (IOException | RuntimeException failure) {
            compensate(uploaded, failure);
            throw failure;
        }
    }

    public Body prepare(String html, String previousHtml, List<UploadFile> existing, List<UploadFile> uploaded) {
        var document = Jsoup.parseBodyFragment(html == null ? "" : html);
        document.outputSettings().prettyPrint(false);
        Map<String, UploadFile> allowed = new LinkedHashMap<>();
        existing.forEach(file -> allowed.put(urls.forImage(file), file));
        uploaded.forEach(file -> allowed.put(urls.forImage(file), file));
        // Previous HTML was accepted with these owned file keys. A CDN change must not turn
        // an unchanged attachment into a deletion. Only URLs from that saved document qualify.
        for (var previousImage : Jsoup.parseBodyFragment(previousHtml).select("img")) {
            String previousUrl = previousImage.attr("src");
            try {
                String path = java.net.URI.create(previousUrl).getPath();
                existing.stream().filter(file -> path != null && path.endsWith("/" + file.getSaveFileName()))
                        .forEach(file -> allowed.put(previousUrl, file));
            } catch (IllegalArgumentException ignored) { /* Invalid stored URLs are not authorized. */ }
        }
        Set<UploadFile> retained = new LinkedHashSet<>();
        for (var image : document.select("img")) {
            String source = image.attr("src");
            if (source.startsWith("attachment:")) {
                try {
                    int index = Integer.parseInt(source.substring("attachment:".length()));
                    source = urls.forImage(uploaded.get(index));
                    image.attr("src", source);
                } catch (NumberFormatException | IndexOutOfBoundsException failure) {
                    throw new ApiException(HttpStatus.BAD_REQUEST);
                }
            }
            UploadFile file = allowed.get(source);
            if (file == null) image.remove();
            else { image.attr("src", urls.forImage(file)); retained.add(file); }
        }
        // Do not silently save files omitted by the submitted document.
        if (!retained.containsAll(uploaded)) throw new ApiException(HttpStatus.BAD_REQUEST);
        return new Body(sanitizer.sanitizeAnnouncement(document.body().html()), new ArrayList<>(retained));
    }

    public void compensate(List<UploadFile> files, Exception original) {
        for (UploadFile file : files) {
            try { storage.delete(file.getSaveFileName()); }
            catch (RuntimeException failure) {
                original.addSuppressed(failure);
                log.error("공고 저장 실패 보상 중 파일 삭제 실패 key={}", file.getSaveFileName(), failure);
            }
        }
    }

    public record Body(String html, List<UploadFile> images) { }
}
