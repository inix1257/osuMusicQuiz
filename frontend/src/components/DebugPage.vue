<template>
  <div class="debug-page">
<!--    <BeatmapRenderer />-->
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
    <input type="text" v-model="userInput" placeholder="Beatmapset ID" />
    <button v-on:click="addBeatmap">Add Beatmap</button>
    <br><br>

    <h2>Alias</h2>
    <div class="alias-container">
      <div class="radio-buttons">
        <label>
          <input type="radio" v-model="selectedAliasType" value="artist" /> Artist
        </label>
        <label>
          <input type="radio" v-model="selectedAliasType" value="title" /> Title
        </label>
        <label>
          <input type="radio" v-model="selectedAliasType" value="creator" /> Creator
        </label>
      </div>
      Source Beatmap: <input type="text" v-model="aliasInput" @input="filterOptions" placeholder="Enter Source..." class="input-alias" />
      <ul v-if="filteredOptions.length" class="autocomplete-options">
        <li v-for="option in filteredOptions" :key="option" @click="selectOption(option)">
          {{ option }}
        </li>
      </ul>
      <br>
      Target Source: <input type="text" v-model="targetAliasInput" @input="filterTargetOptions" placeholder="Enter Target..." class="input-alias" />
      <ul v-if="filteredTargetOptions.length" class="autocomplete-options">
        <li v-for="option in filteredTargetOptions" :key="option" @click="selectTargetOption(option)">
          {{ option }}
        </li>
      </ul>
      <button @click="submitAlias">Add Alias</button>
    </div>

    <h2>Handle blur</h2>
    <input type="text" v-model="blur_beatmapId" placeholder="Beatmapset ID" />
    <button @click="addBlur(blur_beatmapId)">Add blur</button><br>
    <button @click="loadBeatmapReports">Load beatmap reports</button>
    <div v-for="beatmap in beatmapReports" :key="beatmap.id" class="beatmap-container">
      <div class="beatmap">
        <p>BeatmapsetID: {{ beatmap.beatmapsetId }}</p>
        <p>Artist: {{ beatmap.artist }}</p>
        <p>Title: {{ beatmap.title }}</p>
        <p v-if="beatmap.blur"><strong>This map is already blurred. It might be here because the blur wasn't enough - admin have to manually deal with this</strong></p>
        <img class="image-report" :src="`https://assets.ppy.sh/beatmaps/${beatmap.beatmapsetId}/covers/raw.jpg`" alt="Beatmap Image" />
        <p class="report-count">Report Count: {{ beatmap.reportCount }}</p>
        <button class="addblur-button" @click="addBlur(beatmap.beatmapsetId)">Add blur for this beatmap</button>
        <button class="delete-button" @click="deleteReport(beatmap.beatmapsetId)">Dismiss reports for this beatmap</button>
      </div>
    </div>

    <h2>Management</h2>
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
      beatmapReports: [],
      list_artists: [],
      list_titles: [],
      list_creators: [],
      selectedAliasType: 'artist',
      aliasInput: '',
      targetAliasInput: '',
      filteredOptions: [],
      filteredTargetOptions: [],
    }
  },
  mounted() {
    this.getGameRooms()

      apiService.get(`${process.env.VUE_APP_API_URL}/api/possibleAnswers?gamemode=ANY&guessmode=TITLE`, {})
        .then((response) => {
          this.list_titles = response.data;
        })
    apiService.get(`${process.env.VUE_APP_API_URL}/api/possibleAnswers?gamemode=ANY&guessmode=ARTIST`, {})
        .then((response) => {
          this.list_artists = response.data;
        })
    apiService.get(`${process.env.VUE_APP_API_URL}/api/possibleAnswers?gamemode=ANY&guessmode=CREATOR`, {})
        .then((response) => {
          this.list_creators = response.data;
        })
  },
  methods: {
    getGameRooms() {
      apiService.get(`${process.env.VUE_APP_API_URL}/api/gamerooms`)
          .then((response) => {
            this.gamerooms = response.data;
          })
    },

    joinGame(uuid) {
      apiService.post(`${process.env.VUE_APP_API_URL}/api/joinGameLegacy`, {
        gameId: uuid,
        userId: this.userId
      }).then(this.getGameRooms)
    },

    deleteGame(uuid) {
      if (confirm("Are you sure you want to delete this game?: " + uuid)) {
        apiService.post(`${process.env.VUE_APP_API_URL}/api/deleteGame`, {
          gameId: uuid
        }).then(this.getGameRooms)
      }
    },

    kickPlayer(uuid, userId) {
      if (confirm("Are you sure you want to kick this player?: " + userId)) {
        apiService.post('/api/kickPlayer', {
          gameId: uuid,
          targetUserId: userId
        }).then(this.getGameRooms)
      }
    },

    banPlayer(uuid, userId) {
      if (confirm("Are you sure you want to ban this player?: " + userId)) {
        apiService.post('/api/banPlayer', {
          gameId: uuid,
          targetUserId: userId
        }).then(this.getGameRooms)
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

    },

    loadBeatmapReports() {
      apiService.get(`${process.env.VUE_APP_API_URL}/api/beatmapReports`)
          .then((response) => {
            this.beatmapReports = response.data;
          })
    },

    addBlur(beatmapId) {
      var beatmapsetId = String(beatmapId);

      if (beatmapsetId.includes("beatmapsets")) {
        const split = beatmapsetId.split("/");
        beatmapsetId = split[split.length - 1];
      }

      apiService.post(`${process.env.VUE_APP_API_URL}/api/addBlur?beatmapsetId=${beatmapsetId}`)
          .then(() => {
            this.logMessages.push(`Blurred beatmap ${beatmapsetId}`);
            this.loadBeatmapReports();
          })
    },

    deleteReport(beatmapId) {
      apiService.post(`${process.env.VUE_APP_API_URL}/api/deleteReport?beatmapsetId=${beatmapId}`)
          .then(this.loadBeatmapReports)
    },

    filterOptions() {
      let options = [];
      if (this.selectedAliasType === 'artist') {
        options = this.list_artists;
      } else if (this.selectedAliasType === 'title') {
        options = this.list_titles;
      } else if (this.selectedAliasType === 'creator') {
        options = this.list_creators;
      }
      this.filteredOptions = options.filter(option => option.toLowerCase().includes(this.aliasInput.toLowerCase()));
    },
    selectOption(option) {
      this.aliasInput = option;
      this.filteredOptions = [];
    },
    filterTargetOptions() {
      let options = [];
      if (this.selectedAliasType === 'artist') {
        options = this.list_artists;
      } else if (this.selectedAliasType === 'title') {
        options = this.list_titles;
      } else if (this.selectedAliasType === 'creator') {
        options = this.list_creators;
      }
      this.filteredTargetOptions = options.filter(option => option.toLowerCase().includes(this.targetAliasInput.toLowerCase()));
    },
    selectTargetOption(option) {
      this.targetAliasInput = option;
      this.filteredTargetOptions = [];
    },
    clearOptions() {
      this.filteredOptions = [];
    },
    clearTargetOptions() {
      this.filteredTargetOptions = [];
    },

    submitAlias() {
      apiService.post(`${process.env.VUE_APP_API_URL}/api/addAlias`, {
        source: this.aliasInput,
        target: this.targetAliasInput,
        type: this.selectedAliasType.toUpperCase()
      })
    }
  },
  components: {
    BeatmapRenderer
  }
}
</script>
<style scoped>
.debug-page {
  text-align: left;
  overflow: auto;
  height: 90vh;
  padding: 20px;
}

.beatmap-container {
  margin-bottom: 20px;
}

.beatmap {
  border: 1px solid #ccc;
  border-radius: 5px;
  padding: 10px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.image-report {
  height: 300px;
  width: auto;
  display: block;
  margin: 10px 0;
  border: 1px solid #ccc;
  border-radius: 5px;
}

.report-count {
  font-size: 16px;
  margin-bottom: 10px;
}

.delete-button {
  margin: 5px;
  border: 1px solid #ccc;
  border-radius: 5px;
  font-size: 16px;
  color: #333;
  font-family: 'Sen', serif;
  height: 2em;
  padding: 0 10px;
  background-color: #f0f0f0;
  cursor: pointer;
}

.delete-button:hover {
  background-color: #e0e0e0;
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

.alias-container {
  margin-bottom: 20px;
}

.radio-buttons {
  margin-bottom: 10px;
}

.input-alias {
  padding: 5px;
  margin-bottom: 10px;
  width: 50%;
}

.autocomplete-options {
  width: 50%;
  border: 1px solid var(--color-text);
  border-radius: 5px;
  max-height: 150px;
  overflow-y: auto;
  list-style-type: none;
  padding: 0;
  margin: 0;
}

.autocomplete-options li {
  padding: 5px;
  cursor: pointer;
}

.autocomplete-options li:hover {
  background-color: var(--color-disabled);
}
</style>