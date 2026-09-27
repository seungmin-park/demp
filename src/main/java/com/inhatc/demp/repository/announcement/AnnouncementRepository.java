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

    Optional<Announcement> findByDuplicateKey(String key);
    List<Announcement> findByDuplicateKeyIsNull();
    default boolean sourceExists(String key, long exceptId) {
        if (findByDuplicateKey(key).filter(item -> item.getId() != exceptId).isPresent()) return true;
        return findByDuplicateKeyIsNull().stream().filter(item -> item.getId() != exceptId)
                .anyMatch(item -> item.getDescription() != null && item.getCompany() != null && java.util.Objects.equals(key,
                        com.inhatc.demp.domain.announcement.AnnouncementSourceKey.of(item.getDescription().getAccessUrl(), item.getCompany().getName(), item.getCohort())));
    }

    long countByAnnouncementType(AnnouncementType type);

    Optional<Announcement> findByTitle(String title);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Announcement a where a.id = :id")
    Optional<Announcement> findByIdForMutation(@Param("id") long id);
}
