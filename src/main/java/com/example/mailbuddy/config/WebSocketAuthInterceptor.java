package com.example.mailbuddy.config;

import com.example.mailbuddy.jwt.JwtUtil;
import com.example.mailbuddy.service.UserSecurityService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * STOMP 연결 단계 인증 처리
 * - 클라이언트가 WebSocket CONNECT 요청을 보낼 때 Authorization 헤더에 실린 JWT를 검증
 * - 검증 성공 시 인증 객체(Authentication)를 STOMP 세션에 등록
 * - 이후 STOMP 메시지(@MessageMapping)에서 Authentication 파라미터 사용 가능
 */
@Component
@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private final JwtUtil jwtUtil;
    private final UserSecurityService userSecurityService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {

        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);

        // STOMP CONNECT 단계에서만 JWT 검사 실행
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {

            // 클라이언트가 보낸 Authorization 헤더(Bearer 토큰)
            String authHeader = accessor.getFirstNativeHeader("Authorization");

            // 토큰 없으면 연결 자체 차단
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                System.err.println("WebSocket CONNECT: Authorization 헤더 없음");
                return null;
            }

            String token = authHeader.substring(7);

            try {
                // JWT 내부 username 파싱
                String username = jwtUtil.extractUsername(token);

                // username 기반으로 UserDetails 로드
                var userDetails = userSecurityService.loadUserByUsername(username);

                // 실제 토큰 유효성 검증
                if (!jwtUtil.validateToken(token, userDetails)) {
                    System.err.println("WebSocket CONNECT: JWT 검증 실패");
                    return null;
                }

                // STOMP 세션에 저장될 인증 객체 생성
                Authentication auth = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                );

                // STOMP 세션에 인증정보 주입
                accessor.setUser(auth);

            } catch (Exception e) {
                System.err.println("WebSocket CONNECT Exception: " + e.getMessage());
                return null;
            }
        }

        // 수정된 Header 정보를 반영한 STOMP message 반환
        return MessageBuilder.createMessage(
                message.getPayload(),
                accessor.getMessageHeaders()
        );
    }

}
