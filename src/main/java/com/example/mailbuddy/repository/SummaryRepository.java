package com.example.mailbuddy.repository;


import com.example.mailbuddy.entity.Summary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SummaryRepository extends JpaRepository<Summary, Long> {

    // 이미 요약한 내용 중복들어가지 않게 처리  -> gmail_id 입력해서 요약 불러오는 경우 (여러번 누르면 똑같은 내용 쌓임)
    boolean existsByGmail_Id(Long gmailId);
    Optional<Summary> findFirstByGmail_Id(Long gmailId);

    // 로그인한 유저 기준, 발신자 이름, 발신자 이메일 가져와서 요약된 메일 많이 보낸 순으로 정렬하기
    // Object[] 배열엔 [senderEmail, senderName, mailCount]
    @Query("SELECT s.senderEmail, s.senderName, COUNT(s) AS mailCount " +
            "FROM Summary s " +
            "JOIN s.gmail g " +
            "WHERE g.user.id = :userId " +
            "GROUP BY s.senderEmail, s.senderName " +
            "ORDER BY mailCount DESC")
    List<Object[]> findSendersByUserIdWithMailCountOrderByCountDesc(Long userId);

    // 로그인한 유저의 Gmail 엔티티의 userId (=User 엔티티의 기본키)를 통해서 요약된 시간 기준 내림차순으로 모든 요약 메일 가져오기
    List<Summary> findAllByGmail_User_IdOrderByCreatedAtDesc(Long userId);

    // 로그인한 유저의 Gmail 엔티티의 userId (=User 엔티티의 기본키) + 날짜(yyyy-mm-dd)로 이벤트 찾기
    List<Summary> findByGmail_User_IdAndEventDate(Long userId, LocalDate date);

    // 로그인한 유저의 Gmail 엔티티의 userId (=User 엔티티의 기본키) + 해당 월 범위 날짜로 찾기
    List<Summary> findByGmail_User_IdAndEventDateBetween(Long userId, LocalDate startDate, LocalDate endDate);

    // 로그인한 유저의 Gmail 엔티티의 userId (=User 엔티티의 기본키) + 날짜(eventDate)가 null 인 일정 찾기
    List<Summary> findByGmail_User_IdAndEventDateIsNull(Long userId);

}
