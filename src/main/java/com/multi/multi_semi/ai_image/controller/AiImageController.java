package com.multi.multi_semi.ai_image.controller;

import com.multi.multi_semi.ai_image.dto.AiImageDto;
import com.multi.multi_semi.ai_image.service.AiImageService;
import com.multi.multi_semi.ai_image.service.AsyncAiImageGenerationService;
import com.multi.multi_semi.auth.dto.CustomUser;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class AiImageController {

    private final AsyncAiImageGenerationService asyncService;
    private final AiImageService aiImgService;
    private final Map<String, GenerationStatus> taskResults; // 이미지 생성 결과 저장소

    @PostMapping("/generate-request")
    public ResponseEntity<?> generateRequest(
            @RequestParam("image1") MultipartFile image1,
            @RequestParam("image2") MultipartFile image2,
            @RequestParam("prompt") String prompt,
            @AuthenticationPrincipal CustomUser customUser) {

        String email = customUser.getEmail();

        try {
            // MultipartFile의 수명은 1번의 HTTP 요청-응답 사이클
            // @Async로 비동기처리하면 컨트롤러가 이미지 생성 완료 전에 응답
            // 따라서 MultipartFile을 byte로 변환하여 저장해야함
            // 1. @Async를 호출하기 전에, 메인 스레드에서 파일 데이터를 byte[]로 저장
            byte[] image1Bytes = image1.getBytes();
            byte[] image2Bytes = image2.getBytes();

            // 2. "작업 ID" 랜덤 생성
            String taskId = UUID.randomUUID().toString();

            // 3. 비동기 이미지 생성 시작
            // (이 메서드는 백그라운드에서 실행)
            asyncService.generateImageAsync(taskId, image1Bytes, image2Bytes, prompt, email);

            // 4. "작업 ID"만 즉시 클라이언트에게 반환
            return ResponseEntity.ok(Map.of("taskId", taskId));

        } catch (IOException e) {
            // .getBytes()에서 발생할 수 있는 I/O 오류 처리
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error reading file data.");
        }
    }


    // 풀링으로 이미지 생성 상태 조회
    @GetMapping("/generate-status/{taskId}")
    public ResponseEntity<GenerationStatus> getGenerationStatus(@PathVariable("taskId") String taskId) {

        // 1. 결과 저장소에서 작업 ID로 현재 상태를 조회
        GenerationStatus status = taskResults.get(taskId);

        // 2. 작업 완료(SUCCESS 또는 FAILED)되었다면, 결과 저장소에서 제거
        if (status != null && (status.status().equals("SUCCESS") || status.status().equals("FAILED"))) {
            taskResults.remove(taskId);
        }

        // 3. 클라이언트가 너무 빨리 풀링하여 아직 map에 "작업 ID"가 없으면 PENDING(대기 중인) 반환
        if (status == null) {
            return ResponseEntity.ok(GenerationStatus.pending());
        }

        // 4. 완료된 상태(SUCCESS, FAILED, PENDING) 반환
        return ResponseEntity.ok(status);
    }

    // 내가 생성한 이미지 조회
    @GetMapping("/ai-images/my")
    public ResponseEntity<List<AiImageDto>> getMyAiImage(@AuthenticationPrincipal CustomUser customUser) {
        String email = customUser.getEmail();

        List<AiImageDto> imageList;
        imageList = aiImgService.findAiImageByEmail(email);

        return ResponseEntity.ok(imageList);
    }

    // 내가 생성한 이미지 중 선택한 이미지 삭제
    @DeleteMapping("/ai-images")
    public ResponseEntity<?> deleteAiImage(@RequestBody AiImageDto requestDto, @AuthenticationPrincipal CustomUser customUser) {
        String email = customUser.getEmail();

        aiImgService.deleteAiImageByAiImageDto(email, requestDto.getOrgUrl());

        return ResponseEntity.ok("Deleted successfully");
    }

    // 내가 생성한 이미지 다운로드
    @GetMapping("/download-image")
    public ResponseEntity<Resource> downloadImageProxy(@RequestParam("url") String imageUrl) {

        try {
            // 1. JavaScript가 보낸 Azure URL로 URL 객체 생성
            URL url = new URL(imageUrl);

            // 2. Spring 서버가 Azure 서버에 연결 (서버와 서버간 통신엔 CORS 없음)
            URLConnection connection = url.openConnection();

            // 3. 이미지 데이터를 InputStream으로 가져옴
            InputStream inputStream = connection.getInputStream();

            // 4. Spring의 Resource 객체로 래핑
            Resource resource = new InputStreamResource(inputStream);

            // 5. 브라우저에게 "이건 화면에 띄우지 말고 다운로드해라"고 명령
            HttpHeaders headers = new HttpHeaders();
            headers.setContentDispositionFormData("attachment", "generated_image.png");
            headers.setContentType(MediaType.IMAGE_PNG); // PNG 이미지라고 명시

            // 6. 200 OK 상태와, 헤더, 이미지 데이터를 함께 반환
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(resource);

        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
    }

}