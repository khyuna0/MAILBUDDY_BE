package com.example.mailbuddy.handler;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
public class OAuth2LoginFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception)
            throws IOException, ServletException {


        String code = "oauth2_error";
        String msg = "소셜 로그인 중 오류가 발생했습니다.";

        // CustomOAuth2UserService 에서 던진 OAuth2AuthenticationException 처리
        if (exception instanceof OAuth2AuthenticationException authEx) {
            // new OAuth2AuthenticationException(new OAuth2Error("google_email_already_linked"), "이미 다른 계정...") 이거랑 연결
            code = authEx.getError().getErrorCode();  // google_email_already_linked
            msg = authEx.getMessage();                // "이미 다른 계정(...)에 연동된 구글 계정입니다."
        }

        String encodedMsg = URLEncoder.encode(msg, StandardCharsets.UTF_8);
        String encodedCode = URLEncoder.encode(code, StandardCharsets.UTF_8);

        String targetUrl = "http://mailbuddy-s3-fe.s3-website.ap-northeast-2.amazonaws.com/error?status=400&code="
                + encodedCode + "&message=" + encodedMsg;

        // SimpleUrlAuthenticationFailureHandler 가 가진 redirect 기능 사용
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}
