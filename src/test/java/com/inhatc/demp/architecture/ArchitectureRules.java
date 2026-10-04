package com.inhatc.demp.architecture;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import jakarta.persistence.Entity;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.repository.Repository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RestController;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

final class ArchitectureRules {
    private ArchitectureRules() { }
    static void checkControllers(JavaClasses imported) {
        noClasses().that().areAnnotatedWith(RestController.class)
                .should().dependOnClassesThat(DescribedPredicate.describe("persistence access", ArchitectureRules::isPersistence))
                .because("controllers delegate storage coordination to services").check(imported);
    }

    static void checkDomains(JavaClasses imported) {
        noClasses().that(DescribedPredicate.describe("domain objects", candidate -> candidate.isAnnotatedWith(Entity.class)
                        || candidate.getPackageName().startsWith("com.inhatc.demp.domain")))
                .should().dependOnClassesThat(DescribedPredicate.describe("HTTP, services or persistence access", target ->
                        isPersistence(target) || target.isAnnotatedWith(Service.class)
                        || target.getPackageName().startsWith("com.inhatc.demp.service")
                        || target.getPackageName().startsWith("com.inhatc.demp.controller")
                        || target.getPackageName().startsWith("com.inhatc.demp.dto")
                        || target.getPackageName().startsWith("org.springframework.web")
                        || target.getPackageName().startsWith("org.springframework.http")))
                .because("domain objects own invariants without HTTP or I/O coordination").check(imported);
    }

    static void checkEntityMutation(JavaClasses imported) {
        methods().that().areDeclaredInClassesThat().areAnnotatedWith(Entity.class).and().arePublic()
                .should().haveNameNotMatching("set[A-Z].*")
                .because("entities expose intentional state changes").check(imported);
    }

    static void checkServiceTransactions(JavaClasses imported) {
        classes().that(DescribedPredicate.describe("integration tests using services", candidate ->
                        candidate.isAnnotatedWith(SpringBootTest.class) && candidate.getDirectDependenciesFromSelf().stream()
                                .anyMatch(dependency -> dependency.getTargetClass().isAnnotatedWith(Service.class))))
                .should(new ArchCondition<>("observe production commits without test transactions") {
                    @Override
                    public void check(JavaClass serviceTest, ConditionEvents events) {
                        if (hasTransaction(serviceTest)) {
                            events.add(SimpleConditionEvent.violated(serviceTest, serviceTest.getName() + " has a test transaction"));
                        }
                        for (JavaMethod method : serviceTest.getAllMethods()) {
                            if (method.isAnnotatedWith(Transactional.class) || method.isAnnotatedWith("jakarta.transaction.Transactional")) {
                                events.add(SimpleConditionEvent.violated(method, method.getFullName() + " has a test transaction"));
                            }
                        }
                    }
                }).check(imported);
    }

    static void checkMvcPersistence(JavaClasses imported) {
        noClasses().that().areAnnotatedWith(WebMvcTest.class)
                .should().dependOnClassesThat(DescribedPredicate.describe("persistence access", ArchitectureRules::isPersistence))
                .because("MVC slices substitute storage and verify HTTP contracts").check(imported);
    }

    private static boolean isPersistence(JavaClass target) {
        return target.isAssignableTo(Repository.class) || target.isAssignableTo("jakarta.persistence.EntityManager");
    }

    private static boolean hasTransaction(JavaClass target) {
        return target.isAnnotatedWith(Transactional.class) || target.isAnnotatedWith("jakarta.transaction.Transactional");
    }
}
