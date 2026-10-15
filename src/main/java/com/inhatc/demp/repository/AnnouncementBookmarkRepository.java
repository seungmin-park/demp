package com.inhatc.demp.repository;

import com.inhatc.demp.domain.AnnouncementBookmark;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface AnnouncementBookmarkRepository extends JpaRepository<AnnouncementBookmark, Long> {
    Optional<AnnouncementBookmark> findByMemberIdAndAnnouncementId(long memberId, long announcementId);
    Slice<AnnouncementBookmark> findByMemberIdOrderByIdDesc(long memberId, Pageable pageable);
    @Query("select b.announcementId from AnnouncementBookmark b where b.member.id = :memberId and b.announcementId in :ids order by b.announcementId")
    List<Long> findSavedIds(@Param("memberId") long memberId, @Param("ids") Collection<Long> ids);
}
