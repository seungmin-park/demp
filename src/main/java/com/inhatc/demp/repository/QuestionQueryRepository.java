package com.inhatc.demp.repository;

import com.inhatc.demp.domain.Question;
import com.inhatc.demp.dto.question.QuestionSearchCondition;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.List;

import static com.inhatc.demp.domain.QHashtag.hashtag;
import static com.inhatc.demp.domain.QQuestion.question;
import static org.springframework.util.StringUtils.*;

@Repository
@RequiredArgsConstructor
public class QuestionQueryRepository {

    private final JPAQueryFactory jpaQueryFactory;

    public List<Question> findAllBySearchCondition(QuestionSearchCondition questionSearchCondition) {
        return jpaQueryFactory
                .selectFrom(question)
                .join(question.hashtags, hashtag)
                .fetchJoin()
                .where(titleContains(questionSearchCondition.getTitle()),
                        contentContains(questionSearchCondition.getContent())
                        , hashtagIn(questionSearchCondition.getHashtags()))
                .distinct()
                .fetch();
    }

    public List<Question> findAllByHashtags(List<String> hashtags) {
        return jpaQueryFactory
                .selectFrom(question)
                .join(question.hashtags, hashtag)
                .fetchJoin()
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
