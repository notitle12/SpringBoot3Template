package com.example.springboot3template.auth.application.dto.res;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TokenRes {

    private String accessToken;

}
