package com.example.springboot3template.auth.application.service;

import com.example.springboot3template.auth.application.dto.req.LoginReq;
import com.example.springboot3template.auth.application.dto.req.SignUpReq;
import com.example.springboot3template.auth.domain.entity.User;
import com.example.springboot3template.auth.domain.entity.UserRoleEnum;
import com.example.springboot3template.auth.infrastructure.repository.UserRepository;
import com.example.springboot3template.common.globalException.CustomException;
import com.example.springboot3template.common.globalException.ErrorCode;
import com.example.springboot3template.common.security.JwtProvider;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
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

    // 회원가입
    @Transactional
    public void signUp(SignUpReq req) {
        String username = req.getUsername();
        String password = passwordEncoder.encode(req.getPassword());
        UserRoleEnum role  = req.getRole();

        // 회원 중복 확인
        Optional<User> checkUsername = userRepository.findByUsername(username);
        if (checkUsername.isPresent()) {
            throw new CustomException(ErrorCode.DUPLICATE_USERNAME);
        }

        if(role == null) {
            role = UserRoleEnum.USER;
        } else if(role == UserRoleEnum.ADMIN) {
            if(req.getAdminToken() == null || !req.getAdminToken().equals(ADMIN_TOKEN)) {
                throw new CustomException(ErrorCode.INVALID_TOKEN);
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

    // 로그인
    @Transactional
    public void login(LoginReq req, HttpServletResponse res) {
        String username = req.getUsername();
        String password = req.getPassword();

        // 사용자 확인
        User user = userRepository.findByUsername(username).orElseThrow(
            () -> new CustomException(ErrorCode.LOGIN_FAIL_USERNAME)
        );

        // 비밀번호 확인
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new CustomException(ErrorCode.LOGIN_FAIL_PASSWORD);
        }

        // 사용자 권한 가져오기
        UserRoleEnum role = user.getRole();

        // JWT 토큰 생성
        String accessToken = jwtProvider.createAcessToken(username, role);
        // AccessToken 헤더에 추가
        jwtProvider.addAccessTokenToHeader(accessToken, res);

        // JWT 생성 및 쿠키에 저장 후 Response 객체에 추가
        String token = jwtProvider.createRefreshToken(user.getUsername());
        jwtProvider.addRefreshTokenToCookie(token, res);
    }

}
