package com.example.springboot3template.common.globalException;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class CustomException extends RuntimeException {
    private final int code;
    private final String message;
    private final HttpStatus httpStatus;
    private final int status;

    public CustomException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
        this.message = errorCode.getMessage();
        this.httpStatus = errorCode.getHttpStatus();
        this.status = errorCode.getHttpStatus().value();
    }
}