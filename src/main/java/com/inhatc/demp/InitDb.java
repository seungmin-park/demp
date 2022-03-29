package com.inhatc.demp;

import com.inhatc.demp.domain.*;
import com.inhatc.demp.repository.AnswerRepository;
import com.inhatc.demp.repository.QuestionRepository;
import com.inhatc.demp.service.AnnouncementService;
import com.inhatc.demp.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class InitDb {

    private final MemberService memberService;
    private final AnnouncementService announcementService;
    private final QuestionRepository questionRepository;
    private final AnswerRepository answerRepository;

    @PostConstruct
    public void init() {
        initMember();
        initAnnouncement();
    }


    @Transactional
    public void initMember() {
        Answer answer = new Answer("댓글1번");
        Question question = new Question("질문1번", "질문 내용1번");
        Member memberA = new Member("testMemberA", 20);
        Member memberB = new Member("testMemberB", 30);

        question.settingMember(memberA);
        answer.settingQuestion(question);
        answer.settingMember(memberB);
        memberService.join(memberA);
        memberService.join(memberB);
        questionRepository.save(question);
        answerRepository.save(answer);
    }

    @Transactional
    public void initAnnouncement() {

        Company line = new Company("LINE");
        Company woowahan = new Company("우아한 형제들");

        announcementService.join(new Announcement("line.png",line,"2021 라인 공채", LocalDateTime.of(2021,03,04,0,0),LocalDateTime.of(2021,03,21,0,0),
                "React, HTML, CSS","FRONTEND",5000,"3년 이하","채용 공고에 대한 설명","https://recruit.linepluscorp.com/lineplus/login/login?annoId=20007660&classId=&jobId=&entTypeCd=001&sysCompanyCd=LP",
                AnnouncementType.emp));

        announcementService.join(new Announcement("wootech.jpg",woowahan,"2021 우아한 테크코스", LocalDateTime.of(2021,10,03,0,0),LocalDateTime.of(2021,10,27,0,0),
                "Java, Spring, JPA","BACKEND",0,"경력 무관","모집 요강에 대한 설명","https://woowacourse.github.io/apply.html",
                AnnouncementType.edu));
    }
}
