package com.inhatc.demp.repository.announcement;

import static com.inhatc.demp.domain.announcement.QAnnouncement.announcement;
import static org.springframework.util.StringUtils.hasText;

import com.inhatc.demp.domain.announcement.Announcement;
import com.inhatc.demp.domain.announcement.AnnouncementType;
import com.inhatc.demp.domain.announcement.JobPosition;
import com.inhatc.demp.domain.announcement.Language;
import com.inhatc.demp.dto.announcement.AnnouncementSearchCondition;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import java.time.Clock;
import java.time.LocalDateTime;
import com.inhatc.demp.config.TimeConfiguration;
import com.inhatc.demp.dto.announcement.RecruitmentStatus;
import com.inhatc.demp.dto.announcement.Tuition;
import org.springframework.context.annotation.Import;
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
@Import(TimeConfiguration.class)
@RequiredArgsConstructor
public class AnnouncementQueryRepository {

    private final JPAQueryFactory jpaQueryFactory;
    private final Clock recruitmentClock;

    public List<Announcement> findAllByAnnouncementCondition(AnnouncementSearchCondition announcementSearchCondition) {
        return jpaQueryFactory
                .selectFrom(announcement)
                .where(announcement.publicationStatus.eq(com.inhatc.demp.domain.announcement.PublicationStatus.PUBLISHED),
                        typeEq(announcementSearchCondition.getAnnouncementType()),
                        positionIn(announcementSearchCondition.getPositions()),
                        languageIn(announcementSearchCondition.getLanguage()),
                        languagesIn(announcementSearchCondition.getLanguages()),
                        recruitmentMatches(announcementSearchCondition.getRecruitmentStatus()),
                        tuitionMatches(announcementSearchCondition.getTuition()),
                        educationMatches(announcementSearchCondition),
                        paymentGoe(announcementSearchCondition.getPayment()),
                        titleContain(announcementSearchCondition.getTitle()))
                .fetch();
    }

    public Slice<Announcement> findAnnouncementSlice(AnnouncementSearchCondition condition, Pageable pageable) {
        return findSlice(condition, pageable, true);
    }

    public Slice<Announcement> findAdminAnnouncementSlice(AnnouncementSearchCondition condition, Pageable pageable) {
        return findSlice(condition, pageable, false);
    }

    private Slice<Announcement> findSlice(AnnouncementSearchCondition announcementSearchCondition, Pageable pageable, boolean publicOnly) {
        List<Long> ids = jpaQueryFactory.select(announcement.id).from(announcement)
                .where(publicOnly ? announcement.publicationStatus.eq(com.inhatc.demp.domain.announcement.PublicationStatus.PUBLISHED) : null,
                        typeEq(announcementSearchCondition.getAnnouncementType()),
                        positionIn(announcementSearchCondition.getPositions()),
                        languageIn(announcementSearchCondition.getLanguage()),
                        languagesIn(announcementSearchCondition.getLanguages()),
                        recruitmentMatches(announcementSearchCondition.getRecruitmentStatus()),
                        tuitionMatches(announcementSearchCondition.getTuition()),
                        educationMatches(announcementSearchCondition),
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
        List<Announcement> content = loadWithLanguages(contentIds);
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

    public Page<Announcement> findAnnouncementPage(AnnouncementSearchCondition announcementSearchCondition, Pageable pageable) {
        List<Long> ids = jpaQueryFactory.select(announcement.id).from(announcement)
                .where(announcement.publicationStatus.eq(com.inhatc.demp.domain.announcement.PublicationStatus.PUBLISHED),
                        typeEq(announcementSearchCondition.getAnnouncementType()),
                        positionIn(announcementSearchCondition.getPositions()),
                        languageIn(announcementSearchCondition.getLanguage()),
                        languagesIn(announcementSearchCondition.getLanguages()),
                        recruitmentMatches(announcementSearchCondition.getRecruitmentStatus()),
                        tuitionMatches(announcementSearchCondition.getTuition()),
                        educationMatches(announcementSearchCondition),
                        paymentGoe(announcementSearchCondition.getPayment()),
                        titleContain(announcementSearchCondition.getTitle()))
                .orderBy(announcement.id.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();
        Long total = jpaQueryFactory.select(announcement.count()).from(announcement)
                .where(announcement.publicationStatus.eq(com.inhatc.demp.domain.announcement.PublicationStatus.PUBLISHED),
                        typeEq(announcementSearchCondition.getAnnouncementType()),
                        positionIn(announcementSearchCondition.getPositions()),
                        languageIn(announcementSearchCondition.getLanguage()),
                        languagesIn(announcementSearchCondition.getLanguages()),
                        recruitmentMatches(announcementSearchCondition.getRecruitmentStatus()),
                        tuitionMatches(announcementSearchCondition.getTuition()),
                        educationMatches(announcementSearchCondition),
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

    private BooleanExpression languagesIn(List<Language> languages) {
        return languages.stream().map(language -> announcement.description.languages.contains(language))
                .reduce(BooleanExpression::or).orElse(null);
    }

    private BooleanExpression recruitmentMatches(RecruitmentStatus status) {
        if (status == null) return null;
        LocalDateTime now = LocalDateTime.now(recruitmentClock);
        return switch (status) {
            case OPEN -> announcement.recruitmentClosed.isFalse().and(announcement.recruitPeriod.startedDate.loe(now))
                    .and(announcement.recruitPeriod.deadLineDate.goe(now));
            case UPCOMING -> announcement.recruitmentClosed.isFalse().and(announcement.recruitPeriod.startedDate.gt(now));
            case CLOSED -> announcement.recruitmentClosed.isTrue().or(announcement.recruitPeriod.deadLineDate.lt(now));
        };
    }

    private BooleanBuilder educationMatches(AnnouncementSearchCondition filter) {
        BooleanBuilder conditions = new BooleanBuilder();
        if (filter.getDeliveryMode() != null) conditions.and(announcement.education.deliveryMode.eq(filter.getDeliveryMode()));
        if (filter.getRegion() != null) conditions.and(announcement.education.region.eq(filter.getRegion()));
        if (filter.getCommitment() != null) conditions.and(announcement.education.commitment.eq(filter.getCommitment()));
        if (filter.getFundingType() != null) conditions.and(announcement.education.fundingType.eq(filter.getFundingType()));
        if (filter.getSelectionProcess() != null) conditions.and(announcement.education.selectionProcess.eq(filter.getSelectionProcess()));
        if (filter.getLearningLevel() != null) conditions.and(announcement.education.learningLevel.eq(filter.getLearningLevel()));
        if (filter.getStartAfter() != null) conditions.and(announcement.education.learningStartDate.goe(filter.getStartAfter()));
        if (filter.getStartBefore() != null) conditions.and(announcement.education.learningStartDate.loe(filter.getStartBefore()));
        if (filter.getDuration() != null) conditions.and(switch (filter.getDuration()) {
            case SHORT -> announcement.education.durationDays.between(1, 30);
            case MEDIUM -> announcement.education.durationDays.between(31, 90);
            case LONG -> announcement.education.durationDays.between(91, 180);
            case EXTENDED -> announcement.education.durationDays.goe(181);
        });
        if (conditions.hasValue()) conditions.and(announcement.announcementType.eq(AnnouncementType.EDU));
        return conditions;
    }

    private BooleanExpression tuitionMatches(Tuition tuition) {
        if (tuition == null) return null;
        BooleanExpression amount = tuition == Tuition.FREE
                ? announcement.description.payment.eq(0) : announcement.description.payment.gt(0);
        return announcement.announcementType.eq(AnnouncementType.EDU).and(amount);
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
        return hasText(title) ? announcement.title.containsIgnoreCase(title).or(announcement.company.name.containsIgnoreCase(title)) : null;
    }
}
