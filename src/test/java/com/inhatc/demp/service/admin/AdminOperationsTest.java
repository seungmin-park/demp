package com.inhatc.demp.service.admin;

import com.inhatc.demp.domain.*;
import com.inhatc.demp.domain.announcement.*;
import com.inhatc.demp.dto.announcement.AnnouncementUpdateRequest;
import com.inhatc.demp.repository.*;
import com.inhatc.demp.repository.question.QuestionRepository;
import com.inhatc.demp.repository.announcement.AnnouncementRepository;
import com.inhatc.demp.service.FileService;
import java.time.LocalDateTime;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.mock.web.MockMultipartFile;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
class AdminOperationsTest {
    @Autowired AdminAnnouncementService announcements;
    @Autowired AdminCommunityService community;
    @Autowired AdminOverviewService overview;
    @Autowired AnnouncementRepository announcementRepository;
    @Autowired QuestionRepository questionRepository;
    @Autowired AnswerRepository answerRepository;
    @Autowired MemberRepository memberRepository;
    @MockitoBean FileService files;

    @AfterEach
    void cleanup() {
        answerRepository.deleteAll(); questionRepository.deleteAll(); memberRepository.deleteAll(); announcementRepository.deleteAll();
    }

    @Test
    @DisplayName("이미지를 생략한 관리자 수정은 원본 파일을 보존하고 안전한 본문을 커밋한다")
    void updatePreservesImageAndSanitizes() throws Exception {
        Announcement original = announcement();
        announcements.update(original.getId(), request());
        Announcement saved = announcementRepository.findById(original.getId()).orElseThrow();
        assertThat(saved.getTitle()).isEqualTo("수정 교육");
        assertThat(saved.getImage().getSaveFileName()).isEqualTo("old.png");
        assertThat(saved.getAnnouncementType()).isEqualTo(AnnouncementType.EDU);
        assertThat(saved.getDescription().getContent()).contains("<h2>수정</h2>").doesNotContain("script", "onclick");
        assertThat(saved.getDescription().getPayment()).isNull();
        verifyNoInteractions(files);
    }

    @Test
    @DisplayName("교체된 이미지는 DB 커밋을 별도 조회한 뒤에 기존 파일을 삭제한다")
    void replaceDeletesOldAfterCommit() throws Exception {
        Announcement original = announcement();
        AnnouncementUpdateRequest request = request();
        request.setImage(new MockMultipartFile("image", "new.png", "image/png", new byte[]{1}));
        when(files.save(request.getImage())).thenReturn(UploadFile.builder().uploadFileName("new.png").saveFileName("new-key.png").build());
        doAnswer(invocation -> {
            assertThat(announcementRepository.findById(original.getId()).orElseThrow().getImage().getSaveFileName()).isEqualTo("new-key.png");
            return null;
        }).when(files).delete("old.png");
        assertThat(announcements.update(original.getId(), request).cleanupPending()).isFalse();
        verify(files).delete("old.png");
    }

    @Test
    @DisplayName("공고 삭제는 DB 삭제 커밋 후 파일을 정리하고 정리 실패를 응답에 남긴다")
    void deleteReportsCleanupFailure() {
        Announcement original = announcement();
        doAnswer(invocation -> {
            assertThat(announcementRepository.existsById(original.getId())).isFalse();
            throw new IllegalStateException("storage unavailable");
        }).when(files).delete("old.png");
        assertThat(announcements.delete(original.getId()).cleanupPending()).isTrue();
        assertThat(announcementRepository.existsById(original.getId())).isFalse();
    }

    @Test
    @DisplayName("관리자 질문 수정은 작성자와 답변을 보존하고 본문만 정제한다")
    void moderateQuestionWithoutChangingOwner() {
        Question question = question("검색 제목");
        answer(question);
        community.updateQuestion(question.getId(), "관리 제목", "<h2>내용</h2><script>bad()</script>");
        Question saved = questionRepository.findById(question.getId()).orElseThrow();
        assertThat(saved.getTitle()).isEqualTo("관리 제목");
        assertThat(saved.getContent()).contains("<h2>내용</h2>").doesNotContain("script");
        assertThat(saved.getMember().getId()).isEqualTo(question.getMember().getId());
        assertThat(answerRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("질문을 관리 삭제하면 답변도 삭제하고 회원은 유지한다")
    void deleteQuestionCascades() {
        Question question = question("삭제 제목"); answer(question);
        community.deleteQuestion(question.getId());
        assertThat(questionRepository.count()).isZero();
        assertThat(answerRepository.count()).isZero();
        assertThat(memberRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("관리자 답변 수정과 삭제는 별도 조회에 반영하고 질문을 유지한다")
    void moderateAnswer() {
        Question question = question("답변 관리"); Answer answer = answer(question);
        community.updateAnswer(answer.getId(), "<h2>수정 답변</h2><script>bad()</script>");
        assertThat(answerRepository.findById(answer.getId()).orElseThrow().getContent()).contains("수정 답변").doesNotContain("script");
        community.deleteAnswer(answer.getId());
        assertThat(answerRepository.count()).isZero();
        assertThat(questionRepository.existsById(question.getId())).isTrue();
    }

    @Test
    @DisplayName("운영 현황은 DB 건수와 일치하고 제목 검색은 일치하는 질문과 답변만 반환한다")
    void countsAndSearchUseDatabase() {
        announcement(); Question first = question("Spring 질문"); answer(first);
        Question second = Question.builder()
                .title("Vue 질문")
                .content("body")
                .hits(0)
                .recommend(0)
                .dislike(0)
                .build();
        second.assignMember(first.getMember());
        questionRepository.save(second);
        var counts = overview.overview();
        assertThat(counts.announcements()).isEqualTo(1);
        assertThat(counts.bootcamps()).isZero();
        assertThat(counts.questions()).isEqualTo(2);
        assertThat(counts.answers()).isEqualTo(1);
        assertThat(counts.members()).isEqualTo(1);
        var questions = community.questions("spring", 0);
        assertThat(questions.getContent()).extracting(p -> p.title()).containsExactly("Spring 질문");
        assertThat(questions.getTotalElements()).isEqualTo(1);
        assertThat(questions.hasNext()).isFalse();
        assertThat(community.answers("Spring", 0).getContent()).extracting(p -> p.questionId()).containsExactly(first.getId());
    }

    private Announcement announcement() {
        return announcementRepository.save(Announcement.builder().title("원본 공고").company(Company.builder().name("DEMP").build())
                .career(Career.builder()
                        .minCareer(0)
                        .maxCareer(1)
                        .build()).description(Description.builder().content("<p>원본</p>").accessUrl("https://example.test").payment(3000).languages(Set.of(Language.JAVA)).build())
                .image(UploadFile.builder()
                        .uploadFileName("old.png")
                        .saveFileName("old.png")
                        .build()).announcementType(AnnouncementType.EMP).jobPosition(JobPosition.BACKEND)
                .recruitPeriod(RecruitPeriod.builder()
                        .startedDate(LocalDateTime.of(2026,1,1,0,0))
                        .deadLineDate(LocalDateTime.of(2026,12,31,0,0))
                        .build()).build());
    }
    private AnnouncementUpdateRequest request() {
        AnnouncementUpdateRequest request = new AnnouncementUpdateRequest();
        request.setTitle("수정 교육"); request.setCompany("DEMP 교육"); request.setType(AnnouncementType.EDU);
        request.setPosition(JobPosition.BACKEND); request.setLanguage(Set.of(Language.JAVA, Language.SPRING));
        request.setStartedDate(LocalDateTime.of(2026,1,1,0,0)); request.setDeadLineDate(LocalDateTime.of(2026,12,31,0,0));
        request.setContent("<h2 onclick='bad()'>수정</h2><script>bad()</script>"); request.setAccessUrl("https://example.test/apply");
        return request;
    }
    private Question question(String title) {
        Member member = memberRepository.save(Member.builder()
                .username("admin-test-author")
                .password("hashed")
                .roles(List.of("ROLE_USER"))
                .build());
        Question question = Question.builder()
                .title(title)
                .content("원본")
                .hits(0)
                .recommend(0)
                .dislike(0)
                .build();
        question.assignMember(member);
        return questionRepository.save(question);
    }
    private Answer answer(Question question) {
        Answer answer = Answer.builder()
                .content("답변")
                .recommend(0)
                .dislike(0)
                .build();
        answer.assignQuestion(question);
        answer.assignMember(question.getMember());
        return answerRepository.save(answer);
    }
}
