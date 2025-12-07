package com.example.mailbuddy.service;

import com.example.mailbuddy.entity.User;
import com.example.mailbuddy.repository.UserRepository;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;

@Service
public class GoogleTokenService {

    private final UserRepository userRepository;
    private final WebClient googleAuthClient;

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String clientId;

    @Value("${spring.security.oauth2.client.registration.google.client-secret}")
    private String clientSecret;

    public GoogleTokenService(UserRepository userRepository) {
        this.userRepository = userRepository;
        this.googleAuthClient = WebClient.builder()
                .baseUrl("https://oauth2.googleapis.com")
                .build();
    }

    public String getValidAccessToken(User user) {

        // 1) accessToken 자체가 없으면 → 진짜로 연동 안 된 것
        if (user.getGoogleAccessToken() == null) {
            throw new IllegalStateException("구글 토큰 정보가 없습니다. 다시 연동이 필요합니다.");
        }

        // 2) refresh_token 이 없으면 → 자동 갱신은 못 하지만
        //    "현재 저장된 accessToken 그대로 사용" 모드로 동작
        if (user.getGoogleRefreshToken() == null || user.getGoogleTokenExpiry() == null) {
            // 그냥 지금 DB에 있는 access token 사용
            return user.getGoogleAccessToken();
        }

        // 3) 만료 체크 (1분 여유)
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiry = user.getGoogleTokenExpiry();

        if (expiry != null && expiry.isAfter(now.plusMinutes(1))) {
            // 아직 유효 → 기존 access token 사용
            return user.getGoogleAccessToken();
        }

        // 4) 여기까지 왔으면 → 만료로 보고 refresh_token 으로 새 토큰 요청
        return refreshAccessToken(user);
    }

    private String refreshAccessToken(User user) {
        String refreshToken = user.getGoogleRefreshToken();
        if (refreshToken == null) {
            throw new IllegalStateException("저장된 refresh_token 이 없습니다.");
        }

        // 구글 토큰 갱신 엔드포인트 - form-data 로 호출
        String response = googleAuthClient.post()
                .uri("/token")
                .body(BodyInserters.fromFormData("client_id", clientId)
                        .with("client_secret", clientSecret)
                        .with("refresh_token", refreshToken)
                        .with("grant_type", "refresh_token"))
                .retrieve()
                .bodyToMono(String.class)
                .block();

        JSONObject json = new JSONObject(response);
        if (!json.has("access_token")) {
            throw new IllegalStateException("구글에서 access_token 을 받지 못했습니다: " + response);
        }

        String newAccessToken = json.getString("access_token");
        int expiresIn = json.optInt("expires_in", 3600);

        LocalDateTime newExpiry = LocalDateTime.now().plusSeconds(expiresIn);

        // 유저 엔티티에 갱신된 정보 저장
        user.setGoogleAccessToken(newAccessToken);
        user.setGoogleTokenExpiry(newExpiry);
        userRepository.save(user);

        return newAccessToken;
    }
}
