package com.inhatc.demp.domain.announcement;

public enum JobPosition {
    BACKEND("백엔드"),
    FRONTEND("프론트엔드"),
    FULLSTACK("풀스택"),
    ANDROID("안드로이드"),
    IOS("IOS"),
    MACHINE_LEARNING("머신러닝"),
    AI("인공지능"),
    DATA_ENGINEER("데이터 엔지니어"),
    MOBILE_GAME("모바일 게임"),
    GAME_CLIENT("게임 클라이언트"),
    GAME_SERVER("게임 서버"),
    SYSTEM_NETWORK("시스템 네트워크"),
    SYSTEM_SOFTWARE("시스템 소프트웨어"),
    DEVOPS("데브옵스"),
    INTERNET_SECURITY("인터넷 보안"),
    EMBEDDED_SOFTWARE("임베디드 소프트웨어"),
    ROBOTIC_MIDDLEWARE("로봇 미들웨어"),
    QA("QA"),
    IOT("사물 인터넷"),
    APPLICATION_PROGRAM("애플리케이션 프로그램"),
    BLOCK_CHAIN("블록 체인"),
    DATA_SCIENTIST("데이터 사이언티스트"), DATA_ANALYST("데이터 분석가"), DATABASE_ADMIN("DBA"),
    CLOUD_ENGINEER("클라우드 엔지니어"), PLATFORM_ENGINEER("플랫폼 엔지니어"), SRE("SRE"), MLOPS("MLOps"),
    WEB_PUBLISHER("웹 퍼블리셔"), GAME_ENGINE("게임 엔진"), AI_RESEARCH("AI 연구");

    private String text;

    JobPosition(String text) {
        this.text = text;
    }
}
