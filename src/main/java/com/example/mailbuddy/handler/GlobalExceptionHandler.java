package com.example.mailbuddy.handler;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

// 모든 컨트롤러에서 @Valid/@Validated 실패 시 이 핸들러가 자동 실행되고,
// BindingResult 없이도 검증 오류를 JSON 형태로 프론트에 반환

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidationErrors(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach( err ->
                errors.put(err.getField(), err.getDefaultMessage()));

        return ResponseEntity.badRequest().body(errors);
    }

    // DateTimeParseException 에러 잡아서 400에러로 바꾸기
//    @ExceptionHandler(DateTimeParseException.class)
//    public ResponseEntity<?> handleDateTimeParseException(DateTimeParseException ex) {
//        Map<String, String> error = Map.of("birth", "날짜 형식이 올바르지 않습니다. yyyy-MM-dd 형식이어야 합니다.");
//        return ResponseEntity.badRequest().body(error);
//    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        Map<String, String> errors = new HashMap<>();
        if (ex.getCause() instanceof MismatchedInputException mie) {
            List<JsonMappingException.Reference> path = mie.getPath();
            if (!path.isEmpty()) {
                String fieldName = path.get(0).getFieldName();
                errors.put(fieldName, fieldName + " 필드의 값이 잘못되었습니다. 올바른 형식으로 입력해주세요.");
            } else {
                errors.put("error", "입력 데이터 형식이 올바르지 않습니다.");
            }
        } else {
            errors.put("error", "알 수 없는 형식의 데이터가 입력되었습니다.");
        }
        return ResponseEntity.badRequest().body(errors);
    }



}
