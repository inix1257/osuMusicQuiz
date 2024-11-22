<script>
import {useUserStore} from "@/stores/userStore";

export default {
  name: 'ChatSection',
  data() {
    return {
      chatInput: '',
    }
  },
  setup() {
    const userStore = useUserStore();
    return {
      me: userStore.getMe(),
    };
  },
  props: ['messages', 'isConnected', 'formatTimestamp'],
  methods: {
    sendChatMessage() {
      if (!this.chatInput) {
        return;
      }

      this.$emit('send-message', this.chatInput);
      this.chatInput = "";
    },
    highlightMessage(message) {
      const originalStringToHighlight = this.me.username;
      const escapedStringToHighlight = originalStringToHighlight.replace(/[.*+?^${}()|[\]\\]/g, '\\$&'); // Escape special characters
      const highlightedString = `<span class="message-highlight">${originalStringToHighlight}</span>`;
      const strongMessage = message.replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>');
      return strongMessage.replace(new RegExp(`(${escapedStringToHighlight})`, 'gi'), highlightedString);
    }
  },

  computed: {
    messagesWithSeparator() {
      const userLatestTimestamp = {};

      return this.messages.map((message, index) => {
        const previousMessage = this.messages[index - 1];
        const showSeparator = previousMessage && previousMessage.senderUsername !== message.senderUsername;
        const showUsername = !previousMessage || previousMessage.senderUsername !== message.senderUsername;
        const showTimestamp = !previousMessage || previousMessage.senderUsername !== message.senderUsername;

        if (showUsername) {
          userLatestTimestamp[message.senderUsername] = message.timestamp;
        } else {
          message.timestamp = userLatestTimestamp[message.senderUsername];
        }

        return { ...message, showSeparator, showUsername, showTimestamp };
      });
    }
  }
}
</script>

<template>
  <div class="chat">
    <div class="message-wrapper">
      <div class="messages" v-for="message in messagesWithSeparator" :key="message.id" :class="{ 'system-message': message.systemMessage}">
        <hr v-if="message.showSeparator">
        <div v-if="message.showUsername && !message.systemMessage" class="message-sender"><span><img :src="message.senderAvatarUrl" alt="Player's avatar" class="message-player-avatar"></span><span class="message-sender-span">{{ message.senderUsername }}</span></div>
        <div class="message-content" v-html="highlightMessage(message.content)"
             :class="{ 'message-announcement': message.announcement || message.systemMessage }"
        ></div>
<!--        <div class="message-timestamp" v-if="message.showTimestamp">{{ formatTimestamp(message.timestamp) }}-->
      </div>
    </div>
    <input class="message-input" type="text" v-model="chatInput" :disabled="!isConnected" placeholder="Enter your message..." v-on:keydown.enter="sendChatMessage()">
  </div>
</template>

<style>
.chat {
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
  height: 80vh;
  width: 15vw;
  padding: 10px;
  border: 2px solid var(--color-text);
  overflow-y: auto;
}

.message-highlight {
  font-weight: bold;
  background-color: rgba(22, 225, 49, 0.2);
  border: 1px solid rgba(22, 225, 49, 0.4);
  border-radius: 5px;
}

.chat input[type="text"] {
  width: 100%;
  height: 50px;
  box-sizing: border-box;
  margin-top: 5px;
}

.message {
  display: grid;
  grid-template-rows: auto auto;
  grid-template-columns: 1fr auto;
  margin-bottom: 10px;
}

.message-wrapper {
  overflow-y: auto;
  height: 100%;
}

.message-player-avatar {
  width: 1.2em;
  height: 1.2em;
  border-radius: 50%;
  margin-right: 5px;
  box-shadow: 0px 10px 10px rgba(0, 0, 0, 0.1);
  border: 1px solid var(--color-primary);
}

.message-sender {
  grid-row: 1;
  grid-column: 1;
  font-weight: bold;
  text-align: left;
  margin-bottom: 4px;
  display: flex;
  align-items: center;
}

.message-sender-span {
  background: var(--color-secondary);
  padding: 2px 4px;
  border-radius: 5px;
  font-size: 1.0em;
}

.message-content {
  grid-row: 2;
  grid-column: 1 / span 2;
  text-align: left;
  font-size: 1em;
  word-wrap: break-word;
}

.message-timestamp {
  grid-row: 1;
  grid-column: 2;
  text-align: right;
  color: #999;
  font-size: 0.8em;
}

.message-input {
  font-size: 1.0em;
  color: var(--color-text);
  background-color: var(--color-secondary);
  font-family: "Sen", "Noto Sans Korean", "serif";
  border: 2px solid var(--color-primary);
  border-radius: 5px;
}

.system-message {
  grid-template-rows: auto;
}

.system-message-timestamp {
  grid-row: 1;
  grid-column: 2;
  text-align: right;
  color: #999;
  font-size: 0.8em;
}

.system-message .message-content {
  grid-row: 1;
}

.message-announcement {
  background-color: var(--color-secondary);
  padding: 5px;
  border-radius: 5px;
  margin-top: 5px;
}

hr {
  border: 0;
}
</style>