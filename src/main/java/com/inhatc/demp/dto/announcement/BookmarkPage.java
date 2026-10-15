package com.inhatc.demp.dto.announcement;
import java.util.List;
public record BookmarkPage(List<BookmarkEntry> content, boolean hasNext, int number) {}
