package com.multi.multi_semi.main_list.rating.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class TopRatedPlaceDto {

    // 관광지 PK
    private Long placeNo;

    // 관광지 이름
    private String placeTitle;

    // rate 합계 / 개수 (DB에서 가져옴)
    private Long rateSum;
    private Long rateCount;

    // 관광지 이미지
    private String imgUrl;

    // Service에서 계산해서 세팅해 줄 평균 평점
    private Double avgRate;
}
