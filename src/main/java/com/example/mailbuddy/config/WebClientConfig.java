package com.example.mailbuddy.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

// WebClient : 외부 API에 비동기로 요청을 보내고 응답을 받아오는 도구
@Configuration
public class WebClientConfig {

    // Gmail API 전용 WebClient 설정
    @Bean
    @Qualifier("gmailWebClient")
    public WebClient gmailWebClient() {
        return WebClient.builder()
                .baseUrl("https://gmail.googleapis.com/gmail/v1")  // Gmail API 기본 URL
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    // Gemini API 전용 WebClient 설정
    @Bean
    @Qualifier("geminiWebClient")
    public WebClient geminiWebClient() {
        return WebClient.builder()
                .baseUrl("https://generativelanguage.googleapis.com/v1/models/gemini-2.5-flash:generateContent")  // Gemini API URL
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    // 일반적인 용도의 WebClient
    @Bean
    @Qualifier("generalWebClient")
    public WebClient generalWebClient() {
        return WebClient.builder().build();
    }

}