package com.multi.multi_semi.ai_image.controller;

import com.multi.multi_semi.ai_image.dto.AiImgDto;
import com.multi.multi_semi.ai_image.service.AiImageService;
import com.multi.multi_semi.ai_image.service.AsyncImageGenerationService;
import com.multi.multi_semi.auth.dto.CustomUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class ImageController {

    // [변경] 기존 OpenAIService 대신 비동기 서비스를 주입받습니다.
    private final AsyncImageGenerationService asyncService;
    private final AiImageService aiImgService;

    // [추가] 결과 저장소를 주입받습니다.
    private final Map<String, GenerationStatus> taskResults;


    /**
     * [신규] 1. 작업 접수 API
     * - 이 API는 작업만 요청받고 "작업 ID"를 즉시 반환합니다.
     * - @ResponseBody: HTML(Thymeleaf)이 아닌 JSON 데이터를 반환합니다.
     */
    @PostMapping("/generate-request")
    public ResponseEntity<?> generateRequest(
            @RequestParam("image1") MultipartFile image1,
            @RequestParam("image2") MultipartFile image2,
            @RequestParam("prompt") String prompt,
            @AuthenticationPrincipal CustomUser customUser) {

        String email = customUser.getEmail();

        try {
            // [해결]
            // @Async를 호출하기 *전에* 메인 스레드에서 파일 데이터를 byte[]로 미리 읽어 둡니다.
            // (이 코드는 이미 올바르게 작성되어 있었습니다)
            byte[] image1Bytes = image1.getBytes();
            byte[] image2Bytes = image2.getBytes();

            // ★★★ [수정] ★★★
            // 1. 컨트롤러가 직접 고유한 작업 ID를 생성합니다.
            String taskId = UUID.randomUUID().toString();

            // 2. @Async 서비스에는 MultipartFile이 아닌, "taskId"와 "안전한 byte[]"를 전달합니다.
            //    (이 메서드는 void를 반환하고 백그라운드에서 실행됩니다.)
            asyncService.generateImageAsync(taskId, image1Bytes, image2Bytes, prompt, email);

            // 3. 컨트롤러는 "작업 ID"만 즉시 클라이언트에게 반환합니다.
            return ResponseEntity.ok(Map.of("taskId", taskId));

        } catch (IOException e) {
            // .getBytes()에서 발생할 수 있는 I/O 오류 처리
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error reading file data.");
        }
    }

    /**
     * [신규] 2. 상태 확인(폴링) API
     * - 클라이언트가 "작업 ID"로 현재 상태를 물어보는 API입니다.
     * - @ResponseBody: HTML(Thymeleaf)이 아닌 JSON 데이터를 반환합니다.
     */
    @GetMapping("/generate-status/{taskId}")
    @ResponseBody
    public ResponseEntity<GenerationStatus> getGenerationStatus(
            @PathVariable("taskId") String taskId) { // 👈 [수정됨] @PathVariable에 이름을 명시합니다.

        // 1. 결과 저장소에서 작업 ID로 현재 상태를 조회
        GenerationStatus status = taskResults.get(taskId);

        // 2. 만약 작업이 완료(SUCCESS 또는 FAILED)되었다면,
        //    메모리 절약을 위해 맵에서 해당 항목을 제거할 수 있습니다. (선택적)
        if (status != null && (status.status().equals("SUCCESS") || status.status().equals("FAILED"))) {
            taskResults.remove(taskId);
        }

        // 3. 현재 상태(GenerationStatus 객체)를 JSON으로 반환
        if (status == null) {
            // (혹시 모를 예외 처리)
            // 아직 @Async 스레드가 맵에 "PENDING"을 넣기 전일 수 있으므로
            // "PENDING" 상태를 반환하는 것이 더 안전할 수 있습니다.
            return ResponseEntity.ok(GenerationStatus.pending());
        }

        return ResponseEntity.ok(status);
    }

    @GetMapping("/ai-images/my")
    @ResponseBody
    public ResponseEntity<List<AiImgDto>> getMyAiImagesData(
            @AuthenticationPrincipal CustomUser customUser
    ) {
        String email = customUser.getEmail();

        List<AiImgDto> imageList;


        imageList = asyncService.getImagesForUser(email);


        return ResponseEntity.ok(imageList);
    }


    @DeleteMapping("/ai-images")
    public ResponseEntity<?> deleteAiImage(
            @RequestBody AiImgDto requestDto, // JSON으로 { "orgUrl": "..." } 받을 예정
            @AuthenticationPrincipal CustomUser customUser
    ) {
        // 로그인한 유저의 이메일 강제 주입 (보안)
        String email = customUser.getEmail();

        aiImgService.deleteImage(email, requestDto.orgUrl());

        return ResponseEntity.ok("Deleted successfully");
    }







}