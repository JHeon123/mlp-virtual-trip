package com.multi.multi_semi.ai_image.controller;



// 이미지 생성 상태 나타냄
// PENDING <- 생성중
// SUCCESS <- 성공
// FAILED <- 실패
public record GenerationStatus(String status, String imageUrl, String error) {
    public static GenerationStatus pending() {
        return new GenerationStatus("PENDING", null, null);
    }
    public static GenerationStatus success(String imageUrl) {
        return new GenerationStatus("SUCCESS", imageUrl, null);
    }
    public static GenerationStatus failed(String error) {
        return new GenerationStatus("FAILED", null, error);
    }
}
