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
public class OpenAIService {

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

        String base64Img1 = Base64.getEncoder().encodeToString(img1Bytes);
        String base64Img2 = Base64.getEncoder().encodeToString(img2Bytes);

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

        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpPost post = new HttpPost(gpt4oUrl);
            post.setHeader("Authorization", "Bearer " + apiKey);
            post.setHeader("Content-Type", "application/json");
            post.setEntity(new StringEntity(requestBody, StandardCharsets.UTF_8));

            var response = client.execute(post);
            JsonNode json = mapper.readTree(response.getEntity().getContent());

            System.out.println("\n========== GPT-4o API RESPONSE ==========");
            System.out.println(json.toPrettyString());
            System.out.println("========================================\n");

            JsonNode errorNode = json.path("error");
            if (errorNode != null && !errorNode.isMissingNode() && !errorNode.isNull()) {
                throw new IllegalStateException("GPT-4o 요청 실패: " + errorNode.path("message").asText());
            }

            JsonNode outputArray = json.path("output");
            if (!outputArray.isArray() || outputArray.size() == 0) {
                throw new IllegalStateException("GPT-4o 응답 구조가 예상과 다름: " + json.toPrettyString());
            }

            JsonNode textNode = outputArray.get(0).path("content").get(0).path("text");
            if (textNode == null || textNode.isMissingNode()) {
                throw new IllegalStateException("GPT-4o 결과 텍스트를 찾을 수 없음: " + json.toPrettyString());
            }

            String promptText = textNode.asText();
            System.out.println("✅ 생성된 합성 프롬프트:\n" + promptText + "\n");

            return promptText;
        }
    }

    // DALL-E-3 모델로 실제 합성 이미지 생성
    private String generateImageFromPrompt(String finalPrompt) throws Exception {

        String safePrompt = mapper.writeValueAsString(finalPrompt);

        String requestBody = """
        {
          "model": "dall-e-3",
          "prompt": %s,
          "size": "1024x1024"
        }
        """.formatted(safePrompt);

        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpPost post = new HttpPost(imageUrl);
            post.setHeader("Authorization", "Bearer " + apiKey);
            post.setHeader("Content-Type", "application/json");
            post.setEntity(new StringEntity(requestBody, StandardCharsets.UTF_8));

            var response = client.execute(post);
            JsonNode json = mapper.readTree(response.getEntity().getContent());

            System.out.println("\n========== IMAGE API RESPONSE ==========");
            System.out.println(json.toPrettyString());
            System.out.println("========================================\n");

            JsonNode errorNode = json.path("error");
            if (errorNode != null && !errorNode.isMissingNode() && !errorNode.isNull()) {
                String msg = errorNode.path("message").asText();
                if (msg.contains("unable to process your prompt")) {
                    throw new IllegalStateException("⚠️ DALL-E가 프롬프트를 처리할 수 없습니다. 프롬프트 내용을 조금 더 부드럽게 수정해보세요.");
                }
                throw new IllegalStateException("이미지 생성 실패: " + msg);
            }

            JsonNode dataArray = json.path("data");
            if (!dataArray.isArray() || dataArray.size() == 0) {
                throw new IllegalStateException("응답에 이미지 데이터가 없습니다: " + json.toPrettyString());
            }

            // 생성한 이미지 URL 반환
            return dataArray.get(0).path("url").asText();
        }
    }
}