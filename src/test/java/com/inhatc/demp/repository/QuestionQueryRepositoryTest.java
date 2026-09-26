package com.inhatc.demp.repository;

import com.inhatc.demp.domain.Member;
import com.inhatc.demp.domain.Hashtag;
import com.inhatc.demp.domain.Question;
import com.inhatc.demp.domain.QuestionHashtag;
import com.inhatc.demp.dto.question.QuestionList;
import com.inhatc.demp.dto.question.QuestionSearchCondition;
import com.inhatc.demp.repository.question.QuestionQueryRepository;
import com.inhatc.demp.repository.question.QuestionRepository;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.LocalDateTime;
import java.util.List;
import javax.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class QuestionQueryRepositoryTest {
    @Autowired EntityManager entityManager;
    @Autowired MemberRepository members;
    @Autowired QuestionRepository questions;
    @Autowired HashtagRepository hashtags;

    @Test
    @DisplayName("태그 필터가 없으면 태그 없는 질문도 검색 결과에 포함한다")
    void includesQuestionWithoutHashtags() {
        Member member = members.save(new Member("query-member", "hash", List.of("ROLE_USER")));
        Question question = new Question("태그 없는 질문", "내용", 0, 0, 0);
        question.settingMember(member);
        questions.save(question);

        QuestionQueryRepository repository = new QuestionQueryRepository(new JPAQueryFactory(entityManager));
        List<QuestionList> result = repository.findAllBySearchCondition(new QuestionSearchCondition());

        assertThat(result).extracting(QuestionList::getId).containsExactly(question.getId());
        assertThat(repository.findAllByHashtags(List.of())).extracting(Question::getId)
                .containsExactly(question.getId());
    }

    @Test
    @DisplayName("여러 태그는 OR로, 제목과 내용은 AND로 검색하고 질문을 중복 반환하지 않는다")
    void combinesSearchConditionsWithoutDuplicateQuestions() {
        Member member = members.save(new Member("query-member", "hash", List.of("ROLE_USER")));
        Hashtag javaTag = hashtags.save(new Hashtag("JAVA"));
        Hashtag spring = hashtags.save(new Hashtag("SPRING"));
        Hashtag css = hashtags.save(new Hashtag("CSS"));
        Question bothTags = saveQuestion(member, "Java 질문", "guide", javaTag, spring);
        Question springOnly = saveQuestion(member, "Java 실습", "guide", spring);
        saveQuestion(member, "Java CSS", "guide", css);
        Question manual = saveQuestion(member, "Java 문서", "manual", javaTag);
        QuestionSearchCondition condition = new QuestionSearchCondition();
        condition.setTitle("Java");
        condition.setContent("guide");
        condition.setHashtags(List.of("JAVA", "SPRING"));

        QuestionQueryRepository repository = new QuestionQueryRepository(new JPAQueryFactory(entityManager));
        List<QuestionList> result = repository.findAllBySearchCondition(condition);

        assertThat(result).extracting(QuestionList::getId)
                .containsExactlyInAnyOrder(bothTags.getId(), springOnly.getId());
        assertThat(repository.findAllByHashtags(List.of("JAVA", "SPRING")))
                .extracting(Question::getId)
                .containsExactlyInAnyOrder(bothTags.getId(), springOnly.getId(), manual.getId());
    }

    @ParameterizedTest
    @DisplayName("정렬값이 같은 질문도 ID 기준으로 안정되게 페이지를 나눈다")
    @ValueSource(strings = {"createdDate", "hits", "recommend"})
    void slicesTiedQuestionsById(String orderBy) {
        Member member = members.save(new Member("query-member", "hash", List.of("ROLE_USER")));
        Hashtag javaTag = hashtags.save(new Hashtag("JAVA"));
        Hashtag spring = hashtags.save(new Hashtag("SPRING"));
        LocalDateTime sameDate = LocalDateTime.of(2026, 9, 1, 12, 0);
        for (int i = 0; i < 5; i++) {
            Question question = new Question("Java " + i, "guide", 0, 0, 0);
            ReflectionTestUtils.setField(question, "createdDate", sameDate);
            question.settingMember(member);
            QuestionHashtag relation = new QuestionHashtag();
            (i % 2 == 0 ? javaTag : spring).addQuestionHashtag(relation);
            question.addQuestionHashtag(relation);
            questions.save(question);
        }
        saveQuestion(member, "unmatched", "guide", javaTag);
        List<Long> expected = questions.findAll().stream()
                .filter(question -> question.getTitle().startsWith("Java"))
                .map(Question::getId).sorted(java.util.Comparator.reverseOrder()).collect(java.util.stream.Collectors.toList());
        QuestionSearchCondition condition = new QuestionSearchCondition();
        condition.setTitle("Java");
        condition.setContent("guide");
        condition.setHashtags(List.of("JAVA", "SPRING"));
        condition.setOrderBy(orderBy);
        QuestionQueryRepository repository = new QuestionQueryRepository(new JPAQueryFactory(entityManager));

        Slice<QuestionList> first = repository.findSliceBySearchCondition(condition, PageRequest.of(0, 2));
        Slice<QuestionList> second = repository.findSliceBySearchCondition(condition, PageRequest.of(1, 2));
        Slice<QuestionList> third = repository.findSliceBySearchCondition(condition, PageRequest.of(2, 2));
        Slice<QuestionList> empty = repository.findSliceBySearchCondition(condition, PageRequest.of(3, 2));

        assertThat(first.getContent()).extracting(QuestionList::getId)
                .containsExactly(expected.get(0), expected.get(1));
        assertThat(second.getContent()).extracting(QuestionList::getId)
                .containsExactly(expected.get(2), expected.get(3));
        assertThat(third.getContent()).extracting(QuestionList::getId).containsExactly(expected.get(4));
        assertThat(first.hasNext()).isTrue();
        assertThat(second.hasNext()).isTrue();
        assertThat(third.isLast()).isTrue();
        assertThat(empty.getContent()).isEmpty();
        assertThat(empty.isLast()).isTrue();
    }

    private Question saveQuestion(Member member, String title, String content, Hashtag... tags) {
        Question question = new Question(title, content, 0, 0, 0);
        question.settingMember(member);
        for (Hashtag tag : tags) {
            QuestionHashtag relation = new QuestionHashtag();
            tag.addQuestionHashtag(relation);
            question.addQuestionHashtag(relation);
        }
        return questions.save(question);
    }
}
