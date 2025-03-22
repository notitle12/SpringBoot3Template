package com.example.springboot3template.common.security;

import static com.example.springboot3template.common.security.JwtProvider.AUTHORIZATION_HEADER;
import static com.example.springboot3template.common.security.JwtProvider.REFRESH_TOKEN_COOKIE;
import com.example.springboot3template.auth.domain.entity.User;
import com.example.springboot3template.auth.domain.entity.UserRoleEnum;
import com.example.springboot3template.auth.infrastructure.repository.UserRepository;
import com.example.springboot3template.common.globalException.CustomException;
import com.example.springboot3template.common.globalException.ErrorCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import javax.crypto.SecretKey;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtFilter extends OncePerRequestFilter {

    private final UserDetailsService userDetailsService;

    private final UserRepository userRepository;

    private final JwtProvider jwtProvider;

    @Value("${jwt.secret.key}")
    private String secretKey;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest req, @NonNull HttpServletResponse res, @NonNull FilterChain filterChain)
        throws ServletException, IOException {

        // 요청 url 확인
        String url = req.getRequestURI();

        // 로그인, 회원가입 등의 검증이 필요 없는 url일 경우 필터 제외
        if (isAuthorizationPassRequest(url)) {
            log.info("인증 제외 API 요청: {}", url);
            filterChain.doFilter(req, res);
            return;
        }

        // 헤더에서 어세스 토큰 확인
        String accessToken = getAccessTokenFromHeader(req);
        log.info("요청에서 추출된 토큰: {}", accessToken);

        SecretKey key = getSecretKey();

        // 1. AccessToken 검증
        if (StringUtils.hasText(accessToken) && validateToken(accessToken, key)) {
            authenticateUser(accessToken, key);
            log.info("AccessToken 인증 성공");
        }
        // 2. AccessToken이 없거나 유효하지 않으면 → RefreshToken으로 재발급 시도
        else {
            log.warn("AccessToken이 없거나 만료됨");

            boolean reissued = reissueAccessToken(req, res, key);

            if (!reissued) {
                log.error("RefreshToken으로 AccessToken 재발급 실패");
                throw new AuthenticationException("로그인이 필요합니다.") {};
            }
            log.info("RefreshToken으로 AccessToken 재발급 성공 및 인증 완료");
        }

        filterChain.doFilter(req, res);
    }

    // 토큰 검증
    public boolean validateToken(String token, SecretKey key) {
        if (token == null || token.isEmpty()) {
            log.error("토큰 값이 null 또는 비어있습니다.");
            return false;
        }

        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            log.info("토큰 검증 성공");
            return true;

        } catch (SecurityException | MalformedJwtException | SignatureException e) {
            log.error("유효하지 않은 JWT 서명 또는 포맷 오류: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            log.error("ExpiredJwtException - 만료된 토큰: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.error("UnsupportedJwtException - 지원하지 않는 토큰: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.error("IllegalArgumentException - 잘못된 인자: {}", e.getMessage());
        }
        return false;
    }

    // 헤더에서 어세스 토큰 가져오기
    public String getAccessTokenFromHeader(HttpServletRequest req) {
        String bearerToken = req.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        log.warn("Authorization 헤더가 없거나 형식이 잘못되었습니다.");
        return null;
    }

    // HttpServletRequest 에서 Cookie Value : JWT 가져오기
    public String getRefreshTokenFromCookie(HttpServletRequest req) {
        Cookie[] cookies = req.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                log.info("쿠키 이름: {}, 쿠키 값: {}", cookie.getName(), cookie.getValue());
                if (cookie.getName().equals(REFRESH_TOKEN_COOKIE)) {
                    String cookieValue = cookie.getValue();
                    if (cookieValue != null) {
                        // 그냥 토큰을 그대로 사용합니다. URLDecoder.decode는 필요없음
                        log.info("디코딩된 토큰: {}", cookieValue);  // 디코딩 없이 그대로 사용
                        return cookieValue;
                    } else {
                        log.error("쿠키 값이 null 입니다.");
                    }
                }
            }
        }
        log.warn("Authorization 쿠키가 요청에 포함되지 않았습니다.");
        return null;
    }

    // 토큰에서 사용자 정보 가져오기
    public Claims getUserInfoFromToken(String token, SecretKey key) {
        return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody();
    }

    // 인증 통과 url
    private boolean isAuthorizationPassRequest(String path) {
        return path.startsWith("/api/v1/auth/login") || path.startsWith("/api/v1/auth/sign-up");
    }

    private SecretKey getSecretKey() {
        if (secretKey == null || secretKey.isEmpty()) {
            throw new IllegalStateException("JwtFilter에 secretKey가 null입니다.");
        }
        return Keys.hmacShaKeyFor(Decoders.BASE64URL.decode(secretKey));
    }

    // RefreshToken으로 AccessToken 재발급 시도
    private boolean reissueAccessToken(HttpServletRequest req, HttpServletResponse res, SecretKey key) {
        String refreshToken = getRefreshTokenFromCookie(req);

        if (!StringUtils.hasText(refreshToken) || !validateToken(refreshToken, key)) {
            return false;
        }

        // RefreshToken에서 사용자 정보 추출
        Claims claims = getUserInfoFromToken(refreshToken, key);
        String username = claims.getSubject();

        // 유저 정보 조회 (DB에서)

        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        UserRoleEnum role = user.getRole();

        // 사용자 정보 조회 (DB 조회 or UserDetailsService)
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        if (userDetails == null) {
            log.warn("사용자를 찾을 수 없습니다. username={}", username);
            return false;
        }

        // 새로운 AccessToken 생성
        String newAccessToken = jwtProvider.createAcessToken(username, role);

        // 응답 헤더에 새 AccessToken 추가
        jwtProvider.addAccessTokenToHeader(newAccessToken, res);

        // 인증 처리
        authenticateUser(newAccessToken, key);

        log.info("AccessToken 재발급 및 인증 완료");
        return true;
    }

    private void authenticateUser(String token, SecretKey key) {
        Claims claims = getUserInfoFromToken(token, key);
        String username = claims.getSubject();

        // 유저 정보 로드
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);

        // 인증 객체 생성 및 SecurityContextHolder에 저장
        UsernamePasswordAuthenticationToken authentication =
            new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

        SecurityContextHolder.getContext().setAuthentication(authentication);

        log.info("인증 완료: {}", authentication);
    }

}

