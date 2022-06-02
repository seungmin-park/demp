package com.inhatc.demp;

import com.inhatc.demp.domain.*;
import com.inhatc.demp.dto.member.MemberSaveForm;
import com.inhatc.demp.repository.AnswerRepository;
import com.inhatc.demp.repository.HashtagRepository;
import com.inhatc.demp.repository.question.QuestionRepository;
import com.inhatc.demp.service.AnnouncementService;
import com.inhatc.demp.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import static com.inhatc.demp.domain.Language.*;

@Component
@RequiredArgsConstructor
public class InitDb {

    private final MemberService memberService;
    private final AnnouncementService announcementService;
    private final QuestionRepository questionRepository;
    private final AnswerRepository answerRepository;
    private final HashtagRepository hashtagRepository;

    private final BCryptPasswordEncoder bCryptPasswordEncoder;

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
        Hashtag java = new Hashtag("java");
        Hashtag jpa = new Hashtag("jpa");
        Hashtag cs = new Hashtag("cs");
        Hashtag thread = new Hashtag("thread");
        MemberSaveForm testMemberA = new MemberSaveForm("testMemberA", "11111");
        MemberSaveForm testMemberB = new MemberSaveForm("testMemberB", "11111");
        Member memberA = testMemberA.toEntity();
        Member memberB = testMemberB.toEntity();

        questionA.settingMember(memberA);
        answerA.settingQuestion(questionA);
        answerA.settingMember(memberB);
        memberService.join(memberA);
        memberService.join(memberB);
        hashtagRepository.save(java);
        hashtagRepository.save(jpa);
        hashtagRepository.save(cs);
        hashtagRepository.save(thread);
        settingQuestionHashtag(questionA, cs);
        settingQuestionHashtag(questionA, thread);
        questionRepository.save(questionA);
        answerRepository.save(answerA);

        questionB.settingMember(memberB);
        answerB.settingQuestion(questionB);
        answerB.settingMember(memberA);
        settingQuestionHashtag(questionB, java);
        settingQuestionHashtag(questionB, jpa);
        questionRepository.save(questionB);
        answerRepository.save(answerB);
    }

    private void settingQuestionHashtag(Question question, Hashtag hashtag) {
        QuestionHashtag questionHashtag = new QuestionHashtag();
        hashtag.addQuestionHashtag(questionHashtag);
        question.addQuestionHashtag(questionHashtag);
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

        for (int i = 1; i <= 10; i++) {
            announcementService.join(new Announcement("2021라인 공채"+i, new HashSet<>(Set.of(HTML, CSS, React)),"frontend","3년 이하","채용 공고에\r\n대한 설명",
                    "https://recruit.linepluscorp.com/lineplus/login/login?annoId=20007660&classId=&jobId=&entTypeCd=001&sysCompanyCd=LP",i*1000, line, lineImage,
                    AnnouncementType.emp,LocalDateTime.of(2021,03,04,0,0), LocalDateTime.of(2021,03,21,0,0)));
        }

        for (int i = 1; i <= 10; i++) {
            announcementService.join(new Announcement("2021 우아한 테크코스"+i,new HashSet<>(Set.of(JAVA, JPA, SPRING)),"server/backend","경력 무관","모집 요강에 대한 설명",
                    "https://woowacourse.github.io/apply.html",0,woowahan,woowahanImage,AnnouncementType.edu,
                    LocalDateTime.of(2021,10,03,0,0),LocalDateTime.of(2021,10,27,0,0)));
        }
    }
}
