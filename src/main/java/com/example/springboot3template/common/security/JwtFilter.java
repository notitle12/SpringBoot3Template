package com.example.springboot3template.common.security;

import static com.example.springboot3template.common.security.JwtProvider.AUTHORIZATION_HEADER;
import static com.example.springboot3template.common.security.JwtProvider.REFRESH_TOKEN_COOKIE;
import com.example.springboot3template.auth.application.service.RefreshTokenService;
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
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
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

    private final RefreshTokenService refreshTokenService;

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
        log.info("요청에서 추출된 토큰: {}", accessToken); // 개발용
//        log.info("토큰 추출 완료");

        SecretKey key = getSecretKey();

        // 1. AccessToken 검증
        if (StringUtils.hasText(accessToken) && validateToken(accessToken, key)) {

//            if (isBlacklisted(accessToken)) {
//                log.warn("AccessToken이 블랙리스트에 포함되어 있음");
//                throw new CustomException(ErrorCode.INVALID_TOKEN);
//            }
            if (refreshTokenService.isBlackListed(accessToken)) {
                log.warn("블랙리스트 토큰입니다.");
                throw new CustomException(ErrorCode.INVALID_TOKEN);
            }

            authenticateUser(accessToken, key);
            log.info("AccessToken 인증 성공");
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
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        } catch (ExpiredJwtException e) {
            log.error("ExpiredJwtException - 만료된 토큰: {}", e.getMessage());
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        } catch (UnsupportedJwtException e) {
            log.error("UnsupportedJwtException - 지원하지 않는 토큰: {}", e.getMessage());
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        } catch (IllegalArgumentException e) {
            log.error("IllegalArgumentException - 잘못된 인자: {}", e.getMessage());
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }
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

    public String getRefreshTokenFromCookie(HttpServletRequest req) {
        Cookie[] cookies = req.getCookies();
        if (cookies == null || cookies.length == 0) {
            log.warn("요청에 쿠키가 포함되어 있지 않습니다.");
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }

        for (Cookie cookie : cookies) {
            log.info("쿠키 이름: {}, 쿠키 값: {}", cookie.getName(), cookie.getValue()); // 개발용
//            log.info("쿠키 이름: {}", cookie.getName());
            if (REFRESH_TOKEN_COOKIE.equals(cookie.getName())) {
                String cookieValue = cookie.getValue();
                if (StringUtils.hasText(cookieValue)) {
                    log.info("디코딩된 토큰: {}", cookieValue); // 개발용
//                    log.info("토큰 디코딩 완료");
                    return cookieValue;
                } else {
                    log.error("RefreshToken 쿠키는 존재하지만 값이 비어있거나 null입니다.");
                    throw new CustomException(ErrorCode.UNAUTHORIZED);
                }
            }
        }
        log.warn("RefreshToken 쿠키가 요청에 포함되지 않았습니다.");
        throw new CustomException(ErrorCode.UNAUTHORIZED);
    }

    // 토큰에서 사용자 정보 가져오기
    public Claims getUserInfoFromToken(String token, SecretKey key) {
        return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody();
    }

    // 인증 통과 url
    private boolean isAuthorizationPassRequest(String path) {
        return path.startsWith("/api/v1/auth/") || path.startsWith("/api/v1/auth/sign-up");
    }

    // 디코딩 된 시크릿 키 전달
    private SecretKey getSecretKey() {
        if (secretKey == null || secretKey.isEmpty()) {
            throw new IllegalStateException("JwtFilter에 secretKey가 null입니다.");
        }
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretKey));
    }

    // 인증 객체 생성
    private void authenticateUser(String token, SecretKey key) {
        Claims claims = getUserInfoFromToken(token, key);
        String username = claims.getSubject();

        // 유저 정보 로드
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);

        // 인증 객체 생성 및 SecurityContextHolder에 저장
        UsernamePasswordAuthenticationToken authentication =
            new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

        SecurityContextHolder.getContext().setAuthentication(authentication);

        log.info("인증 완료: {}", authentication); // 개발용
//        log.info("인증 완료");
    }

}


