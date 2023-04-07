package com.inhatc.demp.repository.announcement;

import static com.inhatc.demp.domain.announcemnet.QAnnouncement.announcement;
import static org.springframework.util.StringUtils.hasText;

import com.inhatc.demp.domain.announcemnet.Announcement;
import com.inhatc.demp.domain.announcemnet.AnnouncementType;
import com.inhatc.demp.domain.announcemnet.JobPosition;
import com.inhatc.demp.domain.announcemnet.Language;
import com.inhatc.demp.dto.announcement.AnnouncementResponse;
import com.inhatc.demp.dto.announcement.AnnouncementSearchCondition;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.QueryResults;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.stereotype.Repository;

@Slf4j
@Repository
@RequiredArgsConstructor
public class AnnouncementQueryRepository {

    private final JPAQueryFactory jpaQueryFactory;

    public List<Announcement> findAllByAnnouncementCondition(AnnouncementSearchCondition announcementSearchCondition) {
        return jpaQueryFactory
                .selectFrom(announcement)
                .where(typeEq(announcementSearchCondition.getAnnouncementType()),
                        positionIn(announcementSearchCondition.getPositions()),
                        languageIn(announcementSearchCondition.getLanguage()),
                        paymentGoe(announcementSearchCondition.getPayment()),
                        titleContain(announcementSearchCondition.getTitle()))
                .fetch();
    }

    public Slice<AnnouncementResponse> getAnnounceScroll(AnnouncementSearchCondition announcementSearchCondition,
                                                         Pageable pageable) {
        QueryResults<Announcement> results = jpaQueryFactory.selectFrom(announcement)
                .leftJoin(announcement.description.languages)
                .fetchJoin()
                .where(typeEq(announcementSearchCondition.getAnnouncementType()),
                        positionIn(announcementSearchCondition.getPositions()),
                        languageIn(announcementSearchCondition.getLanguage()),
                        paymentGoe(announcementSearchCondition.getPayment()),
                        minCareerLoe(announcementSearchCondition.getCareer()),
                        maxCareerGoe(announcementSearchCondition.getCareer()),
                        titleContain(announcementSearchCondition.getTitle()))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize() + 1)
                .distinct()
                .fetchResults();

        List<AnnouncementResponse> content = new ArrayList<>();
        for (Announcement result : results.getResults()) {
            content.add(new AnnouncementResponse(result));
        }

        boolean hasNext = false;
        if (content.size() > pageable.getPageSize()) {
            content.remove(pageable.getPageSize());
            hasNext = true;
        }

        return new SliceImpl<>(content, pageable, hasNext);
    }

    public Page<Announcement> pagingTest(AnnouncementSearchCondition announcementSearchCondition, Pageable pageable) {
        // TODO: 2022-05-30 firstResult/maxResults specified with collection fetch; applying in memory! 해결 및 메소드명 리팩토링
        QueryResults<Announcement> result = jpaQueryFactory
                .selectFrom(announcement)
                .leftJoin(announcement.description.languages)
                .fetchJoin()
                .where(typeEq(announcementSearchCondition.getAnnouncementType()),
                        positionIn(announcementSearchCondition.getPositions()),
                        languageIn(announcementSearchCondition.getLanguage()),
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

    private BooleanExpression typeEq(AnnouncementType announcementType) {
        return announcementType != null ? announcement.announcementType.eq(announcementType) : null;
    }

    private BooleanExpression positionIn(List<JobPosition> positions) {
        return positions.isEmpty() ? null : announcement.jobPosition.in(positions);
    }

    private BooleanExpression languageIn(Language language) {
        return language == null ? null : announcement.description.languages.contains(language);
    }

    private BooleanExpression paymentGoe(int payment) {
        return payment <= 0 ? null : announcement.description.payment.goe(payment);
    }

    private BooleanExpression minCareerLoe(int career) {
        return career == 0 ? null : announcement.career.minCareer.loe(career);
    }

    private BooleanBuilder maxCareerGoe(int career) {
        BooleanBuilder builder = new BooleanBuilder();
        if (career != 0) {
            builder.and(announcement.career.maxCareer.goe(career));
            builder.or(announcement.career.maxCareer.eq(0));
        }

        return builder;
    }

    private Predicate titleContain(String title) {
        return hasText(title) ? announcement.title.contains(title) : null;
    }
}
