package com.example.mailbuddy.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.Map;

@RestController // ✅ JSON 응답용
@RequestMapping("/api/weather")
public class WeatherController {

    @GetMapping
    public ResponseEntity<?> getWeather(
            @RequestParam double lat, // 위도
            @RequestParam double lon, // 경도
            @RequestParam(required = false) String date // YYYY-MM-DD 형식
    ) {
        // 오늘 날짜
        LocalDate today = LocalDate.now();
        LocalDate target = (date != null) ? LocalDate.parse(date) : today;

        RestTemplate restTemplate = new RestTemplate();
        String url;

        // 🕒 오늘이면 → current 날씨
        if (target.isEqual(today)) {
            url = String.format(
                    "https://api.open-meteo.com/v1/forecast?latitude=%f&longitude=%f&current=weathercode&timezone=Asia/Seoul",
                    lat, lon
            );
        }
        // 🔮 미래 날짜면 → daily 예보 (최대 16일)
        else if (target.isAfter(today) && target.isBefore(today.plusDays(17))) {
            url = String.format(
                    "https://api.open-meteo.com/v1/forecast?latitude=%f&longitude=%f&daily=weathercode&timezone=Asia/Seoul&start_date=%s&end_date=%s",
                    lat, lon, date, date
            );
        }
        // 16일 이후 or 과거 → 기본값
        else {
            return ResponseEntity.ok(Map.of("weathercode", -1)); // -1 = 알 수 없음
        }

        // ✅ 응답을 JSON 형태로 반환
        Map<?, ?> result = restTemplate.getForObject(url, Map.class);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/eventTime")
    public ResponseEntity<?> eventTimeWeather(
            @RequestParam double lat,
            @RequestParam double lon,
            @RequestParam String time // 예: "2025-11-10T01:00"
    ) {
        // 날짜만 추출
        String date = time.substring(0, 10);

        // 하루치 시간별 예보 요청
        String url = String.format(
                "https://api.open-meteo.com/v1/forecast?latitude=%f&longitude=%f"
                        + "&hourly=weathercode&timezone=Asia/Seoul"
                        + "&start_date=%s&end_date=%s",
                lat, lon, date, date
        );

        RestTemplate restTemplate = new RestTemplate();
        Map<?, ?> result = restTemplate.getForObject(url, Map.class);

        // 시간별 데이터 추출
        Map<?, ?> hourly = (Map<?, ?>) result.get("hourly");
        if (hourly != null) {
            var times = (java.util.List<String>) hourly.get("time");
            var codes = (java.util.List<Integer>) hourly.get("weathercode");

            for (int i = 0; i < times.size(); i++) {
                // "2025-11-10T01" 같은 시단위 비교
                if (times.get(i).startsWith(time.substring(0, 13))) {
                    return ResponseEntity.ok(Map.of(
                            "time", times.get(i),
                            "weathercode", codes.get(i)
                    ));
                }
            }
        }
        // 못 찾을 경우
        return ResponseEntity.ok(Map.of("weathercode", -1));
    }

}
