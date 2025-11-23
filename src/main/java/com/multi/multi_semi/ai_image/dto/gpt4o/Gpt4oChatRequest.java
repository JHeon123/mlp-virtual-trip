package com.multi.multi_semi.ai_image.dto.gpt4o;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Gpt4oChatRequest {

    private String model;
    private List<Message> messages;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Message {
        private String role;
        private List<Content> content;
    }

    // [핵심 수정] 여기에 NON_NULL을 붙여야 null인 필드가 사라집니다.
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Content {
        private String type; // "text" or "image_url"

        private String text; // 이미지를 보낼 땐 이 필드가 아예 사라져야 함

        @JsonProperty("image_url")
        private ImageUrl imageUrl; // 텍스트를 보낼 땐 이 필드가 아예 사라져야 함
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ImageUrl {
        private String url;
    }
}
