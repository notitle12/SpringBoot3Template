package com.example.springboot3template.common.globalException;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public class ErrorRes {
    private final int code;        // 커스텀 상태 코드 (예: 1000, 2000)
    private final String message;  // 오류 메시지

    public static ErrorRes of(CustomException ex) {
        return new ErrorRes(ex.getCode(), ex.getMessage());
    }
}
