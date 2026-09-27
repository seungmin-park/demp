package com.inhatc.demp.service;

import com.inhatc.demp.domain.announcement.Announcement;
import com.inhatc.demp.domain.announcement.Career;
import com.inhatc.demp.domain.announcement.Company;
import com.inhatc.demp.domain.announcement.Description;
import com.inhatc.demp.domain.announcement.RecruitPeriod;
import com.inhatc.demp.domain.announcement.UploadFile;
import com.inhatc.demp.dto.announcement.AnnouncementCreateRequest;
import com.inhatc.demp.dto.announcement.AnnouncementResponse;
import com.inhatc.demp.dto.announcement.AnnouncementDetailResponse;
import com.inhatc.demp.dto.announcement.AnnouncementScroll;
import com.inhatc.demp.dto.announcement.AnnouncementSearchCondition;
import com.inhatc.demp.error.ApiException;
import com.inhatc.demp.repository.announcement.AnnouncementQueryRepository;
import com.inhatc.demp.repository.announcement.AnnouncementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnnouncementService {

    private final AnnouncementRepository announcementRepository;
    private final AnnouncementQueryRepository announcementQueryRepository;
    private final FileStorage fileStorage;
    private final ContentSanitizer contentSanitizer;
    private final PlatformTransactionManager transactionManager;
    private final AnnouncementImageUrl imageUrl;

    @Transactional
    public void saveAnnouncementEntity(Announcement announcement) {
        announcement.changeDescription(sanitizeDescription(announcement.getDescription()));
        announcementRepository.save(announcement);
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void createAnnouncement(AnnouncementCreateRequest announcementCreateRequest) throws IOException {
        Career career = new Career(announcementCreateRequest.getMinCareer(), announcementCreateRequest.getMaxCareer());
        RecruitPeriod recruitPeriod = new RecruitPeriod(announcementCreateRequest.getStartedDate(),
                announcementCreateRequest.getDeadLineDate());
        Description description = sanitizeDescription(new Description(announcementCreateRequest.getContent(),
                announcementCreateRequest.getAccessUrl(), announcementCreateRequest.getPayment(),
                announcementCreateRequest.getLanguage()));
        if (announcementRepository.findByTitle(announcementCreateRequest.getTitle()).isPresent()) {
            throw new ApiException(HttpStatus.CONFLICT);
        }
        UploadFile image = announcementCreateRequest.getImage() == null || announcementCreateRequest.getImage().isEmpty()
                ? null : fileStorage.save(announcementCreateRequest.getImage());

        Announcement announcement = Announcement.builder()
                .title(announcementCreateRequest.getTitle())
                .announcementType(announcementCreateRequest.getType())
                .career(career)
                .recruitPeriod(recruitPeriod)
                .description(description)
                .company(new Company(announcementCreateRequest.getCompany()))
                .image(image)
                .jobPosition(announcementCreateRequest.getPosition())
                .build();
        try {
            new TransactionTemplate(transactionManager).executeWithoutResult(
                    status -> announcementRepository.saveAndFlush(announcement));
        } catch (RuntimeException originalFailure) {
            compensate(image, originalFailure);
            throw originalFailure;
        }
    }

    private void compensate(UploadFile image, RuntimeException originalFailure) {
        if (image == null) return;
        try {
            fileStorage.delete(image.getSaveFileName());
        } catch (RuntimeException compensationFailure) {
            originalFailure.addSuppressed(compensationFailure);
            log.error("공고 저장 실패 보상 중 파일 삭제 실패. key={}", image.getSaveFileName(), compensationFailure);
        }
    }

    private Description sanitizeDescription(Description description) {
        if (description == null) {
            return null;
        }
        return new Description(contentSanitizer.sanitize(description.getContent()), description.getAccessUrl(),
                description.getPayment(), description.getLanguages());
    }

    public Slice<AnnouncementResponse> findAnnouncementSlice(AnnouncementSearchCondition announcementSearchCondition, Pageable pageable) {
        return announcementQueryRepository.findAnnouncementSlice(announcementSearchCondition, pageable)
                .map(announcement -> new AnnouncementResponse(announcement,
                        imageUrl.forImage(announcement.getImage())));
    }

    public Optional<AnnouncementDetailResponse> findDetailResponse(Long id) {
        return announcementRepository.findById(id).map(announcement -> AnnouncementDetailResponse.from(
                announcement, imageUrl.forImage(announcement.getImage())));
    }

    public List<AnnouncementScroll> findScrollResponses() {
        return announcementRepository.findAll().stream()
                .map(announcement -> new AnnouncementScroll(announcement,
                        imageUrl.forImage(announcement.getImage())))
                .collect(Collectors.toList());
    }
}
