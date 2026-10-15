package com.inhatc.demp.domain.announcement;

/** A publication requires a role and both recruitment boundaries. */
public class IncompletePublicationException extends IllegalArgumentException {
    public IncompletePublicationException() { super("공개·검토 요청에는 분야, 모집 시작과 마감이 필요합니다."); }
}
