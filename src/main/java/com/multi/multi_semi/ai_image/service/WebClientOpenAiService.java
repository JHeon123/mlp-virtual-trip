package com.multi.multi_semi.ai_image.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.multi.multi_semi.ai_image.dto.dalle3.DallE3ImageRequest;
import com.multi.multi_semi.ai_image.dto.dalle3.DallE3ImageResponse;
import com.multi.multi_semi.ai_image.dto.gpt4o.Gpt4oChatRequest;
import com.multi.multi_semi.ai_image.dto.gpt4o.Gpt4oChatResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.Base64;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WebClientOpenAiService {

    @Value("${openai.api-key}")
    private String apiKey;

    @Value("${openai.gpt4o-url}")
    private String gpt4oUrl;

    @Value("${openai.image-url}")
    private String imageUrl;

    private final ObjectMapper mapper;
    private final WebClient webClient;

    // 전체 프로세스
    public String processFusion(byte[] img1Bytes, byte[] img2Bytes, String userPrompt) throws Exception {
        String prompt = createCompositePrompt(img1Bytes, img2Bytes, userPrompt);
        return generateImageFromPrompt(prompt);
    }

    // GPT-4o로 두 이미지를 분석해 합성용 설명 프롬프트 생성
    private String createCompositePrompt(byte[] img1Bytes, byte[] img2Bytes, String userPrompt) throws Exception {

        // gpt-4o에 이미지 전달 위해 base64로 인코딩
        String base64Img1 = Base64.getEncoder().encodeToString(img1Bytes);
        String base64Img2 = Base64.getEncoder().encodeToString(img2Bytes);

        // HTTP요청 바디 생성
        // 기존의 형식에서 input -> messages, type에서 input_url -> image_url로 수정
        // DTO 빌더를 사용하여 요청 객체 생성
        Gpt4oChatRequest requestDto = Gpt4oChatRequest.builder()
                .model("gpt-4o-mini")
                .messages(List.of(
                        Gpt4oChatRequest.Message.builder()
                                .role("user")
                                .content(List.of(
                                        // 텍스트
                                        Gpt4oChatRequest.Content.builder()
                                                .type("text")
                                                .text(String.format("아래 두 이미지를 자연스럽게 조합해서 '%s' 요구사항을 만족하는 DALL-E용 프롬프트를 만들어줘. 배경, 조명, 색감, 구도 등 시각적 요소를 가능한 한 자세히 작성해.", userPrompt))
                                                .build(),
                                        // 이미지 1
                                        Gpt4oChatRequest.Content.builder()
                                                .type("image_url")
                                                .imageUrl(new Gpt4oChatRequest.ImageUrl("data:image/png;base64," + base64Img1))
                                                .build(),
                                        // 이미지 2
                                        Gpt4oChatRequest.Content.builder()
                                                .type("image_url")
                                                .imageUrl(new Gpt4oChatRequest.ImageUrl("data:image/png;base64," + base64Img2))
                                                .build()
                                ))
                                .build()
                ))
                .build();

        try {
            // WebClient를 사용한 HTTP POST 요청 구성 및 실행
            Gpt4oChatResponse response = webClient.post()
                    .uri(gpt4oUrl)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestDto)
                    .retrieve()
                    .bodyToMono(Gpt4oChatResponse.class) // 응답 본문을 JsonNode로 자동 매핑
                    .block(); // 비동기(Mono)를 동기적으로 기다려 결과를 반환


            System.out.println("\n========== GPT-4o API RESPONSE ==========");
            System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(response));
            System.out.println("========================================\n");

            if (response == null || response.getChoices() == null || response.getChoices().isEmpty()) {
                throw new IllegalStateException("GPT-4o 응답이 비어있거나 구조가 올바르지 않습니다.");
            }

            String promptText = response.getChoices().get(0).getMessage().getContent();

            if (promptText == null || promptText.isBlank()) {
                throw new IllegalStateException("GPT-4o 결과 텍스트가 없습니다.");
            }

            System.out.println("생성된 합성 프롬프트:\n" + promptText + "\n");

            // 프롬프트 반환
            return promptText;
        } catch (WebClientResponseException e) {
            // WebClient에서 발생하는 HTTP 상태 코드 오류 처리
            throw new IllegalStateException("GPT-4o API 통신 오류: " + e.getStatusCode() + " - " + e.getResponseBodyAsString(), e);
        }
    }

    // DALL-E-3 모델로 실제 합성 이미지 생성
    private String generateImageFromPrompt(String finalPrompt) throws Exception {

        // HTTP요청 바디 생성
        // DTO 빌더 사용 (자동 이스케이프 처리됨)
        DallE3ImageRequest requestDto = DallE3ImageRequest.builder()
                .model("dall-e-3")
                .prompt(finalPrompt)
                .size("1024x1024")
                .build();

        // WebClient를 사용한 HTTP POST 요청 구성 및 실행
        try {
            DallE3ImageResponse response = webClient.post()
                    .uri(imageUrl)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestDto)
                    .retrieve()
                    .bodyToMono(DallE3ImageResponse.class)
                    .block();

            System.out.println("\n========== IMAGE API RESPONSE ==========");
            System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(response));
            System.out.println("========================================\n");

            if (response == null || response.getData() == null || response.getData().isEmpty()) {
                throw new IllegalStateException("이미지 생성 응답 데이터가 없습니다.");
            }

            return response.getData().get(0).getUrl();
        } catch (WebClientResponseException e) {
            String errorBody = e.getResponseBodyAsString();
            if (errorBody.contains("unable to process your prompt")) {
                throw new IllegalStateException("DALL-E가 프롬프트를 처리할 수 없습니다. (정책 위반 등)", e);
            }
            throw new IllegalStateException("DALL-E API 통신 오류: " + e.getStatusCode() + " - " + errorBody, e);
        }
    }
}
