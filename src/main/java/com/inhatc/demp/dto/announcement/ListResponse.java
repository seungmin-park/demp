package com.inhatc.demp.dto.announcement;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class ListResponse {

    private List<AnnouncementResponse> result = new ArrayList<>();
    private int pageNum;

    public ListResponse(List<AnnouncementResponse> result, int pageNum) {
        this.result.addAll(result);
        this.pageNum = pageNum;
    }
}
