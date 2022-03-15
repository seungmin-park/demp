package com.inhatc.demp;

import com.inhatc.demp.domain.Announcement;
import com.inhatc.demp.domain.AnnouncementType;
import com.inhatc.demp.domain.Member;
import com.inhatc.demp.service.AnnouncementService;
import com.inhatc.demp.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;

@Component
@RequiredArgsConstructor
public class InitDb {

    private final MemberService memberService;
    private final AnnouncementService announcementService;

    @PostConstruct
    public void init() {
        initMember();
        initAnnouncement();
    }


    @Transactional
    public void initMember() {
        memberService.join(new Member("testMemberA",20));
        memberService.join(new Member("testMemberB",30));
    }

    @Transactional
    public void initAnnouncement() {
        announcementService.join(
                new Announcement("line 공채","line.png",
                        "React, HTML, JS", "1년~3년","우리 조직은 LINE messenger platform의 다양한 기능을 처리할 수 있는 고성능 server 및 storage platform 개발을 담당합니다.\n" +
                        "전 세계 LINE 사용자의 대용량 messaging 트래픽을 처리할 수 있는 서버 아키텍처를 설계/개발/운영하는 경험을 할 수 있습니다.\n" +
                        "\n" +
                        "[신입 LINER가 되시면 이런 업무를 담당할 수 있습니다]\n" +
                        "대용량 트래픽을 처리할 수 있는 messaging server 개발 및 운영\n" +
                        "고성능/대용량 storage platform 개발\n" +
                        "LINE app 기능을 사내외 서비스에 활용할 수 있도록 하기 위한 연동 platform 개발 및 운영\n" +
                        "LINE data platform의 개발 및 운영\n" +
                        "[신입 LINER에게 바랍니다]\n" +
                        "컴퓨터 과학의 기초 지식(자료 구조/알고리즘/데이터베이스/네트워크/운영 체제 등)을 이해하고 활용할 수 있는 분이면 좋아요.\n" +
                        "Java, Python, C/C++ 등의 프로그래밍 언어에 대한 활용 능력이 있으면 좋아요.\n" +
                        "Big Data, ML, 클라우드 플랫폼에 대한 이해와 관심이 있으면 좋아요.\n" +
                        "[필수는 아니지만 이런 경험이 있으면 좋아요]\n" +
                        "오픈소스 소프트웨어 개발이나 커뮤니티 활동 경험도 있으면 좋아요.\n" +
                        "Hadoop ecosystem, Kubernetes, 클라우드 플랫폼 사용 경험이 있으면 좋아요.\n" +
                        "※ 최종 합격 시, LINE PLUS 법인으로 입사하시게 됩니다. (근무지: 분당구 서현동 분당스퀘어)\n" +
                        "※ Mobile에서는 지원서 작성이 불가합니다.", AnnouncementType.emp));
        announcementService.join(
                new Announcement("line 공채","line.png",
                        "React, HTML, JS", "1년~3년", AnnouncementType.emp));
        announcementService.join(
                new Announcement("line 공채","line.png",
                        "React, HTML, JS", "1년~3년", AnnouncementType.emp));
        announcementService.join(
                new Announcement("line 공채","line.png",
                        "React, HTML, JS", "1년~3년", AnnouncementType.emp));
        announcementService.join(
                new Announcement("line 공채","line.png",
                        "React, HTML, JS", "1년~3년", AnnouncementType.emp));
        announcementService.join(
                new Announcement("우아한테크코스","wootech.jpg",
                        "Java, Spring, JPA", "경력무관", AnnouncementType.edu));
        announcementService.join(
                new Announcement("우아한테크코스","wootech.jpg",
                        "Java, Spring, JPA", "경력무관", AnnouncementType.edu));
        announcementService.join(
                new Announcement("우아한테크코스","wootech.jpg",
                        "Java, Spring, JPA", "경력무관", AnnouncementType.edu));
        announcementService.join(
                new Announcement("우아한테크코스","wootech.jpg",
                        "Java, Spring, JPA", "경력무관", AnnouncementType.edu));
        announcementService.join(
                new Announcement("우아한테크코스","wootech.jpg",
                        "Java, Spring, JPA", "경력무관", AnnouncementType.edu));

    }
}
