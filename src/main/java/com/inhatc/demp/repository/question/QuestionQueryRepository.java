package com.inhatc.demp.repository.question;

import com.inhatc.demp.domain.Question;
import com.inhatc.demp.dto.question.QuestionList;
import com.inhatc.demp.dto.question.QuestionSearchCondition;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

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
                ,question.id,question.title,question.hits,question.recomend))
                .from(question)
                .join(question.QuestionHashtags, questionHashtag)
                .join(questionHashtag.hashtag, hashtag)
                .where(titleContains(questionSearchCondition.getTitle()),
                        contentContains(questionSearchCondition.getContent())
                        , hashtagIn(questionSearchCondition.getHashtags()))
                .orderBy(QuestionSort(questionSearchCondition.getOrderBy()))
                .distinct()
                .fetch();
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

    private OrderByNull QuestionSort(String orderBy) {
        if (hasText(orderBy)) {
            if (orderBy.equals("createdDate")) {
                return new OrderByNull(Order.DESC, question.createdDate);
            }
            if (orderBy.equals("hits")) {
                return new OrderByNull(Order.DESC, question.hits);
            }
            if (orderBy.equals("recomend")) {
                return new OrderByNull(Order.DESC, question.recomend);
            }
        }
        return OrderByNull.DEFAULT;
    }
}
