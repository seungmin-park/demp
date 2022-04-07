package com.inhatc.demp.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Embeddable;
@Getter
@Setter
@Embeddable
@AllArgsConstructor
@NoArgsConstructor
public class UploadFile {

    private String uploadFileName;
    private String saveFileName;
}
