package com.inhatc.demp.controller;

import com.inhatc.demp.config.security.MemberPrincipal;
import com.inhatc.demp.dto.reaction.*;
import com.inhatc.demp.service.ContentReactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class ContentReactionController {
    private final ContentReactionService reactions;

    @PutMapping("/question/{id}/reaction")
    public ReactionResponse setQuestionReaction(@AuthenticationPrincipal MemberPrincipal member,
            @PathVariable Long id, @Valid @RequestBody ReactionRequest request) {
        return reactions.setQuestionReaction(member.getMemberId(), id, request.reaction());
    }

    @PutMapping("/answer/{id}/reaction")
    public ReactionResponse setAnswerReaction(@AuthenticationPrincipal MemberPrincipal member,
            @PathVariable Long id, @Valid @RequestBody ReactionRequest request) {
        return reactions.setAnswerReaction(member.getMemberId(), id, request.reaction());
    }
}
