package com.inhatc.demp.dto.reaction;
import com.inhatc.demp.domain.ReactionType;
public record ReactionResponse(int recommend, int dislike, ReactionType myReaction) {}
