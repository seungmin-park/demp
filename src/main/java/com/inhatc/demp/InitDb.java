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
import java.util.ArrayList;
import java.util.Arrays;

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
        Answer answerA = new Answer("질문\\n 답변\\n 테스트",22,11);
        Answer answerB = new Answer("질문\\n 답변\\n 테스트",11,22);
        Question questionA = new Question("접근 제어자가 헷갈려요", "비 전공자라 그런지 CS 부분을 잘 모르겠네요",11,23,1);
        Question questionB = new Question("Java8에서 뭐가 달라진건가요?", "제곧내",110,20,10);
        Member memberA = new Member("testMemberA", 20);
        Member memberB = new Member("testMemberB", 30);

        questionA.settingMember(memberA);
        answerA.settingQuestion(questionA);
        answerA.settingMember(memberB);
        memberService.join(memberA);
        memberService.join(memberB);
        questionRepository.save(questionA);
        answerRepository.save(answerA);

        questionB.settingMember(memberB);
        answerB.settingQuestion(questionB);
        answerB.settingMember(memberA);
        questionRepository.save(questionB);
        answerRepository.save(answerB);
    }

    @Transactional
    public void initAnnouncement() {

        Company line = new Company("LINE");
        Company woowahan = new Company("우아한 형제들");
        UploadFile lineImage = new UploadFile();
        lineImage.setSaveFileName("line.png");
        lineImage.setUploadFileName("line.png");
        UploadFile woowahanImage = new UploadFile();
        woowahanImage.setSaveFileName("wootech.jpg");
        woowahanImage.setUploadFileName("wootech.jpg");


        announcementService.join(new Announcement("2021라인 공채", new ArrayList<>(Arrays.asList("React", "HTML", "CSS")),"frontend","3년 이하","채용 공고에\r\n대한 설명",
                "https://recruit.linepluscorp.com/lineplus/login/login?annoId=20007660&classId=&jobId=&entTypeCd=001&sysCompanyCd=LP",5000, line, lineImage,
                AnnouncementType.emp,LocalDateTime.of(2021,03,04,0,0), LocalDateTime.of(2021,03,21,0,0)));

        announcementService.join(new Announcement("2021 우아한 테크코스",new ArrayList<>(Arrays.asList("Java", "Spring", "JPA")),"server/backend","경력 무관","모집 요강에 대한 설명",
                "https://woowacourse.github.io/apply.html",0,woowahan,woowahanImage,AnnouncementType.edu,
                LocalDateTime.of(2021,10,03,0,0),LocalDateTime.of(2021,10,27,0,0)));
    }
}
