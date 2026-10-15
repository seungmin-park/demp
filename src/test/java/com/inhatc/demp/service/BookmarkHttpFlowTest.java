package com.inhatc.demp.service;

import com.inhatc.demp.config.jwt.JwtTokenProvider;
import com.inhatc.demp.domain.Member;
import com.inhatc.demp.domain.announcement.*;
import com.inhatc.demp.repository.MemberRepository;
import com.inhatc.demp.repository.announcement.AnnouncementRepository;
import java.net.URI;
import java.net.http.*;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.JsonNode;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class BookmarkHttpFlowTest {
    @LocalServerPort int port;
    @Autowired MemberRepository members;
    @Autowired AnnouncementRepository announcements;
    @Autowired JwtTokenProvider tokens;
    @Autowired ObjectMapper mapper;
    @Autowired JdbcTemplate jdbc;
    private final HttpClient client = HttpClient.newHttpClient();
    @AfterEach void cleanup() {
        if (Boolean.TRUE.equals(jdbc.queryForObject("select count(*) > 0 from information_schema.tables where table_name = 'ANNOUNCEMENT_BOOKMARK'", Boolean.class)))
            jdbc.update("delete from announcement_bookmark");
        announcements.deleteAll(); members.deleteAll();
    }
    private Member member(String name) {
        return members.save(Member.builder().username(name).password("unused-hash").roles(List.of("ROLE_USER")).build());
    }
    private Announcement announcement(String title) {
        return announcements.save(Announcement.builder().title(title).company(Company.builder().name("회사").build())
                .announcementType(AnnouncementType.EMP).jobPosition(JobPosition.FULLSTACK)
                .description(Description.builder().content("본문").languages(java.util.Set.of()).accessUrl("https://example.com/" + title).build())
                .recruitPeriod(RecruitPeriod.builder().startedDate(java.time.LocalDateTime.of(2026,1,1,0,0))
                        .deadLineDate(java.time.LocalDateTime.of(2026,10,26,17,0)).build()).build());
    }
    private HttpResponse<String> request(Member member, String method, String path, Object body) throws Exception {
        var builder = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/member/me/bookmarks" + path))
                .header("Content-Type", "application/json");
        if (member != null) builder.header("X-AUTH-TOKEN", tokens.createToken(member.getId().toString(), member.getRoles()));
        return client.send(builder.method(method, body == null ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body))).build(), HttpResponse.BodyHandlers.ofString());
    }
    private JsonNode ok(HttpResponse<String> response) {
        assertThat(response.statusCode()).as(response.body()).isEqualTo(200);
        return mapper.readTree(response.body());
    }
    private void save(Member member, long id, boolean saved) throws Exception {
        var json = ok(request(member, "PUT", "/" + id, Map.of("saved", saved, "memberId", 999)));
        assertThat(json.path("announcementId").asLong()).isEqualTo(id);
        assertThat(json.path("saved").asBoolean()).isEqualTo(saved);
    }

    @Test
    @DisplayName("관심 공고 저장은 커밋되어 별도 조회에서 유지되고 중복 저장과 취소는 멱등이다")
    void persistsAndCancels() throws Exception {
        var a = member("owner"); var item = announcement("관심공고");
        save(a, item.getId(), true); save(a, item.getId(), true);
        var list = ok(request(a, "GET", "", null));
        assertThat(list.path("content").size()).isEqualTo(1);
        assertThat(list.path("content").get(0).path("announcement").path("title").asText()).isEqualTo("관심공고");
        assertThat(list.path("hasNext").asBoolean()).isFalse();
        assertThat(ok(request(a,"GET","/status?announcementIds=" + item.getId(),null)).path("savedIds").get(0).asLong()).isEqualTo(item.getId());
        save(a,item.getId(),false); save(a,item.getId(),false);
        assertThat(ok(request(a,"GET","",null)).path("content").size()).isZero();
    }
    @Test
    @DisplayName("회원별 관심 공고는 격리되며 다른 회원이 해제해도 소유자의 저장을 유지한다")
    void isolatesMembers() throws Exception {
        var a=member("a"); var other=member("b"); var item=announcement("격리");
        save(a,item.getId(),true); save(other,item.getId(),false);
        assertThat(ok(request(other,"GET","",null)).path("content").size()).isZero();
        assertThat(ok(request(a,"GET","",null)).path("content").size()).isEqualTo(1);
    }
    @Test
    @DisplayName("비공개와 삭제 공고는 정보 없이 이용 불가로 남고 해제할 수 있다")
    void hidesUnavailableContent() throws Exception {
        var a=member("a"); var hidden=announcement("비밀제목"); var deleted=announcement("삭제제목");
        save(a,hidden.getId(),true); save(a,deleted.getId(),true);
        hidden.changePublication(PublicationStatus.DRAFT); announcements.save(hidden); announcements.deleteById(deleted.getId());
        var response=request(a,"GET","",null); var content=ok(response).path("content");
        assertThat(content.size()).isEqualTo(2);
        for (var entry:content) { assertThat(entry.path("available").asBoolean()).isFalse(); assertThat(entry.path("announcement").isNull()).isTrue(); }
        assertThat(response.body()).doesNotContain("비밀제목","삭제제목","https://example.com");
        save(a,hidden.getId(),false); save(a,deleted.getId(),false);
        assertThat(ok(request(a,"GET","",null)).path("content").size()).isZero();
        assertThat(request(a,"PUT","/"+hidden.getId(),Map.of("saved",true)).statusCode()).isEqualTo(404);
    }
    @Test
    @DisplayName("저장순 페이지는 경계에서 누락 중복 없이 내용과 다음 페이지 여부를 반환한다")
    void paginates() throws Exception {
        var a=member("a"); var first=announcement("첫째"); var second=announcement("둘째"); var third=announcement("셋째");
        for (var item:List.of(first,second,third)) save(a,item.getId(),true);
        var page=ok(request(a,"GET","?page=0&size=2",null));
        assertThat(page.path("content").size()).isEqualTo(2); assertThat(page.path("hasNext").asBoolean()).isTrue();
        assertThat(page.path("content").get(0).path("announcementId").asLong()).isEqualTo(third.getId());
        assertThat(page.path("content").get(1).path("announcementId").asLong()).isEqualTo(second.getId());
        var last=ok(request(a,"GET","?page=1&size=2",null));
        assertThat(last.path("content").size()).isEqualTo(1); assertThat(last.path("hasNext").asBoolean()).isFalse();
        assertThat(last.path("content").get(0).path("announcementId").asLong()).isEqualTo(first.getId());
    }
    @Test
    @DisplayName("미인증은 401 잘못된 입력은 400 없는 공고 저장은 404이고 기존 저장을 보존한다")
    void rejectsInvalidRequests() throws Exception {
        var a=member("a"); var item=announcement("유지"); save(a,item.getId(),true);
        assertThat(request(null,"GET","",null).statusCode()).isEqualTo(401);
        assertThat(request(null,"PUT","/"+item.getId(),Map.of("saved",true)).statusCode()).isEqualTo(401);
        assertThat(request(a,"PUT","/"+item.getId(),Map.of()).statusCode()).isEqualTo(400);
        assertThat(request(a,"PUT","/0",Map.of("saved",true)).statusCode()).isEqualTo(400);
        assertThat(request(a,"GET","?size=21",null).statusCode()).isEqualTo(400);
        assertThat(request(a,"GET","?page=-1",null).statusCode()).isEqualTo(400);
        assertThat(request(a,"PUT","/9999999",Map.of("saved",true)).statusCode()).isEqualTo(404);
        assertThat(ok(request(a,"GET","",null)).path("content").size()).isEqualTo(1);
    }
    @Test
    @DisplayName("같은 회원의 동시 저장은 모두 성공하고 저장 항목은 하나만 만든다")
    void concurrentSave() throws Exception {
        var a=member("a"); var item=announcement("동시저장");
        try (var executor=Executors.newFixedThreadPool(4)) {
            var tasks=java.util.stream.IntStream.range(0,8).<Callable<Integer>>mapToObj(i -> () -> request(a,"PUT","/"+item.getId(),Map.of("saved",true)).statusCode()).toList();
            for (var result:executor.invokeAll(tasks)) assertThat(result.get()).isEqualTo(200);
        }
        assertThat(ok(request(a,"GET","",null)).path("content").size()).isEqualTo(1);
    }
    @Test
    @DisplayName("로컬 예제의 음수 공고 ID도 실제 존재하면 저장과 재조회를 지원한다")
    void supportsLocalSeedNamespace() throws Exception {
        var a=member("local-owner");var item=announcement("로컬 예제");
        jdbc.update("update announcement set id = -1 where id = ?",item.getId());
        save(a,-1,true);
        assertThat(ok(request(a,"GET","/status?announcementIds=-1",null)).path("savedIds").get(0).asLong()).isEqualTo(-1);
        save(a,-1,false);
    }

}
