package com.inhatc.demp.controller;

import com.inhatc.demp.dto.question.*;
import com.inhatc.demp.service.QuestionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/question")
public class QuestionController {

    private final QuestionService questionService;

    @GetMapping
    public ResponseEntity<List<QuestionList>> list(@ModelAttribute QuestionSearchCondition searchCondition) {
        log.info("QuestionController.list");
        List<QuestionList> result = questionService.findAllBySearchCondition(searchCondition);
        return new ResponseEntity(result, HttpStatus.OK);
    }

    @GetMapping("/{questionId}")
    public ResponseEntity<QuestionDetail> questionDetail(@PathVariable Long questionId) {
        log.info("QuestionController.questionDetail");
        QuestionDetail questionDetail = questionService.findById(questionId);
        if (questionDetail == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        log.info("questionDetail={}", questionDetail);
        return new ResponseEntity<>(questionDetail, HttpStatus.OK);
    }

    @GetMapping("/hashtags")
    public List<String> hashtags() {
        return questionService.findAllHashtags();
    }

    @PostMapping("/add")
    public String addQuestion(@Valid @RequestBody QuestionForm questionForm) {
        log.info("questionForm ={}",questionForm);
        questionService.join(questionForm);
        return "ok";
    }

    @PatchMapping("/update")
    public void updateQuestion(@RequestBody QuestionUpdateForm questionUpdateForm) {
        questionService.updateQuestion(questionUpdateForm);
    }

    @DeleteMapping("/delete")
    public void deleteQuestion(@RequestParam Long questionId) {
        questionService.deleteQuestion(questionId);
    }
}
