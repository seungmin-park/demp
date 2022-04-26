package com.inhatc.demp.service;

import com.inhatc.demp.domain.Announcement;
import com.inhatc.demp.domain.UploadFile;
import com.inhatc.demp.dto.announcement.AnnouncementForm;
import com.inhatc.demp.dto.announcement.AnnouncementSearchCondition;
import com.inhatc.demp.repository.AnnouncementQueryRepository;
import com.inhatc.demp.repository.AnnouncementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
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
    public void save(AnnouncementForm announcementForm) throws IOException {
        UploadFile image = fileService.saveFile(announcementForm.getImage());
        Optional<Announcement> optional = announcementRepository.findByTitle(announcementForm.getTitle());

        if (optional.isPresent()) {
            throw new IllegalStateException("이미 존재하는 공고 입니다.");
        }

        Announcement announcement = new Announcement(announcementForm.getTitle(),announcementForm.getLanguages(), announcementForm.getPosition(),
                announcementForm.getCareer(),announcementForm.getContent(),announcementForm.getAccessUrl(),announcementForm.getPayment(),announcementForm.getCompany(),
                image, announcementForm.getType(),announcementForm.getStartedDate(), announcementForm.getDeadLineDate());

        announcementRepository.save(announcement);
    }

    public PageImpl<Announcement> pageTest(AnnouncementSearchCondition announcementSearchCondition, Pageable pageable) {
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
