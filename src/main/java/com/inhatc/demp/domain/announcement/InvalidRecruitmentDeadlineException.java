package com.inhatc.demp.domain.announcement;
public class InvalidRecruitmentDeadlineException extends IllegalArgumentException {
    public InvalidRecruitmentDeadlineException() { super("마감 방식과 마감 일시를 확인해 주세요."); }
}
