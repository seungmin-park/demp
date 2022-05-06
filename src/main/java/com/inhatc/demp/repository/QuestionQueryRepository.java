package com.inhatc.demp.repository;

import com.inhatc.demp.domain.Question;
import com.inhatc.demp.dto.question.QuestionSearchCondition;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

import static com.inhatc.demp.domain.QHashtag.hashtag;
import static com.inhatc.demp.domain.QQuestion.question;
import static com.inhatc.demp.domain.QQuestionHashtag.*;
import static org.springframework.util.StringUtils.*;

@Repository
@RequiredArgsConstructor
public class QuestionQueryRepository {

    private final JPAQueryFactory jpaQueryFactory;

    public List<Question> findAllBySearchCondition(QuestionSearchCondition questionSearchCondition) {
        return jpaQueryFactory
                .selectFrom(question)
                .join(question.QuestionHashtags, questionHashtag)
                .join(questionHashtag.hashtag, hashtag)
                .where(titleContains(questionSearchCondition.getTitle()),
                        contentContains(questionSearchCondition.getContent())
                        , hashtagIn(questionSearchCondition.getHashtags()))
                .orderBy(QuestionSort(questionSearchCondition.getOrderBy()))
                .distinct()
                .fetch();
    }

    private OrderSpecifier<?> QuestionSort(String orderBy) {
        if (orderBy.equals("createdDate")) {
            return new OrderSpecifier(Order.DESC, question.createdDate);
        }
        if (orderBy.equals("hits")) {
            return new OrderSpecifier(Order.DESC, question.hits);
        }
        if (orderBy.equals("recomend")) {
            return new OrderSpecifier(Order.DESC, question.recomend);
        }
        return null;
    }

    public List<Question> findAllByHashtags(List<String> hashtags) {
        return jpaQueryFactory
                .selectFrom(question)
                .join(question.QuestionHashtags, questionHashtag)
                .join(questionHashtag.hashtag, hashtag)
                .where(hashtagIn(hashtags))
                .distinct()
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
        return hashtags.isEmpty() ? null : hashtag.tagName.in(hashtags);
    }
}
