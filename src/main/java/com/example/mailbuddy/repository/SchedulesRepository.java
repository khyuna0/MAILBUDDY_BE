package com.example.mailbuddy.repository;

import com.example.mailbuddy.entity.User;
import com.example.mailbuddy.entity.Schedules;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface SchedulesRepository extends JpaRepository<Schedules, Long> {

    // 로그인한 유저 + 날짜(yyyy-mm-dd)로 이벤트 찾기
    List<Schedules> findByUserAndEventDate(User user, LocalDate date);

    // 월별: 특정 유저 + 해당 월 범위 날짜로 찾기
    List<Schedules> findByUserAndEventDateBetween(User user, LocalDate startDate, LocalDate endDate);

}