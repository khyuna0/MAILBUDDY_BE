package com.example.mailbuddy.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

@Valid
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SchedulesRequestDto {

    //    @NotBlank(message = "제목을 입력해 주세요")
    private String title;
    private String notes; //노트 null 허용
    private String place; //장소 null 허용
    private LocalDate eventDate;
    private LocalTime eventTime;
}
