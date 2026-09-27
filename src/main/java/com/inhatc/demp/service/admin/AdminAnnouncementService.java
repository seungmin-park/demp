package com.inhatc.demp.service.admin;

import com.inhatc.demp.domain.announcement.*;
import com.inhatc.demp.dto.announcement.AnnouncementUpdateRequest;
import com.inhatc.demp.dto.admin.AdminMutationResult;
import com.inhatc.demp.error.ApiException;
import com.inhatc.demp.repository.announcement.AnnouncementRepository;
import com.inhatc.demp.service.AnnouncementBodyImages;
import java.util.List;
import java.util.ArrayList;
import com.inhatc.demp.service.FileStorage;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public class AdminAnnouncementService {
    private final AnnouncementRepository repository;
    private final AnnouncementBodyImages bodyImages;
    private final java.time.Clock clock;
    private final FileStorage files;
    private final PlatformTransactionManager transactions;

    public AdminMutationResult update(long id, AnnouncementUpdateRequest request) throws IOException {
        return update(id, request, "system");
    }

    public AdminMutationResult update(long id, AnnouncementUpdateRequest request, String actor) throws IOException {
        if (!repository.existsById(id)) throw new ApiException(HttpStatus.NOT_FOUND);
        repository.findByTitle(request.getTitle()).filter(item -> item.getId() != id)
                .ifPresent(item -> { throw new ApiException(HttpStatus.CONFLICT); });
        Career career = new Career(request.getMinCareer(), request.getMaxCareer());
        RecruitPeriod period = new RecruitPeriod(request.getStartedDate(), request.getDeadLineDate());
        UploadFile replacement = request.getImage() == null || request.getImage().isEmpty() ? null : files.save(request.getImage());
        List<UploadFile> uploaded = List.of();
        List<String> oldKeys;
        try {
            uploaded = bodyImages.upload(request.getBodyImages());
            var staged = uploaded;
            oldKeys = new TransactionTemplate(transactions).execute(status -> {
                Announcement item = repository.findByIdForMutation(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND));
                List<String> keys = new ArrayList<>();
                if (replacement != null && imageKey(item) != null) keys.add(imageKey(item));
                var body = bodyImages.prepare(request.getContent(), item.getDescription().getContent(), item.getBodyImages(), staged);
                item.getBodyImages().stream().filter(file -> !body.images().contains(file))
                        .forEach(file -> keys.add(file.getSaveFileName()));
                Description description = request.toDescription(body.html());
                item.replaceBodyImages(body.images());
                item.revise(request.getTitle(), new Company(request.getCompany()), career, period, description,
                        request.getType(), request.getPosition(), replacement);
                item.changeEducation(request.toEducationDetails());
                item.changePublication(request.getPublicationStatus());
                item.recordPublication(request.getSourceName(), request.getSourceIdentifier(), request.getApplicationUrl(),
                        request.isSourceVerified(), actor, java.time.LocalDateTime.now(clock));
                repository.saveAndFlush(item);
                return keys;
            });
        } catch (IOException | RuntimeException failure) {
            bodyImages.compensate(uploaded, failure);
            if (replacement != null) {
                try { files.delete(replacement.getSaveFileName()); }
                catch (RuntimeException cleanup) { failure.addSuppressed(cleanup); log.error("관리자 수정 보상 실패 key={}", replacement.getSaveFileName(), cleanup); }
            }
            throw failure;
        }
        return cleanup(oldKeys);
    }

    public AdminMutationResult delete(long id) {
        List<String> keys = new TransactionTemplate(transactions).execute(status -> {
            Announcement item = repository.findByIdForMutation(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND));
            List<String> oldKeys = new ArrayList<>();
            if (imageKey(item) != null) oldKeys.add(imageKey(item));
            item.getBodyImages().forEach(file -> oldKeys.add(file.getSaveFileName()));
            repository.delete(item);
            return oldKeys;
        });
        return cleanup(keys);
    }

    private static String imageKey(Announcement item) {
        return item.getImage() == null ? null : item.getImage().getSaveFileName();
    }

    private AdminMutationResult cleanup(List<String> keys) {
        boolean pending = false;
        for (String key : keys) {
            if (key == null || key.isBlank()) continue;
            try { files.delete(key); }
            catch (RuntimeException failure) {
                log.error("DB 변경 완료, 공고 파일 정리 재시도 필요 key={}", key, failure);
                pending = true;
            }
        }
        return new AdminMutationResult(pending);
    }
}
