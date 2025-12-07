package com.example.mailbuddy.service;

import com.example.mailbuddy.dto.SummaryDto;
import com.example.mailbuddy.entity.Gmail;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class GeminiService {

    // Gemini -> Mistral 설정으로 변경
    @Value("${mistral.api.key}")
    private String apiKey;

    @Value("${mistral.api.url}")
    private String apiUrl;

    @Value("${mistral.api.model:mistral-small-latest}")
    private String model;

    // 별도 bean/qualifier 안 쓰고, 여기서 builder로 생성
    private final WebClient webClient = WebClient.builder().build();

    private final ObjectMapper om = new ObjectMapper();
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private String callGemini(String prompt) {
        // Mistral chat completions 요청 바디
        Map<String, Object> requestBody = Map.of(
                "model", model,
                "messages", List.of(
                        Map.of(
                                "role", "user",
                                "content", prompt
                        )
                )
                // JSON만 강제하고 싶으면:
                // "response_format", Map.of("type", "json_object")
        );

        System.out.println("[Mistral REQUEST] " + safeJson(requestBody));
        System.out.println("[Mistral URL] " + apiUrl);

        // Mistral 응답은
        // { choices: [ { message: { content: "..."} } ] } 구조
        Map<?, ?> response = webClient.post()
                .uri(apiUrl)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(Map.class)
                .onErrorReturn(Collections.emptyMap())
                .block();

        if (response == null || response.isEmpty()) {
            System.out.println("[Mistral RESPONSE] empty/null");
            return "";
        }

        Object rawChoices = response.get("choices");
        if (!(rawChoices instanceof List<?> choices) || choices.isEmpty()) {
            System.out.println("[Mistral RESPONSE] no choices");
            return "";
        }

        Object firstObj = choices.get(0);
        if (!(firstObj instanceof Map<?, ?> first)) return "";

        Object msgObj = first.get("message");
        if (!(msgObj instanceof Map<?, ?> message)) return "";

        Object contentObj = message.get("content");
        if (!(contentObj instanceof String content)) return "";

        String result = content.trim();
        System.out.println("[Mistral RAW] " + result);
        return result;
    }

    // 프롬프트 + 이메일 텍스트 -> Mistral 호출 -> JSON 파싱 -> SummaryDto
    public SummaryDto summarizeToDto(String text) {
        String prompt = """
    당신의 최우선 규칙은 다음과 같습니다.
    1) '캘린더 일정으로 활용할 수 없는 메일'은 절대로 요약하지 않습니다.
    2) 그런 메일은 아래 JSON의 모든 필드를 null 로 설정합니다.
    3) 이 규칙을 어기는 응답은 잘못된 응답입니다.
    
    당신은 이메일 내용을 분석하여 캘린더 이벤트에 필요한 핵심 정보를 정확히 추출하는 전문 AI 비서입니다.
    반드시 오직 하나의 JSON 객체만 반환하세요. JSON 바깥의 텍스트/설명/코드펜스는 금지합니다.
    
    [목표]
    일정 추가용이기 때문에 캘린더에 추가할 내용이 아닌 광고, 그냥 알림 메일은 모두 버리고 요약하지 않습니다.
    - title: 메일 제목을 그대로 쓰지 말고, 제목+내용을 보고 '내 일정' 형태로 한 줄 요약 (예: 홍길동과 점심 미팅)
    - eventDate: 약속 날짜입니다. 가능하면 최대한 'YYYY-MM-DD' (불가하면 null)
    - eventTime: 약속 시간입니다. 가능하면 최대한 'HH:mm' (불가하면 null)
    - place: 장소명/지명/주소 중 하나 (불가하면 null)
    - name/email: 발신자 이름/이메일
    
    [분류 규칙]
    1) 아래와 같은 메일은 '일정으로 활용할 수 없는 메일'로 간주하고, 모든 필드를 null로 설정합니다.
       - 광고/프로모션/뉴스레터/마케팅 메일
         (예: 쇼핑몰/브랜드/헬스보충제/의류 사이트 등에서 보내는 할인, 세일, 쿠폰, 특가 안내 메일.
              Myprotein, 쿠팡, 11번가, 무신사 등.)
       - 시스템 알림, 보안 알림(예: Google 보안 알림, 로그인 알림, 비밀번호 변경 안내)
       - Google 계정/보안 관련 메일 (2단계 인증, 새로운 로그인, 앱 접근 허용/차단 안내 등)
       - 결제/주문확인/배송/영수증/청구서 메일
       - 구독 확인/계정 설정/약관 변경 등의 알림
       - 서비스 가입/회원가입/계정 생성/환영 메일
         (예: "Hi 경미, welcome to Google AI Studio", "서비스 가입을 환영합니다" 등)
    
    2) 특히 다음 조건 중 하나라도 만족하면 **무조건** 일정이 아니라고 판단하고,
       아래 JSON의 모든 필드를 null 로 설정합니다. 예외는 없습니다.
       - 발신자 이메일에 'noreply' 또는 'no-reply' 문자열이 포함되는 경우
         (예: 'qoogleaistudio-noreply@google.com', 'noreply@accounts.google.com' 등).
       - 발신자 이메일이 다음과 같이 광고/뉴스레터 성격인 경우:
         'info@', 'newsletter@', 'marketing@', 'event@' 로 시작하거나
         도메인이 명백한 쇼핑몰/브랜드 도메인인 경우 (예: 'info@n.myprotein.com').
       - 제목 혹은 내용에 다음 광고/할인 키워드가 포함되며,
         메일 전체가 상품/할인/쿠폰 안내인 경우:
         '할인', '세일', '특가', '쿠폰', '프로모션', '딜', '무료배송',
         'SALE', 'EVENT', 'OFF', 'UP TO', '%', '원 할인', '적립금',
         '블랙프라이데이', '쇼핑 지원금', '핫딜'
       - 제목 혹은 내용에 다음과 같은 환영/가입 안내 키워드가 포함되며,
         구체적인 오프라인/온라인 모임의 날짜·시간·장소가 함께 나오지 않는 경우:
         'welcome to', '환영합니다', '가입이 완료', '계정이 생성',
         '지금 시작해 보세요', '시작하기 가이드', '튜토리얼', '도움말 센터', '온보딩'
       - 구인광고 , 인쿠르트 , 사람인 등 구인은 제외해도 됩니다. 
       - 여행상품, skyscanner등 이런 메일도 제외해주세요.
    
       위 조건에 해당하면 title, eventDate, eventTime, place, notes, name, email
       모든 값이 null 이어야만 올바른 응답입니다.
    
    3) 회의, 약속, 수업, 모임, 행사, 파티, 웨비나처럼
       사람이 어디에 언제쯤 가거나 참여하는 상황이면
       날짜/시간/장소가 다 정확하지 않아도 일정으로 간주하고 title은 반드시 채웁니다.
       단, **할인 기간, 세일 기간, 온라인 쇼핑 이벤트 기간**은
       사람이 특정 장소/시간에 가야 하는 일정이 아니므로 광고로 간주하고 모두 null 로 처리합니다.
    
    4) 위 언급 내용외에 서버점검, 회사 긴급공지 등 **회사 관련 메일**인 경우는
       캘린더로 관리하는 것이 유용하므로 반드시 요약 + 반환해줍니다. 
    
    [중요: eventDate 작성 규칙]
    오늘, 내일, 모레 같은 날짜도 메일 보낸 날짜 기준으로 실제 날짜(YYYY-MM-DD)로 변환해서 반환해줍니다.
    날짜를 알 수 없거나 애매하면 eventDate는 null 로 둡니다.
    크리스마스, 광복절, 식목일 등등 공휴일 같은 일정은 날짜(YYYY-MM-DD)로 변환해서 반환해줍니다.
    
    [중요: eventTime 작성 규칙]
    1) 약속 시간은 상대방이 제안한 시간을 기준으로 추출합니다.
    2) '오전/AM', '오후/PM', '저녁', '밤', '새벽' 등의 표현을 우선적으로 사용해 24시간제로 변환합니다.
       - 예: '오후 1시' → '13:00', '오후 3시 반' → '15:30', '밤 9시' → '21:00'.
    3) '1시에 만나자', '3시쯤 보자'처럼 오전/오후 표기가 없고 숫자만 있을 때는,
       일반적인 약속 시간(점심/오후 모임)으로 가정하고 '13:00', '15:00'처럼 오후 시간대로 변환합니다.
       - 예: '1시에 만나자' → '13:00'
    4) '1시 30분', '1시 반' → '13:30'처럼 분까지 최대한 반영합니다.
    
    [중요: description(=notes) 작성 규칙]
    description은 원문을 복붙하지 말고, title/eventDate/eventTime/place/name/email에 이미 들어간 정보를 제외한
    추가로 알아야 할 포인트만 간략 요약합니다.
    포함할 것: 함께 만나는 사람, 준비물, 주의사항, 변경사항, 액션아이템(할 일), 주최측 요청사항 등.
    한국어로 간단히 작성합니다. (원문이 영어/외국어여도 한국어로 요약)
    형식: 불릿 1~3개, 각 불릿은 12단어(또는 60자) 이내의 짧은 문장.
    포함 금지: 긴 원문 인용, 이메일 서명/주소록/광고문구, 'Subject:', 'From:' 같은 라벨, URL 나열.
    중복 금지: title/eventDate/eventTime/place/name/email에 이미 들어간 정보는 다시 쓰지 않습니다.
    적을 내용이 없다면 notes는 null로 둡니다.
    
    [예시 1: 일정 관련 메일]
    이메일 내용:
    [제목] 우리 내일
    [내용] 여의도 한강공원 가자!! 길동이랑 1시에 만나자! 맛있는거 먹자!
    → 올바른 JSON 예시:
    {
      "title": "여의도 한강공원에서 친구들과 만남",
      "eventDate": null,
      "eventTime": "13:00",
      "place": "여의도 한강공원",
      "notes": "- 길동 포함 2명 이상과 만남",
      "name": "최경미",
      "email": "ckm1533@naver.com"
    }
    
    [예시 2: 광고 메일 – 반드시 모든 필드 null]
    이메일 내용:
    [제목] 최대 70% 할인 시작 🖤 블랙프라이데이
    [발신자] Myprotein <info@n.myprotein.com>
    [내용] 이번 주말까지 단백질 제품 최대 70% 할인! 지금 쇼핑하세요.
    → 올바른 JSON 예시:
    {
      "title": null,
      "eventDate": null,
      "eventTime": null,
      "place": null,
      "notes": null,
      "name": null,
      "email": null
    }
    
    [잘못된 응답 예시]
    발신자: no-reply@accounts.google.com
    제목: 보안 알림
    → 아래와 같이 title 을 채우는 것은 잘못된 응답입니다. 이런 경우에는 모든 필드가 null 이어야 합니다.
    {
      "title": "보안 알림",  // 이렇게 채우면 안 됨
      "eventDate": null,
      "eventTime": null,
      "place": null,
      "notes": null,
      "name": null,
      "email": null
    }
    
    위 예시는 설명을 위한 것일 뿐입니다. 실제 응답에서는 아래 형식의 JSON 객체 한 개만 출력하세요.
    반드시 다음 JSON 형식으로만 응답합니다 (필드 이름과 구조를 그대로 지키세요):
    {
      "title": "string|null",
      "eventDate": "YYYY-MM-DD|null",
      "eventTime": "HH:mm|null",
      "place": "string|null",
      "notes": "string|null",
      "name": "string|null",
      "email": "string|null"
    }
    
    이메일 데이터:
    
    """ + text;
    

        String raw = callGemini(prompt);

        String json = stripToJson(raw);
        System.out.println("[Mistral JSON] " + json);

        if (json.isBlank() || json.charAt(0) != '{') {
            return new SummaryDto();
        }

        try {
            return om.readValue(json, SummaryDto.class);
        } catch (Exception e) {
            System.err.println("[Mistral JSON PARSE ERROR] " + e.getMessage());
            return new SummaryDto();
        }
    }

    // 브리핑용: prompt 전체를 넘기면 1~2줄의 일반 텍스트를 그대로 반환
    public String generateDailyBriefText(String prompt) {
        String raw = callGemini(prompt);
        if (raw == null) return "";
        return raw.trim();
    }

    // 1. Gmail 엔티티 -> 이메일 블록 텍스트 -> 요약 DTO
    public SummaryDto summarizeFromGmail(Gmail gmail) {
        String emailBlock = toEmailBlock(gmail);
        return summarizeToDto(emailBlock);
    }

    // 2. Gmail 엔티티를 프롬프트용 블록으로 변환 (본문 길면 앞/뒤만 유지)
    private String toEmailBlock(Gmail gmail) {
        ZonedDateTime zdt = gmail.getReceivedTime() != null
                ? gmail.getReceivedTime().atZone(KST)
                : ZonedDateTime.now(KST);
        DateTimeFormatter rfcLike = DateTimeFormatter.ofPattern("EEE, d MMM yyyy HH:mm:ss Z", Locale.ENGLISH);
        String received = zdt.format(rfcLike);

        String content = nz(gmail.getContent());
        if (content.length() > 8000) {
            content = content.substring(0, 4000) + "\n...\n" + content.substring(content.length() - 4000);
        }

        return """
                [발신자 이름] %s
                [발신자 이메일] %s
                [받은 시간] %s
                [제목] %s
                [내용] %s
                """.formatted(
                nz(gmail.getSenderName()),
                nz(gmail.getSenderEmail()),
                received,
                nz(gmail.getSubject()),
                content
        );
    }

    private static String nz(String s) { return s == null ? "" : s; }

    // AI가 보낸 텍스트가 코드펜스나 설명과 섞여 있어도 순수 JSON 부분만 추출
    private static String stripToJson(String s) {
        String t = s == null ? "" : s.trim();
        if (t.isEmpty()) return t;
        if (t.startsWith("```")) {
            int first = t.indexOf('{');
            int last = t.lastIndexOf('}');
            if (first >= 0 && last > first) return t.substring(first, last + 1);
        }
        int first = t.indexOf('{');
        int last = t.lastIndexOf('}');
        if (first >= 0 && last > first) return t.substring(first, last + 1);
        return t;
    }

    private String safeJson(Object o) {
        try { return om.writeValueAsString(o); }
        catch (Exception e) { return String.valueOf(o); }
    }
}
