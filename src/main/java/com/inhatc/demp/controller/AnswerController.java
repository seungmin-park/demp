package com.inhatc.demp.controller;

import com.inhatc.demp.domain.Answer;
import com.inhatc.demp.dto.answer.AnswerForm;
import com.inhatc.demp.dto.answer.UpdateAnswerForm;
import com.inhatc.demp.dto.question.QuestionAnswer;
import com.inhatc.demp.repository.AnswerRepository;
import com.inhatc.demp.service.QuestionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/answer")
public class AnswerController {

    private final AnswerRepository answerRepository;
    private final QuestionService questionService;

    @GetMapping("/{questionId}")
    public ResponseEntity<List<QuestionAnswer>> getAnswersByQuestion(@PathVariable Long questionId) {
        log.info("AnswerController.answers");
        List<Answer> answers = answerRepository.findByQuestion_Id(questionId);
        List<QuestionAnswer> result = answers.stream().map(a -> new QuestionAnswer(a)).collect(Collectors.toList());

        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    @PostMapping("/save")
    public List<QuestionAnswer> saveAnswer(@RequestBody AnswerForm answerForm) {
        return questionService.saveAnswer(answerForm);
    }

    @PatchMapping("/update")
    public void updateAnswer(@RequestBody UpdateAnswerForm updateAnswerForm) {
        Answer answer = answerRepository.findById(updateAnswerForm.getAnswerId()).orElseThrow(() -> new NoSuchElementException("해당 데이터 존재x"));
        answer.updateAnswer(updateAnswerForm.getAnswerContent());
    }

    @DeleteMapping("/delete")
    public void deleteAnswer(@RequestParam Long answerId) {
        answerRepository.deleteById(answerId);
    }
}
