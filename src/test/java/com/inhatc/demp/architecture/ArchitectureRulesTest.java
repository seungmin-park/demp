package com.inhatc.demp.architecture;

import architecturefixture.BoundarySamples;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ArchitectureRulesTest {
    @Test
    @DisplayName("Controller의 저장소 직접 의존을 거부한다")
    void rejectsControllerRepository() {
        assertThatThrownBy(() -> ArchitectureRules.checkControllers(new ClassFileImporter().importClasses(BoundarySamples.RepositoryController.class)))
                .isInstanceOf(AssertionError.class).hasMessageContaining("RepositoryController");
    }

    @Test
    @DisplayName("Controller의 기존 Service 협력을 허용한다")
    void allowsControllerService() {
        ArchitectureRules.checkControllers(new ClassFileImporter().importClasses(BoundarySamples.ServiceController.class));
    }

    @Test
    @DisplayName("도메인의 저장소 의존을 거부한다")
    void rejectsDomainRepository() {
        assertThatThrownBy(() -> ArchitectureRules.checkDomains(new ClassFileImporter().importClasses(BoundarySamples.RepositoryDomain.class)))
                .isInstanceOf(AssertionError.class).hasMessageContaining("RepositoryDomain");
    }

    @Test
    @DisplayName("도메인의 HTTP 의존을 거부한다")
    void rejectsDomainHttp() {
        assertThatThrownBy(() -> ArchitectureRules.checkDomains(new ClassFileImporter().importClasses(BoundarySamples.HttpDomain.class)))
                .isInstanceOf(AssertionError.class).hasMessageContaining("HttpDomain");
    }

    @Test
    @DisplayName("도메인의 Service 의존을 거부한다")
    void rejectsDomainService() {
        assertThatThrownBy(() -> ArchitectureRules.checkDomains(new ClassFileImporter().importClasses(BoundarySamples.ServiceDomain.class)))
                .isInstanceOf(AssertionError.class).hasMessageContaining("ServiceDomain");
    }

    @Test
    @DisplayName("엔티티의 공개 setter를 거부한다")
    void rejectsEntitySetter() {
        assertThatThrownBy(() -> ArchitectureRules.checkEntityMutation(new ClassFileImporter().importClasses(BoundarySamples.SetterEntity.class)))
                .isInstanceOf(AssertionError.class).hasMessageContaining("setValue");
    }

    @Test
    @DisplayName("엔티티의 의도를 드러내는 변경을 허용한다")
    void allowsIntentionalMutation() {
        ArchitectureRules.checkEntityMutation(new ClassFileImporter().importClasses(BoundarySamples.IntentionalEntity.class));
        ArchitectureRules.checkDomains(new ClassFileImporter().importClasses(BoundarySamples.IntentionalEntity.class));
    }

    @Test
    @DisplayName("Service 통합 테스트의 클래스 트랜잭션을 거부한다")
    void rejectsClassTransaction() {
        assertThatThrownBy(() -> ArchitectureRules.checkServiceTransactions(new ClassFileImporter().importClasses(BoundarySamples.TransactionalServiceTest.class)))
                .isInstanceOf(AssertionError.class).hasMessageContaining("TransactionalServiceTest");
    }

    @Test
    @DisplayName("Service 통합 테스트의 메서드 트랜잭션을 거부한다")
    void rejectsMethodTransaction() {
        assertThatThrownBy(() -> ArchitectureRules.checkServiceTransactions(new ClassFileImporter().importClasses(BoundarySamples.MethodTransactionalServiceTest.class)))
                .isInstanceOf(AssertionError.class).hasMessageContaining("execute");
    }

    @Test
    @DisplayName("테스트 트랜잭션 없이 실제 commit을 검증하는 구조를 허용한다")
    void allowsCommittedServiceTest() {
        ArchitectureRules.checkServiceTransactions(new ClassFileImporter().importClasses(BoundarySamples.CommittedServiceTest.class));
    }

    @Test
    @DisplayName("MVC slice의 실제 저장소 의존을 거부한다")
    void rejectsMvcRepository() {
        assertThatThrownBy(() -> ArchitectureRules.checkMvcPersistence(new ClassFileImporter().importClasses(BoundarySamples.RepositoryMvcTest.class)))
                .isInstanceOf(AssertionError.class).hasMessageContaining("RepositoryMvcTest");
    }

    @Test
    @DisplayName("MVC slice의 Service 협력을 허용한다")
    void allowsMvcService() {
        ArchitectureRules.checkMvcPersistence(new ClassFileImporter().importClasses(BoundarySamples.ServiceMvcTest.class));
    }
}
