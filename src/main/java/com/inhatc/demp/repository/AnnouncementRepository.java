package com.inhatc.demp.repository;

import com.inhatc.demp.domain.Announcement;
import com.inhatc.demp.domain.AnnouncementType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {

    public List<Announcement> findByAnnouncementType(AnnouncementType annotatedArrayType);
}
