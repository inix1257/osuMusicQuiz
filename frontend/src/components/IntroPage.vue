<script>
import apiService from "../api/apiService";
import WebSocketServiceDaily from "@/api/WebSocketServiceDaily";
import {useUserStore} from "@/stores/userStore";
import {getUserData} from "@/service/authService";
import moment from "moment/moment";
import {FontAwesomeIcon} from "@fortawesome/vue-fontawesome";

export default {
  name: 'IntroPage',
  components: {FontAwesomeIcon},

  setup() {
    const userStore = useUserStore();

    return {
      me: userStore.getMe(),
    };
  },

  data() {
    return {
      dailyNumber: 0,
      imageSourceBase64: "",
      audioSourceBase64: "",
      answerInput: "",
      autocompleteOptions: [],
      possibleAnswers: [],
      selectedOptionIndex: -1,
      inputFocused: false,
      webSocketService: null,
      revealStatus: false,
      currentBeatmap: {},
      loginStatus: false,
      log: [],
      retryCount: 0,
      placeholderText: "Guess the title of the beatmap!",
      dailyGuessLog: [],
      showCopiedMessage: false,
      correctGuess: null,
      timeLeft: '',
    }
  },

  async mounted() {
    apiService.get(`${process.env.VUE_APP_API_URL}/api/possibleAnswers`, {})
        .then((response) => {
          this.possibleAnswers = response.data;
        })
        .catch((error) => {
          console.error('Error getting beatmap', error);
        });

    apiService.get(`${process.env.VUE_APP_API_URL}/api/daily`, {})
        .then(response => {
          this.imageSourceBase64 = response.data.base64;
          this.audioSourceBase64 = response.data.base64;
          this.dailyNumber = response.data.dailyNumber;
        })
        .catch(error => {
          console.error("Error fetching daily osudle:", error);
        });

    this.me = await getUserData();

    if (!this.me) {
      return;
    }

    this.updateTimeLeft();
    setInterval(this.updateTimeLeft, 1000);

    this.loginStatus = true;

    this.webSocketService = new WebSocketServiceDaily(this.me.id);
    this.webSocketService.connect({
          onMessage: (message) => {
            this.log = [...this.log, this.answerInput];
            this.retryCount = message.body;
            this.autocompleteOptions = [];
          },
          onReveal: (message) => {
            this.dailyGuessLog = JSON.parse(message.body);
            this.currentBeatmap = this.dailyGuessLog.dailyGuess.beatmap;

            this.correctGuess = this.dailyGuessLog.guessed;

            const currentTimeUTC = new Date().toISOString();
            localStorage.setItem('dailyTime', currentTimeUTC);

            this.answerInput = this.currentBeatmap.artistAndTitle;
            this.revealStatus = true;

            const audioPlayer = this.$refs.audioPlayer;
            if (audioPlayer) {
              audioPlayer.pause();
              audioPlayer.currentTime = 0;
              audioPlayer.play()
            }
          },
        }
    );

    const audioPlayer = this.$refs.audioPlayer;
    if (audioPlayer) {
      audioPlayer.volume = 0.3;
    }
  },

  beforeUnmount() {
    if (this.webSocketService) {
      this.webSocketService.disconnect();
    }

    clearInterval(this.updateTimeLeft);

    const audioPlayer = this.$refs.audioPlayer;
    if (audioPlayer) {
      audioPlayer.pause();
    }

    this.webSocketService = null;
  },

  methods: {
    closeIntroPage() {
      this.$emit('close-intro');
    },

    updateTimeLeft() {
      const now = new Date();
      const utcNow = new Date(now.toISOString().slice(0, 19) + 'Z');
      const nextMidnight = new Date(Date.UTC(utcNow.getUTCFullYear(), utcNow.getUTCMonth(), utcNow.getUTCDate() + 1, 0, 0, 0));
      const timeDiff = nextMidnight - utcNow;

      const hours = Math.floor(timeDiff / (1000 * 60 * 60));
      const minutes = Math.floor((timeDiff % (1000 * 60 * 60)) / (1000 * 60));
      const seconds = Math.floor((timeDiff % (1000 * 60)) / 1000);

      this.timeLeft = `${hours}h ${minutes}m ${seconds}s`;
    },

    login() {
      // Redirect the user to the osu! OAuth page
      const URL = "https://osu.ppy.sh/oauth/authorize"
      const CLIENT_ID = `${process.env.VUE_APP_OSU_CLIENT_ID}`;
      const REDIRECT_URI = `${process.env.VUE_APP_OSU_REDIRECT_URI}`;
      const SCOPE = "identify";
      const RESPONSE_TYPE = "code";
      const STATE = "state";

      window.location.href = `${URL}?client_id=${CLIENT_ID}&redirect_uri=${REDIRECT_URI}&response_type=${RESPONSE_TYPE}&scope=${SCOPE}&state=${STATE}`;
    },

    getAnswerRate() {
      if (this.currentBeatmap.playcount === 0) {
        return 0; // Avoid division by zero
      }
      return ((this.currentBeatmap.playcount_answer / this.currentBeatmap.playcount) * 100).toFixed(2) + "%";
    },

    formatDate(date) {
      return moment(date).format('MMMM Do YYYY');
    },

    formatDecimal(value) {
      if (value === 0) {
        return '0 pts';
      }
      return value.toFixed(2) + ' pts';
    },

    difficultyClass(difficulty) {
      switch (difficulty) {
        case 'EASY':
          return 'difficulty-easy';
        case 'NORMAL':
          return 'difficulty-normal';
        case 'HARD':
          return 'difficulty-hard';
        case 'INSANE':
          return 'difficulty-insane';
        default:
          return '';
      }
    },

    guessClass(guessed) {
      if (guessed) {
        return 'correct-guess';
      } else {
        return 'incorrect-guess';
      }
    },

    selectAutocompleteOption(index) {
      if (index !== -1) {
        this.answerInput = this.autocompleteOptions[index];
        this.autocompleteOptions = [];
        this.selectedOptionIndex = -1;
      }

      this.submitAnswer()
    },

    submitAnswer() {
      if (!this.answerInput) {
        return;
      }

      this.webSocketService.sendMessage(this.answerInput);
      this.answerInput = "";
    },

    generateShareLink() {
      const retryCount = this.dailyGuessLog.retryCount;
      const guessed = this.dailyGuessLog.guessed;
      let str = `Daily osudle #${this.dailyGuessLog.dailyGuess.id}\n\n`;
      str += `🗺`

      for (let i = 0; i < retryCount - 1; i++) {
        str += `🟥`
      }

      if (guessed) {
        str += `🟩`
      } else {
        str += `🟥`
      }

      for (let i = 0; i < 5 - retryCount; i++) {
        str += `⬜️`
      }

      str += "\n\nhttps://osumusicquiz.com/";

      navigator.clipboard.writeText(str);

      this.showCopiedMessage = true;
      setTimeout(() => {
        this.showCopiedMessage = false;
      }, 3000);

      return str;
    }


  },

  watch: {
    answerInput(newVal) {
      this.inputFocused = true;

      if(!newVal) {
        this.autocompleteOptions = [];
        this.selectedOptionIndex = -1;
        return;
      }

      let exactMatch = this.possibleAnswers.find(answer => answer.toLowerCase() === newVal.toLowerCase());
      let filteredOptions = this.possibleAnswers.filter(
          answer => answer.toLowerCase().includes(newVal.toLowerCase())
      );
      filteredOptions.sort((a, b) => a.length - b.length);

      if (exactMatch) {
        filteredOptions = filteredOptions.filter(option => option !== exactMatch);
        filteredOptions.unshift(exactMatch);
      }

      this.autocompleteOptions = filteredOptions.slice(0, 5);
    },
  },

  computed: {
    getBeatmapUrl() {
      return `https://osu.ppy.sh/beatmapsets/${this.currentBeatmap.beatmapset_id}`;
    },

    getGuessStatus() {
      if (this.correctGuess) {
        return "Correct Answer";
      } else {
        return "Incorrect Answer";
      }
    }
  },
}
</script>

<template>
  <div class="modal-overlay">
    <div>
      <div class="osudle-header">
        <div class="header-content">
          <h1 class="text-osudle">Daily osudle {{ dailyNumber }}</h1>
          <button class="close-button" @click="closeIntroPage">
            <font-awesome-icon :icon="['fas', 'x']" />
          </button>
        </div>
      </div>

      <img :src="'/image/' + this.imageSourceBase64" alt="" class="question-image">
      <br>
      <audio ref="audioPlayer" :src="'/audio/' + this.audioSourceBase64" controls></audio>
      <br>
      <div v-if="retryCount !== 0"><span :class="guessClass(correctGuess)">{{ getGuessStatus }}</span> ({{ retryCount }}/5)</div>
      <div v-if="!loginStatus">
        <p>You need to login with osu! to play daily osudle</p>
      </div>
      <button class="login-button" @click="login" v-if="!loginStatus">
        Click here to login with osu!
        <font-awesome-icon icon="sign-in-alt"/>
      </button>
      <input type="text" v-model="answerInput"
             :placeholder="placeholderText" id="input-answer"
             :readonly="revealStatus"
             v-if="loginStatus"
             v-on:keydown.up.prevent="selectedOptionIndex > 0 ? selectedOptionIndex-- : selectedOptionIndex = autocompleteOptions.length - 1"
             v-on:keydown.down.prevent="selectedOptionIndex < autocompleteOptions.length - 1 ? selectedOptionIndex++ : selectedOptionIndex = 0"
             v-on:keydown.enter.prevent="selectAutocompleteOption(selectedOptionIndex)" v-on:focus="inputFocused = true">
      <div v-if="autocompleteOptions.length && inputFocused" class="autocomplete-dropdown">
        <div v-for="(option, index) in autocompleteOptions" :key="option" @click="selectAutocompleteOption(index)" :class="{ 'selected': index === selectedOptionIndex }" class="autocomplete-option">
          {{ option }}
        </div>
      </div>
      <div class="share-div">
        <div v-if="revealStatus" class="text-nextdle">Next osudle in: <span class="text-nextdletime">{{ timeLeft }}</span></div>
        <div v-if="showCopiedMessage" class="copied-message">Copied to clipboard</div>
        <button class="share-button" v-if="revealStatus" @click="generateShareLink">Share  <font-awesome-icon :icon="['fas', 'share-nodes']" /></button>
      </div>
      <div v-if="revealStatus" class="beatmap-info-container">
        <a :href="getBeatmapUrl" target="_blank">
          <h2>Beatmap Information</h2>
          <div class="beatmap-info-inner">
            <p>Artist: <strong>{{ currentBeatmap.artist }}</strong></p>
            <p>Title: <strong>{{ currentBeatmap.title }}</strong></p>
            <p>Mapper: <strong>{{ currentBeatmap.creator }}</strong></p>
            <p>Answer Rate: <strong>{{ getAnswerRate() }}</strong> <span class="beatmap-info-playcount">({{ currentBeatmap.playcount_answer }}/{{ currentBeatmap.playcount }})</span></p>
            <p><strong><span class="beatmap-info-difficulty" :class="difficultyClass(currentBeatmap.beatmapDifficulty)">[{{ currentBeatmap.beatmapDifficulty }}]</span></strong></p>
            <p>Ranked on <strong>{{ formatDate(currentBeatmap.approved_date) }}</strong></p>
            <br>
<!--            <p>Total Points: {{ answers[me.id].totalPoints.toFixed(2) }}</p>-->
            <div class="beatmap-info-clickme">
              Click to view the beatmap on osu!
            </div>
          </div>
        </a>
      </div>
      <h3 @click="closeIntroPage">> Play Multiplayer</h3>
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
  backdrop-filter: blur(10px);
}

.question-image {
  user-select: none;
  position: relative;
  width: 71vh;
  height: 40vh;
  object-fit: contain;
  border: 3px solid black;
  background-color: var(--color-disabled);
  display: flex;
  justify-content: center;
  align-items: center;
  text-align: center;
}

input {
  width: 71vh;
  height: 3em;
  box-sizing: border-box;
  font-size: 1.2em;
  text-align: center;
  margin-top: 8px;
  font-family: 'Sen', serif;
  background-color: var(--color-secondary);
  color: var(--color-text);
}

.autocomplete-dropdown {
  position: absolute;
  background-color: var(--color-secondary);
  border: 1px solid #ccc;
  width: 71vh;
  z-index: 1;
}

.autocomplete-dropdown .selected {
  background-color: var(--color-disabled);
}

.autocomplete-option {
  display:flex;
  justify-content: center;
  align-items: center;
  height: 2em;
}

.autocomplete-option:hover {
  border: 2px solid #ef3838;
  box-sizing: border-box;
  border-radius: 5px;
}

.beatmap-info-container {
  position: absolute;
  top: 10vh;
  right: 5vw;
  padding: 10px;
  margin-top: 10px;
  border: 1px solid #ccc;
  background-color: var(--color-secondary);
  max-width: 28vh;
  min-width: 28vh;
}

.beatmap-info-container a {
  color: inherit;
  text-decoration: none;
}

.beatmap-info-container a:hover {
  color: inherit;
}

.text-osudle {
  font-size: 2em;
  color: var(--color-text);
  text-align: center;
}

.close-button {
  position: relative;
  top: 0;
  right: 2em;
  color: var(--color-text);
  background: none;
  border: none;
  font-size: 1.5em;
  cursor: pointer;
}

.login-button {
  background-color: var(--color-secondary);
  color: var(--color-text);
  border: 1px solid #ccc;
  border-radius: 5px;
  padding: 10px;
  margin-top: 10px;
  font-size: 1.0em;
  cursor: pointer;
}

.share-button {
  font-family: "Sen", "Noto Sans Korean", "serif";
  font-weight: bold;
  background-color: green;
  color: var(--color-text);
  border: 1px solid #ccc;
  border-radius: 5px;
  padding: 10px;
  margin-top: 10px;
  font-size: 1.2em;
  cursor: pointer;
}

.copied-message {
  position: fixed;
  bottom: 20px;
  right: 20px;
  background-color: #4caf50;
  color: white;
  padding: 10px;
  border-radius: 5px;
  z-index: 1000;
}

.correct-guess {
  font-weight: bold;
  color: #20c020;
}

.incorrect-guess {
  font-weight: bold;
  color: red;
}

.share-div {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 2em;
}

.text-nextdle {
  font-size: 1.2em;
  color: var(--color-text);
}

.text-nextdletime {
  font-weight: bold;
}

.osudle-header {
  display: flex;
  justify-content: center;
  align-items: center;
  position: relative;
}

.header-content {
  display: flex;
  justify-content: center;
  align-items: center;
  width: 100%;
}

.text-osudle {
  flex-grow: 1;
  text-align: center;
}

.close-button {
  position: absolute;
  right: 0;
}

</style>