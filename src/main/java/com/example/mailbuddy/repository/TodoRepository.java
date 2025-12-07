package com.example.mailbuddy.repository;

import com.example.mailbuddy.entity.TodoItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TodoRepository extends JpaRepository<TodoItem, Long> {

    // 사용자별 + 월별 조회
    // 미완료 먼저 -> true 나중에 (오래된순 정렬)
    List<TodoItem> findByUserIdAndYmOrderByDoneAscCreatedAtAsc(Long userId, String ym);

    // 수정, 토글변경, 삭제시 내todo만 가능하게 한번더 보안
    // findById로 사용가능하긴함 -> 보안 취약 / 해두는게 좋음
    Optional<TodoItem> findByIdAndUserId(Long id, Long userId);
    
    // 완료된 목록 일괄삭제
    long deleteByUserIdAndYmAndDoneTrue(Long userId, String ym);
}
