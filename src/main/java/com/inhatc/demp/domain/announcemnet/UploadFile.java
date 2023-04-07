package com.inhatc.demp.domain.announcemnet;

import javax.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Embeddable
@AllArgsConstructor
@NoArgsConstructor
public class UploadFile {

    private String uploadFileName;
    private String saveFileName;
}
