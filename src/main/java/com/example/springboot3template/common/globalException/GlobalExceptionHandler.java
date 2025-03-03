package com.example.springboot3template.common.globalException;

import java.time.LocalDateTime;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // 커스텀 예외 처리
    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ErrorRes> handleCustomException(CustomException ex) {
        return ResponseEntity.status(ex.getHttpStatus()).body(ErrorRes.of(ex));
    }
    // 특정 예외 처리 (IllegalArgumentException)
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorRes> handleIllegalArgumentException(IllegalArgumentException ex) {
        ErrorRes response = new ErrorRes(4000, ex.getMessage(), HttpStatus.BAD_REQUEST, HttpStatus.BAD_REQUEST.value(), LocalDateTime.now());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    // 일반적인 예외 처리 (NullPointerException 등 포함)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorRes> handleGeneralException(Exception ex) {
        ErrorRes response = new ErrorRes(9999, ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR.value(), LocalDateTime.now());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
