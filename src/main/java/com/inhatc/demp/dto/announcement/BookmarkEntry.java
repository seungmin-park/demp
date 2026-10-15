package com.inhatc.demp.dto.announcement;
import java.time.LocalDateTime;
public record BookmarkEntry(long announcementId, LocalDateTime savedAt, boolean available, AnnouncementResponse announcement) {}
