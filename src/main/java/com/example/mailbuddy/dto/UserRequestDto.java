package com.example.mailbuddy.dto;

import com.example.mailbuddy.utils.ValidBirth;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Valid
@Getter
@Setter
public class UserRequestDto {

    @NotBlank
    @Pattern(regexp = "^[a-z0-9]{3,20}$", message = "아이디는 소문자·숫자로 3~20자로 입력하세요.")
    public String username; // 유저 아이디

    @NotBlank
//    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[^A-Za-z0-9])[\\x21-\\x7E]{5,32}$",
//            message = "영문·숫자·특수문자 포함, 공백 제외 5~32자")
    @Pattern(
            regexp = "^[a-zA-Z0-9]{3,32}$",
            message = "비밀번호는 영문·숫자 포함하여 공백 제외 3~32자로 입력하세요.")
    public String password; // 유저 비밀번호

    @NotBlank(message = "생일을 입력해 주세요")
    @ValidBirth
    public String birth;

    @NotBlank
    @Pattern(regexp = "^[가-힣]+$", message = "이름은 공백 없이 한글만 입력 가능합니다.")
    public String name; // 유저 실명

}