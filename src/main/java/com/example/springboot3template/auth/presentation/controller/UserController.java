package com.example.springboot3template.auth.presentation.controller;

import com.example.springboot3template.auth.application.dto.res.GetUserInfoRes;
import com.example.springboot3template.auth.application.service.UserService;
import com.example.springboot3template.auth.infrastructure.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RequestMapping("/api/v1/user")
@RestController
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // 본인 정보 조회

    @GetMapping("/info/me")
    public ResponseEntity<GetUserInfoRes> getMyInfo(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        GetUserInfoRes myInfo = userService.getMyInfo(userDetails);
        return ResponseEntity.ok(myInfo);  // 현재 인증된 사용자 정보 반환
    }

    // 특정 사용자의 정보 조회
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/info/{userId}")
    public ResponseEntity<GetUserInfoRes> getUserInfo(@PathVariable Long userId) {
        GetUserInfoRes userInfo = userService.getUserInfo(userId);
        return ResponseEntity.ok(userInfo);
    }
}
