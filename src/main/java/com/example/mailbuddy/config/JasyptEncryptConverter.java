package com.example.mailbuddy.config;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.jasypt.encryption.StringEncryptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component                // 스프링 빈으로 등록됨
@Converter
// JPA가 이 클래스를 AttributeConverter로 사용함. 엔티티, DB 상호작용 할때, 이 컨버터를 거쳐감
// 엔티티 필드 값 → convertToDatabaseColumn() → DB 저장 (암호화 저장)
// DB 값 → convertToEntityAttribute() → 엔티티 필드로 세팅 (복호화 -> 프론트에 찍어주기)
public class JasyptEncryptConverter implements AttributeConverter<String, String> {


    @Autowired
    private StringEncryptor encryptor;

    @Override
    public String convertToDatabaseColumn(String attribute) {
        // 엔티티 → DB 저장 직전에 호출됨
        if (attribute == null) return null;

        // DB에 저장될 값 (암호화된 문자열)
        return encryptor.encrypt(attribute);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        // DB → 엔티티 조회 시 호출됨
        if (dbData == null) return null;

        // 엔티티로 직접 들어가는 값 (복호화된 평문)
        return encryptor.decrypt(dbData);
    }

}
