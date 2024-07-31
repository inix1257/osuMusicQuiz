package com.inix.omqweb.DTO;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Setter
@Getter
public class AnswerDTO {
    private UUID gameId;
    private String answer;
}
