package com.inhatc.demp.repository.announcement;

import static com.inhatc.demp.domain.announcement.QAnnouncement.announcement;
import static org.springframework.util.StringUtils.hasText;

import com.inhatc.demp.domain.announcement.Announcement;
import com.inhatc.demp.domain.announcement.AnnouncementType;
import com.inhatc.demp.domain.announcement.JobPosition;
import com.inhatc.demp.domain.announcement.Language;
import com.inhatc.demp.dto.announcement.AnnouncementResponse;
import com.inhatc.demp.dto.announcement.AnnouncementSearchCondition;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.stereotype.Repository;

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
        List<Long> ids = jpaQueryFactory.select(announcement.id).from(announcement)
                .where(typeEq(announcementSearchCondition.getAnnouncementType()),
                        positionIn(announcementSearchCondition.getPositions()),
                        languageIn(announcementSearchCondition.getLanguage()),
                        paymentGoe(announcementSearchCondition.getPayment()),
                        minCareerLoe(announcementSearchCondition.getCareer()),
                        maxCareerGoe(announcementSearchCondition.getCareer()),
                        titleContain(announcementSearchCondition.getTitle()))
                .orderBy(announcement.id.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize() + 1)
                .fetch();
        boolean hasNext = ids.size() > pageable.getPageSize();
        List<Long> contentIds = ids.subList(0, Math.min(ids.size(), pageable.getPageSize()));
        List<AnnouncementResponse> content = loadWithLanguages(contentIds).stream()
                .map(AnnouncementResponse::new)
                .collect(Collectors.toList());
        return new SliceImpl<>(content, pageable, hasNext);
    }

    private List<Announcement> loadWithLanguages(List<Long> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        Map<Long, Announcement> byId = jpaQueryFactory.selectFrom(announcement)
                .leftJoin(announcement.description.languages).fetchJoin()
                .where(announcement.id.in(ids))
                .distinct().fetch().stream()
                .collect(Collectors.toMap(Announcement::getId, Function.identity()));
        return ids.stream().map(byId::get).collect(Collectors.toList());
    }

    public Page<Announcement> pagingTest(AnnouncementSearchCondition announcementSearchCondition, Pageable pageable) {
        List<Long> ids = jpaQueryFactory.select(announcement.id).from(announcement)
                .where(typeEq(announcementSearchCondition.getAnnouncementType()),
                        positionIn(announcementSearchCondition.getPositions()),
                        languageIn(announcementSearchCondition.getLanguage()),
                        paymentGoe(announcementSearchCondition.getPayment()),
                        titleContain(announcementSearchCondition.getTitle()))
                .orderBy(announcement.id.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();
        Long total = jpaQueryFactory.select(announcement.count()).from(announcement)
                .where(typeEq(announcementSearchCondition.getAnnouncementType()),
                        positionIn(announcementSearchCondition.getPositions()),
                        languageIn(announcementSearchCondition.getLanguage()),
                        paymentGoe(announcementSearchCondition.getPayment()),
                        titleContain(announcementSearchCondition.getTitle()))
                .fetchOne();
        return new PageImpl<>(loadWithLanguages(ids), pageable, total == null ? 0 : total);
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
