package com.inhatc.demp.service.admin;
import com.inhatc.demp.dto.admin.AdminOverview;
import com.inhatc.demp.domain.announcement.AnnouncementType;
import com.inhatc.demp.repository.*;
import com.inhatc.demp.repository.question.QuestionRepository;
import com.inhatc.demp.repository.announcement.AnnouncementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminOverviewService {
    private final AnnouncementRepository announcements;
    private final QuestionRepository questions;
    private final AnswerRepository answers;
    private final MemberRepository members;
    public AdminOverview overview() {
        return new AdminOverview(announcements.countByAnnouncementType(AnnouncementType.EMP),
                announcements.countByAnnouncementType(AnnouncementType.EDU), questions.count(), answers.count(), members.count());
    }
}
