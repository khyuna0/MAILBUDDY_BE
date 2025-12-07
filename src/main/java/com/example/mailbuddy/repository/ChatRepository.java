package com.example.mailbuddy.repository;

import com.example.mailbuddy.entity.Chat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatRepository extends JpaRepository<Chat, Long> {

    List<Chat> findByRoom_IdOrderByCreatedAtAsc(Long roomId);
    List<Chat> findByUser_IdNot(Long id);// 내 USER_ID 가 아닌 것들 리스트 찾아오기

}
