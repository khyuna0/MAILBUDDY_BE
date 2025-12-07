package com.example.mailbuddy.dto;
import com.example.mailbuddy.entity.ChatRoom;
import com.example.mailbuddy.entity.User;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChatRoomDto {

    private Long id;
    private User user;
    private String username;
    private String lastMessage;
    private boolean resolved;
    private LocalDateTime createdAt;

    // ✅ ChatRoom → ChatRoomDto 변환
    public static ChatRoomDto fromEntity(ChatRoom room) {
        ChatRoomDto dto = new ChatRoomDto();
        dto.setId(room.getId());
        dto.setUser(room.getUser());
        dto.setUsername(room.getUsername());
        dto.setLastMessage(room.getLastMessage());
        dto.setResolved(room.isResolved());
        dto.setCreatedAt(room.getCreatedAt());
        return dto;
    }
}

