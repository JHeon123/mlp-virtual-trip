package com.multi.multi_semi.member.controller;

import com.multi.multi_semi.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class FrontMemberController {

    private final MemberService memberService;

    @GetMapping("/mypage")
    public String myInfoPage(Model model) {
        model.addAttribute("contentFragment", "mypage/info"); // 조각 경로
        model.addAttribute("activePage", "info"); // 활성화할 사이드바 메뉴
        return "layout/mypage-layout"; // 공통 레이아웃 반환
    }

    @GetMapping("/members/edit-info")
    public String editInfoPage(Model model) {
        model.addAttribute("contentFragment", "mypage/edit-info"); // 조각 경로
        model.addAttribute("activePage", "edit-info"); // 활성화할 사이드바 메뉴
        return "layout/mypage-layout"; // 공통 레이아웃 반환
    }

}
