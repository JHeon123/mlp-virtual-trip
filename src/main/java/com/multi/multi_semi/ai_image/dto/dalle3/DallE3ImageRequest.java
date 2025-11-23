package com.multi.multi_semi.ai_image.dto.dalle3;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DallE3ImageRequest {
    private String model;
    private String prompt;
    private String size; // "1024x1024"
}