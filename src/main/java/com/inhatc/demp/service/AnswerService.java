package com.inhatc.demp.service;

import com.inhatc.demp.domain.*;
import com.inhatc.demp.dto.answer.*;
import com.inhatc.demp.dto.question.QuestionAnswer;
import com.inhatc.demp.error.ResourceNotFoundException;
import com.inhatc.demp.repository.AnswerRepository;
import com.inhatc.demp.repository.MemberRepository;
import com.inhatc.demp.repository.question.QuestionRepository;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnswerService {
    private final com.inhatc.demp.repository.ContentReactionRepository reactions;
    private final AnswerRepository answers;
    private final QuestionRepository questions;
    private final MemberRepository members;
    private final ContentSanitizer sanitizer;
    public AnswerPage findAnswerPage(Long questionId, Long actorId, Long before) {
        if (!questions.existsById(questionId)) throw new ResourceNotFoundException();
        List<QuestionAnswer> window = answers.findAnswerWindow(questionId, before, PageRequest.of(0, 21));
        List<QuestionAnswer> content = window.stream().limit(20).toList();
        if (content.isEmpty()) return new AnswerPage(content, null, false);
        var selected = reactions.findByMember_IdAndAnswer_IdIn(actorId,
                content.stream().map(QuestionAnswer::getAnswerId).toList()).stream()
                .collect(Collectors.toMap(vote -> vote.getAnswer().getId(), ContentReaction::getReaction));
        content.forEach(answer -> answer.setMyReaction(selected.getOrDefault(answer.getAnswerId(), ReactionType.NONE)));
        boolean hasNext = window.size() > 20;
        return new AnswerPage(content, hasNext ? content.getLast().getAnswerId().toString() : null, hasNext);
    }
    @Transactional
    public QuestionAnswer createAnswer(Long actorId, AnswerForm form) {
        Member member = members.findById(actorId).orElseThrow(ResourceNotFoundException::new);
        Question question = questions.findById(form.getQuestionId()).orElseThrow(ResourceNotFoundException::new);
        Answer answer = Answer.createFor(member, question, sanitizer.sanitize(form.getAnswerContent()));
        answers.save(answer);
        return new QuestionAnswer(answer);
    }
    @Transactional
    public void update(Long actorId, UpdateAnswerForm form) {
        Answer answer = owned(actorId, form.getAnswerId());
        answer.updateAnswer(sanitizer.sanitize(form.getAnswerContent()));
    }
    @Transactional
    public void delete(Long actorId, Long answerId) { answers.delete(owned(actorId, answerId)); }
    private Answer owned(Long actorId, Long answerId) {
        Answer answer = answers.findById(answerId).orElseThrow(ResourceNotFoundException::new);
        if (!answer.getMember().getId().equals(actorId)) throw new AccessDeniedException("Not owner");
        return answer;
    }
}
