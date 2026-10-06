package com.inhatc.demp.service;

import com.inhatc.demp.DempApplication;
import com.inhatc.demp.dto.member.MemberLoginForm;
import com.inhatc.demp.dto.member.MemberSaveForm;
import com.inhatc.demp.error.ApiException;
import com.inhatc.demp.repository.MemberRepository;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.http.HttpStatus;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest
class SharedLoginDatabaseTest {
    @Autowired MemberService service;
    @Autowired MemberRepository members;
    @Autowired DataSource dataSource;

    @AfterEach
    void cleanUp() {
        members.deleteAll();
    }

    @Test
    @DisplayName("독립된 두 서버 컨텍스트는 동일 DB의 로그인 실패 횟수와 제한을 공유한다")
    void independentApplicationContextsShareLoginProtection() throws Exception {
        String url;
        try (var connection = dataSource.getConnection()) {
            url = connection.getMetaData().getURL();
        }
        service.registerMember(new MemberSaveForm("shared-member", "password"));
        for (int i = 0; i < 4; i++) rejected(service, "wrong", HttpStatus.UNAUTHORIZED);
        new WebApplicationContextRunner()
                .withInitializer(new ConfigDataApplicationContextInitializer())
                .withUserConfiguration(DempApplication.class)
                .withPropertyValues("spring.datasource.url=" + url, "spring.jpa.hibernate.ddl-auto=validate")
                .run(second -> {
                    assertThat(second).hasNotFailed();
                    MemberService otherServer = second.getBean(MemberService.class);
                    assertThat(otherServer).isNotSameAs(service);
                    rejected(otherServer, "wrong", HttpStatus.TOO_MANY_REQUESTS);
                    rejected(service, "password", HttpStatus.TOO_MANY_REQUESTS);
                    rejected(otherServer, "password", HttpStatus.TOO_MANY_REQUESTS);
                });
        rejected(service, "password", HttpStatus.TOO_MANY_REQUESTS);
    }

    private void rejected(MemberService service, String password, HttpStatus expected) {
        var form = new MemberLoginForm(); form.setUsername("shared-member"); form.setPassword(password);
        assertThatThrownBy(() -> service.login(form)).isInstanceOfSatisfying(ApiException.class,
                error -> assertThat(error.getStatus()).isEqualTo(expected));
    }
}
