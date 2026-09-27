package com.inhatc.demp.dto.admin;
import java.util.List;
import org.springframework.data.domain.Page;
public record AdminPage<T>(List<T> content, int number, boolean last, long totalElements) {
    public static <T> AdminPage<T> from(Page<T> page) { return new AdminPage<>(page.getContent(),page.getNumber(),page.isLast(),page.getTotalElements()); }
}
