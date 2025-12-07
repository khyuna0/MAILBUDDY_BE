package com.example.mailbuddy.controller;

import com.example.mailbuddy.dto.ChatRoomDto;
import com.example.mailbuddy.repository.ChatRoomRepository;
import com.example.mailbuddy.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/chat")
@PreAuthorize("hasRole('ADMIN')")
public class ChatRoomController { // ADMIN 만 접근 가능한 채팅방 관리 컨트롤러입니다.

    private final ChatRoomRepository chatRoomRepository;
    private final UserRepository userRepository;

    // USER 가 보낸 전체 채팅 방 리스트 조회
    @GetMapping
    public List<ChatRoomDto> getAllRooms() {
        return chatRoomRepository.findAll()
                .stream()
                .map(ChatRoomDto::fromEntity)
                .toList();
    }

    // 관리자가 채팅 방 삭제
    @DeleteMapping("/{roomId}")
    public ResponseEntity<?> deleteRoom(@PathVariable("roomId") Long roomId, Authentication auth) {

        if (auth == null) { // userRole은 추후 추가 예정
            return ResponseEntity.status(401).body("권한이 없습니다.");
        }
        if (chatRoomRepository.findById(roomId).isEmpty()) {
            return ResponseEntity.status(404).body("삭제할 채팅방이 없습니다.");
        }
        chatRoomRepository.deleteById(roomId);
        return ResponseEntity.ok().body("삭제 성공");
    }


}
