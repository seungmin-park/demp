package com.inhatc.demp.repository.announcement;
import com.inhatc.demp.domain.announcement.AnnouncementReport;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.*;
import java.util.Optional;
import jakarta.persistence.LockModeType;
public interface AnnouncementReportRepository extends JpaRepository<AnnouncementReport, Long> {
    @Query("select r from AnnouncementReport r join fetch r.announcement where :all = true or r.resolvedAt is null")
    Page<AnnouncementReport> findReports(@Param("all") boolean all, Pageable pageable);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from AnnouncementReport r where r.id = :id")
    Optional<AnnouncementReport> findForUpdate(@Param("id") long id);
}
