package com.inhatc.demp.service;

import com.inhatc.demp.domain.announcemnet.Announcement;
import com.inhatc.demp.domain.announcemnet.Career;
import com.inhatc.demp.domain.announcemnet.Company;
import com.inhatc.demp.domain.announcemnet.Description;
import com.inhatc.demp.domain.announcemnet.RecruitPeriod;
import com.inhatc.demp.domain.announcemnet.UploadFile;
import com.inhatc.demp.dto.announcement.AnnouncementCreateRequest;
import com.inhatc.demp.dto.announcement.AnnouncementResponse;
import com.inhatc.demp.dto.announcement.AnnouncementSearchCondition;
import com.inhatc.demp.error.ApiException;
import com.inhatc.demp.repository.announcement.AnnouncementQueryRepository;
import com.inhatc.demp.repository.announcement.AnnouncementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
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

    @Transactional
    public void join(Announcement announcement) {
        announcement.changeDescription(sanitizeDescription(announcement.getDescription()));
        announcementRepository.save(announcement);
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void save(AnnouncementCreateRequest announcementCreateRequest) throws IOException {
        Career career = new Career(announcementCreateRequest.getMinCareer(), announcementCreateRequest.getMaxCareer());
        RecruitPeriod recruitPeriod = new RecruitPeriod(announcementCreateRequest.getStartedDate(),
                announcementCreateRequest.getDeadLineDate());
        Description description = sanitizeDescription(new Description(announcementCreateRequest.getContent(),
                announcementCreateRequest.getAccessUrl(), announcementCreateRequest.getPayment(),
                announcementCreateRequest.getLanguage()));
        if (announcementRepository.findByTitle(announcementCreateRequest.getTitle()).isPresent()) {
            throw new ApiException(HttpStatus.CONFLICT);
        }
        UploadFile image = fileStorage.save(announcementCreateRequest.getImage());

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

    public Slice<AnnouncementResponse> getAnnounceScroll(AnnouncementSearchCondition announcementSearchCondition, Pageable pageable) {
        return announcementQueryRepository.getAnnounceScroll(announcementSearchCondition, pageable);
    }

    public Page<Announcement> pageTest(AnnouncementSearchCondition announcementSearchCondition, Pageable pageable) {
        return announcementQueryRepository.pagingTest(announcementSearchCondition, pageable);
    }

    public List<Announcement> findAllByAnnouncementCondition(AnnouncementSearchCondition announcementSearchCondition) {
        return announcementQueryRepository.findAllByAnnouncementCondition(announcementSearchCondition);
    }

    public Optional<Announcement> findById(Long id) {
        return announcementRepository.findById(id);
    }

    public List<Announcement> findAll() {
        return announcementRepository.findAll();
    }
}
