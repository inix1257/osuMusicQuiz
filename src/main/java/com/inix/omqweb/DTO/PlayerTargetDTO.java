package com.inix.omqweb.DTO;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PlayerTargetDTO {
    private String gameId;
    private String targetUserId;
}
