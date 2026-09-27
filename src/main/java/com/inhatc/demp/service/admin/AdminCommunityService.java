package com.inhatc.demp.service.admin;

import com.inhatc.demp.domain.*;
import com.inhatc.demp.dto.admin.AdminPost;
import com.inhatc.demp.error.ApiException;
import com.inhatc.demp.repository.AnswerRepository;
import com.inhatc.demp.repository.question.QuestionRepository;
import com.inhatc.demp.service.ContentSanitizer;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminCommunityService {
    private final QuestionRepository questions;
    private final AnswerRepository answers;
    private final ContentSanitizer sanitizer;

    public Page<AdminPost> questions(String term, int page) {
        return questions.searchForAdmin(escape(term), page(page)).map(this::questionPost);
    }
    public Page<AdminPost> answers(String term, int page) {
        return answers.searchForAdmin(escape(term), page(page)).map(this::answerPost);
    }
    public AdminPost question(long id) { return questionPost(findQuestion(id)); }
    public AdminPost answer(long id) { return answerPost(findAnswer(id)); }
    @Transactional
    public void updateQuestion(long id, String title, String content) { findQuestion(id).updateQuestion(title, sanitizer.sanitize(content)); }
    @Transactional
    public void updateAnswer(long id, String content) { findAnswer(id).updateAnswer(sanitizer.sanitize(content)); }
    @Transactional
    public void deleteQuestion(long id) { questions.delete(findQuestion(id)); }
    @Transactional
    public void deleteAnswer(long id) { answers.delete(findAnswer(id)); }
    private Question findQuestion(long id) { return questions.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND)); }
    private Answer findAnswer(long id) { return answers.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND)); }
    private AdminPost questionPost(Question item) {
        return new AdminPost(item.getId(), item.getId(), item.getTitle(), item.getContent(), item.getMember().getUsername(),
                item.getQuestionHashtags().stream().map(relation -> relation.getHashtag().getTagName()).toList());
    }
    private AdminPost answerPost(Answer item) {
        return new AdminPost(item.getId(), item.getQuestion().getId(), item.getQuestion().getTitle(), item.getContent(), item.getMember().getUsername(), List.of());
    }
    private static Pageable page(int page) {
        if (page < 0) throw new ApiException(HttpStatus.BAD_REQUEST);
        return PageRequest.of(page, 20, Sort.by(Sort.Direction.DESC, "id"));
    }
    private static String escape(String value) { return value.trim().replace("!", "!!").replace("%", "!%").replace("_", "!_"); }
}
