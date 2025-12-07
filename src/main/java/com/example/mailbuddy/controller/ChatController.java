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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chat")
public class ChatController {

    private final UserRepository userRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatRepository chatRepository;

    // 특정 채팅룸(roomId)의 모든(이전) 채팅 불러오기
    @GetMapping("/{roomId}")
    public List<ChatDto> getChatHistory(@PathVariable("roomId") Long roomId, Authentication auth) {
        // 로그인 한 유저 정보 가져오기
        User user = userRepository.findByUsername(auth.getName()).orElseThrow();
        // 기존 메시지 불러오기
        List<Chat> chats = chatRepository.findByRoom_IdOrderByCreatedAtAsc(roomId);
        return chats.stream()
                .map(ChatDto::fromEntity)
                .toList();
    }

//    // 1:1 채팅 (유저 ↔ 어드민)
//    @MessageMapping("/chat/{roomId}/send")
//    public void sendPrivate(@DestinationVariable Long roomId,
//                            @Valid ChatDto chatDto,
//                            Authentication auth) {
//
//        User user = userRepository.findByUsername(auth.getName()).orElseThrow();
//        ChatRoom room = chatRoomRepository.findById(roomId).orElseThrow();
//
//        Chat chat = new Chat();
//        chat.setRoom(room);
//        chat.setUser(user);
//        chat.setContent(chatDto.getContent()); // 여기서 에러 처리는 안됐다!!
//        chat.setCreatedAt(LocalDateTime.now()); // 여기 시간과 아래 시간의 차이는 ..? db 저장시간
//        chatRepository.save(chat);
//        room.setLastMessage(chatDto.getContent());
//        chatRoomRepository.save(room);
//
//        messagingTemplate.convertAndSend("/topic/chat/" + roomId, Map.of(
//                "sender", user.getUsername(),
//                "role", user.getUserRole().toString(),
//                "content", chatDto.getContent(),
//                "createdAt", LocalDateTime.now().toString()
//        ));
//    }

    // 유저 1인에게 할당 된 채팅 방 있으면 확인하고, 없으면 채팅 방 새로 생성
    @PostMapping("/hasroom")
    public ResponseEntity<?> hasroom(Authentication auth) {
        String username;
        if (auth instanceof OAuth2AuthenticationToken oauth2Token) {
            Map<String, Object> attrs = oauth2Token.getPrincipal().getAttributes();
            username = (String) attrs.get("email"); // Google 사용자 이메일
        } else if (auth instanceof UsernamePasswordAuthenticationToken upToken) {
            username = upToken.getName(); // 일반 로그인 사용자 이름
        } else {
            throw new IllegalStateException("Unknown authentication type: " + auth);
        }
        User user = userRepository.findByUsername(auth.getName()).orElseThrow(
                () -> new RuntimeException("user not found" + username));
        ChatRoom room = chatRoomRepository.findByUser(user)
                .orElseGet(() -> {
                    ChatRoom newRoom = new ChatRoom();
                    newRoom.setUser(user);
                    newRoom.setUsername(user.getUsername());
                    newRoom.setCreatedAt(LocalDateTime.now());
                    newRoom.setResolved(false);
                    return chatRoomRepository.save(newRoom);
                });
        return ResponseEntity.ok(Map.of("roomId", room.getId())); // room 자체를 보내도 되나 ..?
    }

//    // 전체 공지 채팅 (Admin 전용) -- 아직 기능 추가 X
//    @PreAuthorize("hasRole('ADMIN')")
//    @MessageMapping("/api/admin/chat/public")
//    public void publicMessage(@Valid ChatDto chatDto, Authentication auth) {
//        User admin = userRepository.findByUsername(auth.getName()).orElseThrow();
//
//        List<ChatRoom> rooms = chatRoomRepository.findAll();
//        for (ChatRoom room : rooms) {
//            Chat chat = new Chat();
//            chat.setRoom(room);
//            chat.setUser(admin);
//            chat.setContent(chatDto.getContent());
//            chat.setRead(false);
//            chat.setCreatedAt(LocalDateTime.now());
//            chatRepository.save(chat);
//
//            messagingTemplate.convertAndSend("/topic/chat/" + room.getId(), chatDto.getContent());
//        }
//    }
}
