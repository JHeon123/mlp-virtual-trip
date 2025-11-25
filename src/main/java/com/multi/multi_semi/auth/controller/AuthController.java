package com.multi.multi_semi.auth.controller;


import com.multi.multi_semi.auth.service.AuthService;
import com.multi.multi_semi.common.ResponseDto;
import com.multi.multi_semi.common.jwt.TokenProvider;
import com.multi.multi_semi.common.jwt.dto.AccessTokenResponseDto;
import com.multi.multi_semi.common.jwt.dto.TokenDto;
import com.multi.multi_semi.common.jwt.service.TokenService;
import com.multi.multi_semi.member.dto.req.MemberReqDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static org.springframework.http.ResponseEntity.ok;

@RestController
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;
    private final TokenService tokenService;
    private final TokenProvider tokenProvider;

    @PostMapping("/auth/signup")
    public ResponseEntity<ResponseDto> signup(@ModelAttribute MemberReqDto memberReqDto) {
        authService.signup(memberReqDto);
        return ok(new ResponseDto(HttpStatus.CREATED, "회원가입 성공", null));
    }

    @PostMapping("/auth/login")
    public ResponseEntity<ResponseDto> login(@ModelAttribute MemberReqDto memberReqDto) {
        log.info(">>>>>>>>>>>>>{}", memberReqDto);

        TokenDto token = authService.login(memberReqDto); // AT, RT가 모두 담겨있음

        // HttpOnly RefreshToken 쿠키 생성
        ResponseCookie refreshTokenCookie = ResponseCookie.from("refreshToken", token.getRefreshToken())
                .maxAge(tokenProvider.getRefreshTokenExpirySeconds()) // 만료 시간 설정
                .path("/")        // 쿠키 경로
                .httpOnly(true)   // httpOnly 설정(js에서 접근 불가)
                .secure(true)     // HTTPS에서만 전송 (localhost에서는 브라우저가 무시하고 전송 허용)
                .sameSite("None") // CORS 환경용. (None + secure=true)
                .build();

        // Access Token은 DTO에 담아 Body로 전송
        AccessTokenResponseDto accessTokenResponseDto = new AccessTokenResponseDto(token.getAccessToken());

        // Set-Cookie 헤더에 Refresh Token 쿠키를, Body에 AccessToken을 담아 보냄
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString())
                .body(new ResponseDto(HttpStatus.CREATED, "로그인 성공", accessTokenResponseDto));
    }

    @PostMapping("/auth/logout")
    public ResponseEntity<ResponseDto> logout(@RequestHeader("Authorization") String accessToKen) {

        // DB에서 RefreshToken 삭제
        tokenService.deleteRefreshToken(accessToKen);

        // 브라우저의 HttpOnly RefreshToken 쿠키 삭제
        ResponseCookie deleteRtCookie = ResponseCookie.from("refreshToken", "")
                .maxAge(0) // 즉시 만료
                .path("/")
                .httpOnly(true)
                .secure(true)
                .sameSite("None")
                .build();

        return ResponseEntity
                .ok()
                .header(HttpHeaders.SET_COOKIE, deleteRtCookie.toString()) // 쿠키 삭제
                .body(new ResponseDto(HttpStatus.OK, "로그아웃 되었습니다!", null));
    }

    @PostMapping("/auth/refresh")
    public ResponseEntity<ResponseDto> refresh(
            @RequestHeader("Authorization") String expiredAccessToken, // 만료된 엑세스 토큰은 헤더로
            @CookieValue("refreshToken") String clientRefreshToken     // 리프레시 토큰은 쿠키로
            ) {
        // 리프레시 토큰 검증 후 엑세스 토큰 재발급
        String newAccessToken = tokenService.refreshAccessToken(expiredAccessToken, clientRefreshToken);

        // 새 엑세스 토큰만 담아서 반환
        AccessTokenResponseDto responseDto = new AccessTokenResponseDto(newAccessToken);

        return ResponseEntity
                .ok()
                .body(new ResponseDto(HttpStatus.OK, "Access Token 갱신 성공", responseDto));
    }

}