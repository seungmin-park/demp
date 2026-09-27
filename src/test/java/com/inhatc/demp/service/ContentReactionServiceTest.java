package com.inhatc.demp.service;

import com.inhatc.demp.domain.*;
import com.inhatc.demp.repository.*;
import com.inhatc.demp.repository.question.QuestionRepository;
import com.inhatc.demp.error.ResourceNotFoundException;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest
class ContentReactionServiceTest {
    @Autowired ContentReactionService service;
    @Autowired ContentReactionRepository reactions;
    @Autowired MemberRepository members;
    @Autowired QuestionRepository questions;
    @Autowired AnswerRepository answers;
    @Autowired org.springframework.transaction.PlatformTransactionManager transactionManager;

    @AfterEach
    void cleanup() {
        reactions.deleteAllInBatch(); answers.deleteAllInBatch(); questions.deleteAll(); members.deleteAll();
    }

    @ParameterizedTest
    @DisplayName("여러 회원의 동시 반응과 같은 요청 재전송은 각각 한 번만 집계한다")
    @ValueSource(booleans = {true, false})
    void concurrentReactions(boolean onQuestion) throws Exception {
        Member owner = member("owner");
        Question question = question(owner);
        Answer answer = Answer.builder().content("답변").recommend(7).dislike(2).build();
        answer.assignMember(owner); answer.assignQuestion(question); answers.save(answer);
        List<Member> voters = new ArrayList<>();
        for (int i = 0; i < 8; i++) voters.add(member("voter-" + i));
        CountDownLatch ready = new CountDownLatch(8);
        CountDownLatch start = new CountDownLatch(1);
        try (var pool = Executors.newFixedThreadPool(8)) {
            List<Future<?>> tasks = new ArrayList<>();
            for (Member voter : voters) tasks.add(pool.submit(() -> {
                ready.countDown();
                try { if (!start.await(10, TimeUnit.SECONDS)) throw new AssertionError("시작 대기 초과"); }
                catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new RuntimeException(e); }
                for (int repeat = 0; repeat < 2; repeat++) {
                    if (onQuestion) service.setQuestionReaction(voter.getId(), question.getId(), ReactionType.RECOMMEND);
                    else service.setAnswerReaction(voter.getId(), answer.getId(), ReactionType.RECOMMEND);
                }
            }));
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue(); start.countDown();
            for (Future<?> task : tasks) task.get(20, TimeUnit.SECONDS);
        }
        assertThat(reactions.count()).isEqualTo(8);
        assertThat(onQuestion ? questions.findById(question.getId()).orElseThrow().getRecommend()
                : answers.findById(answer.getId()).orElseThrow().getRecommend()).isEqualTo(15);
        assertThat(onQuestion ? questions.findById(question.getId()).orElseThrow().getDislike()
                : answers.findById(answer.getId()).orElseThrow().getDislike()).isEqualTo(2);
    }

    @Test
    @DisplayName("질문과 답변 삭제는 해당 반응도 제거하고 회원을 보존한다")
    void deletionRemovesVotes() {
        Member owner = member("owner"); Question question = question(owner);
        Answer answer = Answer.builder().content("답변").build();
        answer.assignMember(owner); answer.assignQuestion(question); answers.save(answer);
        service.setQuestionReaction(owner.getId(), question.getId(), ReactionType.RECOMMEND);
        service.setAnswerReaction(owner.getId(), answer.getId(), ReactionType.DISLIKE);
        answers.deleteById(answer.getId());
        assertThat(reactions.count()).isEqualTo(1);
        questions.deleteById(question.getId());
        assertThat(reactions.count()).isZero(); assertThat(members.existsById(owner.getId())).isTrue();
    }

    @Test
    @DisplayName("없는 대상 반응은 기록이나 집계를 만들지 않는다")
    void missingTarget() {
        Member owner = member("owner");
        assertThatThrownBy(() -> service.setQuestionReaction(owner.getId(), -1L, ReactionType.RECOMMEND)).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.setAnswerReaction(owner.getId(), -1L, ReactionType.DISLIKE)).isInstanceOf(ResourceNotFoundException.class);
        assertThat(reactions.count()).isZero();
    }

    @ParameterizedTest
    @DisplayName("반응 저장 전에 읽은 본문 수정이 새 반응 집계를 덮어쓰지 않는다")
    @ValueSource(booleans = {true, false})
    void contentEditPreservesConcurrentReaction(boolean onQuestion) {
        Member owner = member("owner"); Question question = question(owner);
        Answer answer = Answer.builder().content("답변").recommend(7).dislike(2).build();
        answer.assignMember(owner); answer.assignQuestion(question); answers.save(answer);
        new org.springframework.transaction.support.TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            if (onQuestion) {
                Question editing = questions.findById(question.getId()).orElseThrow();
                CompletableFuture.runAsync(() -> service.setQuestionReaction(owner.getId(), question.getId(), ReactionType.RECOMMEND)).join();
                editing.updateQuestion("수정 제목", "수정 본문");
            } else {
                Answer editing = answers.findById(answer.getId()).orElseThrow();
                CompletableFuture.runAsync(() -> service.setAnswerReaction(owner.getId(), answer.getId(), ReactionType.RECOMMEND)).join();
                editing.updateAnswer("수정 본문");
            }
        });
        assertThat(onQuestion ? questions.findById(question.getId()).orElseThrow().getRecommend()
                : answers.findById(answer.getId()).orElseThrow().getRecommend()).isEqualTo(8);
        assertThat(onQuestion ? questions.findById(question.getId()).orElseThrow().getContent()
                : answers.findById(answer.getId()).orElseThrow().getContent()).isEqualTo("수정 본문");
    }

    private Member member(String name) {
        return members.save(Member.builder().username(name).password("hash").roles(List.of("ROLE_USER")).build());
    }
    private Question question(Member owner) {
        Question question = Question.builder().title("질문").content("내용").recommend(7).dislike(2).build();
        question.assignMember(owner); return questions.save(question);
    }
}
