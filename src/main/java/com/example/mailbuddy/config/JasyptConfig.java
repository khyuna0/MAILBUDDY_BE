package com.example.mailbuddy.config;

import org.jasypt.encryption.StringEncryptor;
import org.jasypt.encryption.pbe.PooledPBEStringEncryptor;
import org.jasypt.encryption.pbe.config.SimpleStringPBEConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JasyptConfig {
    // DB에 엔티티 암호화 저장 Bean
    // 시큐리티 컨피그 비밀번호 암호화는 단방향이지만 이건 양방향 (복호화가 가능하다!)

    @Value("${jasypt.encryptor.password}")
    private String jasyptKey;

    @Bean("jasyptEncryptor")
    public StringEncryptor jasyptEncryptor() {

        // 실제 암호화/복호화를 수행하는 엔진
        PooledPBEStringEncryptor encryptor = new PooledPBEStringEncryptor();
        // 암호화 설정값을 담는 객체 (알고리즘, 비밀번호 등)
        SimpleStringPBEConfig config = new SimpleStringPBEConfig();

        config.setPassword(jasyptKey);
        config.setAlgorithm("PBEWithMD5AndDES"); // MD5 + DES 조합으로 암호/복호화하는 방식 (암호화 알고리즘임 잘모름...)
        config.setPoolSize("1"); // 암호화를 동시에 몇 스레드에서 처리할 수 있게 할 것인가... 지금은 큐처럼 처리한다는 의미임

        encryptor.setConfig(config);
        return encryptor;
    }

}
