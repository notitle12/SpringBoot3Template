package com.example.springboot3template.auth.application.service;

import com.example.springboot3template.common.security.JwtProvider;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RedisTemplate<String, String> redisTemplate;

    private final JwtProvider jwtProvider;

    // 리프레시 토큰 저장
    public void saveRefreshToken(String username, String refreshToken, long expirationTimeMillis) {
        String token = jwtProvider.substringToken(refreshToken);
        redisTemplate.opsForValue().set("RT:" + username, token, expirationTimeMillis, TimeUnit.MILLISECONDS);
        log.info("리프레시 토큰 저장 완료 - username={}, refreshToken={}", username, token); // 개발용
//        log.info("리프레시 토큰 저장 완료 - username={}, refreshToken 발급완료", username); // 운영용
    }

    // 리프레시 토큰 조회
    public String getRefreshToken(String username) {
        return redisTemplate.opsForValue().get("RT:" + username);
    }

    // 리프레시 토큰 삭제
    public void deleteRefreshToken(String username) {
        redisTemplate.delete("RT:" + username);
        log.info("리프레시 토큰 삭제 완료 - username={}", username);
    }

    // 로그아웃 시 블랙리스트 처리 (옵션)
    public void addBlackList(String accessToken, long expirationTimeMillis) {
        String key = "BL:" + accessToken;
        redisTemplate.opsForValue().set(key, "logout", expirationTimeMillis, TimeUnit.MILLISECONDS);
        log.info("액세스 토큰 블랙리스트 등록 완료 - key={}, expiresIn={}ms", key, expirationTimeMillis);
    }
    // 블랙리스트 여부 확인 (옵션)
    public boolean isBlackListed(String accessToken) {
        String result = redisTemplate.opsForValue().get(accessToken);
        return "logout".equals(result);
    }
}
