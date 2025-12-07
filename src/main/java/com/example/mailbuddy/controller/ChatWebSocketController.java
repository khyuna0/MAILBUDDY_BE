package com.example.mailbuddy.controller;

import com.example.mailbuddy.dto.ChatDto;
import com.example.mailbuddy.entity.Chat;
import com.example.mailbuddy.entity.ChatRoom;
import com.example.mailbuddy.entity.User;
import com.example.mailbuddy.repository.ChatRepository;
import com.example.mailbuddy.repository.ChatRoomRepository;
import com.example.mailbuddy.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;
import java.util.Map;

@Controller  // 주의: WebSocket은 @Controller가 일반적, @RestController는 HTTP 응답용
@RequiredArgsConstructor
public class ChatWebSocketController {

    private final SimpMessagingTemplate messagingTemplate;
    private final UserRepository userRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatRepository chatRepository;

    // WebSocket 메시지 처리 (예: /app/chat/{roomId}/send)
    @MessageMapping("/chat/{roomId}/send")
    public void sendPrivate(@DestinationVariable Long roomId, @Valid ChatDto chatDto, Authentication auth){
        User user = userRepository.findByUsername(auth.getName()).orElseThrow();
        ChatRoom room = chatRoomRepository.findById(roomId).orElseThrow();

        Chat chat = new Chat();
        chat.setRoom(room);
        chat.setUser(user);
        chat.setContent(chatDto.getContent());
        chat.setCreatedAt(LocalDateTime.now());
        chatRepository.save(chat);

        room.setLastMessage(chatDto.getContent());
        chatRoomRepository.save(room);

        messagingTemplate.convertAndSend("/topic/chat/" + roomId, Map.of(
                "sender", user.getUsername(),
                "role", user.getUserRole().toString(),
                "content", chatDto.getContent(),
                "createdAt", LocalDateTime.now().toString()
        ));
    }
}

