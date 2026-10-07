package com.inhatc.demp.config;

import tools.jackson.databind.ObjectMapper;
import com.inhatc.demp.config.jwt.JwtTokenProvider;
import com.inhatc.demp.domain.*;
import com.inhatc.demp.dto.question.QuestionForm;
import com.inhatc.demp.dto.question.QuestionUpdateForm;
import com.inhatc.demp.dto.answer.AnswerForm;
import com.inhatc.demp.dto.answer.UpdateAnswerForm;
import com.inhatc.demp.repository.*;
import com.inhatc.demp.repository.question.QuestionRepository;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ApiSecurityTest {
    @org.springframework.beans.factory.annotation.Value("${spring.jwt.secret}") String jwtSecret;
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired JwtTokenProvider tokens;
    @Autowired MemberRepository members;
    @Autowired QuestionRepository questions;
    @Autowired AnswerRepository answers;
    private final List<Long> memberIds = new ArrayList<>();

    @Test
    @DisplayName("인증 실패는 조회수를 바꾸지 않고 성공 상세만 집계하며 편집 조회는 제외한다")
    void viewCountRequiresSuccessfulAuthenticatedDetail() throws Exception {
        Member actor = member("security-view-count", List.of("ROLE_USER"));
        Question saved = question(actor);
        String path = "/api/question/detail/" + saved.getId();
        mvc.perform(get(path)).andExpect(status().isUnauthorized());
        assertThat(questions.findById(saved.getId()).orElseThrow().getHits()).isZero();
        mvc.perform(get(path).header("X-AUTH-TOKEN", token(actor)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.hits").value(1));
        mvc.perform(get(path).param("recordView", "false").header("X-AUTH-TOKEN", token(actor)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.hits").value(1));
        assertThat(questions.findById(saved.getId()).orElseThrow().getHits()).isEqualTo(1);
    }

    @AfterEach
    void cleanup() {
        answers.deleteAll(answers.findAll().stream().filter(a -> memberIds.contains(a.getMember().getId())).collect(java.util.stream.Collectors.toList()));
        questions.deleteAll(questions.findAll().stream().filter(q -> memberIds.contains(q.getMember().getId())).collect(java.util.stream.Collectors.toList()));
        members.deleteAllById(memberIds);
    }

    @Test
    @DisplayName("가입 응답에서 비밀번호를 제외하고 암호화된 비밀번호를 저장한다")
    void signupProtectsPassword() throws Exception {
        try {
            mvc.perform(post("/api/member/save").param("username", "security-new").param("password", "secret"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.password").doesNotExist())
                    .andExpect(jsonPath("$.username").value("security-new"));
            Member saved = members.findByUsername("security-new").orElseThrow();
            assertThat(new BCryptPasswordEncoder().matches("secret", saved.getPassword())).isTrue();
        } finally {
            members.findByUsername("security-new").ifPresent(m -> memberIds.add(m.getId()));
        }
    }

    @Test
    @DisplayName("중복 회원 가입은 409를 반환하고 기존 회원을 유지한다")
    void rejectsDuplicateSignup() throws Exception {
        Member existing = member("security-duplicate", List.of("ROLE_USER"));
        mvc.perform(post("/api/member/save").param("username", existing.getUsername()).param("password", "secret"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.errorCode").value(409));
        assertThat(members.findByUsername(existing.getUsername()).orElseThrow().getId()).isEqualTo(existing.getId());
    }

    @ParameterizedTest
    @DisplayName("빈 가입 필드는 400을 반환한다")
    @ValueSource(strings = {"username", "password"})
    void rejectsBlankSignup(String blank) throws Exception {
        mvc.perform(post("/api/member/save").param("username", blank.equals("username") ? " " : "security-invalid")
                        .param("password", blank.equals("password") ? " " : "secret"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errorCode").value(400));
        members.findByUsername("security-invalid").ifPresent(m -> memberIds.add(m.getId()));
    }

    @Test
    @DisplayName("없는 계정과 잘못된 비밀번호는 동일한 인증 실패를 반환한다")
    void loginFailureIsUniform() throws Exception {
        Member member = member("security-login", List.of("ROLE_USER"));
        String wrong = mvc.perform(post("/api/member/login").param("username", member.getUsername()).param("password", "wrong"))
                .andExpect(status().isUnauthorized()).andReturn().getResponse().getContentAsString();
        String missing = mvc.perform(post("/api/member/login").param("username", "missing-account").param("password", "wrong"))
                .andExpect(status().isUnauthorized()).andReturn().getResponse().getContentAsString();
        assertThat(wrong).isEqualTo(missing);
    }

    @Test
    @DisplayName("질문 작성자는 요청 이름 대신 인증된 회원으로 저장한다")
    void usesAuthenticatedActor() throws Exception {
        Member actor = member("security-actor", List.of("ROLE_USER"));
        Member forged = member("security-forged", List.of("ROLE_USER"));
        QuestionForm form = new QuestionForm("security-question", "body", forged.getUsername(), new ArrayList<>());
        mvc.perform(post("/api/question/add").header("X-AUTH-TOKEN", token(actor))
                        .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(form)))
                .andExpect(status().isOk());
        Question saved = questions.findAll().stream().filter(q -> q.getTitle().equals("security-question")).findFirst().orElseThrow();
        assertThat(saved.getMember().getId()).isEqualTo(actor.getId());
    }

    @Test
    @DisplayName("타인의 질문 수정과 삭제는 403이고 원본을 유지한다")
    void forbidsForeignQuestionWrites() throws Exception {
        Member owner = member("security-owner", List.of("ROLE_USER"));
        Member attacker = member("security-attacker", List.of("ROLE_USER"));
        Question question = question(owner);
        mvc.perform(patch("/api/question/update").header("X-AUTH-TOKEN", token(attacker)).contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(new QuestionUpdateForm(question.getId(), "changed", "changed", new ArrayList<>()))))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/api/question/delete").header("X-AUTH-TOKEN", token(attacker)).param("questionId", question.getId().toString()))
                .andExpect(status().isForbidden());
        assertThat(questions.findById(question.getId()).orElseThrow().getTitle()).isEqualTo("original");
    }

    @Test
    @DisplayName("타인의 답변 수정과 삭제는 403이고 원본을 유지한다")
    void forbidsForeignAnswerWrites() throws Exception {
        Member owner = member("security-answer-owner", List.of("ROLE_USER"));
        Member attacker = member("security-answer-attacker", List.of("ROLE_USER"));
        Answer answer = Answer.builder().content("original").recommend(0).dislike(0).build();
        answer.assignMember(owner); answer.assignQuestion(question(owner)); answers.save(answer);
        mvc.perform(patch("/api/answer/update").header("X-AUTH-TOKEN", token(attacker)).contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(new UpdateAnswerForm(answer.getId(), "changed"))))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/api/answer/delete").header("X-AUTH-TOKEN", token(attacker)).param("answerId", answer.getId().toString()))
                .andExpect(status().isForbidden());
        assertThat(answers.findById(answer.getId()).orElseThrow().getContent()).isEqualTo("original");
    }

    @Test
    @DisplayName("존재하지 않는 질문은 404를 반환한다")
    void missingQuestionIsNotFound() throws Exception {
        Member actor = member("security-missing", List.of("ROLE_USER"));
        mvc.perform(delete("/api/question/delete").header("X-AUTH-TOKEN", token(actor)).param("questionId", "-1"))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.errorCode").value(404));
    }

    @Test
    @DisplayName("토큰 없는 쓰기 요청은 401이다")
    void missingTokenIsUnauthorized() throws Exception {
        mvc.perform(delete("/api/question/delete").param("questionId", "1"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.errorCode").value(401));
    }

    @Test
    @DisplayName("사용자 권한이 없는 유효 토큰은 403이다")
    void insufficientRoleIsForbidden() throws Exception {
        Member actor = member("security-no-role", List.of("ROLE_GUEST"));
        mvc.perform(delete("/api/question/delete").header("X-AUTH-TOKEN", token(actor)).param("questionId", "1"))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.errorCode").value(403));
    }

    @ParameterizedTest
    @DisplayName("숫자가 아닌 subject와 삭제된 회원 토큰은 401이다")
    @ValueSource(strings = {"not-number", "-1"})
    void invalidSubjectIsUnauthorized(String subject) throws Exception {
        mvc.perform(delete("/api/question/delete").header("X-AUTH-TOKEN", tokens.createToken(subject, List.of("ROLE_USER"))).param("questionId", "1"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.errorCode").value(401));
    }

    @Test
    @DisplayName("변조된 토큰은 401이다")
    void malformedTokenIsUnauthorized() throws Exception {
        mvc.perform(delete("/api/question/delete").header("X-AUTH-TOKEN", "invalid.jwt.token").param("questionId", "1"))
                .andExpect(status().isUnauthorized());
    }


    @ParameterizedTest
    @DisplayName("만료되었거나 만료 시간이 없는 서명 토큰은 401이다")
    @ValueSource(booleans = {true, false})
    void rejectsExpiredOrUndatedToken(boolean expired) throws Exception {
        Member actor = member("security-expiry", List.of("ROLE_USER"));
        io.jsonwebtoken.JwtBuilder builder = io.jsonwebtoken.Jwts.builder().setSubject(actor.getId().toString());
        if (expired) builder.setExpiration(new Date(1));
        String token = builder.signWith(io.jsonwebtoken.SignatureAlgorithm.HS256,
                Base64.getEncoder().encodeToString(jwtSecret.getBytes(java.nio.charset.StandardCharsets.UTF_8))).compact();
        mvc.perform(delete("/api/question/delete").header("X-AUTH-TOKEN", token).param("questionId", "1"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.errorCode").value(401));
    }

    @Test
    @DisplayName("업그레이드 전과 같은 HS256 서명 방식의 토큰으로 인증한다")
    void acceptsLegacySigningContract() throws Exception {
        Member actor = member("security-legacy-token", List.of("ROLE_USER"));
        // JJWT 0.9의 Base64 문자열 키 overload와 독립된 JCA로 이전 wire 계약을 재현한다.
        Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
        String header = encoder.encodeToString("{\"alg\":\"HS256\"}".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        String payload = encoder.encodeToString(mapper.writeValueAsBytes(Map.of(
                "sub", actor.getId().toString(), "roles", List.of("ROLE_USER"),
                "iat", System.currentTimeMillis() / 1000, "exp", System.currentTimeMillis() / 1000 + 3600)));
        String unsigned = header + "." + payload;
        javax.crypto.Mac signer = javax.crypto.Mac.getInstance("HmacSHA256");
        signer.init(new javax.crypto.spec.SecretKeySpec(jwtSecret.getBytes(java.nio.charset.StandardCharsets.UTF_8), "HmacSHA256"));
        String legacyToken = unsigned + "." + encoder.encodeToString(signer.doFinal(unsigned.getBytes(java.nio.charset.StandardCharsets.US_ASCII)));

        mvc.perform(get("/api/question/detail/{questionId}", question(actor).getId())
                        .header("X-AUTH-TOKEN", legacyToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.title").value("original"));
    }

    @Test
    @DisplayName("회원이 삭제되면 기존에 발급한 토큰도 401이다")
    void rejectsDeletedMemberToken() throws Exception {
        Member actor = member("security-deleted", List.of("ROLE_USER"));
        String token = token(actor);
        members.deleteById(actor.getId()); memberIds.remove(actor.getId());
        mvc.perform(delete("/api/question/delete").header("X-AUTH-TOKEN", token).param("questionId", "1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("유효한 토큰의 서명을 변조하면 401로 거절한다")
    void rejectsTamperedSignature() throws Exception {
        Member actor = member("security-tampered", List.of("ROLE_USER"));
        String original = token(actor);
        int signatureStart = original.lastIndexOf('.') + 1;
        char replacement = original.charAt(signatureStart) == 'A' ? 'B' : 'A';
        String tampered = original.substring(0, signatureStart) + replacement
                + original.substring(signatureStart + 1);
        mvc.perform(delete("/api/question/delete").header("X-AUTH-TOKEN", tampered)
                        .param("questionId", "1"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value(401));
    }

    @Test
    @DisplayName("관리자 확인은 비로그인 401과 일반 회원 403을 반환한다")
    void adminAccessRequiresStoredRole() throws Exception {
        mvc.perform(get("/api/admin/me")).andExpect(status().isUnauthorized());
        Member user = member("admin-ordinary", List.of("ROLE_USER"));
        mvc.perform(get("/api/admin/me").header("X-AUTH-TOKEN", token(user)))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/me").header("X-AUTH-TOKEN",
                tokens.createToken(user.getId().toString(), List.of("ROLE_ADMIN", "ROLE_USER"))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("관리자는 서버 회원 정보로 확인하고 권한 회수 직후 기존 토큰도 거절한다")
    void adminRevocationTakesEffectImmediately() throws Exception {
        Member admin = member("admin-revoked", new ArrayList<>(List.of("ROLE_USER", "ROLE_ADMIN")));
        String token = token(admin);
        mvc.perform(get("/api/admin/me").header("X-AUTH-TOKEN", token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.username").value(admin.getUsername()))
                .andExpect(jsonPath("$.id").value(admin.getId())).andExpect(jsonPath("$.password").doesNotExist());
        admin.getRoles().remove("ROLE_ADMIN"); members.save(admin);
        mvc.perform(get("/api/admin/me").header("X-AUTH-TOKEN", token)).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("공개 가입에서 요청한 관리자 역할은 저장되지 않는다")
    void signupCannotRequestAdminRole() throws Exception {
        try {
            mvc.perform(post("/api/member/save").param("username", "admin-injection")
                    .param("password", "secret").param("roles", "ROLE_ADMIN"))
                    .andExpect(status().isOk());
            assertThat(members.findByUsername("admin-injection").orElseThrow().getRoles()).containsExactly("ROLE_USER");
        } finally { members.findByUsername("admin-injection").ifPresent(m -> memberIds.add(m.getId())); }
    }

    @ParameterizedTest
    @DisplayName("회원 반응은 저장되고 중복 요청·전환·취소 후 조회에도 반영된다")
    @ValueSource(strings = {"question", "answer"})
    void persistsReactions(String kind) throws Exception {
        Member voter = member("reaction-voter", List.of("ROLE_USER"));
        Member other = member("reaction-other", List.of("ROLE_USER"));
        Question question = question(voter);
        Answer answer = Answer.builder().content("답변").recommend(2).dislike(1).build();
        answer.assignMember(voter); answer.assignQuestion(question); answers.save(answer);
        long id = kind.equals("question") ? question.getId() : answer.getId();
        int baseUp = kind.equals("question") ? 0 : 2;
        int baseDown = kind.equals("question") ? 0 : 1;
        String path = "/api/" + kind + "/" + id + "/reaction";
        mvc.perform(put(path).contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(java.util.Map.of("reaction", "RECOMMEND"))))
                .andExpect(status().isUnauthorized());
        for (int repeat = 0; repeat < 2; repeat++) {
            mvc.perform(put(path).header("X-AUTH-TOKEN", token(voter)).contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsString(java.util.Map.of("reaction", "RECOMMEND", "memberId", other.getId()))))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.recommend").value(baseUp + 1))
                    .andExpect(jsonPath("$.myReaction").value("RECOMMEND"));
        }
        String readPath = kind.equals("question") ? "/api/question/detail/" + id : "/api/answer/" + question.getId();
        String prefix = kind.equals("question") ? "$" : "$.content[0]";
        mvc.perform(get(readPath).header("X-AUTH-TOKEN", token(voter)))
                .andExpect(status().isOk()).andExpect(jsonPath(prefix + ".myReaction").value("RECOMMEND"));
        mvc.perform(get(readPath).header("X-AUTH-TOKEN", token(other)))
                .andExpect(status().isOk()).andExpect(jsonPath(prefix + ".myReaction").value("NONE"));
        mvc.perform(put(path).header("X-AUTH-TOKEN", token(voter)).contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(java.util.Map.of("reaction", "DISLIKE"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.recommend").value(baseUp))
                .andExpect(jsonPath("$.dislike").value(baseDown + 1));
        for (int repeat = 0; repeat < 2; repeat++) {
            mvc.perform(put(path).header("X-AUTH-TOKEN", token(voter)).contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsString(java.util.Map.of("reaction", "NONE"))))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.recommend").value(baseUp))
                    .andExpect(jsonPath("$.dislike").value(baseDown)).andExpect(jsonPath("$.myReaction").value("NONE"));
        }
    }

    private Member member(String username, List<String> roles) {
        Member member = members.save(Member.builder()
                .username(username)
                .password(new BCryptPasswordEncoder().encode("secret"))
                .roles(roles)
                .build());
        memberIds.add(member.getId()); return member;
    }
    private Question question(Member owner) {
        Question question = Question.builder().title("original").content("body").hits(0).recommend(0).dislike(0).build();
        question.assignMember(owner); return questions.save(question);
    }
    private String token(Member member) { return tokens.createToken(member.getId().toString(), member.getRoles()); }
}
