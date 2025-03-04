package com.example.springboot3template.common.globalException;

import java.time.LocalDateTime;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // 커스텀 예외 처리, 사용자가 정의한 예외
    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ErrorRes> handleCustomException(CustomException ex) {
        return ResponseEntity.status(ex.getHttpStatus()).body(ErrorRes.of(ex));
    }

    // 잘못된 요청 예외 처리 (IllegalArgumentException)
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorRes> handleIllegalArgumentException(IllegalArgumentException ex) {
        ErrorRes response = new ErrorRes(
            4000,
            ex.getMessage(),
            HttpStatus.BAD_REQUEST,
            HttpStatus.BAD_REQUEST.value(),
            LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    // 권한이 부족한 경우 발생하는 AccessDeniedException 처리 403
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorRes> handleAccessDeniedException(AccessDeniedException ex) {
        ErrorRes response = new ErrorRes(
            4030,
            "Forbidden: " + ex.getMessage(),
            HttpStatus.FORBIDDEN,
            HttpStatus.FORBIDDEN.value(),
            LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    // NullPointerException, ArrayIndexOutOfBoundsException 등의 예상하지 못한 모든 예외 처리
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorRes> handleGeneralException(Exception ex) {
        ErrorRes response = new ErrorRes(
            9999,
            ex.getMessage(),
            HttpStatus.INTERNAL_SERVER_ERROR,
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
