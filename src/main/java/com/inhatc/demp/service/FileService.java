package com.inhatc.demp.service;

import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ObjectCannedACL;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.core.sync.RequestBody;
import com.inhatc.demp.domain.announcement.UploadFile;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileService implements FileStorage {

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;
    private final S3Client s3Client;
    private final ImageValidator imageValidator;

    @Override
    public UploadFile save(MultipartFile multipartFile) throws IOException {
        String extension = imageValidator.validatedExtension(multipartFile);

        String originalFilename = multipartFile.getOriginalFilename();
        String saveFileName = UUID.randomUUID() + "." + extension;

        PutObjectRequest request = PutObjectRequest.builder().bucket(bucket).key(saveFileName)
                .contentLength(multipartFile.getSize()).contentType(multipartFile.getContentType())
                .acl(ObjectCannedACL.PUBLIC_READ).build();
        try (InputStream inputStream = multipartFile.getInputStream()) {
            s3Client.putObject(request, RequestBody.fromInputStream(inputStream, multipartFile.getSize()));
        } catch(IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "파일 업로드에 실패했습니다.");
        }

        return new UploadFile(originalFilename, saveFileName);
    }

    @Override
    public void delete(String saveFileName) {
        s3Client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(saveFileName).build());
    }

}
