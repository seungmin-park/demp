package com.inhatc.demp.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ArchitectureTest {
    private static final JavaClasses production = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS).importPackages("com.inhatc.demp");
    private static final JavaClasses project = new ClassFileImporter().importPackages("com.inhatc.demp");

    @Test
    @DisplayName("Controller는 저장소에 직접 접근하지 않는다")
    void controllersUseServices() { ArchitectureRules.checkControllers(production); }

    @Test
    @DisplayName("도메인은 HTTP와 Service 및 저장소에 의존하지 않는다")
    void domainsOwnState() { ArchitectureRules.checkDomains(production); }

    @Test
    @DisplayName("엔티티는 공개 setter 대신 의도가 있는 상태 변경을 제공한다")
    void entitiesOwnMutation() { ArchitectureRules.checkEntityMutation(production); }

    @Test
    @DisplayName("Service 통합 테스트는 테스트 트랜잭션으로 commit 누락을 가리지 않는다")
    void serviceTestsObserveCommits() { ArchitectureRules.checkServiceTransactions(project); }

    @Test
    @DisplayName("MVC slice는 실제 저장소를 의존하지 않는다")
    void mvcTestsOwnHttp() { ArchitectureRules.checkMvcPersistence(project); }
}
