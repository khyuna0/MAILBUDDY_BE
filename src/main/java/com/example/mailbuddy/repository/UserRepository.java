package com.example.mailbuddy.repository;


import com.example.mailbuddy.entity.User;
import com.example.mailbuddy.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    // 아이디(username) 이용해서 사용자 찾기
    Optional<User> findByUsername(String username);

    // 구글 이메일 이용해서 사용자 찾기
    Optional<User> findByGoogleEmail(String googleEmail);

    Optional<User> findByUserRole(UserRole userRole);
}
