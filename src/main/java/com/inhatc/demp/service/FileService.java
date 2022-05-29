package com.inhatc.demp.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.inhatc.demp.domain.UploadFile;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileService {

    @Value("${file.dir}")
    private String fileDir;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;
    private final AmazonS3 amazonS3;

    public String getFullPath(String fileName){
        return fileDir + fileName;
    }

    public UploadFile saveFile(MultipartFile multipartFile) throws IOException {
        if (multipartFile.isEmpty()) {
            return null;
        }

        String originalFilename = multipartFile.getOriginalFilename();
        String saveFileName = getSaveFileName(multipartFile);
//        multipartFile.transferTo(new File(getFullPath(saveFileName)));

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

    private String getSaveFileName(MultipartFile multipartFile) {
        String uuid = UUID.randomUUID().toString();
        String ext = getExt(multipartFile);
        return uuid + "." + ext.toLowerCase();
    }

    private String getExt(MultipartFile multipartFile) {
        int pos = multipartFile.getOriginalFilename().lastIndexOf(".");
        return  multipartFile.getOriginalFilename().substring(pos + 1);
    }
}
