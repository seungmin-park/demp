package com.inhatc.demp.repository.question;

import com.inhatc.demp.domain.Question;
import com.inhatc.demp.dto.question.QuestionList;
import com.inhatc.demp.dto.question.QuestionSearchCondition;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;

import java.util.List;

import static com.inhatc.demp.domain.QHashtag.hashtag;
import static com.inhatc.demp.domain.QQuestion.question;
import static com.inhatc.demp.domain.QQuestionHashtag.questionHashtag;
import static org.springframework.util.StringUtils.hasText;

@Repository
@RequiredArgsConstructor
public class QuestionQueryRepository {

    private final JPAQueryFactory jpaQueryFactory;

    public List<QuestionList> findAllBySearchCondition(QuestionSearchCondition questionSearchCondition) {
        return jpaQueryFactory
                .select(Projections.fields(QuestionList.class
                ,question.id,question.title,question.hits,question.recommend))
                .from(question)
                .where(titleContains(questionSearchCondition.getTitle()),
                        contentContains(questionSearchCondition.getContent()),
                        hashtagIn(questionSearchCondition.getHashtags()))
                .orderBy(primaryOrder(questionSearchCondition.getOrderBy()), question.id.desc())
                .fetch();
    }

    public Slice<QuestionList> findSliceBySearchCondition(QuestionSearchCondition condition, Pageable pageable) {
        List<QuestionList> rows = jpaQueryFactory
                .select(Projections.fields(QuestionList.class,
                        question.id, question.title, question.hits, question.recommend))
                .from(question)
                .where(titleContains(condition.getTitle()),
                        contentContains(condition.getContent()), hashtagIn(condition.getHashtags()))
                .orderBy(primaryOrder(condition.getOrderBy()), question.id.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize() + 1)
                .fetch();
        boolean hasNext = rows.size() > pageable.getPageSize();
        List<QuestionList> content = hasNext ? rows.subList(0, pageable.getPageSize()) : rows;
        return new SliceImpl<>(content, pageable, hasNext);
    }

    public List<Question> findAllByHashtags(List<String> hashtags) {
        return jpaQueryFactory
                .selectFrom(question)
                .where(hashtagIn(hashtags))
                .fetch();
    }

    public List<String> findAllHashtags() {
        return jpaQueryFactory
                .select(hashtag.tagName)
                .from(hashtag)
                .distinct()
                .fetch();
    }

    private BooleanExpression titleContains(String title) {
        return hasText(title) ? question.title.contains(title) : null;
    }

    private BooleanExpression contentContains(String content) {
        return hasText(content) ? question.content.contains(content) : null;
    }

    private BooleanExpression hashtagIn(List<String> hashtags) {
        return hashtags == null || hashtags.isEmpty() ? null : JPAExpressions.selectOne()
                .from(questionHashtag)
                .join(questionHashtag.hashtag, hashtag)
                .where(questionHashtag.question.eq(question), hashtag.tagName.in(hashtags))
                .exists();
    }

    private OrderSpecifier<?> primaryOrder(String orderBy) {
        if (hasText(orderBy)) {
            if (orderBy.equals("createdDate")) {
                return question.createdDate.desc();
            }
            if (orderBy.equals("hits")) {
                return question.hits.desc();
            }
            if (orderBy.equals("recommend")) {
                return question.recommend.desc();
            }
        }
        return question.createdDate.desc();
    }
}
