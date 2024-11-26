package com.inix.omqweb.Message;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Entity
@Setter
@Getter
@ToString
@JsonSerialize
@JsonDeserialize
@Table(name = "message")
public class Message {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    private UUID gameId;

    private String senderId;
    private String senderUsername;
    private String senderAvatarUrl;

    private String content;
    private boolean answer;
    private boolean isSystemMessage;
    private LocalDateTime timestamp = LocalDateTime.now(ZoneOffset.UTC);
}
