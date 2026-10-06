package com.inhatc.demp.controller;

import com.inhatc.demp.config.security.MemberPrincipal;
import com.inhatc.demp.dto.answer.AnswerForm;
import com.inhatc.demp.dto.answer.AnswerPage;
import com.inhatc.demp.dto.answer.UpdateAnswerForm;
import com.inhatc.demp.error.ApiException;
import org.springframework.http.HttpStatus;
import com.inhatc.demp.dto.question.QuestionAnswer;
import com.inhatc.demp.service.AnswerService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/answer")
public class AnswerController {
    private final AnswerService answerService;
    @GetMapping("/{questionId}")
    public AnswerPage getAnswerPage(@AuthenticationPrincipal MemberPrincipal principal,
            @PathVariable Long questionId, @RequestParam(required = false) String before) {
        return answerService.findAnswerPage(questionId, principal.getMemberId(), parseCursor(before));
    }
    private Long parseCursor(String before) {
        if (before == null) return null;
        if (!before.matches("[+-]?[0-9]+")) throw new ApiException(HttpStatus.BAD_REQUEST);
        try { return Long.valueOf(before); }
        catch (NumberFormatException exception) { throw new ApiException(HttpStatus.BAD_REQUEST); }
    }
    @PostMapping("/save")
    public QuestionAnswer saveAnswer(@AuthenticationPrincipal MemberPrincipal principal, @RequestBody AnswerForm form) {
        return answerService.createAnswer(principal.getMemberId(), form);
    }
    @PatchMapping("/update")
    public void updateAnswer(@AuthenticationPrincipal MemberPrincipal principal, @RequestBody UpdateAnswerForm form) {
        answerService.update(principal.getMemberId(), form);
    }
    @DeleteMapping("/delete")
    public void deleteAnswer(@AuthenticationPrincipal MemberPrincipal principal, @RequestParam Long answerId) {
        answerService.delete(principal.getMemberId(), answerId);
    }
}
