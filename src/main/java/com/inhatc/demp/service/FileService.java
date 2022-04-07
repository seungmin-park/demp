package com.inhatc.demp.service;

import com.inhatc.demp.domain.UploadFile;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

@Service
public class FileService {

    @Value("${file.dir}")
    private String fileDir;

    public String getFullPath(String fileName){
        return fileDir + fileName;
    }

    public UploadFile saveFile(MultipartFile multipartFile) throws IOException {
        if (multipartFile.isEmpty()) {
            return null;
        }
        String originalFilename = multipartFile.getOriginalFilename();
        String saveFileName = getSaveFileName(multipartFile);
        multipartFile.transferTo(new File(getFullPath(saveFileName)));

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
