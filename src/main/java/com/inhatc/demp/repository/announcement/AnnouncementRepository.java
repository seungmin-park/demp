package com.inhatc.demp.repository.announcement;

import com.inhatc.demp.domain.announcement.Announcement;
import com.inhatc.demp.domain.announcement.AnnouncementType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;

public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {

    List<Announcement> findByAnnouncementType(AnnouncementType annotatedArrayType);

    long countByAnnouncementType(AnnouncementType type);

    Optional<Announcement> findByTitle(String title);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Announcement a where a.id = :id")
    Optional<Announcement> findByIdForMutation(@Param("id") long id);
}
