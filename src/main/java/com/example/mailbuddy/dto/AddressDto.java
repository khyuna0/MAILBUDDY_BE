package com.example.mailbuddy.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AddressDto {
    private String senderName;
    private String senderEmail;
    private long messageCount;
}