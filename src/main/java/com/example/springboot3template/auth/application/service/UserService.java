package com.example.springboot3template.auth.application.service;

import com.example.springboot3template.auth.application.dto.res.ReadUserInfo;
import com.example.springboot3template.auth.domain.entity.User;
import com.example.springboot3template.auth.infrastructure.repository.UserRepository;
import com.example.springboot3template.common.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    // 본인 정보 조회
    @Transactional(readOnly = true)
    public ReadUserInfo getMyInfo(UserDetailsImpl userDetails) {
        Long userId = userDetails.getUser().getUserId();

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("사용자 정보를 찾을 수 없습니다."));

        ReadUserInfo readMyInfo = new ReadUserInfo();
        readMyInfo.setUsername(user.getUsername());

        return readMyInfo;
    }

}
