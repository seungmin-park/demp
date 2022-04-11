package com.inhatc.demp.service;

import com.inhatc.demp.domain.Announcement;
import com.inhatc.demp.domain.AnnouncementForm;
import com.inhatc.demp.domain.UploadFile;
import com.inhatc.demp.dto.AnnouncementSearchCondition;
import com.inhatc.demp.repository.AnnouncementQueryRepository;
import com.inhatc.demp.repository.AnnouncementRepository;
import lombok.RequiredArgsConstructor;
import org.hibernate.DuplicateMappingException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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

        Announcement announcement = new Announcement(announcementForm.getTitle(),announcementForm.getLanguage(), announcementForm.getPosition(),
                announcementForm.getCareer(),announcementForm.getContent(),announcementForm.getAccessUrl(),announcementForm.getPayment(),announcementForm.getCompany(),
                image, announcementForm.getType(),announcementForm.getStartedDate(), announcementForm.getDeadLineDate());

        announcementRepository.save(announcement);
    }

    private LocalDateTime parsingTime(String formDate) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
        LocalDate ld = LocalDate.parse(formDate, formatter);
        return LocalDateTime.of(ld, LocalDateTime.now().toLocalTime());
    }

    public List<Announcement> findAllByAnnouncementType(AnnouncementSearchCondition AnnouncementSearchCondition) {
        return announcementQueryRepository.findAllByAnnouncementType(AnnouncementSearchCondition);
    }

    public Optional<Announcement> findById(Long id) {
        return announcementRepository.findById(id);
    }

    public List<Announcement> findAll() {
        return announcementRepository.findAll();
    }
}
