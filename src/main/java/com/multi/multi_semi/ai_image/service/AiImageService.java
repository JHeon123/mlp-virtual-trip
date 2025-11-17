package com.multi.multi_semi.ai_image.service;

import com.multi.multi_semi.ai_image.controller.GenerationStatus;
import com.multi.multi_semi.ai_image.dao.AiImgMapper;
import com.multi.multi_semi.ai_image.dto.AiImgDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class AiImageService {

    // 1. 사용자의 원본 OpenAIService (오래 걸리는 작업)
    private final OpenAIService openAIService;

    // 2. AppConfig에서 Bean으로 등록한 결과 저장소
    private final Map<String, GenerationStatus> taskResults;

    // 3. [추가] DB 저장을 위한 Mapper
    private final AiImgMapper aiImgMapper;

    // 4. [추가] yml에서 파일 저장 경로 주입
    // 예: "C:/workspace/org_ai_img/"
    @Value("${file.upload-dir}")
    private String uploadDir;

    // 5. [추가] yml에서 웹 핸들러 경로(접두사) 주입
    // "${file.resource-handler}" (예: "/images/ai/**") 에서 "/**" 를 제거
    // 결과: "/images/ai"
    @Value("#{'${file.resource-handler}'.replace('/**', '')}")
    private String webUrlPrefix;


    public void deleteImage(String email, String webUrl) {
        try {
            // 1. DB에서 먼저 정보 삭제 (혹은 순서 바꿔도 됨)
            // DTO를 재활용해서 파라미터로 넘깁니다.
            AiImgDto dto = new AiImgDto(email, webUrl);
            aiImgMapper.deleteAiImg(dto);

            // 2. 실제 파일 삭제 로직
            // 웹 URL: /images/ai/aaaa-bbbb.png
            // webUrlPrefix: /images/ai (Service 상단에 이미 @Value로 선언되어 있음)

            // URL에서 "파일명"만 추출
            // 예: "/images/ai/aaaa.png" -> "aaaa.png"
            // 혹시 모를 URL 인코딩 처리 (한글 파일명 등 대비)
            String decodedUrl = URLDecoder.decode(webUrl, StandardCharsets.UTF_8);

            // prefix 길이 + 1 (슬래시) 만큼 잘라내기.
            // 만약 webUrlPrefix가 슬래시로 끝나지 않는다면 로직을 맞춰야 합니다.
            // 현재 코드 기준: "/images/ai" + "/" + "파일명" 형태이므로
            String fileName = decodedUrl.replace(webUrlPrefix + "/", "");

            // 3. 실제 파일 경로 생성
            // uploadDir: C:/workspace/org_ai_img/
            Path filePath = Paths.get(uploadDir + fileName);

            // 4. 파일이 존재하면 삭제
            Files.deleteIfExists(filePath);

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("이미지 삭제 중 오류 발생: " + e.getMessage());
        }
    }
}
