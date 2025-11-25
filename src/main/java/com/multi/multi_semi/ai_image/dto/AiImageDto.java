package com.multi.multi_semi.ai_image.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AiImageDto{
    private String memEmail;
    private String orgUrl;
}
