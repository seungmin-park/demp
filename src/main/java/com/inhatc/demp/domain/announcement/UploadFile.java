package com.inhatc.demp.domain.announcement;

import lombok.AccessLevel;
import lombok.Builder;
import jakarta.persistence.Embeddable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode
public class UploadFile {

    private String uploadFileName;
    private String saveFileName;

    @Builder
    private UploadFile(String uploadFileName, String saveFileName) {
        this.uploadFileName = uploadFileName;
        this.saveFileName = saveFileName;
    }
}
