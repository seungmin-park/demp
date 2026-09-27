package com.inhatc.demp.repository;

import com.inhatc.demp.domain.Answer;
import com.inhatc.demp.domain.Member;
import com.inhatc.demp.domain.Question;
import com.inhatc.demp.repository.question.QuestionRepository;
import java.util.List;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

@DataJpaTest
class AnswerRepositoryTest {

    @Autowired
    MemberRepository memberRepository;
    @Autowired
    QuestionRepository questionRepository;
    @Autowired
    AnswerRepository answerRepository;

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
