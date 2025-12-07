package com.example.mailbuddy.controller;

import com.example.mailbuddy.dto.SummaryDto;
import com.example.mailbuddy.entity.Gmail;
import com.example.mailbuddy.entity.Summary;
import com.example.mailbuddy.entity.User;
import com.example.mailbuddy.repository.GmailRepository;
import com.example.mailbuddy.repository.SummaryRepository;
import com.example.mailbuddy.repository.UserRepository;
import com.example.mailbuddy.service.SummaryService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

@RestController
@RequestMapping("/api/summarize")
@RequiredArgsConstructor
public class SummaryController {

    private final SummaryService summaryService;
    private final UserRepository userRepository;
    private final GmailRepository gmailRepository;
    private final SummaryRepository summaryRepository;

    // 로그인한 사용자의 요약 이메일 내림차순으로 전체 불러오기
    @GetMapping("/list")
    public ResponseEntity<?> listAll(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "로그인 정보가 없습니다."));
        }
        // 사용자 조회
        User user = userRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new UsernameNotFoundException("사용자 정보가 없습니다."));
        // Gmail.user.id 기준으로 요약 조회
        List<Summary> summaries = summaryRepository.findAllByGmail_User_IdOrderByCreatedAtDesc(user.getId());
        // DTO 변환
        List<SummaryDto> result = summaries.stream().map(SummaryDto::from).toList();
        return ResponseEntity.ok(result);
    }

    // 날짜 없는 일정 가져오기
    @GetMapping("/nodate")
    public ResponseEntity<?> getNodateList(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    Map.of("error", "로그인 정보가 없습니다."));
        }
        User user = userRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new UsernameNotFoundException("사용자 정보가 없습니다."));
        List<Summary> noDateList = summaryRepository.findByGmail_User_IdAndEventDateIsNull(user.getId());
        return ResponseEntity.ok(noDateList);
    }

    // 날짜별 목록 (yyyy-mm-dd로 검색했을 때 동일 날짜의 데이터 출력)
    @GetMapping("/day")
    public ResponseEntity<?> getByDate(@RequestParam("day") LocalDate date, Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    Map.of("error", "로그인 정보가 없습니다."));
        }
        User user = userRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new UsernameNotFoundException("사용자 정보가 없습니다."));
        List<Summary> dateList = summaryRepository.findByGmail_User_IdAndEventDate(user.getId(), date);
        return ResponseEntity.ok(dateList);
    }

    // 월별 목록 (yyyy-mm-dd로 검색했을 때 해당 월의 데이터 출력)
    @GetMapping("/month")
    public ResponseEntity<?> getByMonth(@RequestParam("month") LocalDate date, Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    Map.of("error", "로그인 정보가 없습니다."));
        }
        User user = userRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new UsernameNotFoundException("사용자 정보가 없습니다."));
        LocalDate startDate = date.withDayOfMonth(1); // 월의 첫날: 2025-11-01
        LocalDate endDate = date.withDayOfMonth(date.lengthOfMonth()); // 월의 마지막 날: 2025-11-30
        List<Summary> monthList = summaryRepository.findByGmail_User_IdAndEventDateBetween(user.getId(), startDate, endDate);
        return ResponseEntity.ok(monthList);
    }

    // 구글 로그인한 사용자의 저장된 이메일을 가져와서 중복없이 요약 + dto 저장
    @PostMapping
    public ResponseEntity<?> summarizeExisting(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpServletResponse.SC_UNAUTHORIZED)
                    .body(Map.of("error", "로그인되지 않음"));
        }
        User user = userRepository.findByUsername(authentication.getName()).orElseThrow(
                () -> new UsernameNotFoundException("username not found: " + authentication.getName()));
        List<Gmail> mails = gmailRepository.findByUser(user);

        List<SummaryDto> results = new ArrayList<>();
        for (Gmail g : mails) {
            SummaryDto dto = summaryService.summarizeSaveFromGmailId(g.getId());
            if (dto != null) results.add(dto);
        }
        return ResponseEntity.ok(results);
    }

    // 요약된 이메일 수정하기
    @PatchMapping("/{id}")
    public ResponseEntity<?> updateSummary(@PathVariable Long id,
                                           @RequestBody SummaryDto dto, Authentication auth) {
        Summary summary = summaryRepository.findById(id).orElseThrow(
                () -> new EntityNotFoundException("요약된 메일이 없습니다.")
        );
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "로그인 정보가 없습니다."));
        }
        if (!auth.getName().equals(summary.getGmail().getUser().getUsername())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "권한이 없습니다."));
        }
        // 변경된 값만 수정해서 setter에 넣어주기
        if (dto.getTitle() != null && !dto.getTitle().trim().isEmpty()) {
            summary.setTitle(dto.getTitle().trim());
        }
        if (dto.getEventDate() != null) {
            summary.setEventDate(LocalDate.parse(dto.getEventDate()));
        }
        if (dto.getEventTime() != null) {
            summary.setEventTime(LocalTime.parse(dto.getEventTime()));
        }

        // 원래 기존은 수정 시 null 값 허용이 안됐는데, 허용되게 수정함
        // 기존 코드
        // if (dto.getNotes() != null && !dto.getNotes().trim().isEmpty()) {
        //            summary.setNotes(dto.getNotes().trim());}

        // place
        if (dto.getPlace() == null || dto.getPlace().trim().isEmpty()) {
            summary.setPlace(null);                // null 로 저장
        } else {
            summary.setPlace(dto.getPlace().trim());
        }

        // notes
        if (dto.getNotes() == null || dto.getNotes().trim().isEmpty()) {
            summary.setNotes(null);                // null 로 저장
        } else {
            summary.setNotes(dto.getNotes().trim());
        }

        summaryRepository.save(summary);
        return ResponseEntity.ok(SummaryDto.from(summary));
    }

    // 요약된 이메일 삭제하기
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id, Authentication auth) {
        Summary summary = summaryRepository.findById(id).orElseThrow(
                () -> new EntityNotFoundException("요약된 메일이 없습니다.")
        );
        if (auth == null || !auth.isAuthenticated())
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "로그인 정보가 없습니다."));
        if (!auth.getName().equals(summary.getGmail().getUser().getUsername())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "권한이 없습니다."));
        }
        summaryRepository.delete(summary);
        return ResponseEntity.ok(Map.of("message", "삭제가 완료되었습니다."));
    }
}
