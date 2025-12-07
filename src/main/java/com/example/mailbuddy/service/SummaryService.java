package com.example.mailbuddy.service;

import com.example.mailbuddy.dto.SummaryDto;
import com.example.mailbuddy.entity.Gmail;
import com.example.mailbuddy.entity.Summary;
import com.example.mailbuddy.repository.GmailRepository;
import com.example.mailbuddy.repository.SummaryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;

@Service
@RequiredArgsConstructor
public class SummaryService {

    private final GmailRepository gmailRepository;
    private final SummaryRepository summaryRepository;
    private final GeminiService geminiService;

    // 이미 요약이 있으면 재저장 없이 dto로 반환
    // 없으면 요약생성 -> 저장 -> dto 반환
    // 일정 외 사항(전부 빈 값) -> null반환 -> 204매핑처리
    @Transactional
    public SummaryDto summarizeSaveFromGmailId(Long gmailId) {
        Gmail gmail = gmailRepository.findById(gmailId)
                .orElseThrow(() -> new IllegalArgumentException("Gmail not found: " + gmailId));

        // 이미 요약되어 있으면 그대로 반환
        var existing = summaryRepository.findFirstByGmail_Id(gmailId);
        if (existing.isPresent()) {
            return SummaryDto.from(existing.get());
        }

        // Gemini 호출 → DTO
        SummaryDto dto = geminiService.summarizeFromGmail(gmail);

        // 일정 아님(= 모든 필드가 null/빈값)이면 저장 스킵
        if (isEmptySummary(dto)) {
            // System.out.println("[SummaryService] non-event mail skipped (gmailId=" + gmailId + ")"); // 요약 제외 메일 확인용
            return null;
        }

        // 저장 엔티티 구성
        Summary s = Summary.builder()
                .gmail(gmail)
                // 요약 제목
                .title(or(nzToNull(dto.getTitle()), nzToNull(gmail.getSubject())))
                // 보낸사람/이메일
                .senderName(or(nzToNull(dto.getName()), nzToNull(gmail.getSenderName())))
                .senderEmail(or(nzToNull(dto.getEmail()), nzToNull(gmail.getSenderEmail())))
                // 장소
                .place(nzToNull(dto.getPlace()))
                // 일정 날짜, 시간
                .eventDate(dto.getEventDate() != null ? LocalDate.parse(dto.getEventDate()) : null)
                .eventTime(dto.getEventTime() != null ? LocalTime.parse(dto.getEventTime()) : null)
                .notes(nzToNull(dto.getNotes()))
                .build();

        Summary saved = summaryRepository.save(s);
        return SummaryDto.from(saved);
    }

    // ========= helpers ==========

    // DTO가 비었는지 (모든 핵심 필드 null/빈문자)
    private static boolean isEmptySummary(SummaryDto dto) {
        if (dto == null) return true;
        return isBlank(dto.getTitle())
                && isBlank(dto.getEventDate())
                && isBlank(dto.getEventTime())
                && isBlank(dto.getPlace())
                && isBlank(dto.getNotes())
                && isBlank(dto.getName())
                && isBlank(dto.getEmail());
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    // 빈문자면 null로 정규화
    private static String nzToNull(String s) {
        return isBlank(s) ? null : s.trim();
    }

    // a가 있으면 a, 없으면 b
    private static String or(String a, String b) {
        return (a != null && !a.isBlank()) ? a : b;
    }
}