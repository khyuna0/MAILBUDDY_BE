package com.example.mailbuddy.dto;

import com.example.mailbuddy.entity.Chat;
import com.example.mailbuddy.entity.User;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class ChatDto {

    @NotBlank(message = "메시지를 입력해 주세요.")
    @Size(max = 300, message = "메시지는 300자를 넘을 수 없습니다.")
    private String content; // 메시지 본문

    private Long roomId;    // 어느 채팅방인지

    private String username;  // 보낸 사람 (username)
    private String userRole; // 보낸 사람의 role

    private String createdAt;
    private boolean isRead;

    public static ChatDto fromEntity(Chat chat) {
        ChatDto dto = new ChatDto();
        dto.setContent(chat.getContent());
        dto.setRoomId(chat.getRoom().getId());
        dto.setUsername(chat.getUser() != null ? chat.getUser().getUsername() : null );
        dto.setUserRole(chat.getUser() != null ? chat.getUser().getUserRole().toString() : null);
        dto.setCreatedAt(chat.getCreatedAt().toString());
        dto.setRead(chat.isRead());
        return dto;
    }
}
