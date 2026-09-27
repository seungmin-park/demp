package com.inhatc.demp.service;

import com.inhatc.demp.domain.announcement.UploadFile;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.*;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@Profile("local")
public class LocalFileStorage implements FileStorage {
    private final Path directory;
    private final ImageValidator validator;
    public LocalFileStorage(@Value("${app.local.upload-dir:.local/uploads}") String directory, ImageValidator validator) {
        this.directory = Path.of(directory).toAbsolutePath().normalize(); this.validator = validator;
    }
    public UploadFile save(MultipartFile file) throws IOException {
        String extension = validator.validatedExtension(file);
        Files.createDirectories(directory);
        String key = UUID.randomUUID() + "." + extension;
        try (var input = file.getInputStream()) { Files.copy(input, directory.resolve(key)); }
        return UploadFile.builder().uploadFileName(file.getOriginalFilename()).saveFileName(key).build();
    }
    public void delete(String key) {
        Path target = directory.resolve(key).normalize();
        if (!target.getParent().equals(directory)) throw new IllegalArgumentException("로컬 이미지 경로가 올바르지 않습니다.");
        try { Files.deleteIfExists(target); } catch (IOException failure) { throw new UncheckedIOException(failure); }
    }
}
