package com.example.springboot3template.auth.presentation.controller;

import com.example.springboot3template.auth.application.dto.res.ReadUserInfo;
import com.example.springboot3template.auth.application.service.UserService;
import com.example.springboot3template.common.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/api/v1/user")
@RestController
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // 본인 정보 조회
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/info/me")
    public ResponseEntity<ReadUserInfo> getMyInfo(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        ReadUserInfo myInfo = userService.getMyInfo(userDetails);
        return ResponseEntity.ok(myInfo);  // 현재 인증된 사용자 정보 반환
    }

}
