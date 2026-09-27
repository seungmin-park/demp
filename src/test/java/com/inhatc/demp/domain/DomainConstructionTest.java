package com.inhatc.demp.domain;

import com.inhatc.demp.domain.announcement.*;
import java.lang.reflect.Modifier;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import static org.assertj.core.api.Assertions.assertThat;

class DomainConstructionTest {
    static Stream<Class<?>> persistentTypes() {
        return Stream.of(Member.class, Question.class, Answer.class, Hashtag.class, QuestionHashtag.class,
                Announcement.class, Career.class, Company.class, Description.class, RecruitPeriod.class,
                UploadFile.class, EducationDetails.class, PublicationRevision.class, AnnouncementReport.class);
    }

    @ParameterizedTest
    @DisplayName("도메인은 공개 생성자로 생성 규칙을 우회할 수 없다")
    @MethodSource("persistentTypes")
    void constructorsAreNotPublic(Class<?> type) {
        assertThat(type.getConstructors()).as(type.getSimpleName()).isEmpty();
    }

    @ParameterizedTest
    @DisplayName("JPA 복원을 위한 기본 생성자는 protected로 유지한다")
    @MethodSource("persistentTypes")
    void jpaConstructorIsProtected(Class<?> type) throws NoSuchMethodException {
        assertThat(Modifier.isProtected(type.getDeclaredConstructor().getModifiers()))
                .as(type.getSimpleName()).isTrue();
    }
}
