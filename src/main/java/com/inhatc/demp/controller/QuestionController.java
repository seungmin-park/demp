package com.inhatc.demp.controller;

import com.inhatc.demp.dto.question.*;
import com.inhatc.demp.service.QuestionService;
import lombok.RequiredArgsConstructor;
import com.inhatc.demp.config.security.MemberPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/question")
public class QuestionController {

    private final QuestionService questionService;

    @GetMapping
    public ResponseEntity<List<QuestionList>> getAllQuestions(@ModelAttribute QuestionSearchCondition searchCondition) {
        List<QuestionList> result = questionService.findAllBySearchCondition(searchCondition);
        return new ResponseEntity(result, HttpStatus.OK);
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
