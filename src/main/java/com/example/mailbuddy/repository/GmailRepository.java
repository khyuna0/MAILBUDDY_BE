package com.example.mailbuddy.repository;

import com.example.mailbuddy.dto.AddressDto;
import com.example.mailbuddy.entity.Gmail;
import com.example.mailbuddy.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface GmailRepository extends JpaRepository<Gmail, Long> {

    // 사용자 엔티티로 이메일 찾기
    List<Gmail> findByUser(User user);

    // 사용자, 메일의 고유 id를 이용해서 gmail 존재 여부 확인(중복 저장 방지)
    boolean existsByUserAndMessageId(User user, String messageId);
    Optional<Gmail> findByUserAndMessageId(User user, String messageId);

}
