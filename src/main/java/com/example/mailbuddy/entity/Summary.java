package com.example.mailbuddy.entity;

import com.example.mailbuddy.config.JasyptEncryptConverter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;


@Table(
        uniqueConstraints = @UniqueConstraint(name = "uq_summary_gmail", columnNames = "gmail_id")
        // 같은 gmail_id 여러번 요약 시도시 중복으로 들어가지 않게 처리
)
@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Summary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 기본키

    // 원본 메일과의 관계
    //ManyToOne은 기본이 LAZY라 성능/지연로딩이 편함 -> OneToOne(LAZY)는 프록시/지연로딩이 가끔 까다롭고 삐끗할 때가 있음
    //나중에 “한 메일에서 요약을 여러 개”로 바꾸고 싶을 때 UNIQUE만 빼면 끝이라 유연
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "gmail_id", nullable = false)
    @JsonIgnore
    private Gmail gmail;

    // 1. 핵심: 일정제목(요약)
//    @Convert(converter = JasyptEncryptConverter.class) // 암호화 저장, 엔티티 - 암호화 / 복호화 - DB
    private String title; // null 허용

    // 2. 메일 보낸사람
    private String senderName; // null 허용

    // 3. 보낸사람의 이메일
    private String senderEmail; // null 허용

    // 4. 약속장소
//    @Convert(converter = JasyptEncryptConverter.class)
    private String place; // null 허용

    // 일정 날짜 / 시간 분리
    //약속날짜
    @Column(name = "event_date", columnDefinition = "DATE")
    private LocalDate eventDate; // 날짜 (yyyy-mm-dd)
    //약속시간
    @Column(name = "event_time", columnDefinition = "TIME")
    private LocalTime eventTime; // 시간 (HH:mm:00)

    // 6. 그외: 만나는사람/준비/주의사항 등
    @Column(columnDefinition = "TEXT")
//    @Convert(converter = JasyptEncryptConverter.class)
    private String notes; // null/빈문자 허용

    @CreationTimestamp
    private LocalDateTime createdAt;

}
