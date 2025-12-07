package com.example.mailbuddy.controller;

import com.example.mailbuddy.entity.User;
import com.example.mailbuddy.repository.GmailRepository;
import com.example.mailbuddy.repository.SummaryRepository;
import com.example.mailbuddy.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/address")
public class AddressController {

    @Autowired
    private SummaryRepository summaryRepository;
    @Autowired
    private UserRepository userRepository;

    // 구글 로그인한 유저의 주소록 가져오기 (이미 메일 많이 보낸 순으로 정렬이 되어 있음)
    @GetMapping("/list")
    public ResponseEntity<?> getAddressesByUser(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "로그인 정보가 없습니다."));
        }
        // 사용자 조회
        User user = userRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new UsernameNotFoundException("사용자 정보가 없습니다."));

        List<Object[]> results = summaryRepository.findSendersByUserIdWithMailCountOrderByCountDesc(user.getId());
        List<Map<String, Object>> addresses = new ArrayList<>();
        for (Object[] obj : results) {
            Map<String, Object> address = new HashMap<>();
            address.put("senderEmail", obj[0]);
            address.put("senderName", obj[1]);
            address.put("mailCount", obj[2]);
            addresses.add(address);
        }
        // addresses[{senderName=김민지, senderEmail=abc@naver.com, mailCount=4},
        // {senderName=최한나, senderEmail=z0z0@naver.com, mailCount=3},
        // {senderName=Emma Kim, senderEmail=haha55@gmail.com, mailCount=2}]
        return ResponseEntity.ok(addresses);
    }
}