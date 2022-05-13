package com.inhatc.demp.service;

import com.inhatc.demp.domain.*;
import com.inhatc.demp.dto.answer.AnswerForm;
import com.inhatc.demp.dto.question.*;
import com.inhatc.demp.repository.AnswerRepository;
import com.inhatc.demp.repository.HashtagRepository;
import com.inhatc.demp.repository.MemberRepository;
import com.inhatc.demp.repository.question.QuestionQueryRepository;
import com.inhatc.demp.repository.question.QuestionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class QuestionService {

    private final QuestionRepository questionRepository;
    private final QuestionQueryRepository questionQueryRepository;
    private final MemberRepository memberRepository;
    private final HashtagRepository hashtagRepository;
    private final AnswerRepository answerRepository;
    @Transactional
    public void join(QuestionForm questionForm) {
        // TODO: 2022-05-03 회원가입 기능 미구현, 임시 회원 객체 대체
        Member testMember = memberRepository.findById(1L).get();
        Question question = new Question(questionForm.getTitle(), questionForm.getContent(), 0, 0, 0);
        ArrayList<String> hashtags = questionForm.getHashtags();

        setHashtags(question, hashtags);
        question.settingMember(testMember);
        questionRepository.save(question);
    }



    public List<Question> findAllOrderBy(Sort sort) {
        return questionRepository.findAll(sort);
    }

    public List<Question> findAll() {
        return questionRepository.findAll();
    }

    public List<Question> findAllByHashtags(List<String> hashtags) {
        return questionQueryRepository.findAllByHashtags(hashtags);
    }

    public List<QuestionList> findAllBySearchCondition(QuestionSearchCondition questionSearchCondition) {
        return questionQueryRepository.findAllBySearchCondition(questionSearchCondition);
    }

    public List<String> findAllHashtags() {
        return questionQueryRepository.findAllHashtags();
    }

    public QuestionDetail findById(Long id) {
        return questionRepository.findById(id)
                .map(q->new QuestionDetail(q))
                .orElseThrow(()->new NoSuchElementException("회원 또는 질문 데이터 존재x"));
    }

    @Transactional
    public void updateQuestion(QuestionUpdateForm questionUpdateForm) {
        Question question = questionRepository.findById(questionUpdateForm.getQuestionId()).orElseThrow(() -> new NoSuchElementException("데이터 존재X"));
        question.updateQuestion(questionUpdateForm.getTitle(), questionUpdateForm.getContent());
        setHashtags(question,questionUpdateForm.getHashtags());
    }

    @Transactional
    public void deleteQuestion(Long id) {
        questionRepository.deleteById(id);
    }

    @Transactional
    public List<QuestionAnswer> saveAnswer(AnswerForm answerForm){
        Member member = memberRepository.findByEmail(answerForm.getMemberEmail()).orElseThrow(()->new NoSuchElementException("회원 또는 질문 데이터 존재x"));
        Question question = questionRepository.findById(answerForm.getQuestionId()).orElseThrow(() -> new NoSuchElementException("회원 또는 질문 데이터 존재x"));
        Answer answer = new Answer(answerForm.getAnswerContent(), 0, 0);
        answer.settingQuestion(question);
        answer.settingMember(member);
        answerRepository.save(answer);

        return convertQuestionAnswer(answerForm);
    }

    private void setHashtags(Question question, ArrayList<String> hashtags) {
        List<Hashtag> hashtagList = hashtagRepository.findAll();
        for (String formHashtag : hashtags) {
            Hashtag hashtag = new Hashtag(formHashtag);
            if (!hashtagList.contains(hashtag)) {
                hashtagRepository.save(hashtag);
            }
            settingQuestionHashtag(question, hashtag);
        }
    }

    private void settingQuestionHashtag(Question question, Hashtag hashtag) {
        QuestionHashtag questionHashtag = new QuestionHashtag();
        hashtag.addQuestionHashtag(questionHashtag);
        question.addQuestionHashtag(questionHashtag);
    }

    private List<QuestionAnswer> convertQuestionAnswer(AnswerForm answerForm) {
        return answerRepository.findByQuestion_Id(answerForm.getQuestionId())
                .stream().map(QuestionAnswer::new)
                .collect(Collectors.toList());
    }
}
