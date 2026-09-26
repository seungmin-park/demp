package com.inhatc.demp.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.inhatc.demp.domain.announcement.UploadFile;
import com.inhatc.demp.error.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileService implements FileStorage {

    private static final byte[] PNG_SIGNATURE = {
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
    };
    private static final byte[] JPEG_SIGNATURE = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};

    @Value("${file.dir}")
    private String fileDir;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;
    @Value("${file.max-size-bytes:5242880}")
    private long maxSizeBytes;
    private final AmazonS3 amazonS3;

    public String getFullPath(String fileName){
        return fileDir + fileName;
    }

    @Override
    public UploadFile save(MultipartFile multipartFile) throws IOException {
        ImageType imageType = validate(multipartFile);

        String originalFilename = multipartFile.getOriginalFilename();
        String saveFileName = UUID.randomUUID() + "." + imageType.extension;

        ObjectMetadata objectMetadata = new ObjectMetadata();
        objectMetadata.setContentLength(multipartFile.getSize());
        objectMetadata.setContentType(multipartFile.getContentType());

        try(InputStream inputStream = multipartFile.getInputStream()) {
            amazonS3.putObject(new PutObjectRequest(bucket, saveFileName, inputStream, objectMetadata)
                    .withCannedAcl(CannedAccessControlList.PublicRead));
        } catch(IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "파일 업로드에 실패했습니다.");
        }

        return new UploadFile(originalFilename, saveFileName);
    }

    @Override
    public void delete(String saveFileName) {
        amazonS3.deleteObject(new DeleteObjectRequest(bucket, saveFileName));
    }

    private ImageType validate(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST);
        }
        if (file.getSize() > maxSizeBytes) {
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
        return imageType;
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
