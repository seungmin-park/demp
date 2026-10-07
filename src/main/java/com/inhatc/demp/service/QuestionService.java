package com.inhatc.demp.service;

import com.inhatc.demp.domain.*;
import com.inhatc.demp.error.ResourceNotFoundException;
import org.springframework.security.access.AccessDeniedException;
import com.inhatc.demp.dto.question.*;
import com.inhatc.demp.repository.MemberRepository;
import com.inhatc.demp.repository.question.QuestionQueryRepository;
import com.inhatc.demp.repository.question.QuestionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class QuestionService {

    private final com.inhatc.demp.repository.ContentReactionRepository reactions;
    private final QuestionRepository questionRepository;
    private final QuestionQueryRepository questionQueryRepository;
    private final MemberRepository memberRepository;
    private final HashtagResolver hashtagResolver;
    private final ContentSanitizer contentSanitizer;
    @Transactional
    public void createQuestion(Long actorId, QuestionForm questionForm) {
        Member author = memberRepository.findById(actorId).orElseThrow(ResourceNotFoundException::new);
        Question question = Question.builder()
                .title(questionForm.getTitle())
                .content(contentSanitizer.sanitize(questionForm.getContent()))
                .hits(0)
                .recommend(0)
                .dislike(0)
                .build();
        ArrayList<String> hashtags = questionForm.getHashtags();

        question.replaceHashtags(hashtagResolver.resolve(hashtags));
        question.assignMember(author);
        questionRepository.save(question);
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
        return questionRepository.findDetailById(id)
                .map(QuestionDetail::new)
                .orElseThrow(ResourceNotFoundException::new);
    }

    public QuestionDetail findById(Long id, Long actorId) {
        QuestionDetail detail = findById(id);
        reactions.findByMember_IdAndQuestion_Id(actorId, id).ifPresent(vote -> detail.setMyReaction(vote.getReaction()));
        return detail;
    }

    @Transactional
    public QuestionDetail recordViewAndGetDetail(Long id, Long actorId) {
        if (questionRepository.incrementHits(id) == 0) {
            throw new ResourceNotFoundException();
        }
        return findById(id, actorId);
    }

    @Transactional
    public void updateQuestion(Long actorId, QuestionUpdateForm questionUpdateForm) {
        Question question = questionRepository.findById(questionUpdateForm.getQuestionId()).orElseThrow(ResourceNotFoundException::new);
        requireOwner(actorId, question);
        question.updateQuestion(questionUpdateForm.getTitle(), contentSanitizer.sanitize(questionUpdateForm.getContent()));
        question.replaceHashtags(hashtagResolver.resolve(questionUpdateForm.getHashtags()));
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

}
