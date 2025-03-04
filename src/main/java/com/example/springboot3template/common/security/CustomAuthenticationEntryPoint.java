package com.example.springboot3template.common.security;

import com.example.springboot3template.common.globalException.ErrorRes;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDateTime;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

@Component
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public CustomAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException {
        // 동적으로 timestamp를 추가하여 ErrorRes 객체 생성
        ErrorRes errorResponse = new ErrorRes(
            4010,
            "로그인이 필요합니다.",
            HttpStatus.UNAUTHORIZED,
            401,
            LocalDateTime.now() // 현재 시간 추가
        );

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        // ObjectMapper를 사용하여 JSON 변환 후 응답 반환
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}
