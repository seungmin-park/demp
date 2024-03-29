package com.inhatc.demp.repository.announcement;

import com.inhatc.demp.domain.announcemnet.Announcement;
import com.inhatc.demp.domain.announcemnet.AnnouncementType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {

    List<Announcement> findByAnnouncementType(AnnouncementType annotatedArrayType);

    Optional<Announcement> findByTitle(String title);
}
