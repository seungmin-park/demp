package com.inhatc.demp.config.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inhatc.demp.error.ApiErrors;
import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;

public final class SecurityErrorWriter {
    private SecurityErrorWriter() {}
    public static void write(ObjectMapper mapper, HttpServletRequest request, HttpServletResponse response, HttpStatus status) throws IOException {
        response.setStatus(status.value());
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        mapper.writeValue(response.getWriter(), ApiErrors.body(status, request.getRequestURI()));
    }
}
