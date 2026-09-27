package com.inhatc.demp.repository.announcement;

import com.inhatc.demp.domain.announcement.Announcement;
import com.inhatc.demp.domain.announcement.AnnouncementType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {

    List<Announcement> findByAnnouncementType(AnnouncementType annotatedArrayType);

    long countByAnnouncementType(AnnouncementType type);

    Optional<Announcement> findByTitle(String title);
}
