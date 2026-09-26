package com.inhatc.demp.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.inhatc.demp.domain.announcemnet.Announcement;
import com.inhatc.demp.domain.announcemnet.AnnouncementType;
import com.inhatc.demp.domain.announcemnet.JobPosition;
import com.inhatc.demp.domain.announcemnet.Language;
import com.inhatc.demp.domain.announcemnet.UploadFile;
import com.inhatc.demp.dto.announcement.AnnouncementCreateRequest;
import com.inhatc.demp.repository.announcement.AnnouncementQueryRepository;
import com.inhatc.demp.repository.announcement.AnnouncementRepository;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

class AnnouncementUploadCompensationTest {

    private final AnnouncementRepository announcementRepository = mock(AnnouncementRepository.class);
    private final AnnouncementQueryRepository announcementQueryRepository = mock(AnnouncementQueryRepository.class);
    private final FileStorage fileStorage = mock(FileStorage.class);
    private final PlatformTransactionManager transactionManager = mock(PlatformTransactionManager.class);
    private final AnnouncementService announcementService = new AnnouncementService(announcementRepository,
            announcementQueryRepository, fileStorage, new ContentSanitizer(), transactionManager);

    @BeforeEach
    void setUp() {
        when(announcementRepository.findByTitle(any())).thenReturn(Optional.empty());
        when(transactionManager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
    }

    @Test
    @DisplayName("업로드 후 DB 저장이 실패하면 저장한 파일을 삭제한다")
    void deletesUploadedFileWhenDatabaseSaveFails() throws IOException {
        AnnouncementCreateRequest request = request("DB 실패 공고");
        DataIntegrityViolationException failure = new DataIntegrityViolationException("db failed");
        when(fileStorage.save(request.getImage())).thenReturn(new UploadFile("image.png", "saved.png"));
        doThrow(failure).when(announcementRepository).saveAndFlush(any(Announcement.class));

        assertThatThrownBy(() -> announcementService.save(request)).isSameAs(failure);

        verify(fileStorage).delete("saved.png");
    }

    @Test
    @DisplayName("DB commit이 실패해도 저장한 파일을 삭제한다")
    void deletesUploadedFileWhenTransactionCommitFails() throws IOException {
        AnnouncementCreateRequest request = request("commit 실패 공고");
        DataIntegrityViolationException failure = new DataIntegrityViolationException("commit failed");
        when(fileStorage.save(request.getImage())).thenReturn(new UploadFile("image.png", "saved.png"));
        doThrow(failure).when(transactionManager).commit(any());

        assertThatThrownBy(() -> announcementService.save(request)).isSameAs(failure);

        verify(fileStorage).delete("saved.png");
    }

    @Test
    @DisplayName("보상 삭제가 실패해도 원래 DB 오류를 유지하고 저장 키로 삭제를 시도한다")
    void preservesOriginalFailureWhenCompensationFails() throws IOException {
        AnnouncementCreateRequest request = request("보상 실패 공고");
        DataIntegrityViolationException failure = new DataIntegrityViolationException("db failed");
        IllegalStateException compensationFailure = new IllegalStateException("delete failed");
        when(fileStorage.save(request.getImage())).thenReturn(new UploadFile("image.png", "traceable-key.png"));
        doThrow(failure).when(announcementRepository).saveAndFlush(any(Announcement.class));
        doThrow(compensationFailure).when(fileStorage).delete("traceable-key.png");

        assertThatThrownBy(() -> announcementService.save(request))
                .isSameAs(failure)
                .satisfies(exception -> assertThat(exception.getSuppressed()).containsExactly(compensationFailure));

        verify(fileStorage).delete("traceable-key.png");
    }

    private AnnouncementCreateRequest request(String title) {
        AnnouncementCreateRequest request = new AnnouncementCreateRequest();
        request.setTitle(title);
        request.setCompany("DEMP");
        request.setType(AnnouncementType.EMP);
        request.setPosition(JobPosition.BACKEND);
        request.setMinCareer(0);
        request.setMaxCareer(1);
        request.setStartedDate(LocalDateTime.of(2026, 9, 1, 0, 0));
        request.setDeadLineDate(LocalDateTime.of(2026, 9, 30, 23, 59));
        request.setContent("<p>채용</p>");
        request.setAccessUrl("https://example.com/jobs");
        request.setPayment(3000);
        request.setLanguage(Set.of(Language.JAVA));
        request.setImage(new MockMultipartFile("image", "image.png", "image/png",
                new byte[] {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A}));
        return request;
    }
}
