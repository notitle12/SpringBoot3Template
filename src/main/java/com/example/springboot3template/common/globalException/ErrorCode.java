package com.example.springboot3template.common.globalException;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorCode {

    // 유저 관련 에러 (1000번대)
    DUPLICATE_USERNAME(1000, "이미 존재하는 아이디입니다.", HttpStatus.CONFLICT), // 409
    INVALID_TOKEN(1001, "유효하지 않은 토큰입니다.", HttpStatus.FORBIDDEN), // 403
    LOGIN_FAIL_USERNAME(1002, "존재하지 않는 아이디 입니다.", HttpStatus.BAD_REQUEST), // 400
    LOGIN_FAIL_PASSWORD(1003, "비밀번호를 틀렸습니다.", HttpStatus.BAD_REQUEST), // 400
    USER_NOT_FOUND(1004, "사용자를 찾을 수 없습니다.", HttpStatus.NOT_FOUND), // 404
    UNAUTHORIZED_ACCESS(1005, "권한이 없습니다.", HttpStatus.FORBIDDEN), // 403
    UNAUTHORIZED(1006, "로그인이 필요합니다.", HttpStatus.UNAUTHORIZED); // 401

    private final int code;
    private final String message;
    private final HttpStatus httpStatus;
}
