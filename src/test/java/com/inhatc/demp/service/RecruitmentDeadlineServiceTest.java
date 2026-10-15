package com.inhatc.demp.service;

import com.inhatc.demp.domain.announcement.*;
import com.inhatc.demp.dto.announcement.*;
import com.inhatc.demp.repository.announcement.*;
import jakarta.validation.Validator;
import java.time.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.validation.DataBinder;
import org.springframework.data.domain.PageRequest;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest
class RecruitmentDeadlineServiceTest {
    @Autowired AnnouncementService service;
    @Autowired AnnouncementRepository repository;
    @Autowired AnnouncementQueryRepository queries;
    @Autowired Validator validator;
    @AfterEach void cleanup() { repository.deleteAll(); }
    private AnnouncementCreateRequest request(String mode) {
        var form=new AnnouncementCreateRequest();
        var values=new MutablePropertyValues();
        values.add("deadlineMode",mode);
        var binder=new DataBinder(form); binder.bind(values);
        assertThat(binder.getBindingResult().getAllErrors()).isEmpty();
        form.setTitle("수시 모집 " + mode); form.setCompany("DEMP"); form.setType(AnnouncementType.EMP);
        form.setPosition(JobPosition.BACKEND); form.setContent("원문에서 확인한 모집 안내");
        form.setAccessUrl("https://example.com/"+mode); form.setPublicationStatus(PublicationStatus.PUBLISHED);
        form.setStartedDate(LocalDateTime.of(2026,1,1,9,0));
        return form;
    }
    @ParameterizedTest
    @DisplayName("채용 시 마감과 상시 모집은 가상 마감 없이 공개 저장되고 모집 중 검색에 포함된다")
    @ValueSource(strings={"UNTIL_FILLED","ALWAYS_OPEN"})
    void publishesWithoutInventedDeadline(String mode) throws Exception {
        var form=request(mode);
        assertThat(validator.validate(form)).isEmpty();
        service.createAnnouncement(form);
        var detail=service.findAdminDetailResponse(repository.findByTitle(form.getTitle()).orElseThrow().getId()).orElseThrow();
        assertThat(detail.getDeadLineDate()).isNull();
        var filter=new AnnouncementSearchCondition(); filter.setRecruitmentStatus(RecruitmentStatus.OPEN);
        assertThat(queries.findAnnouncementSlice(filter,PageRequest.of(0,8)).getContent()).extracting(Announcement::getTitle).containsExactly(form.getTitle());
        var item=repository.findByTitle(form.getTitle()).orElseThrow(); item.changeRecruitmentClosed(true); repository.save(item);
        assertThat(queries.findAnnouncementSlice(filter,PageRequest.of(0,8)).getContent()).isEmpty();
        filter.setRecruitmentStatus(RecruitmentStatus.CLOSED);
        assertThat(queries.findAnnouncementSlice(filter,PageRequest.of(0,8)).getContent()).extracting(Announcement::getTitle).containsExactly(form.getTitle());
    }
    @Test
    @DisplayName("정확한 마감이 없는 일시 지정과 미확인은 공개할 수 없고 초안은 보존한다")
    void rejectsUnconfirmedPublication() throws Exception {
        for (String mode:java.util.List.of("FIXED","UNKNOWN")) {
            var form=request(mode);
            assertThat(validator.validate(form)).isNotEmpty();
            form.setPublicationStatus(PublicationStatus.DRAFT);
            assertThat(validator.validate(form)).isEmpty(); service.createAnnouncement(form);
        }
        assertThat(repository.count()).isEqualTo(2);
    }
    @Test
    @DisplayName("채용 시 마감 선택에 날짜가 남아 있으면 모순된 입력을 거절하고 날짜를 버리지 않는다")
    void rejectsConflictingDeadline() {
        var form=request("UNTIL_FILLED"); form.setDeadLineDate(LocalDateTime.of(2026,10,26,17,0));
        assertThat(validator.validate(form)).isNotEmpty();
    }
}
