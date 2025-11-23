package com.multi.multi_semi.ai_image.dto.dalle3;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DallE3ImageResponse {

    private List<ImageData> data;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ImageData {
        private String url;
    }
}
