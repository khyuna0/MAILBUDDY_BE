package com.example.mailbuddy.repository;

import com.example.mailbuddy.entity.Chat;
import com.example.mailbuddy.entity.ChatRoom;
import com.example.mailbuddy.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    Optional<ChatRoom> findByUser(User user);

}
