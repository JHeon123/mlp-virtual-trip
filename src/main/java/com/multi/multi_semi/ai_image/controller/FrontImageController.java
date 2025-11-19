package com.multi.multi_semi.ai_image.controller;


import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class FrontImageController {

    // "이미지 생성" 화면
    @GetMapping("/generate-request")
    public String aiImagePage() {
        return "/ai-image/generate-ai-image";
    }

    // "내가 생성한 이미지" 화면
    @GetMapping("/ai-images/my")
    public String myAiImagePage(Model model) {
        model.addAttribute("contentFragment", "mypage/my-ai-image"); // 조각 파일 경로 설정
        model.addAttribute("activePage", "my-ai-images"); // 활성화할 레이아웃 설정
        return "layout/mypage-layout";
    }
}
