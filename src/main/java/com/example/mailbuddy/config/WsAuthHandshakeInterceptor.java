package com.example.mailbuddy.config;

import com.example.mailbuddy.jwt.JwtUtil;
import com.example.mailbuddy.service.UserSecurityService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.net.URI;
import java.util.Map;

/**
 * WebSocket 최초 Handshake 단계 인증 처리
 * - WebSocket 연결 직전에 실행
 * - 클라이언트가 /ws/chat?token=JWT 형식으로 보낸 JWT를 검증
 * - 검증 성공 시 Authentication을 WebSocket 세션에 저장
 * - 이후 STOMP 메시지에서도 인증정보 사용 가능
 */
@RequiredArgsConstructor
public class WsAuthHandshakeInterceptor implements HandshakeInterceptor {

    private final JwtUtil jwtUtil;
    private final UserSecurityService userSecurityService;

    @Override
    public boolean beforeHandshake(
            @NonNull ServerHttpRequest request,
            @NonNull ServerHttpResponse response,
            @NonNull WebSocketHandler wsHandler,
            @NonNull Map<String, Object> attributes
    ) {

        // WebSocket 연결 URI 추출
        URI uri = request.getURI();
        String query = uri.getQuery(); // 예: token=xxxx

        // 쿼리에 token 파라미터가 없으면 연결 차단
        if (query == null || !query.contains("token=")) {
            System.err.println("WebSocket Handshake 실패: Query에 token 없음");
            return false;
        }

        // token= 뒤에 오는 JWT 문자열 추출
        String token = query.substring(query.indexOf("token=") + 6);

        try {
            // 1) JWT 내부 username 파싱
            String username = jwtUtil.extractUsername(token);

            // 2) UserDetails 조회 (권한 포함)
            UserDetails userDetails = userSecurityService.loadUserByUsername(username);

            // 3) 토큰 유효성 검사
            if (!jwtUtil.validateToken(token, userDetails)) {
                System.err.println("Handshake 실패: JWT 검증 오류");
                return false;
            }

            // 4) 인증 객체 생성
            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );

            // 스프링 SecurityContext에 인증 저장
            SecurityContextHolder.getContext().setAuthentication(auth);

            // WebSocket 세션 Attributes에도 저장
            attributes.put("username", username);
            attributes.put("auth", auth);

            System.out.println("WebSocket Handshake 성공: " + username);
            return true;

        } catch (Exception e) {
            System.err.println("Handshake Exception: " + e.getMessage());
            return false;
        }
    }

    @Override
    public void afterHandshake(
            @NonNull ServerHttpRequest request,
            @NonNull ServerHttpResponse response,
            @NonNull WebSocketHandler wsHandler,
            Exception exception
    ) {
        // Handshake 이후 처리 없음
    }
}
