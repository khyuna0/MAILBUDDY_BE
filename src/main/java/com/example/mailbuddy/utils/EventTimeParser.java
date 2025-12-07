package com.example.mailbuddy.utils;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

// @Component 등록해야 쓸 수 있는거였나?
@Component
public class EventTimeParser {

    /*
    처음 구글 이메일 저장할 때 이메일 받은 시간은 Localdatetime received_time
    저장된 구글 이메일을 요약할 때 생성되는 이벤트 타임 "time": "2025-11-14T19:00" 이렇게 들어옴
    yyyy-MM-dd HH:mm 형태로 바꿔서 저장할 수 있게 해주는 메서드
    */

    public static String normalizeEventTime(String s) {
        if (s == null || s.isBlank()) return null;
        // T를 공백으로 변환
        String replaced = s.replace('T', ' ');
        // 기본 포맷 시도
        DateTimeFormatter[] formatters = new DateTimeFormatter[] {
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
                DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm"),
                DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss")
        };
        for (DateTimeFormatter formatter : formatters) {
            try {
                LocalDateTime ldt = LocalDateTime.parse(replaced, formatter);
                // 원하는 형태로 String 반환
                return ldt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
            } catch (Exception ignore) {}
        }
        return null;
    }

}



