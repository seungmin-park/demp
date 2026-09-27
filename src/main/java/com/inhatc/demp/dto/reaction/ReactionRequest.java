package com.inhatc.demp.dto.reaction;
import com.inhatc.demp.domain.ReactionType;
import jakarta.validation.constraints.NotNull;
public record ReactionRequest(@NotNull ReactionType reaction) {}
