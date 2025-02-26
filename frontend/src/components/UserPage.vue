<script>
import {useUserStore} from "@/stores/userStore";
import apiService from "@/api/apiService";
import moment from "moment";

export default {
  name: "UserPage",
  setup() {
    const userStore = useUserStore();
    return {
      me: userStore.getMe(),
    };
  },
  data() {
    return {
      player: {},
      achievements: [],
      current_achievement: {},
      selectedAchievement: null,
      notificationMessage: "",
      showNotification: false,
      selectedGameMode: "STD",
      selectedGuessMode: "TITLE",
      gameModes: ["STD", "TAIKO", "CTB", "MANIA"],
      guessModes: ["TITLE", "ARTIST", "CREATOR"],
    };
  },
  props: {
    playerId: {
      type: String,
      required: true
    }
  },
  methods: {
    getUserData() {
      apiService.get(`/api/user?id=${this.playerId}`)
          .then((response) => {
            this.player = response.data;
            this.selectedAchievement = response.data.current_title_achievement;

            if (this.me.id === this.player.id) {
              apiService.get(`/api/achievement`)
                  .then((response) => {
                    this.achievements = response.data;

                    const nullAchievement = {
                      id: -1,
                      name: "None",
                      description: "No achievement selected",
                      icon_url: ""
                    }

                    this.achievements.unshift(nullAchievement);
                  })
            }
          })
    },

    formatDate(date) {
      return moment(date).format('MMMM Do YYYY');
    },

    onAchievementUpdate() {
      clearTimeout(this.showNotification);
      apiService.post('/api/achievement', {
        userId: this.player.id,
        achievementId: this.selectedAchievement.id
      })
          .then(() => {
            apiService.get(`/api/user?id=${this.playerId}`)
                .then((response) => {
                  this.player = response.data;
                  this.selectedAchievement = response.data.current_title_achievement;

                  this.notificationMessage = "Achievement updated to " + this.selectedAchievement.name;
                  this.showNotification = true;
                  setTimeout(() => {
                    this.showNotification = false;
                  }, 3000);
                })
          })
    },

    selectGameMode(gameMode) {
      this.selectedGameMode = gameMode;
    },
    selectGuessMode(guessMode) {
      this.selectedGuessMode = guessMode;
    },

    getTotalStats() {
      const totalGuesses = this.player.playerStats.reduce((acc, stat) => acc + stat.guessed, 0);
      const totalPlays = this.player.playerStats.reduce((acc, stat) => acc + stat.played, 0);
      return `${totalGuesses}/${totalPlays} (${(totalGuesses / totalPlays * 100 || 0).toFixed(2)}%)`;
    },
  },

  mounted() {
    this.getUserData();
  },

  computed: {
    filteredStats() {
      return this.player.playerStats.filter(
          (stat) =>
              stat.gameMode === this.selectedGameMode &&
              stat.guessMode === this.selectedGuessMode
      );
    },
  },
}
</script>

<template>
    <div class="user-info-container">
      <div class="notification-push" v-if="showNotification">
        {{ notificationMessage }}
      </div>
      <div class="user-info" v-if="player.id">
        <a :href="`https://osu.ppy.sh/users/${player.id}`" target="_blank" @click.stop="">
          <div class="user-header">
            <img :src="player.avatar_url" alt="User's avatar" class="user-avatar">
            <div class="user-header-right">
              <p class="user-username">{{ player.username }}</p>
              <p class="user-title" v-if="player.current_title_achievement != null">{{ player.current_title_achievement.name }}</p>
              <p class="user-rank">Rank: <strong>#{{ player.rank }}</strong></p>
            </div>
          </div>
        </a>
        <div class="user-levelpoints">
          <p class="user-points"><strong>{{ player.points }}</strong> pts</p>
          <p class="user-level">(Level <strong>{{ player.level }}</strong>)</p>
        </div>
        <div v-if="me.id === player.id">
          Current Title:
          <select @click.stop="" v-model="selectedAchievement" @change="onAchievementUpdate" class="user-title-selection">
            <option v-for="achievement in achievements" :key="achievement.id" :value="achievement">
              {{ achievement.name }}
            </option>
          </select>
        </div>

        <div class="tabs">
          <div class="game-modes">
            <button v-for="mode in gameModes" :key="mode" @click.stop="selectGameMode(mode)" :class="{ active: selectedGameMode === mode }">
              {{ mode }}
            </button>
          </div>
          <div class="guess-modes">
            <button v-for="mode in guessModes" :key="mode" @click.stop="selectGuessMode(mode)" :class="{ active: selectedGuessMode === mode }">
              {{ mode }}
            </button>
          </div>
        </div>

        <div class="user-stats-container">
          <p class="user-stats" v-for="stat in filteredStats" :key="stat.difficulty">
            <span :class="'difficulty-' + stat.difficulty.toLowerCase()">{{ stat.difficulty.toUpperCase() }}</span>:
            {{ stat.guessed }}/{{ stat.played }}
            ({{ (stat.guessed / stat.played * 100 || 0).toFixed(2) }}%)
          </p>
          <p v-if="filteredStats.length > 0">Mode Total: {{ filteredStats.reduce((acc, stat) => acc + stat.guessed, 0) }}/{{ filteredStats.reduce((acc, stat) => acc + stat.played, 0) }}</p>
          <p v-else>No stats found for this mode</p>

          <p class="user-guesses">User Total: {{ getTotalStats() }}</p>
        </div>

        <p class="user-register-date">OMQ Registration: {{ formatDate(player.register_date) }}</p>
      </div>
    </div>
</template>

<style scoped>
.modal-overlay {
  position: fixed;
  width: 100%;
  height: 100%;
  top: 0;
  left: 0;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
}

.user-info-container {
  position: relative;

  display: flex;
  align-items: center;
  justify-content: center;
}

.user-info {
  width: 50vw;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 20px;
  border: 1px solid #ddd;
  border-radius: 10px;
  margin-bottom: 20px;
  background-color: var(--color-secondary);
}

.user-header {
  display: flex;
  flex-direction: row;
  align-items: center;
}

.user-header-right {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  justify-content: center;
}

.user-header-details {
  display: flex;
  align-items: center;
}

.user-levelpoints {
  display: flex;
  flex-direction: row;
  align-items: center;
  gap: 10px;
}

.user-levelpoints p {
  font-size: 1.5em;
  margin: 0;
}

.user-avatar {
  width: 100px;
  height: 100px;
  border-radius: 50%;
  object-fit: cover;
  margin-bottom: 10px;
  margin-right: 20px;
  border: 3px solid var(--color-text);
}

.user-username {
  font-size: 2.5em;
  font-weight: bold;
  margin-bottom: 0;
}

.user-title {
  font-size: 1.5em;
  margin-top: 0;
  margin-bottom: 0;
}

.user-rank {
  margin-top: 0;
  font-size: 1.8em;
}

.user-points {
  margin-left: 12px;
  font-size: 1.2em;
}

.user-title-selection {
  padding: 10px;
  border: 1px solid #ccc;
  border-radius: 5px;
  margin-top: 8px;
  font-size: 16px;
  background-color: var(--color-secondary);
  color: var(--color-text);
  box-sizing: border-box;
  transition: border-color 0.3s ease;
}

.user-title-selection:focus {
  border-color: #007bff;
  outline: none;
  box-shadow: 0 0 5px rgba(0, 123, 255, 0.5);
}

.user-guesses,
.user-plays,
.user-register-date {
  font-size: 16px;
  margin-bottom: 5px;
}

.user-stats {
  font-size: 16px;
  margin-bottom: 0;
  margin-top: 0.5em;
}

.user-stats-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: 16px;
  margin-top: 16px;
}

.notification-push {
  position: fixed;
  bottom: 20px;
  right: 20px;
  background-color: #4caf50;
  color: white;
  padding: 10px;
  border-radius: 5px;
  z-index: 1000;
}

.tabs {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-top: 1em;
}

.game-modes,
.guess-modes {
  display: flex;
  gap: 10px;
  margin-bottom: 8px;
}

.tabs > * button {
  padding: 10px;
  border: 1px solid #ccc;
  border-radius: 5px;
  background-color: var(--color-secondary);
  color: var(--color-text);
  cursor: pointer;
}

.tabs > * button.active {
  border-color: #007bff;
  background-color: #007bff;
  color: white;
}
</style>