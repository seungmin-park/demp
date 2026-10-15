package com.inhatc.demp.domain.announcement;
public enum DeadlineMode {
    FIXED, UNTIL_FILLED, ALWAYS_OPEN, UNKNOWN;
    public boolean hasNoFixedDeadline() { return this == UNTIL_FILLED || this == ALWAYS_OPEN; }
    public static DeadlineMode resolve(DeadlineMode mode, java.time.LocalDateTime deadline) {
        return mode != null ? mode : deadline == null ? UNKNOWN : FIXED;
    }
}
