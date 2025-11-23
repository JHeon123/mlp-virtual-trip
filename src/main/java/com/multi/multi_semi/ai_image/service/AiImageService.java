package com.multi.multi_semi.ai_image.service;

import com.multi.multi_semi.ai_image.dao.AiImageMapper;
import com.multi.multi_semi.ai_image.dto.AiImageDto;
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
import java.util.List;

@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class AiImageService {

    private final AiImageMapper aiImageMapper;

    // 실제 서버(C드라이브)에 이미지 업로드 할 경로
    // "C:/workspace/org_ai_img/"
    @Value("${file.upload-dir}")
    private String uploadDir;

    // 클라이언트가 이미지에 접근할 때 사용하는 URL 접두사
    // "/images/ai/**" 에서 "**" 를 제거
    // 결과: "/images/ai"/
    @Value("#{'${file.resource-handler}'.replace('**', '')}")
    private String webUrlPrefix;

    public List<AiImageDto> findAiImageByEmail(String email) {
        return aiImageMapper.findByEmail(email);
    }

    public void insertAiImage(String email, String webAccessibleUrl) {
        AiImageDto aiImageDto = new AiImageDto(email, webAccessibleUrl);
        aiImageMapper.insertAiImage(aiImageDto);
    }

    public void deleteAiImageByAiImageDto(String email, String webUrl) {
        try {
            // DB에서 이미지 삭제
            AiImageDto dto = new AiImageDto(email, webUrl);
            aiImageMapper.deleteAiImage(dto);

            // 현재 경로 정리
            // 웹에서 전달한 webUrl: /images/ai/aaaa-bbbb.png
            // webUrlPrefix: /images/ai/

            // 1. webUrl에서 "파일명"만 추출
            // "/images/ai/aaaa.png" -> "aaaa.png"
            // 혹시 모를 URL 인코딩 처리 (한글 파일명 등 대비)
            String decodedUrl = URLDecoder.decode(webUrl, StandardCharsets.UTF_8);

            // 2. webUrlPrefix 길이 만큼 잘라내기
            String fileName = decodedUrl.replace(webUrlPrefix, "");

            // 3. 실제 파일 경로 생성
            // uploadDir: C:/workspace/org_ai_img/
            // filePath : C:/workspace/org_ai_img/aaa.png
            Path filePath = Paths.get(uploadDir + fileName);

            // 4. 파일이 존재하면 삭제
            Files.deleteIfExists(filePath);

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("이미지 삭제 중 오류 발생: " + e.getMessage());
        }
    }
}
