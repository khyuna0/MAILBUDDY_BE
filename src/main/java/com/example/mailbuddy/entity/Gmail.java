package com.example.mailbuddy.entity;

import com.example.mailbuddy.config.JasyptEncryptConverter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Gmail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 기본키

    private String senderName; // 발신자 이름
    private String senderEmail; // 발신자 이메일
    private LocalDateTime receivedTime; // 받은 시간
    @Convert(converter = JasyptEncryptConverter.class)
    private String subject; // 메일 제목

    @Column(columnDefinition = "TEXT")
    @Convert(converter = JasyptEncryptConverter.class)
    private String content; // 메일 내용

    //gmail 중복저장 해결 -> api의 외부 고유키 저장 -> 유니크로 관리
    @Column(unique = true)
    private String messageId;

    // 지메일:유저 = n:1 (지연로딩을 통해 성능 최적화)
    @ManyToOne(fetch = FetchType.LAZY)
//    @ManyToOne
//    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"}) // 단점 user 필드 사용 못함
    @JoinColumn(name= "user_id")
    private User user;

    public Gmail(String senderName, String senderEmail, LocalDateTime receivedTime, String subject, String content) {
        this.senderName = senderName;
        this.senderEmail = senderEmail;
        this.receivedTime = receivedTime;
        this.subject = subject;
        this.content = content;
    }

    public Gmail(String messageId ,String senderName, String senderEmail, LocalDateTime receivedTime, String subject, String content, User user) {
        this.messageId = messageId;
        this.senderName = senderName;
        this.senderEmail = senderEmail;
        this.receivedTime = receivedTime;
        this.subject = subject;
        this.content = content;
        this.user = user;
    }

}
