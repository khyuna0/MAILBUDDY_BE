package com.example.mailbuddy.controller;

import com.example.mailbuddy.dto.SchedulesRequestDto;
import com.example.mailbuddy.entity.User;
import com.example.mailbuddy.entity.Schedules;
import com.example.mailbuddy.repository.UserRepository;
import com.example.mailbuddy.repository.SchedulesRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/schedules")
public class SchedulesController {

    private final UserRepository userRepository;
    private final SchedulesRepository schedulesRepository;

    @Autowired
    public SchedulesController(UserRepository userRepository,
                               SchedulesRepository schedulesRepository) {
        this.userRepository = userRepository;
        this.schedulesRepository = schedulesRepository;
    }

    // 일정 생성
    @PostMapping
    public ResponseEntity<?> write(@Valid @RequestBody SchedulesRequestDto req,
                                   Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    Map.of("error", "로그인 정보가 없습니다."));
        }

        User user = userRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new UsernameNotFoundException("사용자 정보가 없습니다."));

        Schedules s = new Schedules();
        s.setUser(user);
        s.setTitle(req.getTitle());      // subject -> title
        s.setNotes(req.getNotes());      // notes
        s.setPlace(req.getPlace());        // 선택값
        s.setEventDate(req.getEventDate()); // LocalDateTime 그대로
        s.setEventTime(req.getEventTime()); // LocalDateTime 그대로

        schedulesRepository.save(s);
        return ResponseEntity.ok("일정 추가 완료");
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
        List<Schedules> dateList = schedulesRepository.findByUserAndEventDate(user, date);
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
        List<Schedules> monthList = schedulesRepository.findByUserAndEventDateBetween(user, startDate, endDate);
        return ResponseEntity.ok(monthList);
    }

    // 일정 삭제
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id, Authentication auth) {
        Schedules schedule = schedulesRepository.findById(id).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "없는 글입니다."));
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    Map.of("error", "로그인 정보가 없습니다."));
        }
        if (!auth.getName().equals(schedule.getUser().getUsername())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                    Map.of("error", "권한이 없습니다."));
        }
        schedulesRepository.delete(schedule);
        return ResponseEntity.ok("삭제가 완료되었습니다.");
    }

    // 일정 수정
    @PatchMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id,
                                    @Valid @RequestBody SchedulesRequestDto req,
                                    Authentication auth) {
        if (auth == null || !auth.isAuthenticated())
            return ResponseEntity.status(401).body(Map.of("error", "로그인 정보가 없습니다."));
        Schedules s = schedulesRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "없는 글입니다."));
        if (!auth.getName().equals(s.getUser().getUsername()))
            return ResponseEntity.status(403).body(Map.of("error", "권한이 없습니다."));
        // 변경된 값만 setter로 넣어주기 (유효성 체크 확인 ..**)
        // 일정 제목
        if (req.getTitle() != null && !req.getTitle().trim().isEmpty()) {
            s.setTitle(req.getTitle().trim());
        }
        // 일정 설명, 장소, 날짜, 시간 모두 null 가능
        // place
        if (req.getPlace() == null || req.getPlace().trim().isEmpty()) {
            s.setPlace(null);                // null 로 저장
        } else {
            s.setPlace(req.getPlace().trim());
        }

        // notes
        if (req.getNotes() == null || req.getNotes().trim().isEmpty()) {
            s.setNotes(null);                // null 로 저장
        } else {
            s.setNotes(req.getNotes().trim());
        }
        if (req.getEventDate() != null) {
            s.setEventDate(req.getEventDate());
        }
        if (req.getEventTime() != null) {
            s.setEventTime(req.getEventTime());
        }
        schedulesRepository.save(s);
        return ResponseEntity.ok("일정 수정 완료");
    }

}