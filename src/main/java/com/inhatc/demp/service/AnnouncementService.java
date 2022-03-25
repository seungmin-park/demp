package com.inhatc.demp.service;

import com.inhatc.demp.domain.Announcement;
import com.inhatc.demp.domain.AnnouncementType;
import com.inhatc.demp.repository.AnnouncementRepository;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnnouncementService {

    private final AnnouncementRepository announcementRepository;
    private final JPAQueryFactory jpaQueryFactory;

    @Transactional
    public void join(Announcement announcement) {
        announcementRepository.save(announcement);
    }

    public List<Announcement> findByAnnouncementType(AnnouncementType announcementType) {
        return announcementRepository.findByAnnouncementType(announcementType);
    }

    public Optional<Announcement> findById(Long id) {
        return announcementRepository.findById(id);
    }

    public List<Announcement> findAll() {
        return announcementRepository.findAll();
    }
}
