<script>
import apiService from "@/api/apiService";
import {useUserStore} from "@/stores/userStore";

import { toRaw } from "vue";

export default {
  name: "RoomCreateModal",
  props: {
    actionType: {
      type: String,
      required: true,
      validator: value => ['create', 'update'].includes(value)
    },
    game: {
      type: Object,
      required: false
    }
  },
  setup() {
    const userStore = useUserStore();
    return {
      me: userStore.getMe(),
    };
  },
  data() {
    return {
      gameName: "",
      password: "",
      totalQuestions: 20,
      guessingTime: 15,
      cooldownTime: 7,
      autoskip: true,
      difficulty: ["EASY", "NORMAL"],
      guessMode: 'TITLE',
      isLoading: false,
      isRoomSettingLoading: false,
      startYear: 2007,
      endYear: new Date().getFullYear(),
      isExpanded: false,
      poolMode: 'TITLE',
      selectedGameMode: 'STD',
      gameModes: ['STD', 'MANIA', 'CTB', 'TAIKO'],
      displayMode: ["BACKGROUND", "AUDIO"],
      genreType: {
        ANY: 0,
        UNSPECIFIED: 1,
        VIDEO_GAME: 2,
        ANIME: 3,
        ROCK: 4,
        POP: 5,
        OTHER: 6,
        NOVELTY: 7,
        HIP_HOP: 9,
        ELECTRONIC: 10,
        METAL: 11,
        CLASSICAL: 12,
        FOLK: 13,
        JAZZ: 14
      },
      languageType: {
        ANY: 0,
        UNSPECIFIED: 1,
        ENGLISH: 2,
        JAPANESE: 3,
        CHINESE: 4,
        INSTRUMENTAL: 5,
        KOREAN: 6,
        FRENCH: 7,
        GERMAN: 8,
        SWEDISH: 9,
        SPANISH: 10,
        ITALIAN: 11,
        RUSSIAN: 12,
        POLISH: 13,
        OTHER: 14
      },
      selectedGenres: {
        ANY: 0,
        UNSPECIFIED: 1,
        VIDEO_GAME: 2,
        ANIME: 3,
        ROCK: 4,
        POP: 5,
        OTHER: 6,
        NOVELTY: 7,
        HIP_HOP: 9,
        ELECTRONIC: 10,
        METAL: 11,
        CLASSICAL: 12,
        FOLK: 13,
        JAZZ: 14
      },
      selectedLanguages: {
        ANY: 0,
        UNSPECIFIED: 1,
        ENGLISH: 2,
        JAPANESE: 3,
        CHINESE: 4,
        INSTRUMENTAL: 5,
        KOREAN: 6,
        FRENCH: 7,
        GERMAN: 8,
        SWEDISH: 9,
        SPANISH: 10,
        ITALIAN: 11,
        RUSSIAN: 12,
        POLISH: 13,
        OTHER: 14
      },
      selectedTab: 'General',
      tabs: ['General', 'Advanced', 'Genres', 'Languages'],
      isRequestPending: false
    }
  },
  created() {
    if (this.actionType === 'create') {
      this.loadSettings();
    } else if (this.actionType === 'update') {
      this.loadRoomSettings();
    }
  },

  methods: {
    closeModal() {
      this.$emit("close-modal");
    },

    loadRoomSettings() {
      this.isRoomSettingLoading = true;

      this.gameName = this.game.name;
      this.totalQuestions = this.game.totalQuestions;
      this.guessingTime = this.game.guessingTime;
      this.cooldownTime = this.game.cooldownTime;
      this.autoskip = this.game.autoskip;
      this.difficulty = this.game.difficulty;
      this.startYear = this.game.startYear;
      this.endYear = this.game.endYear;
      this.selectedGameMode = this.game.gameMode;
      this.guessMode = this.game.guessMode;
      this.poolMode = this.game.poolMode;
      this.displayMode = this.game.displayMode;

      this.selectedGenres = this.game.genreType.map(genre => this.genreType[genre]);
      this.selectedLanguages = this.game.languageType.map(language => this.languageType[language]);

      this.isRoomSettingLoading = false;
    },

    async updateRoomSettings() {
      if (this.isRequestPending) {
        return;
      }

      if (this.displayMode.length === 0) {
        alert("You must select at least one display mode.");
        return;
      }

      this.isRequestPending = true;

      const updateRoomSettingsDTO = {
        userToken: localStorage.getItem("accessToken"),
        uuid: this.game.uuid,
        name: this.gameName,
        totalQuestions: this.totalQuestions,
        guessingTime: this.guessingTime,
        cooldownTime: this.cooldownTime,
        autoskip: this.autoskip,
        difficulty: this.difficulty,
        startYear: this.startYear,
        endYear: this.endYear,
        gameMode: this.selectedGameMode,
        guessMode: this.guessMode,
        poolMode: this.poolMode,
        displayMode: this.displayMode,
        genreType: this.selectedGenres,
        languageType: this.selectedLanguages
      }

      apiService.post(`${process.env.VUE_APP_API_URL}/api/updateRoomSettings`, updateRoomSettingsDTO)
          .then((response) => {
            this.getGamerooms();
          })
          .catch((error) => {
          })
          .finally(() => {
            this.isLoading = false;
            this.isModalOpen = false;
            this.$emit('toggleRoomSettingsModal');
            this.isRequestPending = false;
          });
    },

    revertSettingsToDefault() {
      this.totalQuestions = 20;
      this.guessingTime = 15;
      this.cooldownTime = 7;
      this.autoskip = true;
      this.difficulty = ["EASY", "NORMAL"];
      this.startYear = 2007;
      this.endYear = new Date().getFullYear();
      this.selectedGameMode = 'STD';
      this.guessMode = 'TITLE';
      this.poolMode = 'DEFAULT';
      this.displayMode = ["BACKGROUND", "AUDIO"];
      this.selectedGenres = [...Object.values(this.genreType)];
      this.selectedLanguages = [...Object.values(this.languageType)];
    },

    joinGame(gameId, isPrivate) {
      this.$router.push({
        name: 'GamePage',
        params: {
          gameId: gameId,
          private: isPrivate
        }
      });
    },

    createNewGame() {
      this.isLoading = true;

      const currentYear = new Date().getFullYear();
      if (this.startYear < 2007) {
        this.startYear = 2007;
      }
      if (this.endYear > currentYear) {
        this.endYear = currentYear;
      }
      if (this.startYear > this.endYear) {
        this.startYear = this.endYear;
      }

      if (!this.gameName) {
        this.gameName = `${this.me.username}'s game`;
      }

      if (this.guessMode === 'DEFAULT') {
        this.guessMode = 'TITLE';
      }

      if (!this.selectedGameMode || this.selectedGameMode === 'TITLE' || this.selectedGameMode === 'DEFAULT') {
        this.selectedGameMode = 'STD';
      }

      if (this.displayMode.includes('PATTERN') && this.selectedGameMode !== 'STD') {
        alert("Pattern mode is only available for standard game mode.");
        this.isLoading = false;
        return;
      }

      const createGameDTO = {
        name: this.gameName,
        password: this.password,
        totalQuestions: this.totalQuestions,
        difficulty: this.difficulty,
        guessingTime: this.guessingTime,
        cooldownTime: this.cooldownTime,
        autoskip: this.autoskip,
        guessMode: this.guessMode,
        startYear: this.startYear,
        endYear: this.endYear,
        gameMode: this.selectedGameMode,
        poolMode: this.poolMode,
        displayMode: this.displayMode,
        genreType: this.selectedGenres,
        languageType: this.selectedLanguages
      };

      if (this.displayMode.length === 0) {
        alert("You must select at least one display mode.");
        this.isLoading = false;
        return;
      }

      // Check if any field is empty
      if (!createGameDTO.name || !createGameDTO.totalQuestions || !createGameDTO.difficulty || !createGameDTO.guessMode) {
        alert("An error occurred. Please try again.")
        this.isLoading = false;
        return;
      }

      apiService.post('/api/createNewGame', createGameDTO)
          .then((response) => {
            localStorage.setItem("gameId", response.data);
            localStorage.setItem("roompw", this.password);

            this.saveSettings();
            this.joinGame(response.data.uuid, response.data.private);
          })
          .finally(() => {
            this.isLoading = false;
            this.closeModal();
          });
    },

    formatLabel(label) {
      if (label === "ANY") {
        return "All";
      }

      let formattedLabel = label.replace(/_/g, ' ');
      let words = formattedLabel.split(' ');
      words = words.map(word => word.charAt(0).toUpperCase() + word.slice(1).toLowerCase());
      formattedLabel = words.join(' ');

      return formattedLabel;
    },

    saveSettings() {
      const settings = {
        totalQuestions: this.totalQuestions,
        guessingTime: this.guessingTime,
        cooldownTime: this.cooldownTime,
        autoskip: this.autoskip,
        difficulty: this.difficulty,
        startYear: this.startYear,
        endYear: this.endYear,
        gameMode: this.selectedGameMode,
        guessMode: this.guessMode,
        poolMode: this.poolMode,
        displayMode: this.displayMode,
        selectedGenres: this.selectedGenres,
        selectedLanguages: this.selectedLanguages
      };
      localStorage.setItem('roomCreateSettings', JSON.stringify(settings));
    },

    loadSettings() {
      const settings = JSON.parse(localStorage.getItem('roomCreateSettings'));
      if (settings) {
        this.totalQuestions = settings.totalQuestions ?? this.totalQuestions;
        this.guessingTime = settings.guessingTime ?? this.guessingTime;
        this.cooldownTime = settings.cooldownTime ?? this.cooldownTime;
        this.autoskip = settings.autoskip ?? this.autoskip;
        this.difficulty = settings.difficulty ?? this.difficulty;
        this.startYear = settings.startYear ?? this.startYear;
        this.endYear = settings.endYear ?? this.endYear;
        this.selectedGameMode = settings.gameMode ?? this.selectedGameMode;
        if (this.selectedGameMode === 'TITLE') {
          this.selectedGameMode = 'STD';
        }
        this.guessMode = settings.guessMode ?? this.guessMode;
        this.poolMode = settings.poolMode ?? this.poolMode;
        if (this.poolMode === 'TITLE') {
          this.poolMode = 'DEFAULT';
        }
        this.displayMode = settings.displayMode ?? this.displayMode;
        this.selectedGenres = settings.selectedGenres ?? this.selectedGenres;
        this.selectedLanguages = settings.selectedLanguages ?? this.selectedLanguages;
      } else {
        this.revertSettingsToDefault();
      }
    },

    handleGenreSelection(value) {
      const index = this.selectedGenres.indexOf(value);
      if (index <= -1) {
        if (value === this.genreType.ANY) {
          this.selectedGenres = [];
        } else {
          this.selectedGenres = this.selectedGenres.filter(genre => genre !== this.genreType.ANY);
        }
      } else {
        if (value === this.genreType.ANY) {
          this.selectedGenres = Object.values(this.genreType);
        }
      }

      if (this.selectedGenres.length === Object.values(this.genreType).length - 1) {
        this.selectedGenres.push(this.genreType.ANY);
      }
    },

    handleLanguageSelection(value) {
      const index = this.selectedLanguages.indexOf(value);
      if (index <= -1) {
        if (value === this.languageType.ANY) {
          this.selectedLanguages = [];
        } else {
          this.selectedLanguages = this.selectedLanguages.filter(language => language !== this.languageType.ANY);
        }
      } else {
        if (value === this.languageType.ANY) {
          this.selectedLanguages = Object.values(this.languageType);
        }
      }

      if (this.selectedLanguages.length === Object.values(this.languageType).length - 1) {
        this.selectedLanguages.push(this.languageType.ANY);
      }
    },
  },

  watch: {
    gameName(newVal) {
      if (newVal.length > 50) {
        this.gameName = newVal.slice(0, 50);
      }
    },

    displayMode(newVal, oldVal) {
      if (newVal.includes('BACKGROUND') && !oldVal.includes('BACKGROUND') && oldVal.includes('PATTERN')) {
        this.displayMode = ['BACKGROUND'];
      }
      if (newVal.includes('AUDIO') && !oldVal.includes('AUDIO') && oldVal.includes('PATTERN')) {
        this.displayMode = ['AUDIO'];
      }
      if (newVal.includes('PATTERN') && !oldVal.includes('PATTERN')) {
        this.displayMode = ['PATTERN'];
      }

    }
  }
}
</script>

<template>
  <div class="modal-background" @click="closeModal">
    <div class="modal" @click.stop>
      <div v-if="isLoading" class="loading-indicator">Please wait...</div>
      <button class="close-button" @click="closeModal">
        <font-awesome-icon :icon="['fas', 'x']" />
      </button>
      <h2>{{ actionType === 'create' ? 'Host New Lobby' : 'Update Settings' }}</h2>
      <div class="modal-content">
        <div class="tabs">
          <button v-for="tab in tabs" :key="tab" @click="selectedTab = tab" :class="{ active: selectedTab === tab }">
            {{ tab }}
          </button>
        </div>
        <div class="tab-content">
            <div v-if="selectedTab === 'General'">
              <div class="form-row">
                <label>Game Name</label>
                <input type="text" v-model="gameName" class="input-gameinfo" :placeholder="`${me.username}'s game`">
              </div>
              <div class="form-row">
                <label>Password</label>
                <input type="password" v-model="password" class="input-gameinfo" :disabled="actionType === 'update'">
              </div>
              <div class="form-row">
                <label class="tooltip-container">Difficulty
                  <span class="tooltip">Select the difficulty levels for the game</span>
                </label>
                <div class="inner-rightalign">
                  <div class="checkbox-div">
                    <input type="checkbox" id="easy" value="EASY" v-model="difficulty" class="difficulty-checkbox">
                    <label for="easy" class="difficulty-label difficulty-easy">E</label>
                  </div>
                  <div class="checkbox-div">
                    <input type="checkbox" id="normal" value="NORMAL" v-model="difficulty" class="difficulty-checkbox">
                    <label for="normal" class="difficulty-label difficulty-normal">N</label>
                  </div>
                  <div class="checkbox-div">
                    <input type="checkbox" id="hard" value="HARD" v-model="difficulty" class="difficulty-checkbox">
                    <label for="hard" class="difficulty-label difficulty-hard">H</label>
                  </div>
                  <div class="checkbox-div">
                    <input type="checkbox" id="insane" value="INSANE" v-model="difficulty" class="difficulty-checkbox">
                    <label for="insane" class="difficulty-label difficulty-insane">I</label>
                  </div>
                  <div class="checkbox-div">
                    <input type="checkbox" id="extra" value="EXTRA" v-model="difficulty" class="difficulty-checkbox">
                    <label for="extra" class="difficulty-label difficulty-extra">X</label>
                  </div>
                </div>
              </div>

              <div class="form-row">
                <label>Year Range</label>
                <div class="inner-rightalign">
                  <input type="number" v-model="startYear" class="input-gameinfo input-yearrange">
                  <strong>~</strong>
                  <input type="number" v-model="endYear" class="input-gameinfo input-yearrange">
                </div>
              </div>

              <div class="form-row">
                <label>Game Mode</label>
                <div class="radio-group inner-rightalign">
                  <select v-model="selectedGameMode" class="dropdown">
                    <option v-for="mode in gameModes" :key="mode" :value="mode">{{ mode }}</option>
                  </select>
                </div>
              </div>

              <div class="form-row">
                <label>Display Mode</label>
                <div class="inner-rightalign">
                  <div class="div-displayModeInnerLeft">
                    <div>
                      <input type="checkbox" id="background" value="BACKGROUND" class="checkbox-displaymode" v-model="displayMode">
                      <label for="background" class="label-displaymode" :class="{ 'displaymode-highlight': (displayMode.includes('BACKGROUND') || displayMode.includes('AUDIO'))}">
                        <font-awesome-icon :icon="['fas', 'image']" />
                        <div>Background</div>
                      </label>
                    </div>
                    <div>
                      <input type="checkbox" id="audio" value="AUDIO" class="checkbox-displaymode" v-model="displayMode">
                      <label for="audio" class="label-displaymode" :class="{ 'displaymode-highlight': (displayMode.includes('BACKGROUND') || displayMode.includes('AUDIO'))}">
                        <font-awesome-icon :icon="['fas', 'music']" />
                        <div>Audio</div>
                      </label>
                    </div>
                  </div>
                  <div>
                    <input type="checkbox" id="pattern" value="PATTERN" class="checkbox-displaymode" v-model="displayMode">
                    <label for="pattern" class="label-displaymode" :class="{ 'displaymode-highlight': displayMode.includes('PATTERN')}">
                      <font-awesome-icon :icon="['fas', 'shapes']" />
                      <div>Pattern</div>
                    </label>
                  </div>
                </div>
              </div>
            </div>
            <div v-if="selectedTab === 'Advanced'">
              <div class="form-row">
                <label>Total Beatmaps</label>
                <select v-model="totalQuestions" class="dropdown">
                  <option value="10">10</option>
                  <option value="20">20</option>
                  <option value="30">30</option>
                  <option value="50">50</option>
                  <option value="100">100</option>
                </select>
              </div>
              <div class="form-row">
                <label>Guess / Cooldown Time</label>
                <div class="inner-rightalign">
                  <select v-model="guessingTime" class="dropdown">
                    <option value="10">10s</option>
                    <option value="15">15s</option>
                    <option value="20">20s</option>
                    <option value="30">30s</option>
                  </select>

                  <select v-model="cooldownTime" class="dropdown">
                    <option value="3">3s</option>
                    <option value="5">5s</option>
                    <option value="7">7s</option>
                    <option value="10">10s</option>
                  </select>
                </div>

              </div>
              <div class="form-row">
                <label for="autoskip" class="tooltip-container">Autoskip Guess Phase
                  <span class="tooltip">Automatically skip the guess phase if everyone submits their answer</span>
                </label>
                <input type="checkbox" id="autoskip" v-model="autoskip">
              </div>
              <div class="form-row">
                <label class="tooltip-container">Guess Type
                  <span class="tooltip">Select the type of guess for the game. Title is the default option.</span>
                </label>
                <select v-model="guessMode" class="dropdown">
                  <option value="TITLE">Title</option>
                  <option value="ARTIST">Artist</option>
                  <option value="CREATOR">Mapper</option>
                </select>
              </div>
              <div class="form-row">
                <label>Pool Mode</label>
                <select v-model="poolMode" class="dropdown">
                  <option value="DEFAULT">Default</option>
                  <option value="TOUHOU">Touhou Only</option>
                  <option value="VOCALOID">Vocaloid Only</option>
                </select>
              </div>
            </div>
            <div v-if="selectedTab === 'Genres'">
              <div class="form-row">
                <div class="checkbox-grid">
                  <div v-for="(value, key) in genreType" :key="'genre-' + key">
                    <input type="checkbox" :id="'genre-' + key" :value="value" v-model="selectedGenres" class="custom-checkbox" @change="handleGenreSelection(value)">
                    <label :for="'genre-' + key" class="custom-checkbox-label">{{ formatLabel(key) }}</label>
                  </div>
                </div>
              </div>
            </div>
            <div v-if="selectedTab === 'Languages'">
              <div class="form-row">
                <div class="checkbox-grid">
                  <div v-for="(value, key) in languageType" :key="'language-' + key">
                    <input type="checkbox" :id="'language-' + key" :value="value" v-model="selectedLanguages" class="custom-checkbox" @change="handleLanguageSelection(value)">
                    <label :for="'language-' + key" class="custom-checkbox-label">{{ formatLabel(key) }}</label>
                  </div>
                </div>
              </div>
            </div>
        </div>
        <div class="save-button-div">
          <button @click="revertSettingsToDefault()" class="save-button button-revert">Use Default Settings</button>
          <button @click="actionType === 'create' ? createNewGame() : updateRoomSettings()" class="save-button">
            {{ actionType === 'create' ? 'Create' : 'Update' }}
          </button>
        </div>

      </div>
    </div>
  </div>
</template>

<style scoped>
.loading-indicator {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  display: flex;
  justify-content: center;
  align-items: center;
  background-color: rgba(0, 0, 0, 0.5);
  color: white;
  font-size: 3em;
  z-index: 1000;
}

.modal-background {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: linear-gradient(135deg, rgba(0, 0, 0, 0.9), rgba(0, 0, 0, 0.7));
  backdrop-filter: blur(10px);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 99;
}

.modal {
  position: relative;
  background: linear-gradient(135deg, var(--color-secondary) 0%, rgba(255, 255, 255, 0.05) 100%);
  padding: 30px;
  border-radius: 20px;
  width: 50em;
  height: 30em;
  box-shadow: 0 20px 50px rgba(0, 0, 0, 0.3);
  border: 1px solid rgba(255, 255, 255, 0.1);
  z-index: 100;
  backdrop-filter: blur(20px);
}

.modal-content {
  display: flex;
}

.tabs {
  display: flex;
  flex-direction: column;
  margin-right: 20px;
}

.tabs button {
  padding: 12px 16px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.05), rgba(255, 255, 255, 0.02));
  color: var(--color-text);
  cursor: pointer;
  text-align: left;
  font-size: 1em;
  font-family: Sen, Arial, sans-serif;
  font-weight: 500;
  transition: all 0.3s ease;
  margin-bottom: 8px;
  border-radius: 10px;
}

.tabs button:hover {
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.1), rgba(255, 255, 255, 0.05));
  border-color: rgba(255, 255, 255, 0.2);
  transform: translateY(-1px);
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.1);
}

.tabs button.active {
  background: linear-gradient(135deg, rgba(96, 165, 250, 0.6), rgba(59, 130, 246, 0.6));
  color: white;
  border-color: rgba(96, 165, 250, 0.4);
  box-shadow: 0 4px 15px rgba(96, 165, 250, 0.3);
  font-weight: 600;
}

.tab-content {
  flex-grow: 1;
}

.form-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.input-yearrange {
  width: 20%;
}

.dropdown {
  font-family: Sen, Arial, sans-serif;
  padding: 10px 12px;
  border-radius: 10px;
  border: 1px solid rgba(255, 255, 255, 0.2);
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.1), rgba(255, 255, 255, 0.05));
  font-size: 16px;
  color: var(--color-text);
  transition: all 0.3s ease;
  outline: none;
  cursor: pointer;
}

.dropdown:focus {
  border-color: rgba(96, 165, 250, 0.5);
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.15), rgba(255, 255, 255, 0.08));
  box-shadow: 0 4px 15px rgba(96, 165, 250, 0.2);
  transform: translateY(-1px);
}

.dropdown:disabled {
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.05), rgba(255, 255, 255, 0.02));
  opacity: 0.5;
  cursor: not-allowed;
}

.checkbox-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
}

.checkbox-grid>div {
  display: flex;
  justify-content: flex-start;
}

.input-gameinfo {
  padding: 12px 16px;
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 10px;
  font-size: 16px;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.1), rgba(255, 255, 255, 0.05));
  color: var(--color-text);
  box-sizing: border-box;
  transition: all 0.3s ease;
  outline: none;
  font-family: Sen, Arial, sans-serif;
}

.input-gameinfo:focus {
  border-color: rgba(96, 165, 250, 0.5);
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.15), rgba(255, 255, 255, 0.08));
  box-shadow: 0 4px 15px rgba(96, 165, 250, 0.2);
  transform: translateY(-1px);
}

.input-gameinfo:disabled {
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.05), rgba(255, 255, 255, 0.02));
  opacity: 0.5;
  cursor: not-allowed;
}

.close-button {
  color: var(--color-text);
  position: absolute;
  top: 15px;
  right: 15px;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.1), rgba(255, 255, 255, 0.05));
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 8px;
  padding: 8px;
  font-size: 1.2em;
  cursor: pointer;
  transition: all 0.3s ease;
}

.close-button:hover {
  background: linear-gradient(135deg, rgba(239, 68, 68, 0.2), rgba(220, 38, 38, 0.1));
  border-color: rgba(239, 68, 68, 0.4);
  color: #ef4444;
  transform: scale(1.1);
}

.save-button-div {
  position: absolute;
  bottom: 10px;
  right: 10px;
  display: flex;
  gap: 10px;
  align-items: center;
}

.save-button {
  height: 2.5em;
  padding: 8px 24px;
  border: 1px solid rgba(255, 255, 255, 0.2);
  background: linear-gradient(135deg, rgba(96, 165, 250, 0.8), rgba(59, 130, 246, 0.8));
  color: white;
  text-align: center;
  text-decoration: none;
  display: inline-block;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  font-family: Sen, Arial, sans-serif;
  border-radius: 10px;
  transition: all 0.3s ease;
  box-shadow: 0 4px 15px rgba(96, 165, 250, 0.3);
}

.save-button:hover {
  background: linear-gradient(135deg, rgba(59, 130, 246, 0.9), rgba(37, 99, 235, 0.9));
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(96, 165, 250, 0.4);
  border-color: rgba(96, 165, 250, 0.4);
}

.inner-rightalign {
  display: flex;
  justify-content: flex-end;
  gap: 20px;
  align-items: center;
}

input[type="checkbox"] {
  appearance: none;
  width: 20px;
  height: 20px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-radius: 6px;
  outline: none;
  cursor: pointer;
  transition: all 0.3s ease;
  position: relative;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.1), rgba(255, 255, 255, 0.05));
}

input[type="checkbox"]:hover {
  border-color: rgba(96, 165, 250, 0.5);
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.15), rgba(255, 255, 255, 0.08));
  transform: scale(1.05);
}

input[type="checkbox"]:checked {
  background: linear-gradient(135deg, rgba(96, 165, 250, 0.6), rgba(59, 130, 246, 0.6));
  border-color: #60a5fa;
  box-shadow: 0 2px 8px rgba(96, 165, 250, 0.3);
}

input[type="checkbox"]:checked::after {
  content: '✔';
  color: white;
  font-size: 14px;
  font-weight: bold;
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.3);
}

input[type="checkbox"] + label {
  margin-left: 4px;
  cursor: pointer;
}

.checkbox-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 10px;
}

.checkbox-grid > div {
  display: flex;
  align-items: center;
}

.custom-checkbox {
  display: none;
}

.custom-checkbox-label {
  display: block;
  width: 100%;
  padding: 12px 16px;
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.3s ease;
  box-sizing: border-box;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.05), rgba(255, 255, 255, 0.02));
  font-weight: 500;
  text-align: center;
}

.custom-checkbox-label:hover {
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.1), rgba(255, 255, 255, 0.05));
  border-color: rgba(255, 255, 255, 0.3);
  transform: translateY(-1px);
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.1);
}

.label-displaymode {
  display: block;
  width: 100%;
  padding: 12px 16px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.3s ease;
  box-sizing: border-box;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.05), rgba(255, 255, 255, 0.02));
  font-weight: 500;
  text-align: center;
}

.custom-checkbox:checked + .custom-checkbox-label {
  background: linear-gradient(135deg, rgba(96, 165, 250, 0.6), rgba(59, 130, 246, 0.6));
  color: white;
  border-color: rgba(96, 165, 250, 0.4);
  box-shadow: 0 4px 15px rgba(96, 165, 250, 0.3);
  transform: translateY(-1px);
}

.button-revert {
  background: linear-gradient(135deg, rgba(107, 114, 128, 0.8), rgba(75, 85, 99, 0.8));
  border-color: rgba(107, 114, 128, 0.4);
}

.button-revert:hover {
  background: linear-gradient(135deg, rgba(75, 85, 99, 0.9), rgba(55, 65, 81, 0.9));
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(107, 114, 128, 0.4);
  border-color: rgba(107, 114, 128, 0.6);
}

.checkbox-div {
  display: flex;
  align-items: center;
}

.difficulty-checkbox {
  display: none;
}

.difficulty-label {
  display: inline-block;
  width: 24px;
  height: 24px;
  line-height: 24px;
  text-align: center;
  border: 2px solid rgba(255, 255, 255, 0.2);
  font-weight: bold;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.3s ease;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.05), rgba(255, 255, 255, 0.02));
  font-size: 0.9em;
}

.difficulty-label:hover {
  border-color: rgba(255, 255, 255, 0.4);
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.1), rgba(255, 255, 255, 0.05));
  transform: scale(1.1);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.difficulty-checkbox:checked + .difficulty-label {
  border: 2px solid var(--color-primary);
  background: linear-gradient(135deg, rgba(96, 165, 250, 0.5), rgba(59, 130, 246, 0.5));
  box-shadow: 0 2px 8px rgba(96, 165, 250, 0.3);
}

.difficulty-checkbox:not(:checked) + .difficulty-label {
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.05), rgba(255, 255, 255, 0.02));
  color: rgba(255, 255, 255, 0.5);
}

.difficulty-checkbox:disabled + .difficulty-label {
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.02), rgba(255, 255, 255, 0.01));
  color: rgba(255, 255, 255, 0.3);
  cursor: not-allowed;
}

.tooltip-container {
  position: relative;
  display: inline-block;
}

.tooltip {
  visibility: hidden;
  width: 15em;
  background: linear-gradient(135deg, rgba(0, 0, 0, 0.9), rgba(0, 0, 0, 0.8));
  color: #fff;
  text-align: center;
  border-radius: 10px;
  padding: 12px 16px;
  position: absolute;
  z-index: 1;
  bottom: 125%;
  left: 50%;
  margin-left: -100px;
  opacity: 0;
  transition: all 0.3s ease;
  border: 1px solid rgba(255, 255, 255, 0.1);
  box-shadow: 0 8px 25px rgba(0, 0, 0, 0.3);
  backdrop-filter: blur(10px);
}

.tooltip-container:hover .tooltip {
  visibility: visible;
  opacity: 1;
}

input[type="checkbox"]:disabled {
  color: var(--color-disabled);
  background-color: var(--color-disabled);
}

.checkbox-displaymode {
  display: none;
}

.label-displaymode {
  color: rgba(255, 255, 255, 0.6);
  display: flex;
  flex-direction: column;
  align-items: center;
  cursor: pointer;
  transition: all 0.3s ease;
  gap: 4px;
}

.label-displaymode:hover {
  color: var(--color-text);
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.1), rgba(255, 255, 255, 0.05));
  border-color: rgba(255, 255, 255, 0.3);
  transform: translateY(-1px);
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.1);
}

input[type="checkbox"]:checked + .label-displaymode {
  color: white;
  background: linear-gradient(135deg, rgba(96, 165, 250, 0.6), rgba(59, 130, 246, 0.6));
  border-color: rgba(96, 165, 250, 0.4);
  box-shadow: 0 4px 15px rgba(96, 165, 250, 0.3);
  transform: translateY(-1px);
}

input[type="checkbox"]:disabled + .label-displaymode {
  color: rgba(255, 255, 255, 0.3);
  cursor: not-allowed;
}

.div-displayModeInnerLeft {
  gap: 12px;
  display: flex;
}

.displaymode-highlight {
  border: 2px solid #60a5fa !important;
  background: linear-gradient(135deg, rgba(96, 165, 250, 0.1), rgba(59, 130, 246, 0.05)) !important;
}
</style>