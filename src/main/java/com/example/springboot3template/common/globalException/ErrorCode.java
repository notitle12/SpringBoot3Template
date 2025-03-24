package com.example.springboot3template.common.globalException;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorCode {

    // 인증 관련
    UNAUTHORIZED(1001, "인증이 필요합니다.", HttpStatus.UNAUTHORIZED), // 401
    INVALID_TOKEN(1002, "유효하지 않은 토큰입니다.", HttpStatus.UNAUTHORIZED), // 401
    UNAUTHORIZED_ACCESS(1003, "권한이 없습니다.", HttpStatus.FORBIDDEN), // 403

    // 유저 관련 에러 (2000번대)
    DUPLICATE_USERNAME(2001, "이미 존재하는 아이디입니다.", HttpStatus.CONFLICT), // 409
    LOGIN_FAIL_USERNAME(2002, "존재하지 않는 아이디 입니다.", HttpStatus.BAD_REQUEST), // 400
    LOGIN_FAIL_PASSWORD(2003, "비밀번호를 틀렸습니다.", HttpStatus.BAD_REQUEST), // 400
    USER_NOT_FOUND(2004, "사용자를 찾을 수 없습니다.", HttpStatus.NOT_FOUND); // 404


    private final int code;
    private final String message;
    private final HttpStatus httpStatus;
}
