package com.inhatc.demp.dto.member;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record NicknameUpdate(@NotBlank @Size(max = 60) String nickname) {}
