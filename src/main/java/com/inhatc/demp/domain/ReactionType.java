package com.inhatc.demp.domain;

public enum ReactionType {
    NONE, RECOMMEND, DISLIKE;

    public int recommendValue() { return this == RECOMMEND ? 1 : 0; }
    public int dislikeValue() { return this == DISLIKE ? 1 : 0; }
}
