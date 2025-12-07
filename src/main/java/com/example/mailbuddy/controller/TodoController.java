// src/main/java/com/example/mailbuddy/controller/TodoController.java
package com.example.mailbuddy.controller;

import com.example.mailbuddy.dto.TodoDto;
import com.example.mailbuddy.entity.TodoItem;
import com.example.mailbuddy.entity.User;
import com.example.mailbuddy.repository.TodoRepository;
import com.example.mailbuddy.repository.UserRepository;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.util.List;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/todo")
@RequiredArgsConstructor
@Validated
public class TodoController {

    private final TodoRepository todoRepository;
    private final UserRepository userRepository;

    //로그인 사용자 id 얻기 -> 요청보낸 사람의 pk(id)가져오기
    private Long currentUserId() {
        //SecurityContextHolder 에서 현재 로그인정보(Authentication) 꺼냄
        //getContext().getAuthentication() -> principal(사용자정보), credentials(비번,토큰), 권한목록 등 들어있음
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated() // 객체없거나, 인증이 안됐거나
                || auth instanceof AnonymousAuthenticationToken) { // 시큐리티가 익명사용자로 처리하면
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }
        //principal -> 요청보낸 사용자 타입에따라 나눔 -> 폼 , 구글
        Object p = auth.getPrincipal();

        // 폼 로그인 -> username 기준으로 우리 User row를 찾고 → 그 User의 id(Long) 를 리턴
        if (p instanceof org.springframework.security.core.userdetails.UserDetails ud) {
            //p instanceof UserDetails ud → principal이 UserDetails 타입이면, ud라는 변수로 꺼냄
            return userRepository.findByUsername(ud.getUsername()) //ud.getUsername() - 로그인할 때 쓴 username
                    .map(User::getId)
                    // Optional<User>를 Optional<Long> (id)로 바꿈 -> 있으면 user.getId() 꺼내고 없으면 빈Optional
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "사용자 없음"));
        }

        // 구글 로그인 하면 SecurityContext의 principal이 DefaultOAuth2User로 바뀜
        // 요청에서 currentId를 부르면 auth.getPrincipal() 타입은 UserDetails가 아니라 DefaultOAuth2User일 수 있음
        // 구글 로그인 -> 구글 이메일로 User를 찾아서 -> 그 User의 id 리턴
        if (p instanceof org.springframework.security.oauth2.core.user.DefaultOAuth2User ou) {
            //principal이 DefaultOAuth2User라면 -> 구글 OAuth2 로그인으로 인증된 상태라면 ou 변수로 받음
            String email = (String) ou.getAttributes().get("email"); // 프로필 정보 중에서 "email" 값을 꺼냄
            if (email == null)
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "이메일 정보 없음");
            
            return userRepository.findByGoogleEmail(email)
                    //User 테이블에서 googleEmail이 이 값인 유저 찾기 -> 반환:Optional<User>
                    .map(User::getId) //찾은 User의 id(pk)꺼냄 -> Optional<Long>
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "사용자 없음"));
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "인증 정보를 인식할 수 없습니다.");
    }

    // 월 목록 조회 -> /api/todo?ym=2025-11
    @GetMapping
    public List<TodoDto> list(@RequestParam String ym) {
        Long userId = currentUserId();
        return todoRepository
                .findByUserIdAndYmOrderByDoneAscCreatedAtAsc(userId, ym)
                // = SELECT * FROM todo_item WHERE user_id = :userId AND ym = :ym ORDER BY done ASC, created_at ASC;
                .stream().map(TodoDto::of).toList();
                // 엔티티 → DTO 변환 :
    }

    // 할일 생성
    @PostMapping
    @Transactional
    public ResponseEntity<TodoDto> create(@Valid @RequestBody TodoDto req) {
        Long userId = currentUserId();
        var userRef = userRepository.getReferenceById(userId);
        TodoItem e = TodoItem.builder()
                .user(userRef)
                .ym(req.getYm())
                .text(req.getText().trim())
                .done(false)
                .build();
        e = todoRepository.save(e);
        return ResponseEntity.created(URI.create("/api/todo/" + e.getId()))
                .body(TodoDto.of(e));
    }

    // 수정: text, toggle만 수정 가능
    @PatchMapping("/{id}")
    @Transactional
    public TodoDto update(@PathVariable Long id, @RequestBody TodoDto req) {
        Long userId = currentUserId();

        TodoItem e = todoRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "존재하지 않는 항목입니다."));

        // text 수정시
        if(req.getText() !=null){
            String t = req.getText().trim();
            if(t.isEmpty()){
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"할 일은 공백으로 변경할 수 없습니다");
            }
            e.setText(t);
        }
        // done값 수정한 경우
        if(req.getDone() != null) {
            e.setDone(req.getDone());
        }
        return TodoDto.of(e);

    }

    // done 토글
    @PatchMapping("/{id}/toggle")
    @Transactional
    public TodoDto toggle(@PathVariable Long id ) {
        Long userId = currentUserId();
        TodoItem e = todoRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "존재하지 않는 항목입니다."));

        e.setDone(!e.isDone());
        return TodoDto.of(e);
    }

    // 삭제
    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        // ResponseEntity<Void> -> DELETE: 204 No Content + 응답 바디 없음 / 본문이 없는(빈) 응답이다 명확하게 표현
        Long userId = currentUserId(); // 해당 유저의 Todo만 삭제 - 없으면 id만존재하면 삭제 가능할수도 있음

        todoRepository.findByIdAndUserId(id, userId)
                .ifPresent(todoRepository::delete);
        //findById의 결과타입 -> Optional<TodoItem>
        //Optional의 메서드 중 하나 opt.ifPresent(e-> todoRepository.delete(e));
        //e -> todoRepository.delete(e)를 간단하게 -> todoRepository::delete
        // 해당 id의 Todo가 있으면 그걸 삭제하고, 없으면 그냥 조용히 지나가라

        return ResponseEntity.noContent().build();
    }

    // 해당 월 완료 항목 모두 삭제
    @DeleteMapping("/completed")
    @Transactional
    public ResponseEntity<Void> clearCompleted(@RequestParam String ym) {
        Long userId = currentUserId();
        todoRepository.deleteByUserIdAndYmAndDoneTrue(userId, ym);
        return ResponseEntity.noContent().build();
    }
}