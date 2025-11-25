package com.multi.multi_semi.auth.controller;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Map;

@Controller
@RequiredArgsConstructor
@Slf4j
public class FrontAuthController {

    @GetMapping("/auth/signup")
    public String signup() {
        return "members/signup";
    }

    @GetMapping("/auth/login")
    public String loginPage() {
        return "members/login";
    }

    // OAuth2 성공 시 리디렉션될 콜백 페이지 반환
    @GetMapping("/oauth-redirect")
    public String oauthCallbackPage() {
        return "common/oauth-callback";
    }

    // 만료된 엑세스토큰으로 서버 접근 시 header.html의 js가 정상작동 되는지 테스트
    @GetMapping("/refresh/test")
    @ResponseBody
    public ResponseEntity<?> refreshTokenTest() {
        return ResponseEntity.ok(Map.of("message", "success"));
    }
}
