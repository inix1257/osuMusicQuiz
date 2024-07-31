package com.inix.omqweb.DTO;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@Builder
public class JoinGameDTO {
    private String gameId;
    private String password;
    private String userId;
    private boolean forceLeave;
}
