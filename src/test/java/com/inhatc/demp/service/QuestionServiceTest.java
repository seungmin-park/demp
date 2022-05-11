package com.inhatc.demp.service;

import com.inhatc.demp.domain.Answer;
import com.inhatc.demp.domain.Question;
import com.inhatc.demp.dto.answer.AnswerForm;
import com.inhatc.demp.dto.question.*;
import com.inhatc.demp.repository.AnswerRepository;
import com.inhatc.demp.repository.HashtagRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Transactional
@SpringBootTest
class QuestionServiceTest {

    @Autowired
    QuestionService questionService;
    @Autowired
    AnswerRepository answerRepository;
    @Autowired
    HashtagRepository hashtagRepository;

    @Test
    @DisplayName("saveQuestion")
    void saveQuestion() throws Exception {
        //given
        QuestionForm questionForm = new QuestionForm("제목테스트", "내용테스트", new ArrayList<>(Arrays.asList("java", "jpa")));
        //when
        questionService.join(questionForm);
        List<Question> result = questionService.findAll();
        //then
        assertThat(result.size()).isEqualTo(3);
        assertThat(result.get(2).getTitle()).isEqualTo(questionForm.getTitle());
        assertThat(result.get(2).getContent()).isEqualTo(questionForm.getContent());
    }

    @Test
    @DisplayName("findAllByHashtag")
    void findAllByHashtag() throws Exception {
        //given
        QuestionForm questionForm = new QuestionForm("제목테스트", "내용테스트", new ArrayList<>(Arrays.asList("java", "jpa")));
        //when
        questionService.join(questionForm);
        //then
        QuestionSearchCondition questionSearchCondition = new QuestionSearchCondition();
        questionSearchCondition.getHashtags().add("java");
        List<Question> result = questionService.findAllByHashtags(questionSearchCondition.getHashtags());
        assertThat(result.size()).isEqualTo(2);
        assertThat(result).extracting("title").containsExactly("접근 제어자가 헷갈려요","제목테스트");
        assertThat(result).extracting("content").containsExactly("비 전공자라 그런지 CS 부분을 잘 모르겠네요","내용테스트");
    }

    @Test
    @DisplayName("findAllHashtags")
    void findAllHashtags() throws Exception {
        //given
        QuestionForm questionForm1 = new QuestionForm("제목테스트1", "내용테스트1", new ArrayList<>(Arrays.asList("java", "jpa")));
        QuestionForm questionForm2 = new QuestionForm("제목테스트2", "내용테스트2", new ArrayList<>(Arrays.asList("spring", "jpa")));
        QuestionForm questionForm3 = new QuestionForm("제목테스트3", "내용테스트3", new ArrayList<>(Arrays.asList("html", "css")));
        //when
        questionService.join(questionForm1);
        questionService.join(questionForm2);
        questionService.join(questionForm3);

        List<String> result = questionService.findAllHashtags();
        //then
        assertThat(result.size()).isEqualTo(7);
        assertThat(result).contains("java", "jpa","cs","thread", "spring", "html", "css");
    }

    @Test
    @DisplayName("saveAnswerException")
    void saveAnswerException() throws Exception {
        //given
        AnswerForm answerForm = new AnswerForm("memberC@memberC", 3L, "댓글 테스트");
        //when

        //then
        assertThatThrownBy(() -> questionService.saveAnswer(answerForm))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    @DisplayName("saveAnswer")
    void saveAnswer() throws Exception {
        //given
        AnswerForm answerForm = new AnswerForm("memberB@memberB", 1L, "댓글 테스트");
        //when
        List<QuestionAnswer> questionAnswers = questionService.saveAnswer(answerForm);
        //then
        assertThat(questionAnswers.size()).isEqualTo(2);
        assertThat(questionAnswers).extracting("content").containsExactly("질문\\n 답변\\n 테스트","댓글 테스트");
    }

    @Test
    @DisplayName("deleteAnswer")
    void deleteAnswer() throws Exception {
        //given
        AnswerForm answerForm = new AnswerForm("memberB@memberB", 1L, "댓글 테스트");
        questionService.saveAnswer(answerForm);
        //when
        answerRepository.deleteById(7L);
        List<Answer> answers = answerRepository.findByQuestion_Id(1L);
        //then
        assertThat(answers.size()).isEqualTo(1);
        assertThat(answers).extracting("content").containsExactly("댓글 테스트");
    }

    @Test
    @DisplayName("deleteQuestion")
    void deleteQuestion() throws Exception {
        //given
        questionService.deleteQuestion(1L);
        //when
        QuestionDetail questionDetail = questionService.findById(1L);
        List<Answer> answers = answerRepository.findByQuestion_Id(1L);
        //then
        assertThat(questionDetail).isNull();
        assertThat(answers.isEmpty()).isTrue();
    }

    @Test
    @DisplayName("updateQuestion")
    void updateQuestion() throws Exception {
        //given
        QuestionUpdateForm questionUpdateForm = new QuestionUpdateForm(1L, "제목 수정 테스트용 텍스트", "내용 수정 테스트용 텍스트", new ArrayList<>(Arrays.asList("1", "2")));
        //when
        questionService.updateQuestion(questionUpdateForm);
        QuestionDetail questionDetail = questionService.findById(questionUpdateForm.getQuestionId());
        List<String> hashtags = questionService.findAllHashtags();
        //then
        assertThat(questionDetail.getTitle()).isEqualTo("제목 수정 테스트용 텍스트");
        assertThat(questionDetail.getContent()).isEqualTo("내용 수정 테스트용 텍스트");
        assertThat(hashtags.size()).isEqualTo(6);
        assertThat(hashtags).contains("java", "jpa", "cs", "thread", "1", "2");
    }

    @Test
    @DisplayName("updateQuestionFail")
    void updateQuestionFail() throws Exception {
        QuestionUpdateForm questionUpdateForm = new QuestionUpdateForm(100L, "제목 수정 테스트용 텍스트", "내용 수정 테스트용 텍스트", new ArrayList<>(Arrays.asList("1", "2")));
        assertThatThrownBy(() -> questionService.updateQuestion(questionUpdateForm))
                .isInstanceOf(NoSuchElementException.class);
    }
}