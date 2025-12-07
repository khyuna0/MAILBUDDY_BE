package com.example.mailbuddy.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LoginResponseDto {
    private String username;
    private String name;
    private String birth;
    private String userRole;
    private String token;
}
