package com.inhatc.demp.dto.admin;
import java.util.List;
public record AdminPost(Long id, Long questionId, String title, String content, String username, List<String> hashtags) {}
