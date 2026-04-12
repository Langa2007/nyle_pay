package com.nyle.nylepay.dto;

import lombok.Data;

@Data
public class CexLinkRequest {
    private Long userId;
    private String exchangeName;
    private String apiKey;
    private String apiSecret;
}
