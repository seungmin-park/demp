package com.inhatc.demp.controller;

import com.inhatc.demp.domain.Question;
import com.inhatc.demp.dto.question.QuestionDetail;
import com.inhatc.demp.dto.question.QuestionForm;
import com.inhatc.demp.dto.question.QuestionList;
import com.inhatc.demp.dto.question.QuestionSearchCondition;
import com.inhatc.demp.service.QuestionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/question")
public class QuestionController {

    private final QuestionService questionService;

    @GetMapping
    public ResponseEntity<List<QuestionList>> list(@ModelAttribute QuestionSearchCondition searchCondition) {
        log.info("QuestionController.list");
        List<QuestionList> result = questionService.findAllBySearchCondition(searchCondition)
                .stream().map(QuestionList::new).collect(Collectors.toList());
        return new ResponseEntity(result, HttpStatus.OK);
    }

    @GetMapping("/{questionId}")
    public ResponseEntity<QuestionDetail> questionDetail(@PathVariable Long questionId) {
        log.info("QuestionController.questionDetail");
        Optional<Question> question = questionService.findById(questionId);
        if (question.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        QuestionDetail result = new QuestionDetail(question.get());
        log.info("result={}", result);
        return new ResponseEntity<>(result, HttpStatus.OK);
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
}
