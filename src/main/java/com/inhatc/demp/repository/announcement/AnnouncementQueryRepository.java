package com.inhatc.demp.repository.announcement;

import com.inhatc.demp.domain.Announcement;
import com.inhatc.demp.domain.AnnouncementType;
import com.inhatc.demp.domain.Language;
import com.inhatc.demp.dto.announcement.AnnouncementResponse;
import com.inhatc.demp.dto.announcement.AnnouncementSearchCondition;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.QueryResults;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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

    public Slice<AnnouncementResponse> getAnnounceScroll(AnnouncementSearchCondition announcementSearchCondition, Pageable pageable){
        QueryResults<Announcement> results = jpaQueryFactory.selectFrom(announcement)
                .leftJoin(announcement.languages)
                .fetchJoin()
                .where(typeEq(announcementSearchCondition.getTypeName()),
                        positionIn(announcementSearchCondition.getPositions()),
                        languageIn(announcementSearchCondition.getLanguages()),
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

    private BooleanExpression languageIn(Set<Language> languages) {
        return languages.isEmpty() ? null : announcement.languages.any().in(languages);
    }

    private BooleanExpression paymentGoe(int payment) {
        return payment <= 0 ? null : announcement.payment.goe(payment);
    }

    private BooleanExpression minCareerLoe(int career) {
        return career == 0 ? null : announcement.minCareer.loe(career);
    }

    private BooleanBuilder maxCareerGoe(int career) {
        BooleanBuilder builder = new BooleanBuilder();
        if (career != 0) {
            builder.and(announcement.maxCareer.goe(career));
            builder.or(announcement.maxCareer.eq(0));
        }

        return builder;
    }

    private Predicate titleContain(String title) {
        return hasText(title) ? announcement.title.contains(title) : null;
    }
}
