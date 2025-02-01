package com.example.springboot3template.auth.application.service;

import com.example.springboot3template.auth.application.dto.req.SignUpReq;
import com.example.springboot3template.auth.domain.entity.User;
import com.example.springboot3template.auth.domain.entity.UserRoleEnum;
import com.example.springboot3template.auth.infrastructure.repository.UserRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String ADMIN_TOKEN = "adminToken";

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void signUp(SignUpReq req) {
        String username = req.getUsername();
        String password = passwordEncoder.encode(req.getPassword());
        UserRoleEnum role  = req.getRole();

        // 회원 중복 확인
        Optional<User> checkUsername = userRepository.findByUsername(username);
        if (checkUsername.isPresent()) {
            throw new IllegalArgumentException("중복된 사용자가 존재합니다.");
        }

        if(role == null) {
            role = UserRoleEnum.USER;
        } else if(role == UserRoleEnum.ADMIN) {
            if(req.getAdminToken() == null || !req.getAdminToken().equals(ADMIN_TOKEN)) {
                throw new IllegalArgumentException("유효한 관리자 토큰이 필요합니다.");
            }
        }

        // 사용자 등록
        User user = User.builder()
            .username(username)
            .password(password)
            .role(role)
            .build();

        userRepository.save(user);
    }

}
