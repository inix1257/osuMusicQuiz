<script>
import axios from "axios";
import {getUserData} from "@/service/authService";
import apiService from "@/api/apiService";

export default {
  name: 'RoomSettingsModal',
  data() {
    return {
      localGame: {...this.game},
      isRequestPending: false,
      isExpanded: false,
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
    }
  },
  props: {
    game: {
      type: Object,
      required: true
    }
  },
  methods: {
    formatLabel(label) {
      // Replace underscores with spaces
      let formattedLabel = label.replace(/_/g, ' ');

      // Split the string into words
      let words = formattedLabel.split(' ');

      // Capitalize the first letter of each word and make the rest of the word lowercase
      words = words.map(word => word.charAt(0).toUpperCase() + word.slice(1).toLowerCase());

      // Join the words back together
      formattedLabel = words.join(' ');

      return formattedLabel;
    },

    toggleRoomSettingsModal() {
      this.$emit('toggleRoomSettingsModal');
    },

    async updateRoomSettings() {
      if (this.isRequestPending) {
        return;
      }

      this.isRequestPending = true;

      const updateRoomSettingsDTO = {
        userToken: localStorage.getItem("accessToken"),
        uuid: this.localGame.uuid,
        name: this.localGame.name,
        totalQuestions: this.localGame.totalQuestions,
        difficulty: this.localGame.difficulty,
        guessingTime: this.localGame.guessingTime,
        cooldownTime: this.localGame.cooldownTime,
        autoskip: this.localGame.autoskip,
        startYear: this.localGame.startYear,
        endYear: this.localGame.endYear,
        poolMode: this.localGame.poolMode,
        genreType: this.localGame.genreType,
        languageType: this.localGame.languageType
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
    }
  },

  watch: {
    'localGame.name': function(newVal) {
      const maxNameLength = 50;
      if (newVal && newVal.length > maxNameLength) {
        this.localGame.name = newVal.substring(0, maxNameLength);
      }
    },

    'localGame.genreType' : function(newVal, oldVal) {
      if (newVal.includes('ANY') && !oldVal.includes('ANY')) {
        // When "ANY" is checked, select all genres
        this.localGame.genreType = Object.keys(this.genreType);
      } else if (!newVal.includes('ANY') && oldVal.includes('ANY')) {
        // When "ANY" is unchecked, deselect all genres
        this.localGame.genreType = [];
      } else if (newVal.length < Object.keys(this.genreType).length) {
        // When any other genre is unchecked, uncheck "ANY"
        const index = this.localGame.genreType.indexOf('ANY');
        if (index !== -1) {
          this.localGame.genreType.splice(index, 1);
        }
      }
    },
    'localGame.languageType' : function(newVal, oldVal) {
      if (newVal.includes('ANY') && !oldVal.includes('ANY')) {
        // When "ANY" is checked, select all languages
        this.localGame.languageType = Object.keys(this.languageType);
      } else if (!newVal.includes('ANY') && oldVal.includes('ANY')) {
        // When "ANY" is unchecked, deselect all languages
        this.localGame.languageType = [];
      } else if (newVal.length < Object.keys(this.languageType).length) {
        // When any other language is unchecked, uncheck "ANY"
        const index = this.localGame.languageType.indexOf('ANY');
        if (index !== -1) {
          this.localGame.languageType.splice(index, 1);
        }
      }
    },
  },
}
</script>

<template>
  <div class="modal">
    <div class="modal-content">
      <span @click="toggleRoomSettingsModal" class="close-button">&times;</span>
      <p>Room Settings</p>
      <form v-if="this.localGame" @submit.prevent="updateRoomSettings">
        <div class="form-row">
          <label>Game Name</label>
          <input type="text" v-model="localGame.name" class="input-gameinfo">
        </div>
        <div class="form-row">
          <label>Password</label>
          <input disabled type="password" class="input-gameinfo">
        </div>
        <div class="form-row">
          <label>Total Questions</label>
          <select v-model="localGame.totalQuestions" class="dropdown">
            <option value="10">10</option>
            <option value="20">20</option>
            <option value="30">30</option>
            <option value="50">50</option>
            <option value="100">100</option>
          </select>
        </div>
        <div class="form-row">
          <label>Owner</label>
          <input type="text" v-model="localGame.owner.username" disabled class="input-gameinfo">
        </div>
        <div class="form-row">
          <label>Difficulty</label>
          <div>
            <input type="checkbox" id="easy" value="EASY" v-model="localGame.difficulty">
            <label for="easy">Easy</label>
          </div>
          <div>
            <input type="checkbox" id="normal" value="NORMAL" v-model="localGame.difficulty">
            <label for="normal">Normal</label>
          </div>
          <div>
            <input type="checkbox" id="hard" value="HARD" v-model="localGame.difficulty">
            <label for="hard">Hard</label>
          </div>
          <div>
            <input type="checkbox" id="insane" value="INSANE" v-model="localGame.difficulty">
            <label for="insane">Insane</label>
          </div>
        </div>
        <div class="form-row">
          <label>Mode</label>
          <select disabled v-model="localGame.mode" class="dropdown">
            <option value="DEFAULT">Default</option>
          </select>
        </div>
        <div class="form-row">
          <label>Guess Time</label>
          <select v-model="localGame.guessingTime" class="dropdown">
            <option value="10">10</option>
            <option value="15">15</option>
            <option value="20">20</option>
            <option value="30">30</option>
          </select>
        </div>
        <div class="form-row">
          <label>Cooldown Time</label>
          <select v-model="localGame.cooldownTime" class="dropdown">
            <option value="3">3</option>
            <option value="5">5</option>
            <option value="7">7</option>
            <option value="10">10</option>
          </select>
        </div>
        <div class="form-row">
          <label for="autoskip">Autoskip Guess Phase</label>
          <input type="checkbox" id="autoskip" v-model="localGame.autoskip">
        </div>
        <div class="form-row">
          <label>Start Year</label>
          <input type="number" v-model="localGame.startYear" class="input-gameinfo">
        </div>
        <div class="form-row">
          <label>End Year</label>
          <input type="number" v-model="localGame.endYear" class="input-gameinfo">
        </div>
        <div class="form-row">
          <label>Pool Mode</label>
          <select v-model="localGame.poolMode" class="dropdown">
            <option value="DEFAULT">Default</option>
            <option value="TOUHOU">Touhou</option>
            <option value="VOCALOID">Vocaloid</option>
          </select>
        </div>

        <div class="form-row">
          <label>Genre</label>
          <div class="checkbox-grid">
            <div v-for="(value, key) in genreType" :key="'genre-' + key">
              <input type="checkbox" :id="'genre-' + key" :value="key" v-model="localGame.genreType">
              <label :for="'genre-' + key">{{ formatLabel(key) }}</label>
            </div>
          </div>
        </div>

        <div class="form-row">
          <label>Language</label>
          <div class="checkbox-grid">
            <div v-for="(value, key) in languageType" :key="'language-' + key">
              <input type="checkbox" :id="'language-' + key" :value="key" v-model="localGame.languageType">
              <label :for="'language-' + key">{{ formatLabel(key) }}</label>
            </div>
          </div>
        </div>

        <div class="form-row">
          <button type="submit" class="save-button" :disabled="isRequestPending">{{ isRequestPending ? 'Saving...' : 'Save' }}</button>
        </div>
      </form>
    </div>
  </div>
</template>

<style scoped>
.modal {
  display: flex;
  justify-content: center;
  align-items: center;
  position: fixed;
  z-index: 2;
  left: 0;
  top: 0;
  width: 100%;
  height: 100%;
  overflow: auto;
  background-color: rgba(0,0,0,0.4);
}

.modal-content {
  background-color: var(--color-secondary);
  margin: auto;
  padding: 20px;
  border: 1px solid #888;
  width: 30vw;
}

.close-button {
  color: #aaaaaa;
  float: right;
  font-size: 28px;
  font-weight: bold;
}

.close-button:hover,
.close-button:focus {
  color: #000;
  text-decoration: none;
  cursor: pointer;
}

.form-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.button-row {
  display: flex;
  justify-content: center;
  gap: 20px;
  width: 100%;
}

.dropdown {
  font-family: Sen, Arial, sans-serif;
  padding: 6px;
  border-radius: 5px;
  border: 1px solid #ccc;
  background-color: #f8f8f8;
  font-size: 16px;
  color: #333;
}

.dropdown:disabled {
  background-color: #d3d3d3;
}

.input-gameinfo {
  padding: 10px;
  border: 1px solid #ccc;
  border-radius: 5px;
  font-size: 16px;
  color: #333;
  box-sizing: border-box;
  transition: border-color 0.3s ease;
}

.input-gameinfo:disabled {
  background-color: #d3d3d3;
}

.save-button {
  width: 100%; /* Full width */
  padding: 10px;
  border: none;
  background-color: #6fb7c7; /* Green background */
  color: white; /* White text */
  text-align: center;
  text-decoration: none;
  display: inline-block;
  font-size: 16px;
  margin: 4px 2px;
  cursor: pointer;
  font-family: Sen, Arial, sans-serif;
  border-radius: 5px;
  transition: background-color 0.3s ease;
}

.save-button:hover {
  background-color: #2984a4; /* Darker green */
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
</style>