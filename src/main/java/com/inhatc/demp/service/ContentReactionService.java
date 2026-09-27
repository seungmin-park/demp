package com.inhatc.demp.service;

import com.inhatc.demp.domain.*;
import com.inhatc.demp.dto.reaction.ReactionResponse;
import com.inhatc.demp.error.ResourceNotFoundException;
import com.inhatc.demp.repository.*;
import com.inhatc.demp.repository.question.QuestionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ContentReactionService {
    private final QuestionRepository questions;
    private final AnswerRepository answers;
    private final MemberRepository members;
    private final ContentReactionRepository reactions;

    @Transactional
    public ReactionResponse setQuestionReaction(Long memberId, Long questionId, ReactionType next) {
        Question question = questions.findByIdForReaction(questionId).orElseThrow(ResourceNotFoundException::new);
        ContentReaction vote = reactions.findByMember_IdAndQuestion_Id(memberId, questionId).orElse(null);
        ReactionType previous = vote == null ? ReactionType.NONE : vote.getReaction();
        question.changeReaction(previous, next);
        if (vote != null) vote.changeTo(next);
        else if (next != ReactionType.NONE) reactions.save(ContentReaction.builder()
                .member(members.findById(memberId).orElseThrow(ResourceNotFoundException::new))
                .question(question).reaction(next).build());
        return new ReactionResponse(question.getRecommend(), question.getDislike(), next);
    }

    @Transactional
    public ReactionResponse setAnswerReaction(Long memberId, Long answerId, ReactionType next) {
        Answer answer = answers.findByIdForReaction(answerId).orElseThrow(ResourceNotFoundException::new);
        ContentReaction vote = reactions.findByMember_IdAndAnswer_Id(memberId, answerId).orElse(null);
        ReactionType previous = vote == null ? ReactionType.NONE : vote.getReaction();
        answer.changeReaction(previous, next);
        if (vote != null) vote.changeTo(next);
        else if (next != ReactionType.NONE) reactions.save(ContentReaction.builder()
                .member(members.findById(memberId).orElseThrow(ResourceNotFoundException::new))
                .answer(answer).reaction(next).build());
        return new ReactionResponse(answer.getRecommend(), answer.getDislike(), next);
    }
}
