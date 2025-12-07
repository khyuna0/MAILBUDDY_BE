package com.example.mailbuddy.controller;

import com.example.mailbuddy.service.GoogleTokenService;
import com.example.mailbuddy.utils.EmailTimeParser;
import com.example.mailbuddy.entity.Gmail;
import com.example.mailbuddy.entity.User;
import com.example.mailbuddy.repository.GmailRepository;
import com.example.mailbuddy.repository.UserRepository;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/gmail")
public class GmailController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GmailRepository gmailRepository;

    @Autowired
    @Qualifier("gmailWebClient")
    private WebClient gmailWebClient;

    @Autowired
    private GoogleTokenService googleTokenService;

    // JWT 인증된 유저의 구글 계정 정보 확인용
    @GetMapping("/userInfo")
    public ResponseEntity<?> getGoogleUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body("로그인이 필요합니다.");
        }

        User user = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + authentication.getName()));

        if (user.getGoogleEmail() == null) {
            return ResponseEntity.ok("구글 계정이 아직 연동되지 않았습니다.");
        }

        return ResponseEntity.ok(
                "[사용자 아이디] " + user.getUsername()
                        + " [연동된 구글 이메일] " + user.getGoogleEmail()
        );
    }

    // 사용자의 메일 목록 가져오기 (최대 100개인듯)
    @GetMapping("/messages")
    public ResponseEntity<?> getMessages(Authentication authentication) {
        User user = getUserWithGoogleToken(authentication);
        String accessToken = googleTokenService.getValidAccessToken(user);

        String mailListResponse = gmailWebClient.get()
                .uri("/users/me/messages")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        return ResponseEntity.ok(mailListResponse);
    }

    // 사용자의 상위 최신 이메일 10개 가져오기 + 엔티티 저장 + 중복방지 처리
    @GetMapping("/messages/save-top10")
    public ResponseEntity<?> saveTop10Messages(Authentication authentication) {
        User user = getUserWithGoogleToken(authentication);
        final String accessToken;

        try {
            accessToken = googleTokenService.getValidAccessToken(user);
        } catch (IllegalStateException e) {
            // refresh_token 없거나 토큰 정보가 깨졌을 때
            return ResponseEntity.status(401).body(
                    Map.of(
                            "code", "google_token_missing",
                            "message", "구글 토큰 정보가 없어 자동 갱신을 할 수 없습니다. 다시 구글 연동을 진행해 주세요."
                    )
            );
        }

        int top10 = 10;

        String mailListTop10Response;
        try {
            mailListTop10Response = gmailWebClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/users/me/messages")
                            .queryParam("maxResults", top10)
                            .build())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
        } catch (WebClientResponseException e) {
            int status = e.getStatusCode().value();
            if (status == 401 || status == 403) {
                return ResponseEntity.status(401).body(
                        Map.of(
                                "code", "google_token_expired",
                                "message", "구글 연동이 만료되었거나 권한이 없습니다. 다시 구글 연동을 진행해 주세요."
                        )
                );
            }
            return ResponseEntity.status(500).body(
                    Map.of(
                            "code", "google_api_error",
                            "message", "Gmail API 호출 중 오류가 발생했습니다: " + e.getMessage()
                    )
            );
        } catch (Exception e) {
            return ResponseEntity.status(500).body(
                    Map.of(
                            "code", "gmail_unknown_error",
                            "message", "메일 저장 중 알 수 없는 오류가 발생했습니다: " + e.getMessage()
                    )
            );
        }

        try {
            JSONObject listJson = new JSONObject(mailListTop10Response);
            JSONArray messagesArray = listJson.optJSONArray("messages");
            if (messagesArray == null || messagesArray.isEmpty()) {
                return ResponseEntity.ok("저장할 메시지가 없습니다.");
            }

            int saved = 0;

            for (int i = 0; i < messagesArray.length(); i++) {
                String messageId = messagesArray.getJSONObject(i).getString("id");

                // 중복이면 스킵
                if (gmailRepository.existsByUserAndMessageId(user, messageId)) {
                    continue;
                }

                String detailResponse = gmailWebClient.get()
                        .uri("/users/me/messages/" + messageId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .retrieve()
                        .bodyToMono(String.class)
                        .block();

                JSONObject json = new JSONObject(detailResponse);
                JSONObject payload = json.getJSONObject("payload");

                String from = "";
                String date = "";
                String subject = "";

                JSONArray headers = payload.getJSONArray("headers");
                for (int j = 0; j < headers.length(); j++) {
                    JSONObject header = headers.getJSONObject(j);
                    String name = header.getString("name");
                    String value = header.getString("value");
                    if ("From".equalsIgnoreCase(name)) {
                        from = value;
                    } else if ("Date".equalsIgnoreCase(name)) {
                        date = value;
                    } else if ("Subject".equalsIgnoreCase(name)) {
                        subject = value;
                    }
                }

                String senderName = "";
                String senderEmail = "";
                if (!from.isEmpty()) {
                    int startIdx = from.indexOf("<");
                    int endIdx = from.indexOf(">");
                    if (startIdx != -1 && endIdx != -1 && endIdx > startIdx) {
                        senderName = from.substring(0, startIdx).trim().replace("\"", "");
                        senderEmail = from.substring(startIdx + 1, endIdx).trim();
                    } else {
                        senderEmail = from.trim();
                    }
                }

                String data = "";
                if (payload.has("body") && payload.getJSONObject("body").has("data")) {
                    data = payload.getJSONObject("body").optString("data", "");
                }
                if (data.isEmpty() && payload.has("parts")) {
                    JSONArray parts = payload.getJSONArray("parts");
                    for (int k = 0; k < parts.length(); k++) {
                        JSONObject part = parts.getJSONObject(k);
                        String mimeType = part.getString("mimeType");
                        if ("text/html".equalsIgnoreCase(mimeType) || "text/plain".equalsIgnoreCase(mimeType)) {
                            data = part.getJSONObject("body").optString("data", "");
                            if (!data.isEmpty()) break;
                        }
                    }
                }

                String bodyContent = "";
                if (!data.isEmpty()) {
                    byte[] decodedBytes = Base64.getUrlDecoder().decode(data);
                    bodyContent = new String(decodedBytes, StandardCharsets.UTF_8);

                    int maxBytes = 65535;
                    if (decodedBytes.length > maxBytes) {
                        int maxChars = 500;
                        if (bodyContent.length() > maxChars) {
                            bodyContent = bodyContent.substring(0, maxChars) + "...";
                        }
                    }
                }

                LocalDateTime koreanReceiveTime = EmailTimeParser.parseReceivedTime(date);

                Gmail gmail = new Gmail(
                        messageId,
                        senderName, senderEmail,
                        koreanReceiveTime,
                        subject,
                        bodyContent,
                        user
                );
                gmailRepository.save(gmail);
                saved++;
            }
            return ResponseEntity.ok(saved + "개 메일을 DB에 저장 완료했습니다.");
        } catch (Exception e) {
            return ResponseEntity.status(500).body(
                    Map.of(
                            "code", "gmail_parse_error",
                            "message", "메일 저장 중 오류 발생: " + e.getMessage()
                    )
            );
        }
    }


    // db에 저장된 모든 메일 목록 조회 (관리용)
    @GetMapping("/get/all")
    public ResponseEntity<List<Gmail>> getAllEmails() {
        List<Gmail> emails = gmailRepository.findAll();
        return ResponseEntity.ok(emails);
    }

    // 해당 사용자의 db에 저장된 메일 목록 조회 (JWT 유저 기준)
    @GetMapping("/get/usermails")
    public ResponseEntity<?> getEmails(Authentication authentication) {
        User user = getUserWithGoogleToken(authentication);
        List<Gmail> emails = gmailRepository.findByUser(user);
        return ResponseEntity.ok(emails);
    }

    // 공통: JWT 인증 + 구글 연동 여부 체크
    private User getUserWithGoogleToken(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UsernameNotFoundException("인증되지 않은 사용자입니다.");
        }

        User user = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + authentication.getName()));

        if (user.getGoogleEmail() == null || user.getGoogleAccessToken() == null) {
            throw new UsernameNotFoundException("구글 계정이 연동되어 있지 않거나 액세스 토큰이 없습니다.");
        }

        return user;
    }
}
