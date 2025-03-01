package com.example.springboot3template.common.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component("customSecurity")
public class CustomSecurityExpression {

    public boolean hasRoleFromHeader(String requiredRole, String roleFromHeader) {
        log.info("Checking role: required={}, fromHeader={}", requiredRole, roleFromHeader);
        return requiredRole.equals(roleFromHeader);
    }
}
