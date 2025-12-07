package com.example.mailbuddy.utils;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented // 이 어노테이션을 붙인 커스텀 애노테이션이 javadoc 같은 문서에 포함되도록 함
@Constraint(validatedBy = BirthValidator.class)
@Target({ ElementType.FIELD }) // 이 애노테이션이 어디에 붙을 수 있는지 지정 (필드, 메서드 ..)
@Retention(RetentionPolicy.RUNTIME) // 컴파일 이후에도 애노테이션 정보를 JVM이 유지하는 범위를 결정. 보통 RUNTIME으로 설정해서 실행 시에도 사용 가능.
// 커스텀 검증 애노테이션은 인터페이스로 정의하되, @를 붙여서 컴파일러와 프레임워크에게 애노테이션으로 인식하게 하는 것임.
public @interface ValidBirth {
    String message() default "생일은 1900-01-01 ~ 오늘 사이의 yyyy-MM-dd 형식이어야 합니다.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
