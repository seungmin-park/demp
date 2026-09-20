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
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

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
        Member member = memberRepository.save(new Member("member-a", "password", List.of("ROLE_USER")));
        Question question = new Question("질문", "내용", 0, 0, 0);
        question.settingMember(member);
        questionRepository.save(question);
        Answer answer = new Answer("질문\\n 답변\\n 테스트", 22, 11);
        answer.settingMember(member);
        answer.settingQuestion(question);
        answerRepository.save(answer);
        Question otherQuestion = new Question("다른 질문", "다른 내용", 0, 0, 0);
        otherQuestion.settingMember(member);
        questionRepository.save(otherQuestion);
        Answer otherAnswer = new Answer("다른 댓글", 0, 0);
        otherAnswer.settingMember(member);
        otherAnswer.settingQuestion(otherQuestion);
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
        Member member = memberRepository.save(new Member("answer-member", "password", List.of("ROLE_USER")));
        Question question = new Question("질문", "내용", 0, 0, 0);
        question.settingMember(member);
        questionRepository.save(question);
        Answer removed = saveAnswer(member, question, "삭제할 댓글");
        Answer retained = saveAnswer(member, question, "남길 댓글");

        answerRepository.deleteById(removed.getId());

        Assertions.assertThat(answerRepository.findByQuestion_Id(question.getId()))
                .extracting(Answer::getId).containsExactly(retained.getId());
    }

    private Answer saveAnswer(Member member, Question question, String content) {
        Answer answer = new Answer(content, 0, 0);
        answer.settingMember(member);
        answer.settingQuestion(question);
        return answerRepository.save(answer);
    }

}
