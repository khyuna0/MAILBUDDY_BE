package com.example.mailbuddy.service;

import com.example.mailbuddy.dto.DailyBriefResponse;
import com.example.mailbuddy.entity.Schedules;
import com.example.mailbuddy.entity.Summary;
import com.example.mailbuddy.entity.User;
import com.example.mailbuddy.repository.SchedulesRepository;
import com.example.mailbuddy.repository.SummaryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DailyBriefService {

    private final SchedulesRepository schedulesRepository;
    private final SummaryRepository summaryRepository;
    private final GeminiService geminiService;

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    // 특정 사용자 + 날짜 기준으로 local + summary 일정들을 모아서 AI 브리핑을 생성
    public DailyBriefResponse createDailyBrief(User user, LocalDate date) {
        // 1) 해당 날짜의 local / summary 일정 조회
        List<Schedules> localEvents =
                schedulesRepository.findByUserAndEventDate(user, date);

        List<Summary> summaryEvents =
                summaryRepository.findByGmail_User_IdAndEventDate(user.getId(), date);

        if (localEvents.isEmpty() && summaryEvents.isEmpty()) {
            return DailyBriefResponse.builder()
                    .hasEvents(false)
                    .build();
        }



        // AI에게 줄 프롬프트 생성
        String prompt = buildPromptForBrief(date, localEvents, summaryEvents);

        // Gemini 호출해서 1~2줄 문장 받기
        String raw = geminiService.generateDailyBriefText(prompt);

        // 줄바꿈 기준으로 메인/서브 라인 분리
        String mainLine = null;
        String subLine = null;
        if (raw != null) {
            String[] lines = raw.split("\\r?\\n");
            if (lines.length > 0) mainLine = lines[0].trim();
            if (lines.length > 1) subLine = lines[1].trim();
        }



        return DailyBriefResponse.builder() // builder?
                .hasEvents(true)
                .mainLine(mainLine)
                .subLine(subLine)
                .build();
    }

    private String buildPromptForBrief(
            LocalDate date,
            List<Schedules> localEvents,
            List<Summary> summaryEvents
    ) {
        StringBuilder sb = new StringBuilder();

        sb.append("당신은 일정 관리 서비스 'MailBuddy'의 브리핑 어시스턴트입니다.\n");
        sb.append("사용자가 선택한 날짜에 등록한 모든 일정을 보고, 한국어로 1~2줄의 짧은 브리핑을 만들어야 합니다.\n\n");

        sb.append("선택된 날짜: ").append(date).append("\n\n");

        sb.append("이 날짜의 전체 일정 목록은 다음과 같습니다.\n");
        sb.append("각 일정은 [출처] [시간] [제목] [장소] [메모] 형식으로 제공됩니다.\n\n");

        for (Summary ev : summaryEvents) {
            sb.append("- [AI 요약] ");
            if (ev.getEventTime() != null) {
                sb.append(ev.getEventTime().format(TIME_FMT)).append(" ");
            }
            sb.append("[제목: ").append(safe(ev.getTitle())).append("] ");
            if (ev.getPlace() != null) {
                sb.append("[장소: ").append(safe(ev.getPlace())).append("] ");
            }
            if (ev.getNotes() != null) {
                sb.append("[메모: ").append(safe(ev.getNotes())).append("] ");
            }
            sb.append("\n");
        }

        for (Schedules ev : localEvents) {
            sb.append("- [사용자 등록] ");
            if (ev.getEventTime() != null) {
                sb.append(ev.getEventTime().format(TIME_FMT)).append(" ");
            }
            sb.append("[제목: ").append(safe(ev.getTitle())).append("] ");
            if (ev.getPlace() != null) {
                sb.append("[장소: ").append(safe(ev.getPlace())).append("] ");
            }
            if (ev.getNotes() != null) {
                sb.append("[메모: ").append(safe(ev.getNotes())).append("] ");
            }
            sb.append("\n");
        }


        sb.append("\n요구사항:\n");
        sb.append("1. 위 일정을 모두 고려해서, 다음 요소를 종합해 말투와 내용을 정해주세요.\n");
        sb.append("   - 전체 일정의 양과 대략적인 분포(아침/오후/저녁)\n");
        sb.append("   - 주로 머무르는 장소가 실내인지, 외출/이동이 많은지\n");
        sb.append("   - 제공된 날씨 정보(특히 주요 약속 장소의 날씨: 비/눈/더위/추위/강풍 등)\n");
        sb.append("2. 사용자가 '오늘 하루를 한눈에 이해할 수 있게' 느끼도록, 핵심 분위기와 주의할 점을 중심으로 요약해 주세요.\n");
        sb.append("3. 친근한 서비스 톤으로, 너무 딱딱하지 않게 작성합니다.\n");
        sb.append("4. 출력 형식은 오직 브리핑 문장만 1~2줄로, 줄바꿈은 '\\n' 하나로만 사용합니다.\n");
        sb.append("5. 불필요한 설명, 따옴표, 마크다운, 머리말(예: '브리핑:')은 넣지 않습니다.\n");
        sb.append("6. 날짜나 일정 개수를 그대로 나열하기보다는, 사용자가 느낄 분위기/주의점/꿀팁 위주로 표현합니다.\n");
        sb.append("7. 날씨 정보가 있을 경우, 주요 약속 장소의 날씨를 반영해 우산/겉옷/더위·추위 대비, 이동 시간 여유 등 실질적인 조언을 한 줄 이상 포함해 주세요.\n\n");
        sb.append("8. 요약시 가끔 \n도 같이 출력되는데 , 요약과 함께 출력되지 않게 확인하고 반환해주세요");

        // 예시 블록
        sb.append("예시 1)\n");
        sb.append("오전에는 회의, 오후에는 병원과 약속까지 꽉 찬 하루네요! 이동 시간이 겹치지 않도록 여유 있게 준비해 보세요.\n");
        sb.append("비 예보가 있으니 우산도 미리 챙겨두면 좋겠어요.\n\n");

        sb.append("예시 2)\n");
        sb.append("오늘은 짧은 일정 몇 개만 있어서 비교적 여유로운 하루예요.\n");
        sb.append("중간중간 쉬는 시간도 넣어서 컨디션 조절해 보세요.\n\n");

        sb.append("=> 위 예시는 참고용일 뿐이며, 실제 일정과 날씨 정보에 맞는 문장을 새로 생성해주세요.\n");
        sb.append("최종 출력은 1~2줄의 브리핑 문장만 반환하세요.");

        return sb.toString();
    }

    // null 방어 + 줄바꿈 제거 + 공백 정리 -> 프롬프트 넣을때 들어가는 일정을 한 줄로 안정적,깔끔하게 만들기
    private String safe(String s) {
        return s == null ? "" : s.replace("\n", " ").trim();
    }
}