package com.inhatc.demp.service;

import com.inhatc.demp.error.ApiException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class ImageValidator {
    private static final byte[] PNG_SIGNATURE = {
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
    };
    private static final byte[] JPEG_SIGNATURE = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};

    @Value("${file.max-size-bytes:5242880}")
    private long maxSizeBytes;

    public String validatedExtension(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty() || file.getSize() > maxSizeBytes) {
            throw new ApiException(HttpStatus.BAD_REQUEST);
        }
        String filename = file.getOriginalFilename();
        if (filename == null || !filename.contains(".")) {
            throw new ApiException(HttpStatus.BAD_REQUEST);
        }
        String extension = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        ImageType imageType = ImageType.from(extension, file.getContentType());
        byte[] header = new byte[PNG_SIGNATURE.length];
        int length;
        try (InputStream input = file.getInputStream()) {
            length = input.read(header);
        }
        if (!imageType.matches(header, length)) {
            throw new ApiException(HttpStatus.BAD_REQUEST);
        }
        return imageType.extension;
    }

    private enum ImageType {
        JPEG("jpg", new String[] {"jpg", "jpeg"}, "image/jpeg", JPEG_SIGNATURE),
        PNG("png", new String[] {"png"}, "image/png", PNG_SIGNATURE);

        private final String extension;
        private final String[] acceptedExtensions;
        private final String contentType;
        private final byte[] signature;

        ImageType(String extension, String[] acceptedExtensions, String contentType, byte[] signature) {
            this.extension = extension;
            this.acceptedExtensions = acceptedExtensions;
            this.contentType = contentType;
            this.signature = signature;
        }

        static ImageType from(String extension, String contentType) {
            return Arrays.stream(values())
                    .filter(type -> Arrays.asList(type.acceptedExtensions).contains(extension))
                    .filter(type -> type.contentType.equalsIgnoreCase(contentType))
                    .findFirst()
                    .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST));
        }

        boolean matches(byte[] header, int length) {
            return length >= signature.length
                    && Arrays.equals(signature, Arrays.copyOf(header, signature.length));
        }
    }
}
