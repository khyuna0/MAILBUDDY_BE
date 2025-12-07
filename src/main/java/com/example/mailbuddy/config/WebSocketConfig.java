package com.example.mailbuddy.config;

import com.example.mailbuddy.jwt.JwtUtil;
import com.example.mailbuddy.service.UserSecurityService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.*;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final WebSocketAuthInterceptor webSocketAuthInterceptor;
    private final JwtUtil jwtUtil;
    private final UserSecurityService userSecurityService;

    // 클라이언트가 최초 WebSocket 연결을 시도하는 엔드포인트 설정
    // 여기서 SockJS 연결 허용, CORS 허용, Handshake 인증 절차 적용
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {

        registry.addEndpoint("/ws/chat")

                // 웹소켓 연결을 허용할 프론트엔드 도메인
                .setAllowedOrigins(
                        "http://localhost:3000",
                        "http://mailbuddy-s3-fe.s3-website.ap-northeast-2.amazonaws.com"
                )

                // 웹소켓 Handshake 단계에서 JWT 검증을 수행
                // jwtUtil + userSecurityService 둘 다 필요함
                .addInterceptors(new WsAuthHandshakeInterceptor(jwtUtil, userSecurityService))

                // WebSocket 미지원 브라우저 대비 SockJS fallback 활성화
                .withSockJS();
    }

    // 메시지 라우팅 규칙 설정
    // "/app" → 클라이언트 → 서버 (메시지 처리)
    // "/topic" → 서버 → 클라이언트 구독 대상에게 전달
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {

        // 서버에서 클라이언트로 푸시될 메시지 경로 prefix
        registry.enableSimpleBroker("/topic");

        // 클라이언트에서 서버로 메시지 보낼 때 prefix
        registry.setApplicationDestinationPrefixes("/app");
    }

    // STOMP 메시지가 서버로 들어오는 단계에서 인증 처리 (JWT 검증)
    // Handshake 이후의 실제 메시지 단계에서 사용자 정보 확인
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(webSocketAuthInterceptor);
    }
}
