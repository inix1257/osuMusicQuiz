<script>
import {useUserStore} from "@/stores/userStore";
import apiService from "@/api/apiService";
import moment from "moment";
import {FontAwesomeIcon} from "@fortawesome/vue-fontawesome";

export default {
  name: "UserPage",
  components: {
    FontAwesomeIcon
  },
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
      return `${totalGuesses}/${totalPlays}`;
    },

    getTotalAccuracy() {
      const totalGuesses = this.player.playerStats.reduce((acc, stat) => acc + stat.guessed, 0);
      const totalPlays = this.player.playerStats.reduce((acc, stat) => acc + stat.played, 0);
      return totalPlays > 0 ? (totalGuesses / totalPlays * 100) : 0;
    },

    getAccuracyColor(percentage) {
      // Ensure percentage is a valid number
      if (!percentage || isNaN(percentage)) return '#6b7280'; // Gray for invalid data
      
      // Clamp percentage between 0 and 100
      const clampedPercentage = Math.max(0, Math.min(100, percentage));
      
      // Create a smooth gradient from red (0%) to green (100%)
      // Using HSL for better color transitions
      const hue = clampedPercentage * 120 / 100; // 0 = red, 120 = green
      const saturation = 70; // Keep saturation consistent
      const lightness = 45 + (clampedPercentage * 15 / 100); // 45% to 60% for good contrast
      
      return `hsl(${hue}, ${saturation}%, ${lightness}%)`;
    }
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
        <!-- User Profile Header -->
        <div class="profile-header">
          <a :href="`https://osu.ppy.sh/users/${player.id}`" target="_blank" @click.stop="">
            <div class="user-header">
              <img :src="player.avatar_url" alt="User's avatar" class="user-avatar">
              <div class="user-header-right">
                <p class="user-username">{{ player.username }}</p>
                <p class="user-title" v-if="player.current_title_achievement != null">{{ player.current_title_achievement.name }}</p>
                <p class="user-rank">Rank: <strong>#{{ player.rank }}</strong></p>
                <p class="user-register-date">Joined: {{ formatDate(player.register_date) }}</p>
              </div>
            </div>
          </a>
          
          <!-- Stats Overview -->
          <div class="stats-overview">
            <div class="stat-card">
              <div class="stat-icon"><FontAwesomeIcon icon="crown" /></div>
              <div class="stat-content">
                <div class="stat-value">{{ player.points }}</div>
                <div class="stat-label">Points</div>
              </div>
            </div>
            <div class="stat-card">
              <div class="stat-icon"><FontAwesomeIcon icon="star" /></div>
              <div class="stat-content">
                <div class="stat-value">{{ player.level }}</div>
                <div class="stat-label">Level</div>
              </div>
            </div>
            <div class="stat-card">
              <div class="stat-icon"><FontAwesomeIcon icon="chart-bar" /></div>
              <div class="stat-content">
                <div class="stat-value">{{ getTotalStats() }}</div>
                <div class="stat-label">Total Stats</div>
              </div>
            </div>
          </div>

          <!-- Achievement Selection (if own profile) -->
          <div v-if="me.id === player.id" class="achievement-section">
            <label class="achievement-label">Current Title:</label>
            <select @click.stop="" v-model="selectedAchievement" @change="onAchievementUpdate" class="user-title-selection">
              <option v-for="achievement in achievements" :key="achievement.id" :value="achievement">
                {{ achievement.name }}
              </option>
            </select>
          </div>
        </div>

        <!-- Game Mode Tabs -->
        <div class="tabs-container">
          <div class="tabs-header">
            <h3>Game Statistics</h3>
          </div>
          
          <div class="tabs">
            <div class="tabs-row">
              <div class="game-modes">
                <div class="mode-buttons">
                  <button v-for="mode in gameModes" :key="mode" @click.stop="selectGameMode(mode)" :class="{ active: selectedGameMode === mode }">
                    {{ mode }}
                  </button>
                </div>
              </div>
              <div class="guess-modes">
                <div class="mode-buttons">
                  <button v-for="mode in guessModes" :key="mode" @click.stop="selectGuessMode(mode)" :class="{ active: selectedGuessMode === mode }">
                    {{ mode }}
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- Statistics Display -->
        <div class="user-stats-container">
          <div v-if="filteredStats.length > 0">
            <div class="stats-grid">
              <div v-for="stat in filteredStats" :key="stat.difficulty" class="stat-item">
                <div class="difficulty-badge" :class="'difficulty-' + stat.difficulty.toLowerCase()">
                  {{ stat.difficulty.toUpperCase() }}
                </div>
                <div class="stat-numbers">
                  <span class="correct-count">{{ stat.guessed }}</span>
                  <span class="separator">/</span>
                  <span class="total-count">{{ stat.played }}</span>
                </div>
                <div class="accuracy-percentage" :style="{ color: getAccuracyColor(stat.guessed / stat.played * 100) }">
                  {{ (stat.guessed / stat.played * 100 || 0).toFixed(1) }}%
                </div>
              </div>
            </div>
            
            <div class="mode-total">
              <div class="total-label">Mode Total:</div>
              <div class="total-numbers">
                <span class="correct-count">{{ filteredStats.reduce((acc, stat) => acc + stat.guessed, 0) }}</span>
                <span class="separator">/</span>
                <span class="total-count">{{ filteredStats.reduce((acc, stat) => acc + stat.played, 0) }}</span>
              </div>
            </div>
          </div>
          
          <div v-else class="no-stats-message">
            <div class="no-stats-icon"><FontAwesomeIcon icon="chart-bar" /></div>
            <div class="no-stats-text">No stats found for this mode</div>
            <div class="no-stats-subtext">Try selecting a different game mode or guess mode</div>
          </div>
          
          <div class="user-total">
            <div class="total-label">Overall Total:</div>
            <div class="total-numbers">
              <span class="correct-count">{{ getTotalStats() }}</span>
            </div>
            <div class="total-accuracy" :style="{ color: getAccuracyColor(getTotalAccuracy()) }">
              ({{ getTotalAccuracy().toFixed(1) }}%)
            </div>
          </div>
        </div>
      </div>
    </div>
</template>

<style scoped>
.user-info-container {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
}

.user-info {
  width: 85vw;
  max-width: 1200px;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 30px;
  background: linear-gradient(135deg, var(--color-body) 0%, rgba(255, 255, 255, 0.02) 100%);
  border-radius: 25px;
  margin-bottom: 20px;
  box-shadow: 0 15px 35px rgba(0, 0, 0, 0.15);
  border: 1px solid rgba(255, 255, 255, 0.05);
  transition: all 0.3s ease;
}

.user-info:hover {
  transform: translateY(-2px);
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.2);
}

/* Profile Header Section */
.profile-header {
  width: 100%;
  display: flex;
  gap: 20px;
  margin-bottom: 25px;
  align-items: stretch;
}

.user-header {
  display: flex;
  flex-direction: row;
  align-items: center;
  padding: 20px;
  background: linear-gradient(135deg, var(--color-secondary) 0%, rgba(255, 255, 255, 0.05) 100%);
  border-radius: 15px;
  box-shadow: 0 8px 25px rgba(0, 0, 0, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.1);
  transition: all 0.3s ease;
  cursor: pointer;
  flex: 1;
}

.user-header:hover {
  transform: translateY(-2px);
  box-shadow: 0 12px 30px rgba(0, 0, 0, 0.15);
  background: linear-gradient(135deg, var(--color-secondary) 0%, rgba(255, 255, 255, 0.08) 100%);
}

.user-header-right {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  justify-content: center;
  margin-left: 20px;
}

.user-avatar {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  object-fit: cover;
  border: 3px solid var(--color-text);
  box-shadow: 0 8px 25px rgba(0, 0, 0, 0.3);
  transition: all 0.3s ease;
}

.user-header:hover .user-avatar {
  transform: scale(1.05);
  box-shadow: 0 12px 35px rgba(0, 0, 0, 0.4);
}

.user-username {
  font-size: 1.8em;
  font-weight: bold;
  margin-bottom: 5px;
  color: var(--color-text);
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.3);
  letter-spacing: 0.5px;
}

.user-title {
  font-size: 1.1em;
  margin-top: 0;
  margin-bottom: 5px;
  color: #a855f7;
  background: linear-gradient(45deg, #a855f7, #8b5cf6);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  font-weight: 600;
  text-shadow: 0 1px 2px rgba(168, 85, 247, 0.3);
}

.user-rank {
  margin-top: 0;
  font-size: 1.2em;
  color: var(--color-text);
  opacity: 0.9;
}

.user-register-date {
  font-size: 0.9em;
  color: var(--color-text);
  opacity: 0.7;
  margin: 0;
}

/* Stats Overview Cards */
.stats-overview {
  display: flex;
  flex-direction: column;
  gap: 12px;
  flex: 1;
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 15px;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.08) 0%, rgba(255, 255, 255, 0.03) 100%);
  border-radius: 12px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.1);
  transition: all 0.3s ease;
  min-width: 140px;
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.15);
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.12) 0%, rgba(255, 255, 255, 0.05) 100%);
}

.stat-icon {
  font-size: 1.5em;
  opacity: 0.8;
}

.stat-content {
  display: flex;
  flex-direction: column;
}

.stat-value {
  font-size: 1.3em;
  font-weight: bold;
  color: var(--color-text);
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.3);
}

.stat-label {
  font-size: 0.8em;
  color: var(--color-text);
  opacity: 0.7;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  text-align: left;
}

/* Achievement Section */
.achievement-section {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 10px;
  padding: 20px;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.05) 0%, rgba(255, 255, 255, 0.02) 100%);
  border-radius: 15px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  flex: 1;
  min-width: 200px;
}

.achievement-label {
  font-size: 1.1em;
  font-weight: 600;
  color: var(--color-text);
  white-space: nowrap;
}

.user-title-selection {
  padding: 12px 16px;
  border: 2px solid rgba(255, 255, 255, 0.2);
  border-radius: 10px;
  font-size: 16px;
  background: linear-gradient(135deg, var(--color-secondary) 0%, rgba(255, 255, 255, 0.05) 100%);
  color: var(--color-text);
  box-sizing: border-box;
  transition: all 0.3s ease;
  cursor: pointer;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.1);
  width: 100%;
}

.user-title-selection:focus {
  border-color: #60a5fa;
  outline: none;
  box-shadow: 0 0 15px rgba(96, 165, 250, 0.3);
  transform: translateY(-1px);
}

.user-title-selection:hover {
  border-color: rgba(255, 255, 255, 0.4);
  background: linear-gradient(135deg, var(--color-secondary) 0%, rgba(255, 255, 255, 0.08) 100%);
}

/* Tabs Container */
.tabs-container {
  width: 100%;
  margin-bottom: 25px;
}

.tabs-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
  padding: 0 5px;
}

.tabs-header h3 {
  font-size: 1.4em;
  font-weight: 700;
  color: var(--color-text);
  margin: 0;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.3);
}

.user-register-date {
  font-size: 0.9em;
  color: var(--color-text);
  opacity: 0.7;
  margin: 0;
}

.tabs {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}

.tabs-row {
  display: flex;
  justify-content: space-between;
  width: 100%;
  gap: 30px;
}

.game-modes,
.guess-modes {
  display: flex;
  gap: 6px;
  padding: 15px 20px;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.05) 0%, rgba(255, 255, 255, 0.02) 100%);
  border-radius: 12px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  flex: 1;
}

.mode-buttons {
  display: flex;
  gap: 6px;
  width: 100%;
  justify-content: center;
}

.tabs > * button {
  padding: 8px 16px;
  border: 2px solid rgba(255, 255, 255, 0.2);
  border-radius: 8px;
  background: linear-gradient(135deg, var(--color-secondary) 0%, rgba(255, 255, 255, 0.05) 100%);
  color: var(--color-text);
  cursor: pointer;
  font-weight: 600;
  transition: all 0.3s ease;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.1);
  text-transform: uppercase;
  letter-spacing: 0.5px;
  font-size: 0.85em;
  min-width: 60px;
}

.tabs > * button:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.15);
  border-color: rgba(255, 255, 255, 0.4);
  background: linear-gradient(135deg, var(--color-secondary) 0%, rgba(255, 255, 255, 0.08) 100%);
}

.tabs > * button.active {
  border-color: #60a5fa;
  background: linear-gradient(135deg, #60a5fa, #3b82f6);
  color: white;
  box-shadow: 0 6px 20px rgba(96, 165, 250, 0.3);
  transform: translateY(-2px);
}

/* Statistics Container */
.user-stats-container {
  width: 100%;
  padding: 25px;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.05) 0%, rgba(255, 255, 255, 0.02) 100%);
  border-radius: 15px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  box-shadow: 0 8px 25px rgba(0, 0, 0, 0.1);
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 15px;
  margin-bottom: 25px;
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 20px;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.08) 0%, rgba(255, 255, 255, 0.03) 100%);
  border-radius: 12px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  transition: all 0.3s ease;
  text-align: center;
}

.stat-item:hover {
  transform: translateY(-3px);
  box-shadow: 0 8px 25px rgba(0, 0, 0, 0.15);
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.12) 0%, rgba(255, 255, 255, 0.05) 100%);
}

.difficulty-badge {
  font-size: 1.3em;
  font-weight: bold;
  padding: 6px 12px;
  border-radius: 20px;
  margin-bottom: 12px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.stat-numbers {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.correct-count {
  font-size: 1.8em;
  font-weight: bold;
  color: #4ade80;
  text-shadow: 0 2px 4px rgba(74, 222, 128, 0.3);
}

.separator {
  font-size: 1.2em;
  color: var(--color-text);
  opacity: 0.6;
}

.total-count {
  font-size: 1.8em;
  font-weight: bold;
  color: var(--color-text);
  opacity: 0.8;
}

.accuracy-percentage {
  font-size: 1.2em;
  font-weight: 600;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.3);
}

.mode-total,
.user-total {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 15px;
  padding: 15px;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.08) 0%, rgba(255, 255, 255, 0.03) 100%);
  border-radius: 10px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  margin-bottom: 15px;
}

.mode-total:last-child,
.user-total:last-child {
  margin-bottom: 0;
}

.total-label {
  font-size: 1.1em;
  font-weight: 600;
  color: var(--color-text);
}

.total-numbers {
  display: flex;
  align-items: center;
  gap: 8px;
}

.total-accuracy {
  font-size: 1.2em;
  font-weight: 600;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.3);
}

.no-stats-message {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
  text-align: center;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.05) 0%, rgba(255, 255, 255, 0.02) 100%);
  border-radius: 15px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  margin-bottom: 25px;
}

.no-stats-icon {
  font-size: 3em;
  margin-bottom: 15px;
  opacity: 0.6;
}

.no-stats-text {
  font-size: 1.3em;
  font-weight: 600;
  color: var(--color-text);
  margin-bottom: 8px;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.3);
}

.no-stats-subtext {
  font-size: 1em;
  color: var(--color-text);
  opacity: 0.7;
  font-weight: 400;
}

.notification-push {
  position: fixed;
  bottom: 20px;
  right: 20px;
  background: linear-gradient(135deg, #4ade80, #22c55e);
  color: white;
  padding: 15px 20px;
  border-radius: 10px;
  z-index: 1000;
  box-shadow: 0 8px 25px rgba(74, 222, 128, 0.3);
  font-weight: 600;
  animation: slideIn 0.3s ease;
}

@keyframes slideIn {
  from {
    transform: translateX(100%);
    opacity: 0;
  }
  to {
    transform: translateX(0);
    opacity: 1;
  }
}

/* Responsive adjustments */
@media (max-width: 768px) {
  .user-info {
    width: 95vw;
    padding: 20px;
  }
  
  .profile-header {
    flex-direction: column;
    gap: 15px;
  }
  
  .stats-overview {
    flex-direction: row;
    justify-content: space-between;
  }
  
  .stat-card {
    min-width: auto;
    flex: 1;
  }
  
  .user-header {
    flex-direction: column;
    text-align: center;
    padding: 15px;
  }
  
  .user-header-right {
    margin-left: 0;
    margin-top: 15px;
    align-items: center;
  }
  
  .user-username {
    font-size: 1.5em;
  }
  
  .user-avatar {
    width: 70px;
    height: 70px;
  }
  
  .achievement-section {
    width: 100%;
    flex-direction: row;
    align-items: center;
    gap: 15px;
  }
  
  .achievement-label {
    white-space: nowrap;
  }
  
  .user-title-selection {
    flex: 1;
  }
  
  .stats-grid {
    grid-template-columns: 1fr;
    gap: 12px;
  }
  
  .game-modes,
  .guess-modes {
    flex-wrap: wrap;
    justify-content: center;
    gap: 6px;
  }
  
  .tabs-row {
    flex-direction: column;
    gap: 15px;
  }

  .mode-buttons {
    justify-content: center;
  }

  .tabs > * button {
    padding: 8px 12px;
    font-size: 0.8em;
  }
  
  .tabs-header {
    flex-direction: column;
    gap: 8px;
    align-items: center;
  }
}
</style>