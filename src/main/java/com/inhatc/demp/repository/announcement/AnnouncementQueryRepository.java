package com.inhatc.demp.repository.announcement;

import com.inhatc.demp.domain.Announcement;
import com.inhatc.demp.domain.AnnouncementType;
import com.inhatc.demp.dto.announcement.AnnouncementSearchCondition;
import com.querydsl.core.QueryResults;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;

import static com.inhatc.demp.domain.QAnnouncement.announcement;
import static org.springframework.util.StringUtils.hasText;

@Slf4j
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

    public Page<Announcement> pagingTest(AnnouncementSearchCondition announcementSearchCondition, Pageable pageable) {
        QueryResults<Announcement> result = jpaQueryFactory
                .selectFrom(announcement)
                .leftJoin(announcement.languages)
                .fetchJoin()
                .where(typeEq(announcementSearchCondition.getTypeName()),
                        positionIn(announcementSearchCondition.getPositions()),
                        languageIn(announcementSearchCondition.getLanguages()),
                        paymentGoe(announcementSearchCondition.getPayment()),
                        titleContain(announcementSearchCondition.getTitle()))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .distinct()
                .fetchResults();

        List<Announcement> content = result.getResults();
        long total = result.getTotal();

        return new PageImpl<>(content, pageable, total);
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