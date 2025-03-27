package com.example.springboot3template.auth.presentation.controller;

import com.example.springboot3template.auth.application.dto.req.LoginReq;
import com.example.springboot3template.auth.application.dto.req.SignUpReq;
import com.example.springboot3template.auth.application.dto.res.TokenRes;
import com.example.springboot3template.auth.application.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/api/v1/auth")
@RestController
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // 회원가입
    @PostMapping("/sign-up")
    public ResponseEntity<?> signUp(@Valid @RequestBody SignUpReq req) {
        authService.signUp(req);
        return ResponseEntity.ok().build();
    }

    // 로그인
    @PostMapping("/login")
    public ResponseEntity<TokenRes> login(@Valid @RequestBody LoginReq req, HttpServletResponse res) {
        TokenRes tokenRes = authService.login(req, res);
        return ResponseEntity.ok().body(tokenRes);
    }

    // 로그아웃
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest req) {
        authService.logout(req);
        return ResponseEntity.ok().build();
    }

    // 엑세스 토큰 재발급
    @PostMapping("/reissue")
    public ResponseEntity<TokenRes> reissueAccessToken(HttpServletRequest request, HttpServletResponse response) {
        TokenRes tokenRes = authService.reissueAccessToken(request, response);
        return ResponseEntity.ok().body(tokenRes);
    }

}
