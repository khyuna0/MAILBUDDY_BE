package com.example.mailbuddy.dto;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DailyBriefResponse {

    // 해당 날짜에 일정이 있는지 여부
    private boolean hasEvents;

    // 메인 한 줄
    private String mainLine;

    // 서브 한 줄 (null도 가능)
    private String subLine;
}
