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
    private final AnnouncementBodyImages bodyImages;
    private final java.time.Clock clock;

    @Transactional
    public void saveAnnouncementEntity(Announcement announcement) {
        announcement.changeDescription(sanitizeDescription(announcement.getDescription()));
        announcementRepository.save(announcement);
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void createAnnouncement(AnnouncementCreateRequest request) throws IOException {
        createAnnouncement(request, "system");
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void createAnnouncement(AnnouncementCreateRequest announcementCreateRequest, String actor) throws IOException {
        Career career = announcementCreateRequest.toCareer();
        RecruitPeriod recruitPeriod = RecruitPeriod.builder()
                .startedDate(announcementCreateRequest.getStartedDate())
                .deadLineDate(announcementCreateRequest.getDeadLineDate())
                .build();
        if (announcementRepository.sourceExists(announcementCreateRequest.sourceKey(), -1)) {
            throw new ApiException(HttpStatus.CONFLICT);
        }
        UploadFile image = announcementCreateRequest.getImage() == null || announcementCreateRequest.getImage().isEmpty()
                ? null : fileStorage.save(announcementCreateRequest.getImage());

        List<UploadFile> uploaded = List.of();
        try {
            uploaded = bodyImages.upload(announcementCreateRequest.getBodyImages());
            var body = bodyImages.prepare(announcementCreateRequest.getContent(), "", List.of(), uploaded);
            Description description = announcementCreateRequest.toDescription(body.html());
        Announcement announcement = Announcement.builder()
                .title(announcementCreateRequest.getTitle())
                .announcementType(announcementCreateRequest.getType())
                .career(career)
                .recruitPeriod(recruitPeriod)
                .description(description)
                .company(Company.builder().name(announcementCreateRequest.getCompany()).build())
                .image(image)
                .jobPosition(announcementCreateRequest.getPosition())
                .build();
        announcement.replaceBodyImages(body.images());
        announcement.changeRecruitment(announcementCreateRequest.getRecruitmentAudience(), announcementCreateRequest.getCohort(), announcementCreateRequest.getStipendAmount(), announcementCreateRequest.getStipendNote());
        announcement.changeEmploymentType(announcementCreateRequest.getEmploymentType());
        announcement.changeEducation(announcementCreateRequest.toEducationDetails());
        announcement.changePublication(announcementCreateRequest.getPublicationStatus() == null ? com.inhatc.demp.domain.announcement.PublicationStatus.DRAFT : announcementCreateRequest.getPublicationStatus());
        announcement.changeRecruitmentClosed(announcementCreateRequest.getRecruitmentClosed());
        announcement.recordPublication(announcementCreateRequest.getSourceName(), announcementCreateRequest.getSourceIdentifier(),
                announcementCreateRequest.getApplicationUrl(), announcementCreateRequest.isSourceVerified(), actor, java.time.LocalDateTime.now(clock));
            new TransactionTemplate(transactionManager).executeWithoutResult(
                    status -> announcementRepository.saveAndFlush(announcement));
        } catch (IOException | RuntimeException originalFailure) {
            bodyImages.compensate(uploaded, originalFailure);
            compensate(image, originalFailure);
            if (originalFailure instanceof org.springframework.dao.DataIntegrityViolationException && announcementRepository.sourceExists(announcementCreateRequest.sourceKey(), -1)) throw new ApiException(HttpStatus.CONFLICT);
            throw originalFailure;
        }
    }

    public List<com.inhatc.demp.domain.announcement.PublicationRevision> findPublicationHistory(long id) {
        return List.copyOf(announcementRepository.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND)).getPublicationHistory());
    }

    private void compensate(UploadFile image, Exception originalFailure) {
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
        return description.withContent(contentSanitizer.sanitize(description.getContent()));
    }

    public Slice<AnnouncementResponse> findAnnouncementSlice(AnnouncementSearchCondition announcementSearchCondition, Pageable pageable) {
        return announcementQueryRepository.findAnnouncementSlice(announcementSearchCondition, pageable)
                .map(announcement -> new AnnouncementResponse(announcement,
                        imageUrl.forImage(announcement.getImage())));
    }

    public Optional<AnnouncementDetailResponse> findDetailResponse(Long id) {
        return announcementRepository.findById(id).filter(Announcement::isPublished).map(announcement -> AnnouncementDetailResponse.from(
                announcement, imageUrl.forImage(announcement.getImage())));
    }

    public Optional<AnnouncementDetailResponse> findAdminDetailResponse(Long id) {
        return announcementRepository.findById(id).map(item -> AnnouncementDetailResponse.from(item, imageUrl.forImage(item.getImage())));
    }

    public Slice<AnnouncementResponse> findAdminAnnouncementSlice(AnnouncementSearchCondition condition, Pageable pageable) {
        return announcementQueryRepository.findAdminAnnouncementSlice(condition, pageable)
                .map(item -> new AnnouncementResponse(item, imageUrl.forImage(item.getImage())));
    }

    public List<AnnouncementScroll> findScrollResponses() {
        return announcementRepository.findAll().stream().filter(Announcement::isPublished)
                .map(announcement -> new AnnouncementScroll(announcement,
                        imageUrl.forImage(announcement.getImage())))
                .collect(Collectors.toList());
    }
}
