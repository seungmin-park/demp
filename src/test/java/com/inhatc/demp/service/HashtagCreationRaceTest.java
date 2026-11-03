package com.inhatc.demp.service;

import com.inhatc.demp.domain.Member;
import com.inhatc.demp.dto.question.QuestionForm;
import com.inhatc.demp.repository.HashtagRepository;
import com.inhatc.demp.repository.MemberRepository;
import com.inhatc.demp.repository.question.QuestionRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@SpringBootTest
class HashtagCreationRaceTest {
    @Autowired QuestionService questions;
    @Autowired QuestionRepository questionRepository;
    @Autowired MemberRepository members;
    @MockitoSpyBean HashtagRepository hashtags;

    @AfterEach
    void cleanup() {
        questionRepository.deleteAll();
        hashtags.deleteAllInBatch();
        members.deleteAll();
    }

    @Test
    @DisplayName("두 회원이 동시에 같은 새 태그로 질문을 작성해도 두 질문 모두 저장되고 태그는 한 행이다")
    void concurrentQuestionsShareOneNewTag() throws Exception {
        Member first = member("race-writer-a");
        Member second = member("race-writer-b");
        // Both transactions must miss the tag before either inserts it.
        CountDownLatch bothLookedUp = new CountDownLatch(2);
        var delegate = mockingDetails(hashtags).getMockCreationSettings().getDefaultAnswer();
        doAnswer(invocation -> {
            Object found = delegate.answer(invocation);
            bothLookedUp.countDown();
            bothLookedUp.await(10, TimeUnit.SECONDS);
            return found;
        }).when(hashtags).findByTagName(eq("race-tag"));

        try (var executor = Executors.newFixedThreadPool(2)) {
            var results = executor.invokeAll(List.of(write(first, "동시 질문 A"), write(second, "동시 질문 B")), 20, TimeUnit.SECONDS);
            List<String> outcomes = new ArrayList<>();
            for (Future<String> result : results) outcomes.add(result.get());
            assertThat(outcomes).containsExactly("saved", "saved");
        }
        assertThat(hashtags.findAll()).extracting("tagName").containsExactly("race-tag");
        assertThat(questionRepository.findAll()).extracting("title").containsExactlyInAnyOrder("동시 질문 A", "동시 질문 B");
    }

    private Callable<String> write(Member author, String title) {
        return () -> {
            try {
                questions.createQuestion(author.getId(), new QuestionForm(title, "본문", new ArrayList<>(List.of("race-tag"))));
                return "saved";
            } catch (RuntimeException failure) {
                return failure.getClass().getSimpleName();
            }
        };
    }

    private Member member(String username) {
        return members.save(Member.builder().username(username).password("unused").roles(List.of("ROLE_USER")).build());
    }
}
