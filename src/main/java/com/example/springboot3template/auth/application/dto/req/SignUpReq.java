package com.example.springboot3template.auth.application.dto.req;

import com.example.springboot3template.auth.domain.entity.User;
import com.example.springboot3template.auth.domain.entity.UserRoleEnum;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SignUpReq {

    private String username;

    private String password;

    private UserRoleEnum role;

    private String adminToken;


}
