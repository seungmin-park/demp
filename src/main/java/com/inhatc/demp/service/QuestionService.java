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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

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
    private final PlatformTransactionManager transactionManager;

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void createQuestion(Long actorId, QuestionForm questionForm) {
        writeRetryingConcurrentTagCreation(() -> saveNewQuestion(actorId, questionForm));
    }

    private void saveNewQuestion(Long actorId, QuestionForm questionForm) {
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

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void updateQuestion(Long actorId, QuestionUpdateForm questionUpdateForm) {
        writeRetryingConcurrentTagCreation(() -> applyQuestionUpdate(actorId, questionUpdateForm));
    }

    private void applyQuestionUpdate(Long actorId, QuestionUpdateForm questionUpdateForm) {
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

    /**
     * Another writer can commit the same new tag between this transaction's lookup and insert. The unique
     * tag_name constraint then rejects this insert, and only a new transaction (a new snapshot) can see the
     * committed tag, so the whole write runs once more. A second rejection is a different fault and propagates.
     */
    private void writeRetryingConcurrentTagCreation(Runnable write) {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        try {
            transaction.executeWithoutResult(status -> write.run());
        } catch (DataIntegrityViolationException tagCreatedConcurrently) {
            transaction.executeWithoutResult(status -> write.run());
        }
    }

    private void requireOwner(Long actorId, Question question) {
        if (!question.getMember().getId().equals(actorId)) throw new AccessDeniedException("Not owner");
    }

}
