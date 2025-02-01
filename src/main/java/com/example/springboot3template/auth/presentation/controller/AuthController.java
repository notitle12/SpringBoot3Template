package com.example.springboot3template.auth.presentation.controller;

import com.example.springboot3template.auth.application.dto.req.SignUpReq;
import com.example.springboot3template.auth.application.service.AuthService;
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

    @PostMapping("/sign-up")
    public ResponseEntity<?> signUp(@RequestBody SignUpReq req) {
        authService.signUp(req);
        return ResponseEntity.ok().build();
    }

}
