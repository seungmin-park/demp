package com.inhatc.demp.service;

import com.inhatc.demp.domain.announcemnet.Announcement;
import com.inhatc.demp.domain.announcemnet.UploadFile;
import com.inhatc.demp.dto.announcement.AnnouncementCreateRequest;
import com.inhatc.demp.dto.announcement.AnnouncementResponse;
import com.inhatc.demp.dto.announcement.AnnouncementSearchCondition;
import com.inhatc.demp.repository.announcement.AnnouncementQueryRepository;
import com.inhatc.demp.repository.announcement.AnnouncementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnnouncementService {

    private final AnnouncementRepository announcementRepository;
    private final AnnouncementQueryRepository announcementQueryRepository;
    private final FileService fileService;
    @Transactional
    public void join(Announcement announcement) {
        announcementRepository.save(announcement);
    }

    @Transactional
    public void save(AnnouncementCreateRequest announcementCreateRequest) throws IOException {
        UploadFile image = fileService.saveFile(announcementCreateRequest.getImage());
        Optional<Announcement> optional = announcementRepository.findByTitle(announcementCreateRequest.getTitle());

        if (optional.isPresent()) {
            throw new IllegalStateException("이미 존재하는 공고 입니다.");
        }

        Announcement announcement = Announcement.builder()
                .title(announcementCreateRequest.getTitle())
                .announcementType(announcementCreateRequest.getType())
                .career(announcementCreateRequest.getCareer())
                .recruitPeriod(announcementCreateRequest.getRecruitPeriod())
                .description(announcementCreateRequest.getDescription())
                .company(announcementCreateRequest.getCompany())
                .image(image)
                .jobPosition(announcementCreateRequest.getPosition())
                .build();


        announcementRepository.save(announcement);
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
