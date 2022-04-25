package com.inhatc.demp.repository;

import com.inhatc.demp.domain.Announcement;
import com.inhatc.demp.domain.AnnouncementType;
import com.inhatc.demp.dto.announcement.AnnouncementSearchCondition;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

import static com.inhatc.demp.domain.QAnnouncement.*;
import static org.springframework.util.StringUtils.*;

@Repository
@RequiredArgsConstructor
public class AnnouncementQueryRepository {

    private final JPAQueryFactory jpaQueryFactory;

    public List<Announcement> findAllByAnnouncementCondition(AnnouncementSearchCondition announcementSearchCondition) {
        return jpaQueryFactory
                .selectFrom(announcement)
                .where(typeEq(announcementSearchCondition.getTypeName()),
                        positionIn(announcementSearchCondition.getPositions()),
                        languageIn(announcementSearchCondition.getLanguages()),
                        paymentGoe(announcementSearchCondition.getPayment()),
                        titleContain(announcementSearchCondition.getTitle()))
                .fetch();
    }

    private BooleanExpression typeEq(String announcementType) {
        return hasText(announcementType) ? announcement.announcementType.eq(AnnouncementType.valueOf(announcementType)) : null;
    }

    private BooleanExpression positionIn(List<String> positions) {
        return positions.isEmpty() ? null : announcement.position.in(positions);
    }

    private BooleanExpression languageIn(List<String> languages) {
        return languages.isEmpty() ? null : announcement.languages.any().in(languages);
    }

    private BooleanExpression paymentGoe(int payment) {
        return payment <= 0 ? null : announcement.payment.goe(payment);
    }

    private Predicate titleContain(String title) {
        return hasText(title) ? announcement.title.contains(title) : null;
    }
}
