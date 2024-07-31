package com.inix.omqweb.Message;

import com.inix.omqweb.Game.Game;
import com.inix.omqweb.Game.GameManager;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

import java.util.UUID;
import java.util.logging.Logger;

@Controller
@RequiredArgsConstructor
public class MessageController {
    private final GameManager gameManager;
    private final MessageService messageService;

    @MessageMapping("/{roomId}")
    public void chat(@DestinationVariable UUID roomId, Message message) {
        if(!message.isAnswer()){
            sendMessage(roomId, message);
        }
        else gameManager.submitAnswer(roomId, message.getSenderId(), message.getContent());

        Game game = gameManager.getGameById(roomId);

        if (game == null) return;

        gameManager.updateActivity(game);
    }

    @SendTo("/room/{roomId}")
    public Message sendMessage(@DestinationVariable UUID roomId, Message message) {
        messageService.sendMessage(roomId, message);
        return message;
    }


}