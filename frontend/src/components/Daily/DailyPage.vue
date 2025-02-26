<script>
import apiService from "../../api/apiService";
import WebSocketServiceDaily from "@/api/WebSocketServiceDaily";
import {useUserStore} from "@/stores/userStore";
import {getUserData} from "@/service/authService";
import moment from "moment/moment";
import {FontAwesomeIcon} from "@fortawesome/vue-fontawesome";
import DailyArchive from "@/components/Daily/DailyArchive.vue";

export default {
  name: 'IntroPage',
  components: {DailyArchive, FontAwesomeIcon},

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
      volume: localStorage.getItem("volume") / 100,
      isPlaying: false,
      currentTime: 0,
      duration: 0,
      playbackTimeout: null,
      tooltipVisible: false,
      isLoading: true,
      isWebsocketConnected: false,
      latestGuess: null,
      showArchive: false,
      startDate: new Date(Date.UTC(2024, 10, 18)),
    }
  },

  async mounted() {
    apiService.get(`${process.env.VUE_APP_API_URL}/api/possibleAnswers?gamemode=STD&guessmode=TITLE`, {})
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
          this.retryCount = response.data.retryCount;
          this.correctGuess = response.data.guessed;

          this.isLoading = false;

          if (response.data.dailyGuessLog) {
            this.dailyGuessLog = response.data.dailyGuessLog;
            this.currentBeatmap = this.dailyGuessLog.dailyGuess.beatmap;
            this.revealStatus = true;
          }
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
    ).then(() => {
      this.isWebsocketConnected = true;
    });

    const audioPlayer = this.$refs.audioPlayer;
    if (audioPlayer) {
      audioPlayer.addEventListener('timeupdate', this.updateProgress);
    }

    setTimeout(() => {
      const audioPlayer = this.$refs.audio;
      if (audioPlayer) {
        const volume = localStorage.getItem("volume");
        if (volume) {
          this.volume = volume;
          audioPlayer.volume = volume / 100;
        } else {
          audioPlayer.volume = 0.2;
        }
      }
    }, 1000);
  },

  beforeUnmount() {
    if (this.webSocketService) {
      this.webSocketService.disconnect();
    }

    clearInterval(this.updateTimeLeft);

    const audioPlayer = this.$refs.audioPlayer;
    if (audioPlayer) {
      audioPlayer.pause();
      audioPlayer.removeEventListener('timeupdate', this.updateProgress);
    }

    this.webSocketService = null;
  },

  methods: {
    getTodayDailyId() {
      const today = new Date(Date.UTC(new Date().getUTCFullYear(), new Date().getUTCMonth(), new Date().getUTCDate()));
      return this.calculateDailyId(today);
    },

    calculateDailyId(date) {
      if (!date) return '';
      const timeDiff = date - this.startDate;
      const dailyId = Math.floor(timeDiff / (1000 * 60 * 60 * 24)) + 1;
      return dailyId > 0 ? dailyId : '';
    },

    closeIntroPage() {
      if (this.webSocketService) {
        this.webSocketService.disconnect();
        this.webSocketService = null;
      }
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

    formatDate(date) {
      return moment(date).format('MMMM Do YYYY');
    },

    guessClass(guessed) {
      if (guessed) {
        return 'correct-guess';
      } else {
        return 'incorrect-guess';
      }
    },

    selectAutocompleteOption(index) {
      if (index === -1) {
        this.answerInput = this.autocompleteOptions[0];
      } else {
        this.answerInput = this.autocompleteOptions[index];
      }

      this.submitAnswer()
    },

    submitAnswer() {
      if (!this.answerInput) {
        return;
      }

      this.latestGuess = this.answerInput;

      this.webSocketService.sendMessage(this.answerInput, this.dailyNumber);
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
    },

    playAudio() {
      if (this.isLoading) {
        return;
      }

      const audioPlayer = this.$refs.audioPlayer;
      if (audioPlayer) {
        this.controlAudioPlayback(audioPlayer);
        audioPlayer.play();
        this.duration = audioPlayer.duration;
      }
    },

    controlAudioPlayback(audioPlayer) {
      var playbackDuration = 0;
      switch (Number(this.retryCount)) {
        case 0:
          playbackDuration = 0.3;
          break;
        case 1:
          playbackDuration = 1;
          break;
        case 2:
          playbackDuration = 3;
          break;
        case 3:
          playbackDuration = 5;
          break;
        case 4:
          playbackDuration = 10;
          break;
        default:
          playbackDuration = 0.5;
          break;
      }

      audioPlayer.currentTime = 0;
      if (!this.correctGuess) {
        this.playbackTimeout = setTimeout(() => {
          audioPlayer.pause();
        }, playbackDuration * 1000);
      }
    },

    updateProgress() {
      const audioPlayer = this.$refs.audioPlayer;
      if (audioPlayer) {
        this.currentTime = audioPlayer.currentTime;
        this.duration = audioPlayer.duration;
      }
    },

    stopAudio() {
      const audioPlayer = this.$refs.audioPlayer;
      if (audioPlayer) {
        audioPlayer.pause();
        audioPlayer.currentTime = 0;
      }
      if (this.playbackTimeout) {
        clearTimeout(this.playbackTimeout);
        this.playbackTimeout = null;
      }
    },

    onPlay() {
      this.isPlaying = true;
    },

    onPause() {
      this.isPlaying = false;
    },

    handleKeyPress(event) {
      if (event.key === 'Enter') {
        this.selectAutocompleteOption(this.selectedOptionIndex);
      }
    },

    openArchivePage() {
      this.showArchive = true;
    },

    handleOpenDaily(day) {
      this.showArchive = false;
      this.dailyNumber = day;
      this.imageSourceBase64 = "";
      this.audioSourceBase64 = "";
      this.answerInput = "";
      this.autocompleteOptions = [];
      this.selectedOptionIndex = -1;
      this.inputFocused = false;
      this.revealStatus = false;
      this.currentBeatmap = {};
      this.log = [];
      this.retryCount = 0;
      this.placeholderText = "Guess the title of the beatmap!";
      this.dailyGuessLog = [];
      this.showCopiedMessage = false;
      this.correctGuess = null;
      this.timeLeft = '';
      this.volume = localStorage.getItem("volume") / 100;
      this.isPlaying = false;
      this.currentTime = 0;
      this.duration = 0;
      this.playbackTimeout = null;
      this.tooltipVisible = false;
      this.isLoading = true;
      this.latestGuess = null;
      this.showArchive = false;

      apiService.get(`${process.env.VUE_APP_API_URL}/api/daily?dailyId=${day}`, {})
          .then(response => {
            this.imageSourceBase64 = response.data.base64;
            this.audioSourceBase64 = response.data.base64;
            this.dailyNumber = response.data.dailyNumber;
            this.retryCount = response.data.retryCount;
            this.correctGuess = response.data.guessed;

            this.isLoading = false;

            if (response.data.dailyGuessLog) {
              this.dailyGuessLog = response.data.dailyGuessLog;
              this.currentBeatmap = this.dailyGuessLog.dailyGuess.beatmap;
              this.revealStatus = true;
            }
          });
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

    volume(newVal) {
      const audioPlayer = this.$refs.audioPlayer;
      if (audioPlayer) {
        audioPlayer.volume = newVal;
      }
    }
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
  <div class="modal-overlay" @click.prevent="closeIntroPage">
    <DailyArchive v-if="showArchive" @open-daily="handleOpenDaily"/>
    <div v-if="revealStatus && !showArchive" class="beatmap-info-container">
      <a :href="getBeatmapUrl" target="_blank">
        <h2>Beatmap Information</h2>
        <div class="beatmap-info-inner">
          <p>Artist: <strong>{{ currentBeatmap.artist }}</strong></p>
          <p>Title: <strong>{{ currentBeatmap.title }}</strong></p>
          <p>Mapper: <strong>{{ currentBeatmap.creator }}</strong></p>
          <p>Ranked on <strong>{{ formatDate(currentBeatmap.approved_date) }}</strong></p>
          <br>
          <div class="beatmap-info-clickme">
            Click to view the beatmap on osu!
          </div>
        </div>
      </a>
    </div>
    <div v-if="!showArchive" class="modal-content" @click.stop="">
      <div class="osudle-header">
        <div class="header-content">
          <h1 class="text-osudle">Daily osudle <strong>#{{ dailyNumber }}</strong></h1>
          <div class="button-archive" @click.prevent="openArchivePage">
            <font-awesome-icon :icon="['fas', 'clock-rotate-left']"/>
          </div>
        </div>
        <div v-if="getTodayDailyId() !== dailyNumber" class="osudle-header-archivetext">
          (You are viewing an archived osudle)
        </div>
      </div>

      <div class="question-image-container">
        <div class="question-image-cover" v-if="(retryCount < 3 && !revealStatus)">?</div>
        <div class="image-wrapper">
          <img v-if="retryCount >= 3 || revealStatus" :src="'/image/' + this.imageSourceBase64" alt="" class="question-image">
        </div>
      </div>
      <audio ref="audioPlayer" :src="'/audio/' + this.audioSourceBase64"
      @play="onPlay" @pause="onPause"></audio>
      <div class="progress-container">
        <progress :value="currentTime" :max="duration"></progress>
      </div>
      <div class="audio-player-controls">
        <button v-if="!isPlaying" @click="playAudio" class="audio-player-controls-button" :class="{ 'button-play-loading': isLoading || !isWebsocketConnected }">
          <font-awesome-icon :icon="['fas', 'play']"/>
        </button>
        <button v-else @click="stopAudio" class="audio-player-controls-button">
          <font-awesome-icon :icon="['fas', 'stop']"/>
        </button>
        <div class="div-volume">
          <font-awesome-icon :icon="['fas', 'volume-high']"/>
          <input class="input-volume" type="range" v-model="volume" min="0" max="1" step="0.01">
        </div>
        <div class="tooltip-container">
          <font-awesome-icon class="icon-osudle-help" :icon="['fas', 'circle-question']" @mouseover="tooltipVisible = true" @mouseleave="tooltipVisible = false"/>
          <div v-if="tooltipVisible" class="tooltip">
            1st Try: 0.3s<br>
            2nd Try: 1s<br>
            3rd Try: 3s<br>
            4th Try: 5s + BG<br>
            5th Try: 10s
          </div>
        </div>
      </div>
      <div v-if="retryCount !== 0">
        <div class="div-yourguess">Your Guess: <span :class="guessClass(correctGuess)">{{ latestGuess }}</span> ({{ retryCount }}/5)</div>
      </div>
      <div v-if="!loginStatus">
        <p>You need to login with osu! to play daily osudle</p>
      </div>
      <button class="login-button" @click="login" v-if="!loginStatus">
        Click here to login with osu!
        <font-awesome-icon icon="sign-in-alt"/>
      </button>
      <div class="div-input">
        <input type="text" v-model="answerInput"
               :placeholder="placeholderText" id="input-answer"
               :readonly="revealStatus"
               v-if="loginStatus"
               v-on:keydown.up.prevent="selectedOptionIndex > 0 ? selectedOptionIndex-- : selectedOptionIndex = autocompleteOptions.length - 1"
               v-on:keydown.down.prevent="selectedOptionIndex < autocompleteOptions.length - 1 ? selectedOptionIndex++ : selectedOptionIndex = 0"
               @keydown="handleKeyPress" v-on:focus="inputFocused = true">
        <div v-if="autocompleteOptions.length && inputFocused" class="autocomplete-dropdown">
          <div v-for="(option, index) in autocompleteOptions" :key="option" @click="selectAutocompleteOption(index)" :class="{ 'selected': index === selectedOptionIndex }" class="autocomplete-option">
            {{ option }}
          </div>
        </div>
      </div>

      <div class="share-div">
        <div v-if="revealStatus" class="text-nextdle">Next osudle in: <span class="text-nextdletime">{{ timeLeft }}</span></div>
        <div v-if="showCopiedMessage" class="copied-message">Copied to clipboard</div>
        <button class="share-button" v-if="revealStatus" @click="generateShareLink">Share  <font-awesome-icon :icon="['fas', 'share-nodes']" /></button>
      </div>
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

.modal-content {
  position: relative;
  border-radius: 5px;
  padding: 20px;
  display: flex;
  flex-direction: column;
  align-items: center;
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

#input-answer {
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
  font-family: 'Exo 2', 'Sen', serif;
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
  font-family: 'Exo 2', 'Sen', serif;
  font-size: 2em;
  font-weight: normal;
  color: var(--color-text);
  text-align: center;
  flex-grow: 1;
  margin: 0 0;
}

.osudle-header-archivetext {
  font-size: 1em;
  color: var(--color-subtext);
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
  font-family: "Sen", "Noto Sans Korean", "serif";
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
  margin-top: 1em;
}

.text-nextdle {
  font-size: 1.2em;
  color: var(--color-text);
}

.text-nextdletime {
  font-weight: bold;
}

.osudle-header {
  justify-content: center;
  align-items: center;
  position: relative;
  margin-bottom: 1em;
}

.header-content {
  display: flex;
  justify-content: center;
  align-items: center;
  width: 100%;
}

.close-button {
  position: absolute;
  right: 0;
}

.audio-player-controls {
  display: flex;
  justify-content: center;
  align-items: center;
  margin-top: 10px;
  gap: 25px;
}

.button-play-loading {
  color: #3a3737 !important;
  border: 2px solid #3a3737 !important;
}

.audio-player-controls-button {
  width: 50px;
  height: 50px;
  font-size: 1.5em;
  border: 2px solid var(--color-text);
  background-color: var(--color-secondary);
  color: var(--color-text);
  margin-top: 0.5em;
  margin-bottom: 0.5em;
  cursor: pointer;
  border-radius: 50%;
}

.progress-container {
  width: 100%;
  display: flex;
  justify-content: center;
  align-items: center;
  margin-top: 10px;
  border-radius: 5px;
}

progress {
  width: 100%;
  height: 10px;
  -webkit-appearance: none;
  appearance: none;
  border-radius: 5px;
  background-color: var(--color-disabled);
  border: 1px solid var(--color-body);
}

progress::-webkit-progress-bar {
  background-color: var(--color-disabled);
  border-radius: 5px;
}

progress::-webkit-progress-value {
  background-color: var(--color-secondary);
  border-radius: 5px;
}

.image-wrapper {
  position: relative;
  width: 71vh;
  height: 40vh;
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

.question-image-cover {
  position: absolute;
  background-color: #201e1e;
  width: 72vh;
  height: 41vh;
  z-index: 1;
  display: flex;
  justify-content: center;
  align-items: center;
  text-align: center;
  border: 3px solid var(--color-body);
  border-radius: 10px;
  font-size: 5em;
  font-weight: bold;
}

.question-image-container {
  display: flex;
  justify-content: center;
  align-items: center;
  position: relative;
}

#input-answer {
  font-size: 1.2em;
  text-align: center;
  width: 71vh;
  height: 3em;
  margin-top: 8px;
  font-family: 'Sen', serif;
  background-color: var(--color-secondary);
  color: var(--color-text);
}

.input-volume {
  width: 100px;
  height: 20px;
}

.div-volume {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 10px;
  background: var(--color-secondary);
  padding: 10px;
  border-radius: 20px;
  border: 2px solid var(--color-body);
}

.icon-osudle-help {
  font-size: 1.5em;
  color: var(--color-text);
  cursor: pointer;
}

.tooltip-container {
  position: relative;
  display: inline-block;
}

.tooltip {
  position: absolute;
  bottom: 100%;
  left: 50%;
  transform: translateX(-50%);
  background-color: #333;
  color: #fff;
  padding: 5px;
  border-radius: 3px;
  white-space: nowrap;
  z-index: 10;
  opacity: 0.9;
  text-align: left;
}

.div-yourguess {
  font-size: 1.2em;
  font-weight: normal;
}

.button-archive {
  background-color: var(--color-secondary);
  color: var(--color-text);
  border: 1px solid #ccc;
  border-radius: 50%;
  padding: 0.3em 0.5em;
  font-size: 1.0em;
  cursor: pointer;
  margin-left: 1em;
}
</style>