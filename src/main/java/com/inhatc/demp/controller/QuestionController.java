package com.inhatc.demp.controller;

import com.inhatc.demp.domain.Question;
import com.inhatc.demp.dto.question.QuestionDetail;
import com.inhatc.demp.dto.question.QuestionList;
import com.inhatc.demp.repository.QuestionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/question")
public class QuestionController {

    private final QuestionRepository questionRepository;

    @GetMapping
    public ResponseEntity<List<QuestionList>> list(@RequestParam String orderBy) {
        log.info("QuestionController.list");
        List<Question> questions = questionRepository.findAll(Sort.by(Sort.Direction.DESC,orderBy));
        List<QuestionList> result = questions.stream().map(QuestionList::new).collect(Collectors.toList());
        return new ResponseEntity(result,HttpStatus.OK);
    }

    @GetMapping("/{questionId}")
    public ResponseEntity<QuestionDetail> questionDetail(@PathVariable Long questionId) {
        log.info("QuestionController.questionDetail");
        Optional<Question> question = questionRepository.findById(questionId);
        if (question.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        QuestionDetail result = new QuestionDetail(question.get());

        return new ResponseEntity<>(result, HttpStatus.OK);
    }
}
