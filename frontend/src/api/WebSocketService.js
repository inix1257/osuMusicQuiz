import SockJS from "sockjs-client";
import {Stomp} from "@stomp/stompjs";

export default class WebSocketService {
    constructor(gameId) {
        this.stompClient = null;
        this.gameId = gameId;
    }

    connect(callbacks) {
        return new Promise((resolve, reject) => {
            var socket = new SockJS(`${process.env.VUE_APP_API_URL}/ws-stomp`);
            this.stompClient = Stomp.over(socket);
            this.stompClient.debug = () => {};
            this.stompClient.connect(
                {},
                () => {
                    this.stompClient.subscribe(`/room/${this.gameId}/message`, callbacks.onMessage);
                    this.stompClient.subscribe(`/room/${this.gameId}/system/gamestatus`, callbacks.onGameStatus);
                    this.stompClient.subscribe(`/room/${this.gameId}/system/gameprogress`, callbacks.onGameProgress);
                    this.stompClient.subscribe(`/room/${this.gameId}/system/gameleaderboard`, callbacks.onGameLeaderboard);
                    this.stompClient.subscribe(`/room/${this.gameId}/system/answer`, callbacks.onAnswer);
                    this.stompClient.subscribe(`/room/${this.gameId}/system/answerSubmission`, callbacks.onAnswerSubmission);
                    this.stompClient.subscribe(`/room/${this.gameId}/system/playersAnswers`, callbacks.onPlayersAnswers);
                    this.stompClient.subscribe(`/room/${this.gameId}/system/players`, callbacks.onPlayers);
                    this.stompClient.subscribe(`/room/${this.gameId}/system/roomSettings`, callbacks.onRoomSettings);
                    this.stompClient.subscribe(`/room/${this.gameId}/system/announcement`, callbacks.onAnnouncement);
                    this.stompClient.subscribe(`/room/${this.gameId}/system/blurReveal`, callbacks.onBlurReveal);
                    this.stompClient.subscribe(`/room/${this.gameId}/system/playerKick`, callbacks.onPlayerKick);
                    this.stompClient.subscribe(`/room/${this.gameId}/system/playerInactivityKick`, callbacks.onPlayerInactivityKick);
                    console.log('WebSocket connection established');
                    resolve(true);
                },
                (error) => {
                    console.error('WebSocket connection error', error);
                    console.log('Reconnecting in 5 seconds...');
                    resolve(false);
                }
            )
        });
    }

    disconnect() {
        if (this.stompClient) {
            this.stompClient.disconnect();
        }
    }

    sendMessage(message) {
        console.log('sending message', message)
        this.stompClient.send(`/send/${message.gameId}`, {}, JSON.stringify(message));
    }
}