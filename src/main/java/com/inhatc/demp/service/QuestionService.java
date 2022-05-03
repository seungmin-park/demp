package com.inhatc.demp.service;

import com.inhatc.demp.domain.Hashtag;
import com.inhatc.demp.domain.Member;
import com.inhatc.demp.domain.Question;
import com.inhatc.demp.dto.question.QuestionForm;
import com.inhatc.demp.repository.MemberRepository;
import com.inhatc.demp.repository.QuestionQueryRepository;
import com.inhatc.demp.repository.QuestionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class QuestionService {

    private final QuestionRepository questionRepository;
    private final QuestionQueryRepository questionQueryRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public void join(QuestionForm questionForm) {
        // TODO: 2022-05-03 회원가입 기능 미구현, 임시 회원 객체 대체
        Member testMember = memberRepository.findById(1L).get();
        ArrayList<String> formHashtag = questionForm.getHashtags();
        Question question = new Question(questionForm.getTitle(), questionForm.getContent(), 0, 0, 0);
        List<Hashtag> hashtags = question.getHashtags();
        for (String hashtag : formHashtag) {
            hashtags.add(new Hashtag(hashtag));
        }
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

    public List<String> findAllHashtags() {
        return questionQueryRepository.findAllHashtags();
    }

    public Optional<Question> findById(Long id) {
        return questionRepository.findById(id);
    }
}
