package com.inhatc.demp.service;

import com.inhatc.demp.domain.announcement.*;
import com.inhatc.demp.dto.announcement.AnnouncementSearchCondition;
import com.inhatc.demp.repository.announcement.AnnouncementRepository;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.beans.BeanWrapperImpl;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest
class AnnouncementViewsTest {
    @Autowired AnnouncementService service;
    @Autowired AnnouncementRepository repository;
    @AfterEach void cleanup() { repository.deleteAll(); }

    @Test
    @DisplayName("공개 상세 성공마다 응답·커밋 저장·목록 조회수를 하나씩 증가시킨다")
    void commitsEverySuccessfulView() {
        var item = published();
        assertThat(hits(service.recordViewAndGetDetail(item.getId()).orElseThrow())).isEqualTo(1);
        assertThat(hits(repository.findById(item.getId()).orElseThrow())).isEqualTo(1);
        assertThat(hits(service.recordViewAndGetDetail(item.getId()).orElseThrow())).isEqualTo(2);
        assertThat(hits(service.findDetailResponse(item.getId()).orElseThrow())).isEqualTo(2);
        var page = service.findAnnouncementSlice(new AnnouncementSearchCondition(), PageRequest.of(0, 8));
        assertThat(page.getContent()).hasSize(1); assertThat(page.hasNext()).isFalse();
        assertThat(hits(page.getContent().getFirst())).isEqualTo(2);
    }

    @Test
    @DisplayName("순수 상세·관리자·관련 목록·일반 목록은 조회수를 바꾸지 않는다")
    void pureReadsDoNotRecordViews() {
        var item = published();
        service.findDetailResponse(item.getId()); service.findAdminDetailResponse(item.getId());
        service.findScrollResponses(); service.findAnnouncementSlice(new AnnouncementSearchCondition(), PageRequest.of(0, 8));
        assertThat(hits(repository.findById(item.getId()).orElseThrow())).isZero();
    }

    @Test
    @DisplayName("없는 공고와 비공개 공고의 상세 실패는 조회수를 증가시키지 않는다")
    void unavailableAnnouncementsDoNotRecordViews() {
        var hidden = item(PublicationStatus.DRAFT);
        assertThat(service.recordViewAndGetDetail(hidden.getId())).isEmpty();
        assertThat(service.recordViewAndGetDetail(Long.MAX_VALUE)).isEmpty();
        assertThat(hits(repository.findById(hidden.getId()).orElseThrow())).isZero();
    }

    @Test
    @DisplayName("동시 상세 16회는 누락 없이 응답 1부터16과 최종 저장16을 반환한다")
    void concurrentViewsAreNotLost() throws Exception {
        var item = published(); var executor = Executors.newFixedThreadPool(8); var start = new CountDownLatch(1);
        try {
            var requests = new ArrayList<Future<Long>>();
            for (int i = 0; i < 16; i++) requests.add(executor.submit(() -> { start.await(); return hits(service.recordViewAndGetDetail(item.getId()).orElseThrow()); }));
            start.countDown(); var returned = new ArrayList<Long>();
            for (var request : requests) returned.add(request.get(15, TimeUnit.SECONDS));
            assertThat(returned).containsExactlyInAnyOrderElementsOf(java.util.stream.LongStream.rangeClosed(1,16).boxed().toList());
            assertThat(hits(repository.findById(item.getId()).orElseThrow())).isEqualTo(16);
        } finally { start.countDown(); executor.shutdownNow(); assertThat(executor.awaitTermination(15, TimeUnit.SECONDS)).isTrue(); }
    }

    private long hits(Object response) {
        var fields = new BeanWrapperImpl(response);
        assertThat(fields.isReadableProperty("hits")).as("조회수 응답과 저장 계약").isTrue();
        return ((Number) fields.getPropertyValue("hits")).longValue();
    }
    private Announcement published() { return item(PublicationStatus.PUBLISHED); }
    private Announcement item(PublicationStatus status) {
        var item = Announcement.builder().title("공고 조회 검증").announcementType(AnnouncementType.EMP).jobPosition(JobPosition.BACKEND)
                .company(Company.builder().name("DEMP").build()).career(Career.builder().minCareer(0).maxCareer(0).build())
                .description(Description.builder().content("본문").accessUrl("https://example.test/views").languages(Set.of(Language.JAVA)).build())
                .recruitPeriod(RecruitPeriod.builder().startedDate(LocalDateTime.of(2026,1,1,0,0)).deadLineDate(LocalDateTime.of(2026,12,31,0,0)).build()).build();
        item.changePublication(status); return repository.save(item);
    }
}
