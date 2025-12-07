package com.example.mailbuddy.dto;

import com.example.mailbuddy.entity.TodoItem;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TodoDto {

    private Long id;

    @Pattern(regexp = "^\\d{4}-\\d{2}$", message = "ym은 YYYY-MM 형식이어야 합니다.")
    @NotBlank(message = "해당년도-월은 필수입니다.")
    private String ym;

    @NotBlank(message = "할일 입력은 필수 입니다.")
    private String text;
    private Boolean done;

    public static TodoDto of(TodoItem e) {
        return TodoDto.builder()
                .id(e.getId())
                .ym(e.getYm())
                .text(e.getText())
                .done(e.isDone())
                .build();
    }
}
