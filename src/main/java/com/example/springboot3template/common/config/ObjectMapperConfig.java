package com.example.springboot3template.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ObjectMapperConfig {
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper()
            .registerModule(new JavaTimeModule()) // Java 8 날짜/시간 지원 추가
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS); // ISO-8601 형식 유지
    }
}
