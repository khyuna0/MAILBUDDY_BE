package com.example.mailbuddy.service;

import com.example.mailbuddy.repository.GmailRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Service;

@Service
public class GmailService {

    @Autowired
    private GmailRepository gmailRepository;

    private final OAuth2AuthorizedClientService authorizedClientService;
    public GmailService(OAuth2AuthorizedClientService authorizedClientService) {
        this.authorizedClientService = authorizedClientService;
    }

    // 인증된 사용자 정보에서 OAuth2 클라이언트 + Token 가져오기
    public String getUserToken(OAuth2AuthenticationToken authentication) {
        String clientRegistrationId = authentication.getAuthorizedClientRegistrationId();
        OAuth2AuthorizedClient client = authorizedClientService.loadAuthorizedClient(
                clientRegistrationId,
                authentication.getName()
        );
        return client.getAccessToken().getTokenValue();
    }


}
