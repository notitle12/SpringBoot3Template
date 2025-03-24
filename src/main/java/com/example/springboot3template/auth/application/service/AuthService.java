package com.example.springboot3template.auth.application.service;

import com.example.springboot3template.auth.application.dto.req.LoginReq;
import com.example.springboot3template.auth.application.dto.req.SignUpReq;
import com.example.springboot3template.auth.application.dto.res.TokenRes;
import com.example.springboot3template.auth.domain.entity.User;
import com.example.springboot3template.auth.domain.entity.UserRoleEnum;
import com.example.springboot3template.auth.infrastructure.repository.UserRepository;
import com.example.springboot3template.common.globalException.CustomException;
import com.example.springboot3template.common.globalException.ErrorCode;
import com.example.springboot3template.common.security.JwtFilter;
import com.example.springboot3template.common.security.JwtProvider;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Optional;
import javax.crypto.SecretKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final JwtFilter jwtFilter;

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
    public TokenRes login(LoginReq req, HttpServletResponse res) {
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

        // 엑세스 토큰 생성
        String accessToken = jwtProvider.createAcessToken(username, role);

        // 리프레시 토큰 생성 및 쿠키에 저장 후 Response 객체에 추가
        String token = jwtProvider.createRefreshToken(user.getUsername());
        jwtProvider.addRefreshTokenToCookie(token, res);

        // Access Token은 응답 Body로 내려줌
        return new TokenRes(accessToken);
    }

    @Transactional
    public TokenRes reissueAccessToken(HttpServletRequest request, HttpServletResponse response) {

        // 1. SecretKey 생성(디코딩 된 상태)
        SecretKey key = jwtProvider.getSecretKey();

        // 2. 리프레시 토큰 꺼내기 (쿠키에서)
        String refreshToken = jwtFilter.getRefreshTokenFromCookie(request);
        if (!StringUtils.hasText(refreshToken)) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }

        // 3. 토큰 유효성 검증
        boolean isValid = jwtFilter.validateToken(refreshToken, key);
        if (!isValid) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }

        // 4. 토큰에서 사용자 정보 추출
        Claims claims = jwtFilter.getUserInfoFromToken(refreshToken, key);
        String username = claims.getSubject();

        // 5. 사용자 조회 (DB에서)
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        UserRoleEnum role = user.getRole();

        // 6. 새로운 액세스 토큰 생성
        String accessToken = jwtProvider.createAcessToken(username, role);

        // 7. 응답 헤더에 새로운 액세스 토큰 추가 (선택)
        jwtProvider.addAccessTokenToHeader(accessToken, response);

        log.info("AccessToken 재발급 완료 - username: {}", username);

        return new TokenRes(accessToken);
    }

}
