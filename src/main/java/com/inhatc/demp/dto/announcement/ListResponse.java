package com.inhatc.demp.dto.announcement;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

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
