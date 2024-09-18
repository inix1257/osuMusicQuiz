<script>
import apiService from "@/api/apiService";
import {useUserStore} from "@/stores/userStore";

export default {
  name: "RoomCreateModal",
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
      mode: "DEFAULT",
      isLoading: false,
      startYear: 2007,
      endYear: new Date().getFullYear(),
      isExpanded: false,
      poolMode: 'DEFAULT',
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
      selectedGenres: Object.values({
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
      }),

      selectedLanguages: Object.values({
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
      }),
    }
  },
  created() {
    this.loadSettings();
  },

  methods: {
    closeModal() {
      this.$emit("close-modal");
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

      if (this.difficulty.length === 0) {
        // Set all difficulties if none are selected
        this.difficulty = ["EASY", "NORMAL", "HARD", "INSANE"];
      }

      // Check if the required fields are filled
      if (!this.gameName) {
        this.gameName = `${this.me.username}'s game`;
      }

      const createGameDTO = {
        name: this.gameName,
        password: this.password,
        totalQuestions: this.totalQuestions,
        difficulty: this.difficulty,
        guessingTime: this.guessingTime,
        cooldownTime: this.cooldownTime,
        autoskip: this.autoskip,
        mode: this.mode,
        startYear: this.startYear,
        endYear: this.endYear,
        poolMode: this.poolMode,
        displayMode: this.displayMode,
        genreType: this.selectedGenres,
        languageType: this.selectedLanguages
      };

      // Check if any field is empty
      if (!createGameDTO.name || !createGameDTO.totalQuestions || !createGameDTO.difficulty || !createGameDTO.mode) {
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

    saveSettings() {
      const settings = {
        totalQuestions: this.totalQuestions,
        guessingTime: this.guessingTime,
        cooldownTime: this.cooldownTime,
        autoskip: this.autoskip,
        difficulty: this.difficulty,
        startYear: this.startYear,
        endYear: this.endYear,
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
        this.totalQuestions = settings.totalQuestions;
        this.guessingTime = settings.guessingTime;
        this.cooldownTime = settings.cooldownTime;
        this.autoskip = settings.autoskip;
        this.difficulty = settings.difficulty;
        this.startYear = settings.startYear;
        this.endYear = settings.endYear;
        this.poolMode = settings.poolMode;
        this.displayMode = settings.displayMode;
        this.selectedGenres = settings.selectedGenres;
        this.selectedLanguages = settings.selectedLanguages;
      }
    }
  },

  watch: {
    gameName(newVal) {
      if (newVal.length > 50) {
        this.gameName = newVal.slice(0, 50);
      }
    },

    selectedGenres(newVal, oldVal) {
      if (newVal.includes(0) && !oldVal.includes(0)) {
        this.selectedGenres = Object.values(this.genreType);
      } else if (!newVal.includes(0) && oldVal.includes(0)) {
        this.selectedGenres = [];
      } else if (newVal.length < Object.keys(this.genreType).length) {
        const index = this.selectedGenres.indexOf(0);
        if (index !== -1) {
          this.selectedGenres.splice(index, 1);
        }
      }
    },
    selectedLanguages(newVal, oldVal) {
      if (newVal.includes(0) && !oldVal.includes(0)) {
        this.selectedLanguages = Object.values(this.languageType);
      } else if (!newVal.includes(0) && oldVal.includes(0)) {
        this.selectedLanguages = [];
      } else if (newVal.length < Object.keys(this.languageType).length) {
        const index = this.selectedLanguages.indexOf(0);
        if (index !== -1) {
          this.selectedLanguages.splice(index, 1);
        }
      }
    },
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
      <h2>Host New Game</h2>
      <form @submit.prevent="createNewGame">
        <div class="form-row">
          <label>Game Name</label>
          <input type="text" v-model="gameName" class="input-gameinfo" :placeholder="`${me.username}'s game`">
        </div>
        <div class="form-row">
          <label>Password</label>
          <input type="password" v-model="password" class="input-gameinfo">
        </div>
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
          <label>Difficulty</label>
          <div>
            <input type="checkbox" id="easy" value="EASY" v-model="difficulty">
            <label for="easy">Easy</label>
          </div>
          <div>
            <input type="checkbox" id="normal" value="NORMAL" v-model="difficulty">
            <label for="normal">Normal</label>
          </div>
          <div>
            <input type="checkbox" id="hard" value="HARD" v-model="difficulty">
            <label for="hard">Hard</label>
          </div>
          <div>
            <input type="checkbox" id="insane" value="INSANE" v-model="difficulty">
            <label for="insane">Insane</label>
          </div>
        </div>
        <div class="form-row" v-if="false">
          <label>Mode</label>
          <select disabled v-model="mode" class="dropdown">
            <option value="DEFAULT">Default</option>
            <!--              <option value="PATTERN">Pattern</option>-->
          </select>
        </div>
        <div class="form-row">
          <label>Guess Time</label>
          <select v-model="guessingTime" class="dropdown">
            <option value="10">10</option>
            <option value="15">15</option>
            <option value="20">20</option>
            <option value="30">30</option>
          </select>
        </div>
        <div class="form-row">
          <label>Cooldown Time</label>
          <select v-model="cooldownTime" class="dropdown">
            <option value="3">3</option>
            <option value="5">5</option>
            <option value="7">7</option>
            <option value="10">10</option>
          </select>
        </div>
        <div class="form-row">
          <label>Year Range</label>
          <input type="number" v-model="startYear" class="input-gameinfo input-yearrange">
          ~
          <input type="number" v-model="endYear" class="input-gameinfo input-yearrange">
        </div>
        <!--             EXPAND            -->
        <!--             EXPAND            -->
        <!--             EXPAND            -->
        <button type="button" class="expand-button" @click="isExpanded = !isExpanded">
          <font-awesome-icon :icon="['fas', 'caret-down']" v-if="!isExpanded"/>
          <font-awesome-icon :icon="['fas', 'caret-up']" v-else/>
        </button>

        <div class="form-row" v-if="isExpanded">
          <label for="autoskip">Autoskip Guess Phase</label>
          <input type="checkbox" id="autoskip" v-model="autoskip">
        </div>
        <div class="form-row" v-if="isExpanded">
          <label>Pool Mode</label>
          <select v-model="poolMode" class="dropdown">
            <option value="DEFAULT">Default</option>
            <option value="TOUHOU">Touhou</option>
            <option value="VOCALOID">Vocaloid</option>
          </select>
        </div>
        <div class="form-row" v-if="isExpanded">
          <label>Display Mode</label>
          <div>
            <input type="checkbox" id="background" value="BACKGROUND" v-model="displayMode">
            <label for="background">Background</label>
          </div>
          <div>
            <input type="checkbox" id="audio" value="AUDIO" v-model="displayMode">
            <label for="audio">Audio</label>
          </div>
        </div>
        <div class="form-row" v-if="isExpanded">
          <label>Genre</label>
          <div class="checkbox-grid">
            <div v-for="(value, key) in genreType" :key="'genre-' + key">
              <input type="checkbox" :id="'genre-' + key" :value="value" v-model="selectedGenres">
              <label :for="'genre-' + key">{{ formatLabel(key) }}</label>
            </div>
          </div>
        </div>

        <div class="form-row" v-if="isExpanded">
          <label>Language</label>
          <div class="checkbox-grid">
            <div v-for="(value, key) in languageType" :key="'language-' + key">
              <input type="checkbox" :id="'language-' + key" :value="value" v-model="selectedLanguages">
              <label :for="'language-' + key">{{ formatLabel(key) }}</label>
            </div>
          </div>
        </div>

        <div class="form-row">
          <button type="submit" class="save-button">Create</button>
        </div>
      </form>
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
}

.modal-background {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: rgba(0, 0, 0, 0.8);
  display: flex;
  justify-content: center;
  align-items: center;
}

.modal {
  position: relative;
  background-color: var(--color-secondary);
  padding: 20px;
  border-radius: 10px;
  width: 80%;
  max-width: 500px;
  box-shadow: 0px 10px 20px rgba(0, 0, 0, 0.2);
}

.form-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.input-yearrange {
  width: 30%;
}

.dropdown {
  font-family: Sen, Arial, sans-serif;
  padding: 6px;
  border-radius: 5px;
  border: 1px solid #ccc;
  background-color: var(--color-secondary);
  font-size: 16px;
  color: var(--color-text);
}

.dropdown:disabled {
  background-color: var(--color-disabled);
}

.expand-button {
  border: none;
  outline: none;
  background-color: var(--color-secondary);
  color: var(--color-text);
  padding: 10px 20px;
  font-size: 1em;
  font-family: Sen, Arial, sans-serif;
  cursor: pointer;
  border-radius: 10px;
  transition: background-color 0.3s ease;
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
  padding: 10px;
  border: 1px solid #ccc;
  border-radius: 5px;
  font-size: 16px;
  background-color: var(--color-secondary);
  color: var(--color-text);
  box-sizing: border-box;
  transition: border-color 0.3s ease;
}

.input-gameinfo:disabled {
  background-color: var(--color-disabled);
}

.close-button {
  position: absolute;
  top: 10px;
  right: 10px;
  background: none;
  border: none;
  font-size: 1.5em;
  cursor: pointer;
}

.save-button {
  width: 100%;
  padding: 10px;
  border: none;
  background-color: var(--color-primary);
  color: white;
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
  background-color: #2984a4;
}
</style>