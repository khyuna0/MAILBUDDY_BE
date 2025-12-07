// src/main/java/com/example/mailbuddy/handler/CustomOAuth2SuccessHandler.java
package com.example.mailbuddy.handler;

import com.example.mailbuddy.entity.User;
import com.example.mailbuddy.jwt.JwtUtil;
import com.example.mailbuddy.repository.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;

@Component
public class CustomOAuth2SuccessHandler implements AuthenticationSuccessHandler {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserRepository userRepository;

    // Spring Security가 OAuth2 토큰(Access/Refresh)을 담아주는 저장소
    @Autowired
    private OAuth2AuthorizedClientRepository authorizedClientRepository;

    @Autowired
    private HttpSession httpSession; // LINK_JWT 꺼내려고 사용

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication)
            throws IOException, ServletException {

        // 1 인증 객체를 OAuth2 전용 토큰으로 캐스팅
        OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;

        // 2 세션에 저장해둔 우리 서비스의 JWT 꺼내기
        String jwt = (String) httpSession.getAttribute("LINK_JWT");
        if (jwt == null) {
            // JWT 없으면 연동할 사용자 찾을 수 없음 → 그냥 에러 페이지나 로그인 페이지로 보내도 됨
            System.out.println("[OAuth2Success] LINK_JWT 가 세션에 없습니다.");
            response.sendRedirect("http://mailbuddy-s3-fe.s3-website.ap-northeast-2.amazonaws.com/login");
            return;
        }

        String username;
        try {
            username = jwtUtil.extractUsername(jwt);
        } catch (Exception e) {
            System.out.println("[OAuth2Success] JWT 파싱 실패: " + e.getMessage());
            response.sendRedirect("http://mailbuddy-s3-fe.s3-website.ap-northeast-2.amazonaws.com/login");
            return;
        }

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("JWT 사용자 정보를 찾을 수 없습니다: " + username));

        // 3 구글에서 발급해준 AccessToken / RefreshToken 꺼내기
        OAuth2AuthorizedClient client =
                authorizedClientRepository.loadAuthorizedClient(
                        oauthToken.getAuthorizedClientRegistrationId(),
                        oauthToken,
                        request);

        if (client == null) {
            System.out.println("[OAuth2Success] OAuth2AuthorizedClient 가 null 입니다.");
            response.sendRedirect("http://mailbuddy-s3-fe.s3-website.ap-northeast-2.amazonaws.com/login");
            return;
        }

        // Access Token
        if (client.getAccessToken() != null) {
            String accessToken = client.getAccessToken().getTokenValue();
            user.setGoogleAccessToken(accessToken);

            if (client.getAccessToken().getExpiresAt() != null) {
                LocalDateTime expiry = LocalDateTime.ofInstant(
                        client.getAccessToken().getExpiresAt(),
                        ZoneId.of("Asia/Seoul")
                );
                user.setGoogleTokenExpiry(expiry);
            }
        }

        // Refresh Token
        if (client.getRefreshToken() != null) {
            String refreshToken = client.getRefreshToken().getTokenValue();
            user.setGoogleRefreshToken(refreshToken);
        }

        // 4 구글 이메일은 principal 에서 가져오기 (CustomOAuth2UserService에서 이미 넣었을 수도 있음)
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        Map<String, Object> attributes = oAuth2User.getAttributes();
        String googleEmail = (String) attributes.get("email");
        if (googleEmail != null) {
            user.setGoogleEmail(googleEmail);
        }

        // 5 최종 저장
        userRepository.save(user);

        // 6 연동 성공 후 프론트의 /schedule 로 리다이렉트
        response.sendRedirect("http://mailbuddy-s3-fe.s3-website.ap-northeast-2.amazonaws.com/schedule");
    }
}
