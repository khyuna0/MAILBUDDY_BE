package com.example.mailbuddy.dto;

import com.example.mailbuddy.entity.Gmail;
import com.example.mailbuddy.entity.Summary;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)  // 알 수 없는 키가와도 파싱 실패하지 않도록
public class SummaryDto {

    //   엔티티의 notes를 여기로 매핑할 때는 from(...)에서 넣어줌
    private Long id;         // 저장된 Summary PK (엔티티→응답 시)
    private String title;    // 일정 제목

    //JsonAlias -> 모델이 "Date", "date", "Time" 등 막 섞어서 줄 수 있는 상황을 방어
    @JsonAlias({"Date", "date"})
    private String eventDate; // 일정날짜

    @JsonAlias({"time", "Time"})
    private String eventTime; // 일정시간

    private String place;    // 장소
    private String notes;    // 추가 내용
    private String name;     // 발신자 이름 (senderName)
    private String email;    // 발신자 이메일 (senderEmail)

    // Entity -> DTO (클라이언트 응답용)
    public static SummaryDto from(Summary s) {
        return SummaryDto.builder()
                .id(s.getId())
                .title(s.getTitle())
                .eventDate(s.getEventDate() != null ? String.valueOf(s.getEventDate()) : null)
                .eventTime(s.getEventTime() != null ? String.valueOf(s.getEventTime()) : null)
                .place(s.getPlace())
                .notes(s.getNotes())
                .name(s.getSenderName())
                .email(s.getSenderEmail())
                .build();
    }


}
