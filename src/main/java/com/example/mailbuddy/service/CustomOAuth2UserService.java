// src/main/java/com/example/mailbuddy/service/CustomOAuth2UserService.java
package com.example.mailbuddy.service;

import com.example.mailbuddy.entity.User;
import com.example.mailbuddy.repository.UserRepository;
import com.example.mailbuddy.jwt.JwtUtil;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@Service
// 구글 OAuth 로그인 시 현재 JWT 유저와 구글 이메일/토큰 연결하는 서비스
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    @Autowired
    private UserRepository userRepository;

    private final HttpSession httpSession;  // LINK_JWT 저장용

    @Autowired
    private JwtUtil jwtUtil;

    public CustomOAuth2UserService(HttpSession httpSession) {
        this.httpSession = httpSession;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        // 1) 구글에서 받는 유저 속성 (프로필 정보)
        Map<String, Object> attributes = oAuth2User.getAttributes();

        // 구글에서 받은 이메일
        String googleEmail = (String) attributes.get("email");
        if (googleEmail == null) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("email_not_found"),
                    "Google email not found"
            );
        }

        // AccessToken / Expiry 꺼내기
        var accessToken = userRequest.getAccessToken();
        String accessTokenValue = accessToken.getTokenValue();

        LocalDateTime expiryTime = null;
        if (accessToken.getExpiresAt() != null) {
            expiryTime = LocalDateTime.ofInstant(
                    accessToken.getExpiresAt(),
                    ZoneId.of("Asia/Seoul")
            );
        }

        // SecurityConfig 의 customOAuth2AuthorizationRequestResolver 에서 세션에 넣어둔 JWT
        String jwt = (String) httpSession.getAttribute("LINK_JWT");
        if (jwt == null) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("missing_jwt"),
                    "연동을 위해 필요한 JWT 토큰이 없습니다. 먼저 사이트에 로그인 후 다시 시도해 주세요."
            );
        }

        // JWT 에서 username 꺼내기
        String loggedInUsername;
        try {
            loggedInUsername = jwtUtil.extractUsername(jwt);
        } catch (Exception e) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("invalid_jwt"),
                    "JWT 토큰이 유효하지 않습니다."
            );
        }

        // 현재 JWT 유저 엔티티 찾기
        User user = userRepository.findByUsername(loggedInUsername)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + loggedInUsername));

        // 이 계정에 이미 다른 구글 계정이 연동되어 있는지 확인
        String alreadyLinkedGoogle = user.getGoogleEmail();
        if (alreadyLinkedGoogle != null && !alreadyLinkedGoogle.equals(googleEmail)) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error(
                            "user_already_linked_other_google",
                            "이미 이 계정에는 다른 Google 계정이 연동되어 있습니다.",
                            null
                    ),
                    "이미 이 계정에는 다른 Google 계정이 연동되어 있습니다."
            );
        }

        // 다른 유저가 이미 이 구글 이메일을 쓰고 있는지 확인
        userRepository.findByGoogleEmail(googleEmail).ifPresent(existing -> {
            if (!existing.getId().equals(user.getId())) {
                throw new OAuth2AuthenticationException(
                        new OAuth2Error(
                                "google_email_already_linked",
                                "이미 다른계정(" + existing.getUsername() + ")에 연동된 구글 계정 입니다.",
                                null
                        ),
                        "이미 다른 계정에 연동된 구글 계정 입니다."
                );
            }
        });

        // 구글메일 연동 정보 & access token / 만료 시각 저장
        user.setGoogleEmail(googleEmail);
        user.setGoogleAccessToken(accessTokenValue);

        if (expiryTime != null) {
            user.setGoogleTokenExpiry(expiryTime);
        }

        User linkedUser = userRepository.save(user);

        // 권한 세팅
        Collection<SimpleGrantedAuthority> authorities =
                List.of(new SimpleGrantedAuthority("ROLE_" + linkedUser.getUserRole().name()));

        final User finalUser = linkedUser;
        return new DefaultOAuth2User(
                authorities,
                attributes,
                "email") {
            @Override
            public String getName() {
                // SecurityContext 에서는 username(우리 사이트 계정) 기준으로 보이게
                return finalUser.getUsername();
            }
        };
    }
}
