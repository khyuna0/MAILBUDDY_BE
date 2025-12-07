package com.example.mailbuddy.config;

import com.example.mailbuddy.entity.User;
import com.example.mailbuddy.handler.CustomOAuth2SuccessHandler;
import com.example.mailbuddy.handler.OAuth2LoginFailureHandler;
import com.example.mailbuddy.jwt.JwtAuthenticationFilter;
import com.example.mailbuddy.jwt.JwtUtil;
import com.example.mailbuddy.repository.UserRepository;
import com.example.mailbuddy.service.CustomOAuth2UserService;
import com.example.mailbuddy.service.UserSecurityService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;


import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private CustomOAuth2UserService customOAuth2UserService;

    @Autowired
    private OAuth2LoginFailureHandler oauth2LoginFailureHandler;

    @Autowired
    private ClientRegistrationRepository clientRegistrationRepository;

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Autowired
    private UserSecurityService userSecurityService;

    // JWT에서 username 꺼내기 위해
    @Autowired
    private JwtUtil jwtUtil;

    // 유저 / refresh_token 확인 위해
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CustomOAuth2SuccessHandler customOAuth2SuccessHandler;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())

                // JWT 기준으로 stateless, 다만 HttpSession 자체는 OAuth2에서 내부적으로 사용 가능
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()


                        // WebSocket 핸드셰이크는 JWT 말고 STOMP 단계에서 따로 인증할 거라 HTTP는 열어둠
                        .requestMatchers("/ws/**").permitAll()


                        // 공개 API
                        .requestMatchers("/", "/index.html", "/login", "/error").permitAll()
                        .requestMatchers("/api/auth/login", "/api/auth/signup").permitAll()

                        // OAuth2 시작/콜백은 공개
                        .requestMatchers("/oauth2/**", "/login/oauth2/**").permitAll()

                        // 권한 분리
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/user/**").hasRole("USER")

                        // 나머지는 모두 인증 필요(JWT)
                        .anyRequest().authenticated()
                )

                // 폼 로그인은 사용하지 않음 (JWT 로그인으로 대체)
                .formLogin(form -> form.disable())

                // OAuth2 로그인 (구글 연동용)
                .oauth2Login(oauth2 -> oauth2
                        .loginPage("/login").permitAll()
                        .authorizationEndpoint(authz -> authz
                                .authorizationRequestResolver(customOAuth2AuthorizationRequestResolver())
                        )
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(customOAuth2UserService)
                        )
                        .failureHandler(oauth2LoginFailureHandler)
                        // 구글 refresh 토큰저장  , 성공 핸들러
                        .successHandler(customOAuth2SuccessHandler)
                );

        // JWT 필터를 UsernamePasswordAuthenticationFilter 앞에 추가
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // AuthenticationManager 주입 (AuthController에서 사용)
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    // OAuth2 Authorization Request를 커스터마이징 -> prompt=select_account + jwt_token 세션에 저장
    @Bean
    public OAuth2AuthorizationRequestResolver customOAuth2AuthorizationRequestResolver() {

        DefaultOAuth2AuthorizationRequestResolver defaultResolver =
                new DefaultOAuth2AuthorizationRequestResolver(
                        clientRegistrationRepository,
                        "/oauth2/authorization"
                );

        return new OAuth2AuthorizationRequestResolver() {

            @Override
            public OAuth2AuthorizationRequest resolve(HttpServletRequest request) {
                saveJwtTokenToSession(request);
                OAuth2AuthorizationRequest authorizationRequest = defaultResolver.resolve(request);
                return customize(request, authorizationRequest);   // ★ request 같이 전달
            }

            @Override
            public OAuth2AuthorizationRequest resolve(HttpServletRequest request,
                                                      String clientRegistrationId) {
                saveJwtTokenToSession(request);
                OAuth2AuthorizationRequest authorizationRequest =
                        defaultResolver.resolve(request, clientRegistrationId);
                return customize(request, authorizationRequest);   // ★ request 같이 전달
            }

            // 프론트에서 넘긴 jwt token을 세션에 저장
            private void saveJwtTokenToSession(HttpServletRequest request) {
                String jwt = request.getParameter("jwt_token");
                if (jwt != null && !jwt.isEmpty()) {
                    request.getSession(true).setAttribute("LINK_JWT", jwt);
                }
            }

            // ★ 여기서 HttpServletRequest 를 같이 받아서 사용
            private OAuth2AuthorizationRequest customize(
                    HttpServletRequest request,
                    OAuth2AuthorizationRequest authorizationRequest
            ) {
                if (authorizationRequest == null) {
                    return null;
                }

                // 1. 기존 파라미터 복사
                Map<String, Object> additionalParameters =
                        new HashMap<>(authorizationRequest.getAdditionalParameters());

                // 2. 세션에서 LINK_JWT 꺼내서 username 찾기
                HttpSession session = request.getSession(false);
                String jwt = null;
                if (session != null) {
                    Object obj = session.getAttribute("LINK_JWT");
                    if (obj instanceof String s) {
                        jwt = s;
                    }
                }

                // 기본값: 아직 누구인지 모르거나 에러 -> "처음 동의" 가정 → consent
                boolean hasRefreshToken = false;
                if (jwt != null) {
                    try {
                        String username = jwtUtil.extractUsername(jwt);
                        Optional<User> optionalUser = userRepository.findByUsername(username);

                        if (optionalUser.isPresent()) {
                            User user = optionalUser.get();
                            // DB에 refresh_token 이 이미 저장되어 있으면 true
                            hasRefreshToken = (user.getGoogleRefreshToken() != null);
                        }
                    } catch (Exception e) {
                        // JWT 파싱 실패하면 그냥 hasRefreshToken=false 로 둠
                        System.out.println("JWT 파싱 실패 (prompt 결정 시): " + e.getMessage());
                    }
                }

                // 3. offline 모드는 항상 유지 (refresh_token 받을 수 있게)
                additionalParameters.put("access_type", "offline");

                // 4. 유저별로 prompt 다르게
                if (hasRefreshToken) {
                    // 이미 refresh_token 받은 유저 → 동의 화면 필요 없음, 계정 선택 정도만
                    additionalParameters.put("prompt", "select_account");
                    System.out.println("[OAuth2] 기존 refresh_token 보유 → prompt=select_account");
                } else {
                    // 아직 refresh_token 없는 유저 → 동의 화면 통해 최초 한 번 받아야 함
                    additionalParameters.put("prompt", "consent");
                    System.out.println("[OAuth2] refresh_token 없음 → prompt=consent");
                }

                return OAuth2AuthorizationRequest.from(authorizationRequest)
                        .additionalParameters(additionalParameters)
                        .build();
            }
        };
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:3000",  "http://mailbuddy-s3-fe.s3-website.ap-northeast-2.amazonaws.com"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
