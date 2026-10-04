package architecturefixture;

import jakarta.persistence.Entity;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.repository.Repository;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RestController;

public final class BoundarySamples {
    private BoundarySamples() { }
    public interface SampleRepository extends Repository<IntentionalEntity, Long> { }
    @Service public static class SampleService { }
    @RestController public static class RepositoryController { SampleRepository repository; }
    @RestController public static class ServiceController { SampleService service; }
    @Entity public static class RepositoryDomain { SampleRepository repository; }
    @Entity public static class HttpDomain { ResponseEntity<String> response; }
    @Entity public static class ServiceDomain { SampleService service; }
    @Entity public static class SetterEntity { public void setValue(String value) { } }
    @Entity public static class IntentionalEntity { public void update(String value) { } }
    @SpringBootTest @Transactional public static class TransactionalServiceTest { SampleService service; }
    @SpringBootTest public static class MethodTransactionalServiceTest {
        SampleService service;
        @Transactional public void execute() { }
    }
    @SpringBootTest public static class CommittedServiceTest { SampleService service; }
    @WebMvcTest public static class RepositoryMvcTest { SampleRepository repository; }
    @WebMvcTest public static class ServiceMvcTest { SampleService service; }
}
