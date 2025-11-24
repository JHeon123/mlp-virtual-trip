package com.multi.multi_semi.member.controller;


import com.multi.multi_semi.auth.dto.CustomUser;
import com.multi.multi_semi.common.ResponseDto;
import com.multi.multi_semi.common.jwt.service.TokenService;
import com.multi.multi_semi.member.dto.req.MemberReqDto;
import com.multi.multi_semi.member.dto.res.MemberResDto;
import com.multi.multi_semi.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;
    private final TokenService tokenService;

    @GetMapping("/members")
    public ResponseEntity<ResponseDto> findMemberByEmail(@AuthenticationPrincipal CustomUser customUser){
        System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>> customUser = " + customUser);
        String email = customUser.getEmail();

        MemberResDto memberResDto = memberService.findMemberByEmail(email);

        return ResponseEntity.ok(new ResponseDto(HttpStatus.OK, "회원조회성공", memberResDto));
    }

    @GetMapping("/members/{no}")
    public ResponseEntity<ResponseDto> findMemberByNo(@PathVariable("no") String no){
        MemberResDto memberResDto = memberService.findMemberByNo(Long.parseLong(no));

        return ResponseEntity.ok(new ResponseDto(HttpStatus.OK, "회원조회성공", memberResDto));
    }

    // 지영님이 만든거. 이거 수정하면 프론트도 수정해야함. review-form.html
    @GetMapping("/members/me")
    public ResponseEntity<?> getCurrentUser(@AuthenticationPrincipal CustomUser customUser) {

        if (customUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "로그인 필요"));
        }

        return ResponseEntity.ok(Map.of("email", customUser.getEmail(), "id", memberService.findMemberByEmail(customUser.getEmail()).getId()));
    }

    @PatchMapping("/members/edit-info")
    public ResponseEntity<ResponseDto> updateMemberInfo(@AuthenticationPrincipal CustomUser customUser, @RequestBody MemberReqDto memberReqDto){
        String email = customUser.getEmail();

        memberService.updateMemberInfo(email, memberReqDto);

        return ResponseEntity.ok(new ResponseDto(HttpStatus.OK, "회원정보 수정 성공", null));
    }

    @DeleteMapping("/members")
    public ResponseEntity<ResponseDto> deleteMember(@AuthenticationPrincipal CustomUser customUser, @RequestHeader("Authorization") String accessToKen){
        String email = customUser.getEmail();

        // DB에서 RT 삭제 및 회원 탈퇴
        tokenService.deleteRefreshToken(accessToKen);
        memberService.deleteMemberByEmail(email);

        // HttpOnly RT 쿠키 삭제 (Max-Age=0)
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
                .body(new ResponseDto(HttpStatus.OK, "회원 탈퇴 및 로그아웃 성공", null));
    }

}
