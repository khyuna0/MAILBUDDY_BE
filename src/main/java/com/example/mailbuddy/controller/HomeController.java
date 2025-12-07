package com.example.mailbuddy.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HomeController {
    // 왜 이 controller 가 있어야만 로그인 성공 후 페이지가 새롭게 실행될까?
    // SecurityConfig에서 로그인 성공 후 이동할 페이지를 defaultSuccessUrl에 넣어뒀는데
    // 실제로 우리가 home을 보여줄 수 있는 컨트롤러 매핑 부분이 없는거지 ..?
    // 그러니까 이 컨트롤러가 있어야만 localhost:8888/ 이 부분으로 가는거지!
    @GetMapping("/")
    public ResponseEntity<String> home() {
        return ResponseEntity.ok("Welcome home");
    }
}
