package com.example.mailbuddy.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.Map;


// 테스트용임. 지워도 ok
@RestController
@RequestMapping("/api/geocode")
public class GeocodeController { // 장소명으로 위도, 경도 찾기 api

    @GetMapping
    public ResponseEntity<?> getLatLon (@RequestParam String name) {
        String url = "https://geocoding-api.open-meteo.com/v1/search?name=" + name + "&count=1&language=ko";
        RestTemplate restTemplate = new RestTemplate();
        Map <?, ?> result = restTemplate.getForObject(url, Map.class);
        return ResponseEntity.ok(result);
    }
}
