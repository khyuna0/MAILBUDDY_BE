package com.example.mailbuddy.dto;

import com.example.mailbuddy.utils.ValidBirth;
import jakarta.validation.GroupSequence;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UpdateProfileRequest {

    // 1. 유효성 검증이 중복 될 때 우선순위를 정하는 방법 (필드 전체에 적용해줘야 함)
    // 검증 그룹 마커 인터페이스 정의
    public interface NotBlankGroup {}
    public interface PatternGroup {}
    public interface ValidBirthGroup {}

    // 그룹 시퀀스 정의
    @GroupSequence({ NotBlankGroup.class, PatternGroup.class, ValidBirthGroup.class })
    public interface ValidationSequence {}

    @NotBlank(message = "이름을 입력해 주세요", groups = NotBlankGroup.class)
    @Pattern(regexp = "^[가-힣]+$", message = "이름은 한글만 입력 가능합니다.", groups = PatternGroup.class)
    private String name;

    @NotNull(message = "생일을 입력해 주세요", groups = NotBlankGroup.class)
    @ValidBirth(message = "생일은 1900-01-01 이후여야 합니다.", groups = ValidBirthGroup.class)
    public String birth;

//    @Pattern(
//            regexp = "^(|(?=.*[A-Za-z])(?=.*\\d)(?=.*[^A-Za-z0-9])[\\x21-\\x7E]{5,32})$",
//            message = "영문·숫자·특수문자 포함, 공백 제외 5~32자")
    @Pattern(
            regexp = "^[a-zA-Z0-9]{3,32}$",
            message = "비밀번호는 공백 제외, 영문·숫자 포함하여 3~32자로 입력하세요.", groups = PatternGroup.class)
    private String password;

}
