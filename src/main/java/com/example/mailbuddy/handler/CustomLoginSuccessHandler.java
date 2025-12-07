package com.example.mailbuddy.handler;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
// 폼 로그인 성공 시 세션에 username 저장하는 핸들러
public class CustomLoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final HttpSession httpSession;

    public CustomLoginSuccessHandler(HttpSession httpSession) {
        this.httpSession = httpSession;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        String username = authentication.getName(); // 로그인한 username 얻기
        httpSession.setAttribute("LOGGED_IN_USER", username); // 세션에 'LOGGED_IN_USER' 이름으로 username 저장
        super.onAuthenticationSuccess(request, response, authentication); // 기본 로그인 성공 처리 계속 진행 (페이지 리다이렉트 등)
        // SecurityConfig의 .formLogin()에 defaultSuccessUrl(...)이나 .successForwardUrl(...) 같은 설정이 있으면 그걸 따른다
        // 없으면 기본적으로 이전에 접근하려던 페이지(로그인 강제 전 URL)로 이동
        // 이전 URL이 없으면 / (홈)으로 이동함
        // 특정 페이지로 가게 하려면: 로그인 성공 후 /dashboard 로 리다이렉트
        // response.sendRedirect("/dashboard");
    }

}
