package com.inhatc.demp.repository;

import com.inhatc.demp.domain.Answer;
import com.inhatc.demp.domain.Member;
import com.inhatc.demp.domain.Question;
import com.inhatc.demp.dto.question.QuestionAnswer;
import org.springframework.data.domain.PageRequest;
import com.inhatc.demp.repository.question.QuestionRepository;
import java.util.List;
import java.util.stream.IntStream;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;

@DataJpaTest
class AnswerRepositoryTest {

    @Autowired
    MemberRepository memberRepository;
    @Autowired
    QuestionRepository questionRepository;
    @Autowired
    AnswerRepository answerRepository;
    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("새 답변 테이블은 질문과 답변 ID 순서의 커서 인덱스를 생성한다")
    void createsQuestionAnswerCursorIndex() {
        List<String> columns = jdbcTemplate.queryForList("""
                SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.INDEX_COLUMNS
                WHERE TABLE_SCHEMA = 'PUBLIC' AND TABLE_NAME = 'ANSWER'
                  AND INDEX_NAME = 'IDX_ANSWER_QUESTION_CURSOR'
                ORDER BY ORDINAL_POSITION
                """, String.class);

        Assertions.assertThat(columns).containsExactly("QUESTION_ID", "ANSWER_ID");
    }

    @Test
    @DisplayName("질문에 연결된 답변을 조회한다")
    void findsOnlyAnswersForRequestedQuestion() {
        //given
        Member member = memberRepository.save(Member.builder()
                .username("member-a")
                .password("password")
                .roles(List.of("ROLE_USER"))
                .build());
        Question question = Question.builder().title("질문").content("내용").hits(0).recommend(0).dislike(0).build();
        question.assignMember(member);
        questionRepository.save(question);
        Answer answer = Answer.builder().content("질문\\n 답변\\n 테스트").recommend(22).dislike(11).build();
        answer.assignMember(member);
        answer.assignQuestion(question);
        answerRepository.save(answer);
        Question otherQuestion = Question.builder().title("다른 질문").content("다른 내용").hits(0).recommend(0).dislike(0).build();
        otherQuestion.assignMember(member);
        questionRepository.save(otherQuestion);
        Answer otherAnswer = Answer.builder().content("다른 댓글").recommend(0).dislike(0).build();
        otherAnswer.assignMember(member);
        otherAnswer.assignQuestion(otherQuestion);
        answerRepository.save(otherAnswer);
        //when
        List<Answer> answers = answerRepository.findByQuestion_Id(question.getId());

        //then
        Assertions.assertThat(answers).extracting(Answer::getId).containsExactly(answer.getId());
        Assertions.assertThat(answers.get(0).getContent()).isEqualTo("질문\\n 답변\\n 테스트");
        Assertions.assertThat(answers.get(0).getRecommend()).isEqualTo(22);
        Assertions.assertThat(answers.get(0).getDislike()).isEqualTo(11);
    }
    @Test
    @DisplayName("존재하지 않는 질문의 답변 목록은 비어 있다")
    void returnsEmptyForMissingQuestion() {
        Assertions.assertThat(answerRepository.findByQuestion_Id(-1L)).isEmpty();
    }

    @Test
    @DisplayName("답변 조회 창은 DB에서 21행으로 제한하고 최신 ID부터 읽는다")
    void readsBoundedAnswerWindow() {
        Member member = memberRepository.save(Member.builder().username("window-reader").password("unused").roles(List.of("ROLE_USER")).build());
        Question question = Question.builder().title("질문").content("본문").build();
        question.assignMember(member);
        questionRepository.save(question);
        List<Answer> saved = answerRepository.saveAll(IntStream.range(0, 22)
                .mapToObj(i -> Answer.createFor(member, question, "답변-" + i)).toList());

        var window = answerRepository.findAnswerWindow(question.getId(), null, PageRequest.of(0, 21));

        Assertions.assertThat(window).hasSize(21);
        Assertions.assertThat(window).extracting(QuestionAnswer::getAnswerId)
                .containsExactlyElementsOf(saved.reversed().subList(0, 21).stream().map(Answer::getId).toList());
        Assertions.assertThat(window.getFirst().getUsername()).isEqualTo("window-reader");
        Assertions.assertThat(window.getFirst().getContent()).isEqualTo("답변-21");
    }

    @Test
    @DisplayName("선택한 답변만 삭제하고 같은 질문의 다른 답변은 유지한다")
    void deletesOnlySelectedAnswer() {
        Member member = memberRepository.save(Member.builder()
                .username("answer-member")
                .password("password")
                .roles(List.of("ROLE_USER"))
                .build());
        Question question = Question.builder().title("질문").content("내용").hits(0).recommend(0).dislike(0).build();
        question.assignMember(member);
        questionRepository.save(question);
        Answer removed = saveAnswer(member, question, "삭제할 댓글");
        Answer retained = saveAnswer(member, question, "남길 댓글");

        answerRepository.deleteById(removed.getId());

        Assertions.assertThat(answerRepository.findByQuestion_Id(question.getId()))
                .extracting(Answer::getId).containsExactly(retained.getId());
    }

    private Answer saveAnswer(Member member, Question question, String content) {
        Answer answer = Answer.builder().content(content).recommend(0).dislike(0).build();
        answer.assignMember(member);
        answer.assignQuestion(question);
        return answerRepository.save(answer);
    }

}
