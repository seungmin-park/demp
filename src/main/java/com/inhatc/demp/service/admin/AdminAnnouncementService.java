package com.inhatc.demp.service.admin;

import com.inhatc.demp.domain.announcement.*;
import com.inhatc.demp.dto.announcement.AnnouncementUpdateRequest;
import com.inhatc.demp.dto.admin.AdminMutationResult;
import com.inhatc.demp.error.ApiException;
import com.inhatc.demp.repository.announcement.AnnouncementRepository;
import com.inhatc.demp.service.ContentSanitizer;
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
    private final ContentSanitizer sanitizer;
    private final FileStorage files;
    private final PlatformTransactionManager transactions;

    public AdminMutationResult update(long id, AnnouncementUpdateRequest request) throws IOException {
        if (!repository.existsById(id)) throw new ApiException(HttpStatus.NOT_FOUND);
        repository.findByTitle(request.getTitle()).filter(item -> item.getId() != id)
                .ifPresent(item -> { throw new ApiException(HttpStatus.CONFLICT); });
        Career career = new Career(request.getMinCareer(), request.getMaxCareer());
        RecruitPeriod period = new RecruitPeriod(request.getStartedDate(), request.getDeadLineDate());
        Description description = new Description(sanitizer.sanitize(request.getContent()), request.getAccessUrl(),
                request.getPayment(), request.getLanguage());
        UploadFile replacement = request.getImage() == null || request.getImage().isEmpty() ? null : files.save(request.getImage());
        String oldKey;
        try {
            oldKey = new TransactionTemplate(transactions).execute(status -> {
                Announcement item = repository.findByIdForMutation(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND));
                String key = imageKey(item);
                item.revise(request.getTitle(), new Company(request.getCompany()), career, period, description,
                        request.getType(), request.getPosition(), replacement);
                repository.saveAndFlush(item);
                return replacement == null ? null : key;
            });
        } catch (RuntimeException failure) {
            if (replacement != null) {
                try { files.delete(replacement.getSaveFileName()); }
                catch (RuntimeException cleanup) { failure.addSuppressed(cleanup); log.error("관리자 수정 보상 실패 key={}", replacement.getSaveFileName(), cleanup); }
            }
            throw failure;
        }
        return cleanup(oldKey);
    }

    public AdminMutationResult delete(long id) {
        String key = new TransactionTemplate(transactions).execute(status -> {
            Announcement item = repository.findByIdForMutation(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND));
            String oldKey = imageKey(item);
            repository.delete(item);
            return oldKey;
        });
        return cleanup(key);
    }

    private static String imageKey(Announcement item) {
        return item.getImage() == null ? null : item.getImage().getSaveFileName();
    }

    private AdminMutationResult cleanup(String key) {
        if (key == null || key.isBlank()) return new AdminMutationResult(false);
        try { files.delete(key); return new AdminMutationResult(false); }
        catch (RuntimeException failure) {
            log.error("DB 변경 완료, 공고 파일 정리 재시도 필요 key={}", key, failure);
            return new AdminMutationResult(true);
        }
    }
}
