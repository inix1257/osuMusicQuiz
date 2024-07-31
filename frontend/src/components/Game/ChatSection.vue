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
      const stringToHighlight = this.me.username;
      const highlightedString = `<span class="message-highlight">${stringToHighlight}</span>`;
      return message.replace(new RegExp(stringToHighlight, 'gi'), highlightedString);
    },
  }
}
</script>

<template>
  <div class="chat">
    <div class="message-wrapper">
      <div class="messages" v-for="message in messages" :key="message.id" :class="{ 'system-message': message.systemMessage}">
        <div v-if="!message.systemMessage" class="message-sender">{{ message.senderUsername }}</div>
        <div class="message-content" :class="{ 'message-announcement': message.announcement }" v-html="highlightMessage(message.content)"></div>
        <div v-if="!message.systemMessage" class="message-timestamp">{{ formatTimestamp(message.timestamp) }}</div>
      </div>
    </div>
    <input class="message-input" type="text" v-model="chatInput" :disabled="!isConnected" placeholder="Enter your message" v-on:keydown.enter="sendChatMessage()">
  </div>
</template>

<style scoped>
.chat {
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
  height: 80vh;
  width: 18vw;
  padding: 10px;
  border: 2px solid var(--color-text);
  overflow-y: auto;
}

.chat input[type="text"] {
  width: 100%;
  height: 50px;
  box-sizing: border-box;
  margin-top: auto;
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

.message-sender {
  grid-row: 1;
  grid-column: 1;
  font-weight: bold;
  text-align: left;
  font-size: 1em;
}

.message-content {
  grid-row: 2;
  grid-column: 1 / span 2;
  text-align: left;
  font-size: 1em;
  word-wrap: break-word;
}

.message-highlight {
  font-weight: bold;
  background-color: rgba(22, 225, 49, 0.2);
  border: 1px solid rgba(22, 225, 49, 0.4);
  border-radius: 5px;
}

.message-timestamp {
  grid-row: 1;
  grid-column: 2;
  text-align: right;
  color: #999;
  font-size: 0.8em;
}

.message-input {
  color: var(--color-text);
  background-color: var(--color-secondary);
  font-family: "Sen", "Noto Sans Korean", "serif";
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
}
</style>