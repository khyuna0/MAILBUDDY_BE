// src/main/java/com/example/mailbuddy/controller/BriefController.java
package com.example.mailbuddy.controller;

import com.example.mailbuddy.dto.DailyBriefResponse;
import com.example.mailbuddy.entity.User;
import com.example.mailbuddy.repository.UserRepository;
import com.example.mailbuddy.service.DailyBriefService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/brief")
public class BriefController {

    private final DailyBriefService dailyBriefService;
    private final UserRepository userRepository;


    // 선택날짜 일정 있다면 브리핑 -> 프론트에서 selectedDate로 오늘만 브리핑 보여주기
    //GET /api/brief/day?day=2025-11-21
    @GetMapping("/day")
    public ResponseEntity<?> getDailyBrief(
            @RequestParam("day") LocalDate date,
            Authentication auth
    ) {
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "로그인 정보가 없습니다."));
        }

        User user = userRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new UsernameNotFoundException("사용자 정보가 없습니다."));

        DailyBriefResponse res = dailyBriefService.createDailyBrief(user, date);
        return ResponseEntity.ok(res);
    }
}
