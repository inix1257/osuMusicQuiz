<template>
  <div id="app">
    <BeatmapRenderer />
    <div class="log-container">
      <div v-for="logMessage in logMessages" :key="logMessage" class="log-message">
        <p>{{ logMessage }}</p>
      </div>
    </div>
    <h2>Send Announcement</h2>
    <input type="text" v-model="announcementContent" placeholder="Announcement" class="input-announcement"/>
    <button v-on:click="sendAnnouncement">Send</button>
    <br><br>

    <h2>Add Beatmap</h2>
    <input type="text" v-model="userInput" placeholder="Beatmap ID" />
    <button v-on:click="addBeatmap">Add Beatmap</button>
    <br><br>
    <input type="text" v-model="userId" placeholder="Your userid" />

    <div v-for="gameroom in gamerooms" :key="gameroom.uuid">
      <div class="gameroom">
        <h2 class="gameroom-name">{{ gameroom.name }}</h2>
        <p>{{ gameroom.creationDate }}</p>
        <div v-for="(player, index) in gameroom.players" :key="player" class="player-info" :class="{ 'owner-highlight': player.username === gameroom.owner.username }">
          <img :src="player.avatar_url" alt="Player's avatar" class="avatar">
          <p>{{index}}: {{ player.username }}</p>
          <button v-on:click="kickPlayer(gameroom.uuid, player.id)">Kick</button>
          <button v-on:click="banPlayer(gameroom.uuid, player.id)">Ban</button>
        </div>
        <button v-on:click="joinGame(gameroom.uuid)">Join</button>
        <button v-on:click="deleteGame(gameroom.uuid)">Nuke</button>
      </div>
    </div>

    <div>
      <br><br><br><br><br><br><br><br>
    </div>
  </div>
</template>

<script>
import axios from "axios";
import apiService from "@/api/apiService";
import BeatmapRenderer from "@/components/osu/Render/BeatmapRenderer.vue";

export default {
  name: 'DebugPage',
  data() {
    return {
      userInput: "",
      beatmap: Object,
      gamelog: [],
      username: "",
      userId: "",
      gameName: "",
      blur_beatmapId: "",
      isModalOpen: false,
      totalQuestions: 10,
      difficulty: "NORMAL",
      mode: "MUSIC",
      gamerooms: [],
      announcementContent: "",
      logMessages: [],
    }
  },
  mounted() {
    apiService.get(`${process.env.VUE_APP_API_URL}/api/gamerooms`)
        .then((response) => {
          this.gamerooms = response.data;
        })
  },
  methods: {
    joinGame(uuid) {
      apiService.post(`${process.env.VUE_APP_API_URL}/api/joinGameLegacy`, {
        gameId: uuid,
        userId: this.userId
      })
    },

    deleteGame(uuid) {
      if (confirm("Are you sure you want to delete this game?: " + uuid)) {
        apiService.post(`${process.env.VUE_APP_API_URL}/api/deleteGame`, {
          gameId: uuid
        })
      }
    },

    kickPlayer(uuid, userId) {
      if (confirm("Are you sure you want to kick this player?: " + userId)) {
        apiService.post('/api/kickPlayer', {
          gameId: uuid,
          targetUserId: userId
        })
      }
    },

    banPlayer(uuid, userId) {
      if (confirm("Are you sure you want to ban this player?: " + userId)) {
        apiService.post('/api/banPlayer', {
          gameId: uuid,
          targetUserId: userId
        })
      }
    },

    sendAnnouncement() {
      if (confirm("Are you sure you want to send this announcement?: " + this.announcementContent)) {
        apiService.post(`${process.env.VUE_APP_API_URL}/api/ingameAnnouncement`, {
          content: this.announcementContent
        })
      }
    },

    addBeatmap() {
      const beatmapsetId = this.userInput;

      if (beatmapsetId.includes("beatmapsets")) {
        const split = beatmapsetId.split("/");
        this.userInput = split[split.length - 1];
      }

      apiService.post(`${process.env.VUE_APP_API_URL}/api/addBeatmap`,  {
        beatmapsetId: this.userInput
      })
          .then((response) => {
            if (response.data) {
              // handle success
              this.logMessages.push(response.data);
            } else {
              // handle failure
              this.logMessages.push("map already exists : " + this.userInput);
            }
          })
          .catch((error) => {
            // handle error
          });

    }
  },
  components: {
    BeatmapRenderer
  }
}
</script>
<style scoped>
#app {
  overflow: auto;
  height: 80vh;
}

.log-container {
}

.log-message {
  border: 1px solid black;
  margin: 5px;
  padding: 5px;
}

.gameroom {
  border: 1px solid black;
  margin: 5px;
  padding: 5px;

}

input {
  margin: 5px;
  border: 1px solid #ccc;
  border-radius: 5px;
  font-size: 16px;
  color: #333;
  font-family: 'Sen', serif;
  height: 2em;
}

.input-announcement {
  width: 50%;
  height: 5em;
}

.player-info {
  display: flex;
  align-items: center;
}

button {
  margin: 5px;
  border: 1px solid #ccc;
  border-radius: 5px;
  font-size: 16px;
  color: #333;
  font-family: 'Sen', serif;
  height: 2em;
  padding: 0 10px;
}
</style>