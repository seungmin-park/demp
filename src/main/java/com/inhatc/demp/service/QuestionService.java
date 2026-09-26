package com.inhatc.demp.service;

import com.inhatc.demp.domain.*;
import com.inhatc.demp.error.ResourceNotFoundException;
import org.springframework.security.access.AccessDeniedException;
import com.inhatc.demp.dto.question.*;
import com.inhatc.demp.repository.HashtagRepository;
import com.inhatc.demp.repository.MemberRepository;
import com.inhatc.demp.repository.question.QuestionQueryRepository;
import com.inhatc.demp.repository.question.QuestionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class QuestionService {

    private final QuestionRepository questionRepository;
    private final QuestionQueryRepository questionQueryRepository;
    private final MemberRepository memberRepository;
    private final HashtagRepository hashtagRepository;
    private final ContentSanitizer contentSanitizer;
    @Transactional
    public void join(Long actorId, QuestionForm questionForm) {
        Member author = memberRepository.findById(actorId).orElseThrow(ResourceNotFoundException::new);
        Question question = new Question(questionForm.getTitle(), contentSanitizer.sanitize(questionForm.getContent()), 0, 0, 0);
        ArrayList<String> hashtags = questionForm.getHashtags();

        question.replaceHashtags(resolveHashtags(hashtags));
        question.settingMember(author);
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

    public Slice<QuestionList> findSliceBySearchCondition(QuestionSearchCondition condition, Pageable pageable) {
        return questionQueryRepository.findSliceBySearchCondition(condition, pageable);
    }

    public List<String> findAllHashtags() {
        return questionQueryRepository.findAllHashtags();
    }

    public QuestionDetail findById(Long id) {
        return questionRepository.findById(id)
                .map(QuestionDetail::new)
                .orElseThrow(ResourceNotFoundException::new);
    }

    @Transactional
    public void updateQuestion(Long actorId, QuestionUpdateForm questionUpdateForm) {
        Question question = questionRepository.findById(questionUpdateForm.getQuestionId()).orElseThrow(ResourceNotFoundException::new);
        requireOwner(actorId, question);
        question.updateQuestion(questionUpdateForm.getTitle(), contentSanitizer.sanitize(questionUpdateForm.getContent()));
        question.replaceHashtags(resolveHashtags(questionUpdateForm.getHashtags()));
    }

    @Transactional
    public void deleteQuestion(Long actorId, Long id) {
        Question question = questionRepository.findById(id).orElseThrow(ResourceNotFoundException::new);
        requireOwner(actorId, question);
        questionRepository.delete(question);
    }

    private void requireOwner(Long actorId, Question question) {
        if (!question.getMember().getId().equals(actorId)) throw new AccessDeniedException("Not owner");
    }

    private List<Hashtag> resolveHashtags(ArrayList<String> hashtags) {
        Set<String> names = new LinkedHashSet<>();
        if (hashtags != null) {
            for (String rawName : hashtags) {
                if (rawName != null && !rawName.trim().isEmpty()) {
                    names.add(rawName.trim());
                }
            }
        }
        List<Hashtag> resolved = new ArrayList<>();
        for (String name : names) {
            resolved.add(hashtagRepository.findByTagName(name)
                    .orElseGet(() -> hashtagRepository.save(new Hashtag(name))));
        }
        return resolved;
    }

}
