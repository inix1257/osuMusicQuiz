package com.inix.omqweb.Message;

import com.inix.omqweb.osuAPI.PlayerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MessageService {
    private final PlayerRepository playerRepository;
    private final SimpMessagingTemplate template;


    public void sendMessage(UUID roomId, Message message) {
        this.template.convertAndSend("/room/" + roomId +"/message", message);
    }

    public void sendSystemMessage(UUID gameId, String content) {
        Message message = new Message();
        message.setSenderUsername("System");
        message.setContent(content);
        message.setSystemMessage(true);
        sendMessage(gameId, message);
    }
}