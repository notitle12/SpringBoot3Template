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

    private final RefreshTokenService refreshTokenService;

    private static final long REFRESH_TOKEN_TTL = 14 * 24 * 60 * 60 * 1000L;

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
        String refreshToken = jwtProvider.createRefreshToken(user.getUsername());
        jwtProvider.addRefreshTokenToCookie(refreshToken, res);

        refreshTokenService.saveRefreshToken(username, refreshToken, REFRESH_TOKEN_TTL); // 2주

        // Access Token은 응답 Body로 내려줌
        return new TokenRes(accessToken);
    }

    // 로그아웃
    @Transactional
    public void logout(HttpServletRequest request) {
        String accessToken = jwtFilter.getAccessTokenFromHeader(request);
        String refreshToken = jwtFilter.getRefreshTokenFromCookie(request);

        if (!StringUtils.hasText(accessToken)) {
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }

        // 1. AccessToken -> 블랙리스트로 등록
        long expiration = jwtProvider.getExpiration(accessToken);
        refreshTokenService.addBlackList(accessToken, expiration);

        // 2. RefreshToken 삭제 (key = username 또는 식별자)
        String username = jwtProvider.getUsernameFromToken(refreshToken);
        refreshTokenService.deleteRefreshToken(username);

        log.info("로그아웃 완료 - accessToken 블랙리스트 처리 & refreshToken 삭제");
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
            log.info("토큰이 유효하지 않습니다.");
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }

        // 4. 토큰에서 사용자 정보 추출
        Claims claims = jwtFilter.getUserInfoFromToken(refreshToken, key);
        String username = claims.getSubject();

        // 5. 사용자 조회 (DB에서)
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        UserRoleEnum role = user.getRole();

        // Redis에 저장된 refreshToken과 비교
        String savedRefreshToken = refreshTokenService.getRefreshToken(username);
        if (!refreshToken.equals(savedRefreshToken)) {
            log.warn("탈취된 토큰일 수 있습니다.");
            throw new CustomException(ErrorCode.INVALID_TOKEN); // 탈취 가능성
        }

        // 6. 새로운 액세스 토큰 생성
        String accessToken = jwtProvider.createAcessToken(username, role);

        // 7. 응답 헤더에 새로운 액세스 토큰 추가 (선택)
        jwtProvider.addAccessTokenToHeader(accessToken, response);

        // 8. 기존의 리프레시 토큰 삭제
        refreshTokenService.deleteRefreshToken(username);

        // 9. 새로운 리프레시 토큰 발급
        String newRefreshToken = jwtProvider.createRefreshToken(username);

        // 10. 쿠키에 새로 발급된 리프레시 토큰 저장
        jwtProvider.addRefreshTokenToCookie(newRefreshToken, response);

        // 11. 레디스에 새로 발급된 리프레시 토큰 저장
        refreshTokenService.saveRefreshToken(username, newRefreshToken, REFRESH_TOKEN_TTL);

        log.info("AccessToken 재발급 완료 - username: {}", username);

        return new TokenRes(accessToken);
    }

}
