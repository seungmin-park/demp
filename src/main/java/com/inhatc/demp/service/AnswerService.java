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
    public List<QuestionAnswer> findByQuestion(Long questionId) {
        return answers.findByQuestion_Id(questionId).stream().map(QuestionAnswer::new).collect(Collectors.toList());
    }
    public List<QuestionAnswer> findByQuestion(Long questionId, Long actorId) {
        var selected = reactions.findByMember_IdAndAnswer_Question_Id(actorId, questionId).stream()
                .collect(Collectors.toMap(vote -> vote.getAnswer().getId(), ContentReaction::getReaction));
        List<QuestionAnswer> result = findByQuestion(questionId);
        result.forEach(answer -> answer.setMyReaction(selected.getOrDefault(answer.getAnswerId(), ReactionType.NONE)));
        return result;
    }
    @Transactional
    public List<QuestionAnswer> createAnswerAndList(Long actorId, AnswerForm form) {
        Member member = members.findById(actorId).orElseThrow(ResourceNotFoundException::new);
        Question question = questions.findById(form.getQuestionId()).orElseThrow(ResourceNotFoundException::new);
        Answer answer = Answer.builder().content(sanitizer.sanitize(form.getAnswerContent())).recommend(0).dislike(0).build();
        answer.assignMember(member); answer.assignQuestion(question); answers.save(answer);
        return findByQuestion(question.getId(), actorId);
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
