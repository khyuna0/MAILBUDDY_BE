package com.example.mailbuddy.controller;

import com.example.mailbuddy.dto.*;
import com.example.mailbuddy.entity.User;
import com.example.mailbuddy.repository.UserRepository;
import com.example.mailbuddy.jwt.JwtUtil;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
// 회원가입 + 로그인 + 마이페이지
public class AuthController {

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtUtil jwtUtil;

    // 회원가입
    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody @Valid UserRequestDto req, BindingResult result) {
        Map<String, String> errors = new HashMap<>();
        if (userRepository.findByUsername(req.getUsername()).isPresent()) { // 아이디 중복 여부 검사
            errors.put("idError", "이미 존재하는 아이디입니다.");
        }
        if (result.hasErrors()) {
            result.getFieldErrors().forEach(err -> {
                        errors.put(err.getField(), err.getDefaultMessage());
                    }
            );
        }
        if (!errors.isEmpty()) {
            return ResponseEntity.badRequest().body(errors);
        }
        User user = new User();
        user.setUsername(req.getUsername());
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        user.setName(req.getName());
        user.setBirth(req.getBirth());
        userRepository.save(user);
        return ResponseEntity.ok().body("가입 완료");
    }

    // JWT 로그인 (폼 로그인 대체)
    @PostMapping("/login")
    public ResponseEntity<?> login(
            @RequestBody @Valid LoginRequestDto req,
            BindingResult result
    ) {
        // 1) DTO 검증 에러 (username/password 비어있거나 바인딩 실패) 처리
        if (result.hasErrors()) {
            Map<String, String> errors = new HashMap<>();
            result.getFieldErrors().forEach(err ->
                    errors.put(err.getField(), err.getDefaultMessage())
            );
            // 어떤 에러인지 프론트에서 볼 수 있게 400 바디로 내려줌
            return ResponseEntity.badRequest().body(errors);
        }

        try {
            UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(req.getUsername(), req.getPassword());

            Authentication authentication = authenticationManager.authenticate(authToken);

            User user = userRepository.findByUsername(authentication.getName())
                    .orElseThrow(() ->
                            new UsernameNotFoundException("username not found: " + authentication.getName())
                    );

            String token = jwtUtil.generateToken(user.getUsername(), user.getUserRole().name());

            LoginResponseDto responseDto = new LoginResponseDto(
                    user.getUsername(),
                    user.getName(),
                    user.getBirth(),
                    user.getUserRole().name(),
                    token
            );

            return ResponseEntity.ok(responseDto);
        } catch (BadCredentialsException ex) {
            // 아이디/비번 틀린 경우 -> 401
            return ResponseEntity.status(HttpServletResponse.SC_UNAUTHORIZED)
                    .body(Map.of("error", "아이디 또는 비밀번호가 올바르지 않습니다."));
        }
    }

    // 로그인한 유저의 username, name, birth .. 등 정보 가져오기 - 마이페이지 수정 (JWT Authentication 이용)
    @GetMapping("/me")
    public ResponseEntity<?> me(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }

        String username = authentication.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        String googleEmail = user.getGoogleEmail();
        boolean googleLinked = (googleEmail != null && !googleEmail.isBlank());

        Map<String, Object> body = new HashMap<>();  //응답 dto 만들어도 됨
        body.put("username", user.getUsername());
        body.put("name", user.getName());
        body.put("birth", user.getBirth());
        body.put("userRole", user.getUserRole().name());

        body.put("googleEmail", googleEmail);
        body.put("googleLinked", googleLinked);

        return ResponseEntity.ok(body);
    }

    // 마이페이지 수정
    @PatchMapping("/profile")
    public ResponseEntity<?> updateProfile(
            @Validated(UpdateProfileRequest.ValidationSequence.class)
            @RequestBody UpdateProfileRequest req,
            BindingResult result,
            Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpServletResponse.SC_UNAUTHORIZED)
                    .body(Map.of("error", "로그인되지 않음"));
        }

        // 검증 에러 있으면 400으로 메시지 반환
        if (result.hasErrors()) {
            Map<String, String> errors = new HashMap<>();
            result.getFieldErrors().forEach(err ->
                    errors.put(err.getField(), err.getDefaultMessage())
            );
            return ResponseEntity.badRequest().body(errors);
        }

        User user = userRepository.findByUsername(authentication.getName()).orElseThrow(
                () -> new UsernameNotFoundException("username not found: " + authentication.getName())
        );

        // 이름
        if (req.getName() != null && !req.getName().trim().isEmpty()) {
            user.setName(req.getName().trim());
        }
        // 생일
        if (req.getBirth() != null) {
            user.setBirth(req.getBirth());
        }
        // 비밀번호
        if (req.getPassword() != null && !req.getPassword().trim().isEmpty()) {
            user.setPassword(passwordEncoder.encode(req.getPassword().trim()));
        }
        userRepository.save(user);
        return ResponseEntity.ok(new MypageResponseDto(
                user.getUsername(),
                user.getName(),
                user.getBirth()
        ));
    }
}
