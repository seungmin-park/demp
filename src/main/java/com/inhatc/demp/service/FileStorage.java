package com.inhatc.demp.service;

import com.inhatc.demp.domain.announcemnet.UploadFile;
import java.io.IOException;
import org.springframework.web.multipart.MultipartFile;

public interface FileStorage {

    UploadFile save(MultipartFile file) throws IOException;

    void delete(String key);
}
