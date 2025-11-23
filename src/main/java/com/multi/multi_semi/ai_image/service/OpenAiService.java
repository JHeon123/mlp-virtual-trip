package com.multi.multi_semi.ai_image.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class OpenAiService {

    @Value("${openai.api-key}")
    private String apiKey;

    @Value("${openai.gpt4o-url}")
    private String gpt4oUrl;

    @Value("${openai.image-url}")
    private String imageUrl;

    private final ObjectMapper mapper = new ObjectMapper();

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
        String requestBody = """
        {
          "model": "gpt-4o-mini",
          "input": [
            {
              "role": "user",
              "content": [
                {"type": "input_text", "text": "아래 두 이미지를 자연스럽게 조합해서 '%s' 요구사항을 만족하는 구체적 설명 프롬프트를 만들어줘. 이 프롬프트는 dall-e-3 모델에 바로 쓸 수 있도록 시각적 세부 묘사(배경, 구도, 조명 등)를 포함해야 해."},
                {"type": "input_image", "image_url": "data:image/png;base64,%s"},
                {"type": "input_image", "image_url": "data:image/png;base64,%s"}
              ]
            }
          ]
        }
        """.formatted(userPrompt, base64Img1, base64Img2);

        // HTTP 클라이언트 기본설정으로 생성
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            // POST요청 객체 생성
            HttpPost post = new HttpPost(gpt4oUrl);
            post.setHeader("Authorization", "Bearer " + apiKey);
            post.setHeader("Content-Type", "application/json");
            post.setEntity(new StringEntity(requestBody, StandardCharsets.UTF_8));

            // 응답을 읽어 JSON으로 파싱
            var response = client.execute(post);
            JsonNode json = mapper.readTree(response.getEntity().getContent());

            System.out.println("\n========== GPT-4o API RESPONSE ==========");
            System.out.println(json.toPrettyString());
            System.out.println("========================================\n");

            // 응답에 ERROR가 있으면 예외 발생
            JsonNode errorNode = json.path("error");
            if (errorNode != null && !errorNode.isMissingNode() && !errorNode.isNull()) {
                throw new IllegalStateException("GPT-4o 요청 실패: " + errorNode.path("message").asText());
            }

            // 응답에서 output 추출해서 없으면 예외 발생
            JsonNode outputArray = json.path("output");
            if (!outputArray.isArray() || outputArray.size() == 0) {
                throw new IllegalStateException("GPT-4o 응답 구조가 예상과 다름: " + json.toPrettyString());
            }

            // 추출한 output에서 text를 추출하여 비어있으면 예외 발생
            JsonNode textNode = outputArray.get(0).path("content").get(0).path("text");
            if (textNode == null || textNode.isMissingNode()) {
                throw new IllegalStateException("GPT-4o 결과 텍스트를 찾을 수 없음: " + json.toPrettyString());
            }

            // 추출한 text를 프롬프트에 저장
            String promptText = textNode.asText();
            System.out.println("✅ 생성된 합성 프롬프트:\n" + promptText + "\n");

            // 프롬프트 반환
            return promptText;
        }
    }

    // DALL-E-3 모델로 실제 합성 이미지 생성
    private String generateImageFromPrompt(String finalPrompt) throws Exception {

        // GPT-4o가 생성한 프롬프트 문자열을 JSON 값으로 안전하게 변환 (이스케이프 처리 포함)
        String safePrompt = mapper.writeValueAsString(finalPrompt);

        // HTTP요청 바디 생성
        String requestBody = """
        {
          "model": "dall-e-3",
          "prompt": %s,
          "size": "1024x1024"
        }
        """.formatted(safePrompt);

        // HTTP 클라이언트 기본설정으로 생성
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            // POST요청 객체 생성
            HttpPost post = new HttpPost(imageUrl);
            post.setHeader("Authorization", "Bearer " + apiKey);
            post.setHeader("Content-Type", "application/json");
            post.setEntity(new StringEntity(requestBody, StandardCharsets.UTF_8));

            // 응답 받아서 JSON으로 파싱
            var response = client.execute(post);
            JsonNode json = mapper.readTree(response.getEntity().getContent());

            System.out.println("\n========== IMAGE API RESPONSE ==========");
            System.out.println(json.toPrettyString());
            System.out.println("========================================\n");

            // 응답에 ERROR가 있으면 예외 발생
            JsonNode errorNode = json.path("error");
            if (errorNode != null && !errorNode.isMissingNode() && !errorNode.isNull()) {
                String msg = errorNode.path("message").asText();
                if (msg.contains("unable to process your prompt")) {
                    throw new IllegalStateException("⚠️ DALL-E가 프롬프트를 처리할 수 없습니다. 프롬프트 내용을 조금 더 부드럽게 수정해보세요.");
                }
                throw new IllegalStateException("이미지 생성 실패: " + msg);
            }

            // 응답에서 DATA추출해서 비어있으면 예외 발생
            JsonNode dataArray = json.path("data");
            if (!dataArray.isArray() || dataArray.size() == 0) {
                throw new IllegalStateException("응답에 이미지 데이터가 없습니다: " + json.toPrettyString());
            }

            // DATA에서 URL 추출해 반환
            return dataArray.get(0).path("url").asText();
        }
    }
}