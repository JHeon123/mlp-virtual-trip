package com.multi.multi_semi.ai_image.dao;

import com.multi.multi_semi.ai_image.dto.AiImageDto;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface AiImageMapper {
    void insertAiImage(AiImageDto dto);

    List<AiImageDto> findByEmail(String memEmail);

    void deleteAiImage(AiImageDto dto);
}
