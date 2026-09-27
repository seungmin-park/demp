package com.inhatc.demp.controller;

import com.inhatc.demp.config.security.MemberPrincipal;
import com.inhatc.demp.dto.answer.AnswerForm;
import com.inhatc.demp.dto.answer.UpdateAnswerForm;
import com.inhatc.demp.dto.question.QuestionAnswer;
import com.inhatc.demp.service.AnswerService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/answer")
public class AnswerController {
    private final AnswerService answerService;
    @GetMapping("/{questionId}")
    public List<QuestionAnswer> getAnswersByQuestion(@AuthenticationPrincipal MemberPrincipal principal, @PathVariable Long questionId) { return answerService.findByQuestion(questionId, principal.getMemberId()); }
    @PostMapping("/save")
    public List<QuestionAnswer> saveAnswer(@AuthenticationPrincipal MemberPrincipal principal, @RequestBody AnswerForm form) {
        return answerService.createAnswerAndList(principal.getMemberId(), form);
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
