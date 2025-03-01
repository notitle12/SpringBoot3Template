package com.example.springboot3template.common.security;

import static com.example.springboot3template.common.security.JwtProvider.AUTHORIZATION_HEADER;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import javax.crypto.SecretKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtFilter implements Filter {

    @Value("${jwt.secret.key}") // Base64 Encode 한 SecretKey
    private String secretKey;

    private final UserDetailsService userDetailsService;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;
        String url = req.getRequestURI();

        if (isAuthorizationPassRequest(url)) {
            // 회원가입, 로그인 관련 API 는 인증 필요없이 요청 진행
            chain.doFilter(req, res); // 다음 Filter 로 이동
            return;
        }

        // 나머지 API 요청은 인증 처리 진행
        // 토큰 확인
        String token = getTokenFromRequest(req);
        log.info("요청에서 추출된 토큰: {}", token);

        if (StringUtils.hasText(token)) {
            SecretKey key = getSecretKey();

            if (!validateToken(token, key)) {
                throw new IllegalArgumentException("Token Error");
            }

//            Claims info = getUserInfoFromToken(token, key);
//            String username = info.getSubject();
//            log.info("토큰에서 추출된 사용자 이름: {}", username);

            // JWT에서 사용자 정보 추출
            Claims claims = getUserInfoFromToken(token, key);
            String username = claims.getSubject();
            String role = claims.get("auth", String.class);

            log.info("JWT에서 추출된 사용자 정보 - username: {}, role: {}", username, role);

            // 사용자 정보를 HTTP 헤더에 추가
            res.setHeader("X-User-Id", username);
            res.setHeader("X-User-Role", role);

//            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
//            UsernamePasswordAuthenticationToken authentication =
//                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
//            SecurityContextHolder.getContext().setAuthentication(authentication);

//            log.info("인증 완료: {}", SecurityContextHolder.getContext().getAuthentication());
        } else {
            log.warn("토큰이 요청에 포함되지 않았습니다.");
        }
        chain.doFilter(request, response);
    }

    // HttpServletRequest 에서 Cookie Value : JWT 가져오기
    public String getTokenFromRequest(HttpServletRequest req) {
        Cookie[] cookies = req.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                log.info("쿠키 이름: {}, 쿠키 값: {}", cookie.getName(), cookie.getValue());
                if (cookie.getName().equals(AUTHORIZATION_HEADER)) {
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
            log.error("Invalid JWT signature, 유효하지 않는 JWT 서명 입니다.");
        } catch (ExpiredJwtException e) {
            log.error("Expired JWT token, 만료된 JWT token 입니다.");
        } catch (UnsupportedJwtException e) {
            log.error("Unsupported JWT token, 지원되지 않는 JWT 토큰 입니다.");
        } catch (IllegalArgumentException e) {
            log.error("JWT claims is empty, 잘못된 JWT 토큰 입니다.");
        }
        return false;
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
}
