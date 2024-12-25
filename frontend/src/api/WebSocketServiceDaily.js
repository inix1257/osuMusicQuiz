import SockJS from "sockjs-client";
import {Stomp} from "@stomp/stompjs";

export default class WebSocketService {
    constructor(userId) {
        this.stompClient = null;
        this.userId = userId;
    }

    connect(callbacks) {
        return new Promise((resolve, reject) => {
            var socket = new SockJS(`${process.env.VUE_APP_API_URL}/ws-stomp`);
            this.stompClient = Stomp.over(socket);
            this.stompClient.debug = () => {};
            this.stompClient.connect(
                {},
                () => {
                    this.stompClient.subscribe(`/daily/${this.userId}`, callbacks.onMessage);
                    this.stompClient.subscribe(`/daily/${this.userId}/reveal`, callbacks.onReveal);
                    resolve(true);
                },
                (error) => {
                    console.error(error);
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

    sendMessage(message, dailyNumber) {
        const userId = this.userId;

        this.stompClient.send(`/send/daily`, {}, JSON.stringify({
            dailyId: dailyNumber,
            senderId: userId,
            content: message,
        }));
    }
}