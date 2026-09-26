package com.inhatc.demp.service;

import com.inhatc.demp.domain.Answer;
import com.inhatc.demp.domain.Hashtag;
import com.inhatc.demp.domain.Member;
import com.inhatc.demp.domain.Question;
import com.inhatc.demp.dto.answer.AnswerForm;
import com.inhatc.demp.dto.question.QuestionAnswer;
import com.inhatc.demp.dto.question.QuestionDetail;
import com.inhatc.demp.dto.question.QuestionForm;
import com.inhatc.demp.dto.question.QuestionUpdateForm;
import com.inhatc.demp.repository.AnswerRepository;
import com.inhatc.demp.repository.HashtagRepository;
import com.inhatc.demp.repository.MemberRepository;
import com.inhatc.demp.repository.question.QuestionRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
class QuestionServiceTest {

    @Autowired
    private QuestionService questionService;
    @Autowired
    private AnswerService answerService;
    @Autowired
    private AnswerRepository answerRepository;
    @Autowired
    private QuestionRepository questionRepository;
    @Autowired
    private HashtagRepository hashtagRepository;
    @Autowired
    private MemberRepository memberRepository;

    @AfterEach
    void cleanUp() {
        answerRepository.deleteAllInBatch();
        questionRepository.deleteAll();
        hashtagRepository.deleteAllInBatch();
        memberRepository.deleteAll();
    }

    @Test
    @DisplayName("질문과 해시태그를 저장한 결과를 다시 조회한다")
    void saveQuestion() {
        Member member = memberRepository.save(new Member("question-service-member", "password", List.of("ROLE_USER")));
        QuestionForm form = new QuestionForm("제목테스트", "내용테스트", "question-service-member", new ArrayList<>(List.of("test-java", "test-jpa")));

        questionService.join(member.getId(), form);

        List<Question> result = questionRepository.findAll().stream()
                .filter(question -> question.getTitle().equals("제목테스트"))
                .collect(Collectors.toList());
        assertThat(result).hasSize(1);
        QuestionDetail saved = questionService.findById(result.get(0).getId());
        assertThat(saved.getTitle()).isEqualTo("제목테스트");
        assertThat(saved.getContent()).isEqualTo("내용테스트");
        assertThat(saved.getUsername()).isEqualTo("question-service-member");
        assertThat(saved.getHashtags()).containsExactlyInAnyOrder("test-java", "test-jpa");
    }

    @Test
    @DisplayName("선택한 해시태그에 해당하는 질문만 조회한다")
    void findAllByHashtag() {
        Member member = memberRepository.save(new Member("question-service-member", "password", List.of("ROLE_USER")));
        saveQuestion(member, "Java 질문", "Java 내용", "test-java", "test-jpa");
        saveQuestion(member, "두 번째 Java 질문", "두 번째 Java 내용", "test-java");
        saveQuestion(member, "CSS 질문", "CSS 내용", "test-css");

        List<Question> result = questionService.findAllByHashtags(List.of("test-java"));

        assertThat(result).extracting(Question::getTitle, Question::getContent)
                .containsExactlyInAnyOrder(
                        tuple("Java 질문", "Java 내용"),
                        tuple("두 번째 Java 질문", "두 번째 Java 내용"));
    }

    @Test
    @DisplayName("저장한 해시태그를 중복 없이 조회한다")
    void findAllHashtags() {
        Member member = memberRepository.save(new Member("question-service-member", "password", List.of("ROLE_USER")));
        saveQuestion(member, "질문1", "내용1", "test-java", "test-jpa");
        saveQuestion(member, "질문2", "내용2", "test-spring", "test-jpa");
        saveQuestion(member, "질문3", "내용3", "test-html", "test-css");

        assertThat(questionService.findAllHashtags()).containsExactlyInAnyOrder("test-java", "test-jpa", "test-spring", "test-html", "test-css");
    }

    @Test
    @DisplayName("같은 이름의 태그를 두 질문에 등록해도 태그는 하나만 저장한다")
    void reusesHashtagByName() {
        Member member = memberRepository.save(new Member("question-service-member", "hash", List.of("ROLE_USER")));
        questionService.join(member.getId(), new QuestionForm("첫 질문", "내용", member.getUsername(),
                new ArrayList<>(List.of("JAVA"))));
        questionService.join(member.getId(), new QuestionForm("둘째 질문", "내용", member.getUsername(),
                new ArrayList<>(List.of("JAVA"))));

        assertThat(hashtagRepository.findAll()).extracting(Hashtag::getTagName).containsExactly("JAVA");
        assertThat(questionRepository.findAll()).hasSize(2);
    }

    @Test
    @DisplayName("한 질문의 태그를 교체해도 다른 질문의 같은 이름 태그 연결은 유지된다")
    void replacingOneQuestionsTagsKeepsOtherQuestionsRelation() {
        Member member = memberRepository.save(new Member("question-service-member", "hash", List.of("ROLE_USER")));
        questionService.join(member.getId(), new QuestionForm("첫 질문", "내용", member.getUsername(),
                new ArrayList<>(List.of("JAVA"))));
        questionService.join(member.getId(), new QuestionForm("둘째 질문", "내용", member.getUsername(),
                new ArrayList<>(List.of("JAVA"))));
        List<Question> saved = questionRepository.findAll();

        questionService.updateQuestion(member.getId(), new QuestionUpdateForm(saved.get(0).getId(),
                "첫 질문", "내용", new ArrayList<>(List.of("SPRING"))));

        assertThat(questionService.findById(saved.get(0).getId()).getHashtags()).containsExactly("SPRING");
        assertThat(questionService.findById(saved.get(1).getId()).getHashtags()).containsExactly("JAVA");
        assertThat(hashtagRepository.findAll()).extracting(Hashtag::getTagName)
                .containsExactlyInAnyOrder("JAVA", "SPRING");
    }

    @Test
    @DisplayName("태그는 공백과 중복을 제거하고 대소문자 차이는 보존한다")
    void normalizesTagNamesWithoutChangingCase() {
        Member member = memberRepository.save(new Member("question-service-member", "hash", List.of("ROLE_USER")));
        questionService.join(member.getId(), new QuestionForm("질문", "내용", member.getUsername(),
                new ArrayList<>(List.of(" JAVA ", "JAVA", " ", "java"))));
        Question saved = questionRepository.findAll().get(0);

        assertThat(questionService.findById(saved.getId()).getHashtags()).containsExactlyInAnyOrder("JAVA", "java");
        assertThat(hashtagRepository.findAll()).extracting(Hashtag::getTagName)
                .containsExactlyInAnyOrder("JAVA", "java");
    }

    @Test
    @DisplayName("동일한 태그로 질문을 수정해도 관계와 태그는 중복되지 않는다")
    void updatingWithSameTagKeepsSingleRelation() {
        Member member = memberRepository.save(new Member("question-service-member", "hash", List.of("ROLE_USER")));
        questionService.join(member.getId(), new QuestionForm("질문", "내용", member.getUsername(),
                new ArrayList<>(List.of("JAVA"))));
        Question question = questionRepository.findAll().get(0);

        questionService.updateQuestion(member.getId(), new QuestionUpdateForm(question.getId(), "질문", "내용",
                new ArrayList<>(List.of("JAVA"))));

        assertThat(questionService.findById(question.getId()).getHashtags()).containsExactly("JAVA");
        assertThat(hashtagRepository.findAll()).extracting(Hashtag::getTagName).containsExactly("JAVA");
    }

    @Test
    @DisplayName("회원이 없으면 답변을 저장하지 않고 기존 답변을 유지한다")
    void saveAnswerException() {
        List<Long> existingIds = answerRepository.findAll().stream().map(Answer::getId).collect(Collectors.toList());
        Member member = memberRepository.save(new Member("question-service-member", "password", List.of("ROLE_USER")));
        Question question = saveQuestion(member, "질문", "내용");
        AnswerForm form = new AnswerForm("missing-member", question.getId(), "댓글 테스트");

        assertThatThrownBy(() -> answerService.save(-1L, form)).isInstanceOf(com.inhatc.demp.error.ResourceNotFoundException.class);
        assertThat(answerRepository.findAll()).extracting(Answer::getId).containsExactlyInAnyOrderElementsOf(existingIds);
    }

    @Test
    @DisplayName("답변을 저장하고 해당 질문의 답변 목록을 반환한다")
    void saveAnswer() {
        Member member = memberRepository.save(new Member("question-service-member", "password", List.of("ROLE_USER")));
        Question question = new Question("질문", "내용", 0, 0, 0);
        question.assignMember(member);
        questionRepository.save(question);
        Answer existingAnswer = new Answer("기존 댓글", 2, 1);
        existingAnswer.assignMember(member);
        existingAnswer.assignQuestion(question);
        answerRepository.save(existingAnswer);

        List<QuestionAnswer> result = answerService.save(member.getId(), new AnswerForm("question-service-member", question.getId(), "댓글 테스트"));

        assertThat(result).extracting(QuestionAnswer::getContent, QuestionAnswer::getUsername)
                .containsExactlyInAnyOrder(tuple("기존 댓글", "question-service-member"), tuple("댓글 테스트", "question-service-member"));
        assertThat(answerRepository.findByQuestion_Id(question.getId())).extracting(Answer::getContent)
                .containsExactlyInAnyOrder("기존 댓글", "댓글 테스트");
    }

    @Test
    @DisplayName("질문 삭제 시 연결된 답변도 삭제한다")
    void deleteQuestion() {
        Member member = memberRepository.save(new Member("question-service-member", "password", List.of("ROLE_USER")));
        Question question = saveQuestion(member, "질문", "내용", "test-java");
        Long questionId = question.getId();
        answerService.save(member.getId(), new AnswerForm(member.getUsername(), questionId, "댓글"));

        questionService.deleteQuestion(member.getId(), questionId);

        assertThatThrownBy(() -> questionService.findById(questionId)).isInstanceOf(com.inhatc.demp.error.ResourceNotFoundException.class);
        assertThat(questionRepository.findById(questionId)).isEmpty();
        assertThat(answerRepository.findByQuestion_Id(questionId)).isEmpty();
    }

    @Test
    @DisplayName("질문 제목과 내용을 수정하고 기존 해시태그를 교체한다")
    void updateQuestion() {
        Member member = memberRepository.save(new Member("question-service-member", "password", List.of("ROLE_USER")));
        Question question = saveQuestion(member, "원래 제목", "원래 내용", "test-java");
        Long questionId = question.getId();

        questionService.updateQuestion(member.getId(), new QuestionUpdateForm(questionId, "수정 제목", "수정 내용", new ArrayList<>(List.of("test-jpa"))));

        QuestionDetail saved = questionService.findById(questionId);
        assertThat(saved.getTitle()).isEqualTo("수정 제목");
        assertThat(saved.getContent()).isEqualTo("수정 내용");
        assertThat(saved.getHashtags()).containsExactly("test-jpa");
        assertThat(questionService.findAllHashtags()).containsExactlyInAnyOrder("test-java", "test-jpa");
    }

    @Test
    @DisplayName("없는 질문을 수정하면 예외가 발생하고 기존 질문은 유지된다")
    void updateQuestionFail() {
        List<Long> existingIds = questionRepository.findAll().stream().map(Question::getId).collect(Collectors.toList());
        QuestionUpdateForm form = new QuestionUpdateForm(999L, "수정 제목", "수정 내용", new ArrayList<>(List.of("test-jpa")));

        assertThatThrownBy(() -> questionService.updateQuestion(-1L, form)).isInstanceOf(com.inhatc.demp.error.ResourceNotFoundException.class);
        assertThat(questionRepository.findAll()).extracting(Question::getId).containsExactlyInAnyOrderElementsOf(existingIds);
    }
    @Test
    @DisplayName("질문이 없으면 답변을 저장하지 않고 기존 답변을 유지한다")
    void rejectsAnswerForMissingQuestion() {
        memberRepository.save(new Member("question-service-member", "password", List.of("ROLE_USER")));
        List<Long> existingIds = answerRepository.findAll().stream().map(Answer::getId).collect(Collectors.toList());
        AnswerForm request = new AnswerForm("question-service-member", -1L, "댓글");

        assertThatThrownBy(() -> answerService.save(memberRepository.findByUsername("question-service-member").orElseThrow().getId(), request)).isInstanceOf(com.inhatc.demp.error.ResourceNotFoundException.class);

        assertThat(answerRepository.findAll()).extracting(Answer::getId)
                .containsExactlyInAnyOrderElementsOf(existingIds);
    }


    @Test
    @DisplayName("질문 등록과 수정은 HTML을 정제하고 별도 조회에 반영한다")
    void persistsSanitizedContent() {
        Member member = memberRepository.save(new Member("question-service-member", "hash", List.of("ROLE_USER")));
        questionService.join(member.getId(), new QuestionForm("safe-question", "<b>safe</b><script>bad()</script>", "forged", new ArrayList<>()));
        Question saved = questionRepository.findAll().get(0);
        assertThat(saved.getContent()).isEqualTo("<b>safe</b>");
        assertThat(questionService.findById(saved.getId()).getContent()).isEqualTo("<b>safe</b>");
        questionService.updateQuestion(member.getId(), new QuestionUpdateForm(saved.getId(), "safe-question", "<p onclick='bad()'>changed</p>", new ArrayList<>()));
        assertThat(questionRepository.findById(saved.getId()).orElseThrow().getContent()).isEqualTo("<p>changed</p>");
        assertThat(questionService.findById(saved.getId()).getContent()).isEqualTo("<p>changed</p>");
    }

    private Question saveQuestion(Member member, String title, String content, String... tags) {
        questionService.join(member.getId(), new QuestionForm(title, content, member.getUsername(),
                new ArrayList<>(List.of(tags))));
        return questionRepository.findAll().stream()
                .filter(question -> question.getTitle().equals(title))
                .findFirst().orElseThrow();
    }

}
