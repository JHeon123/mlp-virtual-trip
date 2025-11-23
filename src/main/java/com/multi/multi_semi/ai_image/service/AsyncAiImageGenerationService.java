package com.multi.multi_semi.ai_image.service;

import com.multi.multi_semi.ai_image.controller.GenerationStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AsyncAiImageGenerationService {

    // private final OpenAiService openAIService;
    private final WebClientOpenAiService webClientOpenAiService;
    private final AiImageService aiImageService;
    private final Map<String, GenerationStatus> taskResults;

    // 실제 서버(C드라이브)에 이미지 업로드 할 경로
    // "C:/workspace/org_ai_img/"
    @Value("${file.upload-dir}")
    private String uploadDir;

    // 클라이언트가 이미지에 접근할 때 사용하는 URL 접두사
    // "/images/ai/**" 에서 "**" 를 제거
    // 결과: "/images/ai"/
    @Value("#{'${file.resource-handler}'.replace('**', '')}")
    private String webUrlPrefix;

    // 이미지 생성 비동기 처리
    @Async
    public void generateImageAsync(String taskId, byte[] image1Bytes, byte[] image2Bytes, String prompt, String email) {
        try {
            // 1. OpenAI로부터 이미지 생성 (OpenAI가 반환한 웹 URL. 1시간만 유효함)
            String openAiImageUrl = webClientOpenAiService.processFusion(image1Bytes, image2Bytes, prompt);

            // 2. 랜덤한 파일명 생성 (예: abcd.png)
            String newFileName = UUID.randomUUID().toString() + ".png";

            // 3. 파일이 실제 서버(C드라이브)에 저장될 전체 경로 (예: C:/workspace/org_ai_img/abcd.png)
            Path localFilePath = Paths.get(uploadDir, newFileName);

            // 4. 저장경로가 없으면 생성 (C:/workspace/org_ai_img)
            Files.createDirectories(localFilePath.getParent());

            // 5. OpenAI URL(openAiImageUrl)에서 InputStream을 열어 이미지를 다운로드
            // (try-with-resources로 InputStream 자동 close)
            try (InputStream in = new URL(openAiImageUrl).openStream()) {
                // 다운로드한 이미지를 서버(C드라이브)의 이미지 저장소에 저장
                Files.copy(
                        in, // 다운로드한 이미지
                        localFilePath, //서버의 이미지 저장소
                        StandardCopyOption.REPLACE_EXISTING // 이미 파일이 있으면 덮어쓰기
                );
            }

            // 6. 클라이언트가 접근할 수 있는 최종 URL 생성 (예: "/images/ai/abcd.png")
            String webAccessibleUrl = webUrlPrefix + newFileName;

            // 7. 생성한 이미지 DB에 저장
            aiImageService.insertAiImage(email, webAccessibleUrl);

            // 8. 작업 성공 시, DB의 URL 반환
            taskResults.put(taskId, GenerationStatus.success(webAccessibleUrl));

        } catch (Exception e) {
            // 작업 실패 시 결과 저장소에 "FAILED" 상태와 에러 메시지 저장
            taskResults.put(taskId, GenerationStatus.failed(e.getMessage()));
            e.printStackTrace(); // 서버 로그에는 전체 에러 출력
        }
    }

}