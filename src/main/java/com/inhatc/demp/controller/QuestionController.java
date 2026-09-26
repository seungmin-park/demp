package com.inhatc.demp.controller;

import com.inhatc.demp.dto.question.*;
import com.inhatc.demp.error.ApiException;
import com.inhatc.demp.service.QuestionService;
import lombok.RequiredArgsConstructor;
import com.inhatc.demp.config.security.MemberPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/question")
public class QuestionController {

    private final QuestionService questionService;

    @GetMapping
    public ResponseEntity<QuestionSliceResponse> getQuestionSlice(
            @ModelAttribute QuestionSearchCondition searchCondition,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new ApiException(HttpStatus.BAD_REQUEST);
        }
        return ResponseEntity.ok(new QuestionSliceResponse(
                questionService.findSliceBySearchCondition(searchCondition, PageRequest.of(page, size))));
    }

    @GetMapping("/detail/{questionId}")
    public ResponseEntity<QuestionDetail> getQuestion(@PathVariable Long questionId) {
        return ResponseEntity.ok(questionService.findById(questionId));
    }

    @GetMapping("/hashtags")
    public List<String> getAllHashtags() {
        return questionService.findAllHashtags();
    }

    @PostMapping("/add")
    public String saveQuestion(@AuthenticationPrincipal MemberPrincipal principal, @Valid @RequestBody QuestionForm questionForm) {
        questionService.join(principal.getMemberId(), questionForm);
        return "ok";
    }

    @PatchMapping("/update")
    public void updateQuestion(@AuthenticationPrincipal MemberPrincipal principal, @RequestBody QuestionUpdateForm questionUpdateForm) {
        questionService.updateQuestion(principal.getMemberId(), questionUpdateForm);
    }

    @DeleteMapping("/delete")
    public void deleteQuestion(@AuthenticationPrincipal MemberPrincipal principal, @RequestParam Long questionId) {
        questionService.deleteQuestion(principal.getMemberId(), questionId);
    }
}
