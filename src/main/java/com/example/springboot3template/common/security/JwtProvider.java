package com.example.springboot3template.common.security;

import com.example.springboot3template.auth.domain.entity.UserRoleEnum;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import java.security.Key;
import java.util.Date;
import javax.crypto.SecretKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
public class JwtProvider {
    // Header KEY 값
    public static final String AUTHORIZATION_HEADER = "Authorization";
    // Cookie
    public static final String REFRESH_TOKEN_COOKIE = "RefreshToken";
    // 사용자 권한 값의 KEY
    public static final String AUTHORIZATION_KEY = "auth";
    // Token 식별자
    public static final String BEARER_PREFIX = "Bearer ";
    // 토큰 만료시간
//    private final long ACCESS_TOKEN_TIME = 5 * 60 * 1000L; // 5분
    private final long ACCESS_TOKEN_TIME = 1 * 60 * 1000L; // 5분
    private final long REFRESH_TOKEN_TIME = 14 * 24 * 60 * 60 * 1000L; // 2주


    @Value("${jwt.secret.key}") // Base64 Encode 한 SecretKey
    private String secretKey;
    private Key key;
    private final SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.HS256;

    @PostConstruct
    public void init() {
        if (secretKey == null || secretKey.isEmpty()) {
            throw new IllegalStateException("JwtProvider에 secretKey가 null입니다.");
        }
        // JwtProvider
        byte[] bytes = Decoders.BASE64.decode(secretKey);
        key = Keys.hmacShaKeyFor(bytes);

    }

    // 토큰 생성
    public String createAcessToken(String username, UserRoleEnum role) {
        Date date = new Date();
        String auth = role.getAuthority();
        log.info("AccessToken 생성: username={}, role={}", username, auth);

        return BEARER_PREFIX +
            Jwts.builder()
                .setSubject(username) // 사용자 식별자값(ID)
                .claim(AUTHORIZATION_KEY, auth) // 사용자 권한
                .setIssuedAt(date) // 발급일
                .setExpiration(new Date(date.getTime() + ACCESS_TOKEN_TIME)) // 만료 시간
                .signWith(key, signatureAlgorithm) // 암호화 알고리즘
                .compact();
    }

    public String createRefreshToken(String username) {
        Date now = new Date();
        log.info("RefreshToken 생성: username={}", username);

        return BEARER_PREFIX +
            Jwts.builder()
                .setSubject(username)
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + REFRESH_TOKEN_TIME))
                .signWith(key, signatureAlgorithm)
                .compact();
    }

    // 어세스 토큰 헤더에 추가
    public void addAccessTokenToHeader(String accessToken, HttpServletResponse res) {

        res.setHeader(AUTHORIZATION_HEADER, accessToken);
        log.info("AccessToken이 응답 헤더에 추가되었습니다.");
    }

    // JWT Cookie 에 저장
    public void addRefreshTokenToCookie(String refreshToken, HttpServletResponse res) {

        String token = substringToken(refreshToken);

        Cookie cookie = new Cookie(REFRESH_TOKEN_COOKIE, token); // Name-Value
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setMaxAge(14 * 24 * 60 * 60); // 2주

        // Response 객체에 Cookie 추가
        res.addCookie(cookie);
        log.info("쿠키가 response에 추가되었습니다.");
    }

    // JWT 토큰 substring
    public String substringToken(String tokenValue) {
        if (StringUtils.hasText(tokenValue) && tokenValue.startsWith(BEARER_PREFIX)) {
            return tokenValue.substring(7);
        }
        log.error("Not Found Token");
        throw new NullPointerException("Not Found Token");
    }

    public SecretKey getSecretKey() {
        return (SecretKey) this.key;
    }

}


