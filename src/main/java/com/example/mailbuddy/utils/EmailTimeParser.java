package com.example.mailbuddy.utils;

import jakarta.mail.internet.MailDateFormat; // starter-mail 사용 시 제공
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.time.*;
import java.util.Date;

@Component
public class EmailTimeParser {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final MailDateFormat MDF = new MailDateFormat();

    /**
     * 이메일에서 받은 3가지 시간 정보 중 가장 믿을만한 시간 하나를 찾는 함수
     * 가장 안정적인 시간 결정 1) Date 헤더 → 2) Received 헤더 → 3) Gmail internalDate(ms)
     * 모두 실패하면 null
     */
    public static LocalDateTime resolveBestTime(String receivedHeader, String dateHeader, Long internalDateMillis) {
        // 1) Date 헤더 먼저 시도
        Date d = tryParse(dateHeader);
        // 2) 실패 시 Received 헤더 시도
        if (d == null) d = tryParse(receivedHeader);

        if (d != null) {
            return Instant.ofEpochMilli(d.getTime()).atZone(KST).toLocalDateTime();
        }
        // 3) 최후 폴백: Gmail internalDate(epoch ms)
        if (internalDateMillis != null) {
            return Instant.ofEpochMilli(internalDateMillis).atZone(KST).toLocalDateTime();
        }
        return null;
    }

    // 기존 코드 호환용 (원래 parseReceivedTime(String)만 호출하던 곳을 그대로 살리고 싶을 때)
    // 개별 파싱 : parseReceivedTime, parseDateHeader, fromInternalDate
    public static LocalDateTime parseReceivedTime(String receivedHeader) {
        Date d = tryParse(receivedHeader);
        if (d == null) return null;
        return Instant.ofEpochMilli(d.getTime()).atZone(KST).toLocalDateTime();
    }

    public static LocalDateTime parseDateHeader(String dateHeader) {
        Date d = tryParse(dateHeader);
        if (d == null) return null;
        return Instant.ofEpochMilli(d.getTime()).atZone(KST).toLocalDateTime();
    }

    public static LocalDateTime fromInternalDate(Long internalDateMillis) {
        if (internalDateMillis == null) return null;
        return Instant.ofEpochMilli(internalDateMillis).atZone(KST).toLocalDateTime();
    }

    // 한국 시간 문자열 포맷
    public static String formatKoreanTime(LocalDateTime kst) {
        if (kst == null) return "시간 정보 없음";
        return kst.toString().replace('T', ' ') + " (KST)";
    }

    // ---- 내부 유틸 : 날짜 문자열 파싱 시도 ----
    private static Date tryParse(String header) {
        if (header == null || header.isBlank()) return null;
        try {
            // MailDateFormat이 괄호(UTC), 약어(PST/KST) 등 대부분을 알아서 처리
            // MailDateFormat의 parse()는 이메일 날짜에서 자주 나오는 복잡한 형태를 알아서 처리하는 가장 신뢰성 높은 파서
            return MDF.parse(header);
        } catch (ParseException e) {
            return null;
        }
    }
}
