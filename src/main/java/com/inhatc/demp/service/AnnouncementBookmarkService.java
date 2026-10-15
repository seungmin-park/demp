package com.inhatc.demp.service;

import com.inhatc.demp.domain.AnnouncementBookmark;
import com.inhatc.demp.dto.announcement.*;
import com.inhatc.demp.error.ApiException;
import com.inhatc.demp.repository.*;
import com.inhatc.demp.repository.announcement.AnnouncementRepository;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnnouncementBookmarkService {
    private final AnnouncementBookmarkRepository bookmarks;
    private final MemberRepository members;
    private final AnnouncementRepository announcements;
    private final AnnouncementImageUrl imageUrl;
    private final Clock clock;

    @Transactional
    public BookmarkState setSaved(long memberId, long announcementId, boolean saved) {
        validateId(announcementId);
        var member = members.findByIdForAccountUpdate(memberId).orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED));
        var existing = bookmarks.findByMemberIdAndAnnouncementId(memberId, announcementId);
        if (saved) {
            announcements.findByIdForMutation(announcementId).filter(item -> item.isPublished())
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND));
            if (existing.isEmpty()) bookmarks.save(AnnouncementBookmark.saveFor(member, announcementId, LocalDateTime.now(clock)));
        } else existing.ifPresent(bookmarks::delete);
        return new BookmarkState(announcementId, saved);
    }

    public List<Long> savedIds(long memberId, List<Long> ids) {
        if (ids.size() > 100) throw new ApiException(HttpStatus.BAD_REQUEST);
        ids.forEach(AnnouncementBookmarkService::validateId);
        return ids.isEmpty() ? List.of() : bookmarks.findSavedIds(memberId, ids);
    }

    public BookmarkPage list(long memberId, int page, int size) {
        if (page < 0 || size < 1 || size > 20) throw new ApiException(HttpStatus.BAD_REQUEST);
        var slice = bookmarks.findByMemberIdOrderByIdDesc(memberId, PageRequest.of(page, size));
        var ids = slice.getContent().stream().map(AnnouncementBookmark::getAnnouncementId).toList();
        var published = ids.isEmpty() ? Map.<Long, com.inhatc.demp.domain.announcement.Announcement>of()
                : announcements.findPublishedForBookmarks(ids).stream().collect(Collectors.toMap(item -> item.getId(), item -> item));
        var entries = slice.getContent().stream().map(bookmark -> {
            var item = published.get(bookmark.getAnnouncementId());
            return new BookmarkEntry(bookmark.getAnnouncementId(), bookmark.getSavedAt(), item != null,
                    item == null ? null : new AnnouncementResponse(item, imageUrl.forImage(item.getImage())));
        }).toList();
        return new BookmarkPage(entries, slice.hasNext(), page);
    }
    private static void validateId(long id) { if (id == 0) throw new ApiException(HttpStatus.BAD_REQUEST); }
}
