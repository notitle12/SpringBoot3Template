package com.example.springboot3template.auth.application.service;

import com.example.springboot3template.auth.application.dto.req.LoginReq;
import com.example.springboot3template.auth.application.dto.req.SignUpReq;
import com.example.springboot3template.auth.application.dto.res.ReadUserInfo;
import com.example.springboot3template.auth.domain.entity.User;
import com.example.springboot3template.auth.domain.entity.UserRoleEnum;
import com.example.springboot3template.auth.infrastructure.repository.UserRepository;
import com.example.springboot3template.common.security.JwtProvider;
import com.example.springboot3template.common.security.UserDetailsImpl;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final JwtProvider jwtProvider;
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

    @Transactional
    public void login(LoginReq req, HttpServletResponse res) {
        String username = req.getUsername();
        String password = req.getPassword();

        // 사용자 확인
        User user = userRepository.findByUsername(username).orElseThrow(
            () -> new IllegalArgumentException("등록된 사용자가 없습니다.")
        );

        // 비밀번호 확인
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }

        // JWT 생성 및 쿠키에 저장 후 Response 객체에 추가
        String token = jwtProvider.createToken(user.getUsername(), user.getRole());
        jwtProvider.addJwtToCookie(token, res);
    }

//    @Transactional(readOnly = true)
//    public ReadUserInfo readMyInfo(UserDetailsImpl userDetails) {
//        Long userId = userDetails.getUser().getUserId();
//
//        User user = userRepository.findById(userId)
//            .orElseThrow(() -> new RuntimeException("사용자 정보를 찾을 수 없습니다."));
//
//        ReadUserInfo readMyInfo = new ReadUserInfo();
//        readMyInfo.setUsername(user.getUsername());
//
//        return readMyInfo;
//    }

}
