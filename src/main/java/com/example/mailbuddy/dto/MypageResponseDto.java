package com.example.mailbuddy.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class MypageResponseDto {
    // password 가 실수로 노출될 위험이 있기 때문에  민감정보 차단 + 응답 스펙 고정용 따로 만드는게 낫다

    private String username; // 아이디
    private String name;     // 이름
    private String birth;    // 생일 (YYYY-MM-DD)
    private String userRole; // 유저Role

    public MypageResponseDto(String username, String name, String birth) {
        this.username = username;
        this.name = name;
        this.birth = birth;
    }
}
