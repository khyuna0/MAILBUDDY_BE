package com.example.mailbuddy.entity;

import com.example.mailbuddy.config.JasyptEncryptConverter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 유저 고유 키

    @Column(unique = true)
    private String username; // 유저 아이디

    @JsonIgnore // user 엔티티를 불러오는 경우 비밀번호 노출 시키지 않기 위함
    private String password; // 유저 비밀번호

    private String name; // 유저 이름

    private String birth; // 유저 생일

    @Column(unique = true)
    private String googleEmail; // 구글 이메일 연동

    // 구글 OAuth2 액세스 토큰 (암호화 + TEXT 컬럼)
    @JsonIgnore
    @Convert(converter = JasyptEncryptConverter.class)
    @Column(columnDefinition = "TEXT")
    private String googleAccessToken;

    // 리프레시 토큰도 길 수 있으니 TEXT 로 - 선택
    @JsonIgnore
    @Convert(converter = JasyptEncryptConverter.class)
    @Column(columnDefinition = "TEXT")
    private String googleRefreshToken;

    // 액세스 토큰 만료 시각
    private LocalDateTime googleTokenExpiry;

    @Enumerated(EnumType.STRING)
    private UserRole userRole;

    @PrePersist // 엔티티가 DB에 INSERT 되기 전에 호출됨
    public void setUserRole() {
        if (this.userRole == null) {
            if (this.username != null && this.username.equals("admin")) {
                this.userRole = UserRole.ADMIN;
                return;
            }
            this.userRole = UserRole.USER; // 기본 값을 USER 로 설정
        }
    }
}
