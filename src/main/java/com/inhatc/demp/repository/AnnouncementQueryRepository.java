package com.inhatc.demp.repository;

import com.inhatc.demp.domain.Announcement;
import com.inhatc.demp.domain.AnnouncementType;
import com.inhatc.demp.domain.QAnnouncement;
import com.inhatc.demp.dto.AnnouncementSearchCondition;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.List;

import static com.inhatc.demp.domain.QAnnouncement.*;

@Repository
@RequiredArgsConstructor
public class AnnouncementQueryRepository {

    private final JPAQueryFactory jpaQueryFactory;

    public List<Announcement> findAllByAnnouncementType(AnnouncementSearchCondition announcementSearchCondition) {
        return jpaQueryFactory
                .selectFrom(announcement)
                .where(typeEq(announcementSearchCondition.getTypeName()))
                .fetch();
    }

    private BooleanExpression typeEq(String announcementType) {
        return StringUtils.hasText(announcementType) ? announcement.announcementType.eq(AnnouncementType.valueOf(announcementType)) : null;
    }
}
