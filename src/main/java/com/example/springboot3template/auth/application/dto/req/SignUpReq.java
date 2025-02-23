package com.example.springboot3template.auth.application.dto.req;

import com.example.springboot3template.auth.domain.entity.UserRoleEnum;
import lombok.Data;

@Data
public class SignUpReq {

    private String username;

    private String password;

    private UserRoleEnum role;

    private String adminToken;

}
