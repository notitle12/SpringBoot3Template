package com.example.springboot3template.auth.application.dto.req;

import com.example.springboot3template.auth.domain.entity.UserRoleEnum;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SignUpReq {

    @NotBlank(message = "사용자 이름(username)은 필수 입력값입니다.")
    @Size(min = 4, max = 10, message = "사용자 이름(username)은 4자리 이상 10자리 이하로 입력해야 합니다.")
    @Pattern(regexp = "^[a-zA-Z0-9]+$", message = "사용자 이름(username)은 영어와 숫자만 입력 가능합니다.")
    private String username;

    @NotBlank(message = "비밀번호(password)는 필수 입력값입니다.")
    @Size(min = 4, max = 10, message = "비밀번호(password)는 4자리 이상 10자리 이하로 입력해야 합니다.")
    @Pattern(regexp = "^[a-zA-Z0-9]+$", message = "비밀번호(password)는 영어와 숫자만 입력 가능합니다.")
    private String password;

    private UserRoleEnum role;

    private String adminToken;

}
