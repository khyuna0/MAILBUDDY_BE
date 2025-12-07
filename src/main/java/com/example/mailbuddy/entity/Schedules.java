package com.example.mailbuddy.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Getter
@Setter
public class Schedules {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 사용자 추가 일정 고유 키

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user; // 작성한 유저

    private String title; // 일정 제목

    private String place; // 일정 장소

    @Column
    private String notes; // 일정 설명

    @Column(name = "event_date", nullable = false, columnDefinition = "DATE")
    private LocalDate eventDate; // 날짜 (yyyy-mm-dd)

    @Column(name = "event_time", nullable = false, columnDefinition = "TIME")
    private LocalTime eventTime; // 시간 (HH:mm:00)

}