package com.inix.omqweb.DTO;

import lombok.Data;

@Data
public class AccessTokenDTO {
    private String access_token;
    private String expires_in;
    private String token_type;
    private String refresh_token;
}
