<script>
import moment from "moment";
import {useUserStore} from "@/stores/userStore";
import {FontAwesomeIcon} from "@fortawesome/vue-fontawesome";
import RoomSettingsModal from "@/components/Modal/RoomSettingsModal.vue";
import WebSocketService from "@/api/WebSocketService";

import icon_report from "@/assets/siren.png";
import apiService from "../../api/apiService";
import UserPage from "@/components/UserPage.vue";
import ChatSection from "@/components/Game/ChatSection.vue";


export default {
  name: 'GamePage',
  components: {ChatSection, UserPage, FontAwesomeIcon, RoomSettingsModal},
  setup() {
    const userStore = useUserStore();

    return {
      me: userStore.getMe(),
    };
  },
  data() {
    return {
      game: null,
      gameId: null,
      password: "",
      userId: "",
      answerInput: "",
      messages: [],
      stompClient: null,
      isPlaying: false,
      isGuessing: false,
      isLoading: true,
      answerSubmitted: false,
      ownGuessRight: false,
      players: [],
      answers: [],
      imageSourceBase64: "",
      audioSourceBase64: "",
      timeLeft: 0,
      countdownInterval: null,
      currentBeatmap: null,
      currentAnswerInfo: null,
      loadErrorCount: 0,
      autocompleteOptions: [],
      possibleAnswers: [],
      selectedOptionIndex: -1,
      inputFocused: false,
      isScrolling: true,
      audio: null,
      volume: 20,
      toggleImage: true,
      toggleAudio: true,
      isRoomSettingsModalOpen: false,
      showSettingsWarning: false,
      showUserDropdown: false,
      showReportDropdown: false,
      showDropdown: false,
      showUserSettings: false,
      showPlayerInfoModal: false,
      dropdownUser: null,
      webSocketService: null,
      icon_report: icon_report,
      lastAnswerTime: null,
      highlightStartButton: false,
      isConnected: false,
      userpageId: null,
      kicked: false,
      compactViewMode: false,
      inactivityTimer: null,
      showInactivityWarning: false,
      answerInputPlaceHolder: "Guess the title of the beatmap!",
    };
  },
  methods: {
    startGame() {
      this.isLoading = true;
      apiService.post('/api/startGame', {
        gameId: this.gameId,
        userId: this.userId
      })
          .then((response) => {
          })
          .catch((error) => {
            alert("An error occurred while starting the game. Please try again later.")
          })
          .finally(() => {
            this.isLoading = false;
          });
    },

    leaveRoom() {
      this.$router.push('/');
    },

    requestLeaveRoom() {
      apiService.post('/api/leaveGame', {
        gameId: this.gameId,
        userId: this.me.id,
      })
          .then((response) => {
            if (response.data) {
              //
            } else {
              console.error('Error leaving game');
            }
          })
          .catch((error) => {
            console.error('Error leaving game', error);
          });
    },

    beforeUnload(event) {
      this.requestLeaveRoom();
    },

    sendChatMessage(content) {
      this.resetInactivityTimer()

      this.webSocketService.sendMessage({
        gameId: this.gameId,
        senderId: this.me.id,
        senderUsername: this.me.username,
        senderAvatarUrl: this.me.avatar_url,
        content: content,
        answer: false,
      });
    },

    submitAnswer() {
      const cooldown = 100; // Cooldown period in milliseconds

      this.resetInactivityTimer()

      // If the method was called within the cooldown period, return
      if (this.lastAnswerTime && Date.now() < this.lastAnswerTime + cooldown) {
        return;
      }

      // Ignore empty answers
      if (!this.answerInput || this.answerInput === "") {
        return;
      }

      // Ignore messages if the game is not in progress
      if (!this.isGuessing) {
        return;
      }

      let contentToSend = (this.autocompleteOptions[this.selectedOptionIndex] || this.answerInput)

      this.webSocketService.sendMessage({
        gameId: this.gameId,
        senderId: this.me.id,
        senderUsername: this.me.username,
        content: contentToSend,
        answer: true,
      });

      this.answerSubmitted = true;
      this.inputFocused = false;

      this.lastAnswerTime = Date.now();
    },

    formatTimestamp(timestamp) {
      // Create a Date object from the timestamp
      const date = new Date(timestamp);

      // Convert the Date object to a string using locale settings
      const localTimestamp = date.toLocaleString('en-US', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit',
        hour12: false
      });

      // Format the local timestamp using moment.js
      return moment(localTimestamp, 'MM/DD/YYYY, HH:mm:ss').format('h:mm:ss a');
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

    selectAutocompleteOption(index) {
      // Ignore if the game is not in progress
      if (!this.isGuessing) {
        return;
      }

      // Ignore empty answers
      // if (!this.autocompleteOptions[index] || this.autocompleteOptions[index] === "") {
      //   return;
      // }

      if (index !== -1) {
        this.answerInput = this.autocompleteOptions[index];
        this.autocompleteOptions = []; // Hides the dropdown
        this.selectedOptionIndex = -1; // Reset the selected option index
      }

      this.submitAnswer()
    },

    kickPlayer(player) {
      // Ask if this is a ban
      var playerId = player.id;
      var url = '/api/kickPlayer';

      this.showDropdown = false;

      if (!confirm("Do you want to kick " + player.username + "?")) {
        return;
      }

      if (confirm("Do you want to ban " + player.username + " from this lobby?")) {
        url = '/api/banPlayer';
      }

      apiService.post(url, {
        gameId: this.gameId,
        targetUserId: playerId
      })
          .then((response) => {
            // Handle response
          })
          .catch((error) => {
            // Handle error
          });
    },

    transferHost(player) {
      var playerId = player.id;

      this.showDropdown = false;

      if (!confirm("Do you want to transfer the host to " + player.username + "?")) {
        return;
      }

      apiService.post(`${process.env.VUE_APP_API_URL}/api/transferHost`, {
        gameId: this.gameId,
        targetUserId: playerId
      })
          .then((response) => {
            // Handle response
          })
          .catch((error) => {
            // Handle error
          });
    },

    openDropdown(player) {
      this.dropdownUser = player;
      this.showDropdown = !this.showDropdown;
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

    difficultyName(diff) {
      switch (diff) {
        case 'EASY':
          return 'E';
        case 'NORMAL':
          return 'N';
        case 'HARD':
          return 'H';
        case 'INSANE':
          return 'I';
        default:
          return '';
      }
    },

    getRate(guessed, played) {
      if (played === 0) {
        return 0; // Avoid division by zero
      }
      return ((guessed / played) * 100).toFixed(2) + "%";
    },

    toggleRoomSettingsModal() {
      if (this.isPlaying && !this.isRoomSettingsModalOpen) {
        this.showSettingsWarning = true;
        setTimeout(() => {
          this.showSettingsWarning = false;
        }, 3000); // hide the warning after 3 seconds
      } else {
        this.isRoomSettingsModalOpen = !this.isRoomSettingsModalOpen;
      }
    },

    toggleAudioVolume() {
      this.toggleAudio = !this.toggleAudio;

      const audioPlayer = this.$refs.audio;
      if (audioPlayer) {
        audioPlayer.volume = this.toggleAudio ? this.volume / 100 : 0;
      }
    },

    toggleReportDropdown() {
      this.showReportDropdown = !this.showReportDropdown;

      if (this.showReportDropdown) {
        setTimeout(() => {
          this.showReportDropdown = false;
        }, 3000); // Hide the dropdown menu after 3 seconds
      }
    },

    reportBeatmap(reportType) {
      this.showReportDropdown = false;
      apiService.post(`${process.env.VUE_APP_API_URL}/api/reportBeatmap`, {
        encryptedBeatmapsetId: this.audioSourceBase64,
        userId: this.me.id,
        beatmapReportType: reportType,
      })
    },

    async joinGame(password, isForceLeave) {
      return apiService.post(`${process.env.VUE_APP_API_URL}/api/joinGame`, {
        gameId: this.gameId,
        password: password ? password : this.password,
        forceLeave: isForceLeave
      })
          .then(async (response) => {
            if (response.data) {
              this.players = response.data.players;
              this.game = response.data;
              this.setInputPlaceholder();

            } else {
              alert("Either you already have joined this game, or the game does not exist.")
              this.$router.push('/');
            }
          })
          .catch((error) => {
            switch (error.response.status) {
              case 401:
                alert("Token is invalid. Please log in again.");
                break;
              case 403:
                alert("The game you are trying to join is either full or has already started.");
                break;
              case 404:
                alert("The game you are trying to join does not exist.");
                break;
              case 406:
                alert("You are banned from this game.");
                break;
              case 409:
                if (isForceLeave) break;
                if (confirm("You are already in another game. Do you want to leave that game and join this one?")) {
                  this.joinGame(password, true);
                  return;
                } else {
                  this.deleteGame()
                }
                break;
              case 423:
                alert("The password you entered is incorrect.");
                break;
              case 429:
                if (isForceLeave) break;
                if (confirm("You are already in another game. Do you want to leave that game and join this one?")) {
                  this.joinGame(password, true);
                  return;
                }
                break;
              default:
                alert("An error occurred while joining the game. Please try again later. ERR_CODE: " + error.response.status);
                break;
            }
            this.$router.push('/');
          })
          .finally(() => {
            this.isLoading = false;
            localStorage.removeItem("roompw");
          });
    },

    async deleteGame() {
      return apiService.post(`${process.env.VUE_APP_API_URL}/api/deleteGame`, {
        gameId: this.gameId
      })
          .then(() => {

          })
          .catch((error) => {
            console.error('Error deleting game', error);
          });
    },

    resetInactivityTimer() {
      clearTimeout(this.inactivityTimer);
      this.showInactivityWarning = false;
      this.inactivityTimer = setTimeout(() => {
        alert("You have been flagged as inactive. Click OK if you are still here.")
        this.resetInactivityTimer();
      }, 60000 * 4); // 4 minutes
    },

    setInputPlaceholder() {
      if (this.game.gameMode === 'DEFAULT') {
        this.answerInputPlaceHolder = "Guess the title of the beatmap!";
      } else if (this.game.gameMode === 'ARTIST') {
        this.answerInputPlaceHolder = "Guess the artist of the beatmap!";
      } else if (this.game.gameMode === 'CREATOR') {
        this.answerInputPlaceHolder = "Guess the mapper of the beatmap!";
      }
    }
  },

  async mounted() {
    this.gameId = this.$route.params.gameId;
    var isPrivate = this.$route.params.private;
    var roompw = localStorage.getItem("roompw");

    if (isPrivate === "true" && roompw === null) {
      this.password = prompt("Please enter the password to join the game.");
      localStorage.setItem("roompw", this.password);
    }

    await this.joinGame(roompw, false);
    var possibleAnswerURL = 'possibleAnswers';

    if (this.game) {
      if (this.game.gameMode === 'ARTIST') {
        possibleAnswerURL = 'possibleAnswers_artist';
      } else if (this.game.gameMode === 'CREATOR') {
        possibleAnswerURL = 'possibleAnswers_creator';
      }
    }

    apiService.get(`${process.env.VUE_APP_API_URL}/api/${possibleAnswerURL}`, {})
        .then((response) => {
          this.possibleAnswers = response.data;
        })
        .catch((error) => {
          console.error('Error getting beatmap', error);
        });

    // window.addEventListener('beforeunload', this.beforeUnload);
    // window.addEventListener('unload', this.requestLeaveRoom);


    setTimeout(() => {
      const audioPlayer = this.$refs.audio;
      if (audioPlayer) {
        audioPlayer.volume = this.volume / 100;
      }
    }, 2000);

    const messageContainer = this.$el.querySelector('.message-wrapper');
    messageContainer.addEventListener('scroll', () => {
      const {scrollTop, scrollHeight, clientHeight} = messageContainer;
      this.isScrolling = scrollTop + clientHeight >= scrollHeight * 0.9;
    });

    this.resetInactivityTimer();


    this.webSocketService = new WebSocketService(this.gameId);
    this.webSocketService.connect({
      onMessage: (message) => {
        // Handle chat messages
        var msg = JSON.parse(message.body);

        // Adjust timezone
        msg.timestamp = new Date();

        this.messages.push(msg);

        // Remove the oldest message if the message count exceeds 100
        if (this.messages.length > 100) {
          this.messages.shift();
        }
      },
      onGameStatus: (message) => {
        // Handle game status updates
        if (message.body === "gameStart") {
          this.isPlaying = true;
          this.isGuessing = true;
          this.game.questionIndex = 1;

          this.players.forEach((player) => {
            player.totalPoints = 0;
            player.totalGuess = 0;
            player.guessedRight = false;
          });

          var possibleAnswerURL = 'possibleAnswers';

          if (this.game.gameMode === 'ARTIST') {
            possibleAnswerURL = 'possibleAnswers_artist';
          } else if (this.game.gameMode === 'CREATOR') {
            possibleAnswerURL = 'possibleAnswers_creator';
          }

          apiService.get(`${process.env.VUE_APP_API_URL}/api/${possibleAnswerURL}`, {})
              .then((response) => {
                this.possibleAnswers = response.data;
              })
              .catch((error) => {
                console.error('Error getting beatmap', error);
              });
        } else if (message.body === "gameEnd") {
          this.isPlaying = false;
          this.highlightStartButton = true;
          setTimeout(() => {
            this.highlightStartButton = false;
          }, 2000); // highlight for 2 seconds
        } else if (message.body === "gameDeleted") {
          this.kicked = true;
          alert("This game has been deleted due to inactivity.");
          this.$router.push('/');
        }
      },
      onGameProgress: (message) => {
        // Ignore if game is null
        if (!this.game) {
          return;
        }
        // Handle game progress updates
        this.isGuessing = true;
        this.answerInput = "";
        this.inputFocused = false;

        // If display mode does not include AUDIO, stop the audio
        if (!this.game.displayMode.includes('AUDIO')) {
          const audioPlayer = this.$refs.audio;
          if (audioPlayer) {
            audioPlayer.pause();
            audioPlayer.currentTime = 0;
          }
        }

        var encryptedBeatmapInfo = JSON.parse(message.body);
        this.ownGuessRight = false;
        if (this.game.displayMode.includes('BACKGROUND')) {
          this.imageSourceBase64 = encryptedBeatmapInfo.base64;
        }
        if (this.game.displayMode.includes('AUDIO')) {
          this.audioSourceBase64 = encryptedBeatmapInfo.base64;
        }
        this.timeLeft = this.game.guessingTime - 1;
        this.game.questionIndex++;
        this.countdownInterval = setInterval(() => {
          this.timeLeft--;
          if (this.timeLeft <= 0) {
            clearInterval(this.countdownInterval);
          }
        }, 1000);

        this.players.forEach((player) => {
          player.guessedRight = false;
          player.answerSubmitted = false;
        });
      },
      onGameLeaderboard: (message) => {
        // Handle game leaderboard updates
        this.isGuessing = false;
        this.inputFocused = false;

        this.autocompleteOptions = [];

        clearInterval(this.countdownInterval)

        this.timeLeft = 0;

        const audioPlayer = this.$refs.audio;
        if (audioPlayer) {
          audioPlayer.pause();
          audioPlayer.currentTime = 0;
          audioPlayer.play()
        }


        var leaderboard = JSON.parse(message.body);

        // Iterate over the players array
        this.players.forEach((player) => {
          if (!player) return;
          // Find the corresponding entry in the leaderboard object
          let leaderboardEntry = leaderboard[player.username];
          // If the leaderboard entry exists, update the player's point property
          if (leaderboardEntry) {
            if (!player.totalGuess) {
              player.totalGuess = 0;
              player.totalPoints = 0;
            }

            player.guessedRight = leaderboardEntry.totalGuess > player.totalGuess;
            if (player.id === this.me.id && player.guessedRight) {
              this.ownGuessRight = true;
            }
            // Update the point property
            player.totalGuess = leaderboardEntry.totalGuess;
            player.totalPoints = leaderboardEntry.totalPoints;
          } else {
            player.guessedRight = false;
          }
        });
      },
      onAnswer: (message) => {
        // Handle answer updates
        this.currentBeatmap = JSON.parse(message.body);

        if (this.game.gameMode === 'DEFAULT') {
          this.answerInput = this.currentBeatmap.title;
        } else if (this.game.gameMode === 'ARTIST') {
          this.answerInput = this.currentBeatmap.artist;
        } else if (this.game.gameMode === 'CREATOR') {
          this.answerInput = this.currentBeatmap.creator;
        }

        this.answerSubmitted = false;
      },
      onAnswerSubmission: (message) => {
        // Handle answer submission updates
        var answerSubmissions = JSON.parse(message.body);

        this.players.forEach((player) => {
          player.answerSubmitted = answerSubmissions[player.id];
        });
      },
      onPlayersAnswers: (message) => {
        // Handle players' answers updates
        this.answers = JSON.parse(message.body);
      },
      onPlayers: (message) => {
        // Handle players updates
        // Save the previous player list for points
        const previousPlayers = this.players;
        this.players = JSON.parse(message.body);

        // Check if the player list has changed
        // Do I exist in previous players, but not in the new list?

        // Update the points of the players
        this.players.forEach((player) => {
          const previousPlayer = previousPlayers.find((p) => p.id === player.id);
          if (previousPlayer) {
            player.totalGuess = previousPlayer.totalGuess;
            player.totalPoints = previousPlayer.totalPoints;
            player.guessedRight = previousPlayer.guessedRight;
          }
        });
      },
      onRoomSettings: (message) => {
        // Handle room settings updates
        this.game = JSON.parse(message.body);
        this.showDropdown = false;
        this.setInputPlaceholder();
      },
      onAnnouncement: (message) => {
        // Handle announcement updates
        this.messages.push({
          announcement: true,
          systemMessage: true,
          content: message.body,
          timestamp: Date.now(),
        });
      },
      onBlurReveal: (message) => {
        // Handle blur reveal updates
        this.imageSourceBase64 = message.body;
      },
      onPlayerKick: (message) => {
        // Handle player kick updates
        const player = JSON.parse(message.body);
        if (player.id === this.me.id) {
          if (this.webSocketService){
            this.webSocketService.disconnect();
          }
          this.kicked = true;
          this.$router.push('/');
          alert("You have been kicked from the game.");
        }
      },
      onPlayerInactivityKick: (message) => {
        const player = JSON.parse(message.body);
        if (player.id === this.me.id) {
          if (this.webSocketService){
            this.webSocketService.disconnect();
          }
          this.kicked = true;
          this.$router.push('/');
          alert("You have been kicked from the game due to inactivity.");
        }
      }
    }).then(success => {
      this.isConnected = true;
      this.messages.push({
        systemMessage: true,
        content: "Successfully connected to the game room.",
        timestamp: Date.now(),
      });
    });
  },

  beforeUnmount() {
    if (this.webSocketService){
      this.webSocketService.disconnect();
    }

    // window.removeEventListener('beforeunload', this.beforeUnload);
    // window.removeEventListener('unload', this.requestLeaveRoom);

    clearInterval(this.countdownInterval)
    clearInterval(this.inactivityTimer)
  },

  beforeRouteLeave(to, from, next) {
    if (!this.game) {
      next();
      return;
    }

    if (this.kicked) {
      next();
      return;
    }

    if (confirm("Are you sure you want to leave the game?")) {
      this.requestLeaveRoom();
      next();
    } else {
      next(false);
    }
  },

  computed: {
    modeIcon() {
      switch (this.game.mode) {
        case 'MUSIC':
          return 'music';
        case 'BACKGROUND':
          return 'map';
        case 'PATTERN':
          return 'circle';
        default:
          return '';
      }
    },

    getAnswerRate() {
      if (this.currentBeatmap.playcount === 0) {
        return 0; // Avoid division by zero
      }
      return ((this.currentBeatmap.playcount_answer / this.currentBeatmap.playcount) * 100).toFixed(2) + "%";
    },

    getBeatmapUrl() {
      return `https://osu.ppy.sh/beatmapsets/${this.currentBeatmap.beatmapset_id}`;
    },

    sortedPlayers() {
      const playersCopy = this.players.map(player => ({...player}));
      return playersCopy.sort((a, b) => b.totalPoints - a.totalPoints).slice(0, 8);
    },
  },
  watch: {
    messages: {
      handler() {
        this.$nextTick(() => {
          if (this.isScrolling) {
            const messageContainer = this.$el.querySelector('.message-wrapper');
            messageContainer.scrollTop = messageContainer.scrollHeight;
          }
        });
      },
      deep: true
    },
    audioSourceBase64: function(newVal, oldVal) {
      this.$nextTick(() => {
        let audioPlayer = this.$refs.audio;
        if (audioPlayer) {
          audioPlayer.load();
          if (newVal) {
            audioPlayer.play();
          }
        }
      });
    },

    answerInput(newVal) {
      if (!this.isGuessing) return;

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
      let audioPlayer = this.$refs.audio;
      if (audioPlayer) {
        audioPlayer.volume = newVal / 100;
        localStorage.setItem("volume", newVal);
      }
    },

    me: {
      handler(newVal) {
        if (!newVal) {
          alert("An error occurred while fetching user data.");
        }
      },
      immediate: true,
    }
  },
};
</script>

<template>
  <div id="app">
    <div class="game-container">
      <div class="game-content">
        <div v-if="this.game" class="game-room-info">
          <div class="game-room-info-left">
            <button v-on:click="leaveRoom" class="leave-room-button">
              <font-awesome-icon :icon="['fas', 'door-open']" class="info-icon" />
            </button>
            <div class="game-name">{{ this.game.name }}</div>
            <div>Host: {{ this.game.owner.username }} / {{ players.length }} players</div>
            <div v-if="!this.game.ranked" class="game-room-info-unranked">Unranked</div>
          </div>

          <div class="game-room-info-middle">
            <button v-if="isConnected && !isPlaying && this.me && this.me.id === this.game.owner.id" v-on:click="startGame" class="start-game-button" :class="{ 'start-game-button-highlight': highlightStartButton }">Start Game</button>
            <div v-else class="game-in-progress">{{ this.timeLeft }}</div>
          </div>

          <div class="game-room-info-right">
            <div v-if="showSettingsWarning" class="settings-warning">
              You can't change room settings while playing
            </div>
            <font-awesome-icon :icon="['fas', 'gear']" v-if="this.me && this.me.id === this.game.owner.id" v-on:click="toggleRoomSettingsModal" class="gameroom-settingsIcon"/>
            <div class="icon-text-container">
              <div class="icon-container">
                {{ this.game.startYear }} - {{ this.game.endYear }}
              </div>
              <div class="info-text">Year</div>
            </div>
            <div class="icon-text-container">
              <div class="icon-container">
                <div v-for="diff in this.game.difficulty" :key="diff" :class="difficultyClass(diff)" class="gameroom-difficulty">{{ difficultyName(diff) }}</div>
              </div>
              <div class="info-text">Difficulty</div>
            </div>
<!--            <div class="icon-text-container">-->
<!--              <div class="icon-container">-->
<!--                <font-awesome-icon :icon="['fas', modeIcon]" class="info-icon" />-->
<!--              </div>-->
<!--              <div class="info-text">Mode</div>-->
<!--            </div>-->
            <div class="icon-text-container">
              <div class="info-icon">{{ this.game.questionIndex }}/{{ this.game.totalQuestions }}</div>
              <div class="info-text">Current / Total</div>
            </div>
          </div>
        </div>

        <RoomSettingsModal v-if="this.game && isRoomSettingsModalOpen" v-on:toggleRoomSettingsModal="toggleRoomSettingsModal" :game="game" />

        <div v-if="this.game">
          <div class="question-image-container">
            <div class="question-image-cover" v-if="!toggleImage || (!this.game.displayMode.includes('BACKGROUND') && isGuessing)"></div>
            <div class="image-wrapper">
              <img :src="'/image/' + this.imageSourceBase64" alt="" class="question-image">
              <img :src="icon_report" alt="" class="icon-report" v-on:click="toggleReportDropdown" v-if="isPlaying"/>
              <font-awesome-icon :icon="['fas', 'volume-high']" class="icon-showSettings" @click="showUserSettings = !showUserSettings"/>
              <div class="settings-container" v-if="showUserSettings">
                <input type="range" min="0" max="100" value="50" class="slider" id="volume" v-model="volume" :disabled="!toggleAudio"/>
                <div class="settings-toggles">
                  <font-awesome-icon :icon="['fas', 'eye']" v-if="toggleImage" v-on:click="toggleImage = !toggleImage"/>
                  <font-awesome-icon :icon="['fas', 'eye-slash']" v-else v-on:click="toggleImage = !toggleImage"/>
                  <font-awesome-icon :icon="['fas', 'volume-high']" v-if="toggleAudio" v-on:click="toggleAudioVolume"/>
                  <font-awesome-icon :icon="['fas', 'volume-xmark']" v-else v-on:click="toggleAudioVolume"/>
                </div>
              </div>
              <div v-if="showReportDropdown" class="dropdown-menu">
                <p v-on:click="reportBeatmap('RESOURCE_MISSING')">missing mp3/bg</p>
                <p v-on:click="reportBeatmap('BG_SPOILER')">bg spoils title</p>
              </div>
            </div>
          </div>

          <div class="audio-input-container">
            <audio hidden ref="audio" v-if="this.game && (this.game.displayMode.includes('AUDIO'))" id="audioPlayer">
              <source :src="'/audio/' + this.audioSourceBase64" type="audio/mpeg">
              Your browser does not support the audio element.
            </audio>
          </div>

          <div class="input-container">
            <div class="input-icon-wrapper">
              <input type="text"
                     :class="{ answerSubmitted: answerSubmitted, highlight: ownGuessRight }"
                     v-model="answerInput" :placeholder="answerInputPlaceHolder"
                     :readonly="!isGuessing"
                     v-on:keydown.up.prevent="selectedOptionIndex > 0 ? selectedOptionIndex-- : selectedOptionIndex = autocompleteOptions.length - 1"
                     v-on:keydown.down.prevent="selectedOptionIndex < autocompleteOptions.length - 1 ? selectedOptionIndex++ : selectedOptionIndex = 0"
                     v-on:keydown.enter.prevent="selectAutocompleteOption(selectedOptionIndex)" v-on:focus="inputFocused = true" class="answer-input">
              <font-awesome-icon :icon="['fas', 'check']" class="check-icon" :class="{ visible: answerSubmitted }" />
              <font-awesome-icon :icon="['fas', 'maximize']" class="button-switch-viewmode" v-if="!compactViewMode" @click="compactViewMode = true"/>
              <font-awesome-icon :icon="['fas', 'minimize']" class="button-switch-viewmode" v-else @click="compactViewMode = false"/>
              <div v-if="autocompleteOptions.length && inputFocused" class="autocomplete-dropdown">
                <div v-for="(option, index) in autocompleteOptions" :key="option" @click="selectAutocompleteOption(index)" :class="{ 'selected': index === selectedOptionIndex }" class="autocomplete-option">
                  {{ option }}
                </div>
              </div>
            </div>
          </div>

          <div v-if="currentBeatmap && !isGuessing" class="beatmap-info-container">
            <a :href="getBeatmapUrl" target="_blank">
              <h2>Beatmap Information</h2>
              <div class="beatmap-info-inner">
                <p>Artist: <strong>{{ currentBeatmap.artist }}</strong></p>
                <p>Title: <strong>{{ currentBeatmap.title }}</strong></p>
                <p>Mapper: <strong>{{ currentBeatmap.creator }}</strong></p>
                <p>Answer Rate: <strong>{{ getAnswerRate }}</strong> <span class="beatmap-info-playcount">({{ currentBeatmap.playcount_answer }}/{{ currentBeatmap.playcount }})</span></p>
                <p><strong><span class="beatmap-info-difficulty" :class="difficultyClass(currentBeatmap.beatmapDifficulty)">[{{ currentBeatmap.beatmapDifficulty }}]</span></strong></p>
                <p>Ranked on <strong>{{ formatDate(currentBeatmap.approved_date) }}</strong></p>
                <br>
                <p v-if="answers[me.id]">Difficulty Bonus: {{ answers[me.id].difficultyBonus.toFixed(2) }}</p>
                <p v-if="answers[me.id]">Speed Bonus: {{ answers[me.id].speedBonus.toFixed(2) }}</p>
                <p v-if="answers[me.id] && answers[me.id].poolSizeBonus < 1.00">Pool Size Penalty: {{ answers[me.id].poolSizeBonus.toFixed(2) }}</p>
                <p v-if="answers[me.id] && answers[me.id].poolSizeBonus > 1.00">Pool Size Bonus: {{ answers[me.id].poolSizeBonus.toFixed(2) }}</p>
                <p v-if="answers[me.id]">Total Points: {{ answers[me.id].totalPoints.toFixed(2) }}</p>
                <div class="beatmap-info-clickme">
                  Click to view the beatmap on osu!
                </div>
              </div>
            </a>
          </div>

          <div class="leaderboard">
            <h2>Leaderboard</h2>
            <div v-for="(player, index) in sortedPlayers" :key="player.id" class="leaderboard-player-info" :class="{ 'leaderboard-player-highlight': player.guessedRight }" @click.stop="showPlayerInfoModal = true; userpageId = player.id">
              <div class="leaderboard-player-rank">#{{ index + 1 }}</div>
              <div class="leaderboard-player-name">{{ player.username }}</div>
              <div class="leaderboard-player-points">{{ formatDecimal(player.totalPoints ? player.totalPoints : 0) }} <span class="leaderboard-player-totalguess">({{ player.totalGuess ? player.totalGuess : 0 }})</span></div>
            </div>
          </div>
        </div>

        <div class="players-container" v-if="!compactViewMode" :class="{'players-container-overflow': players.length > 8}">

          <div v-for="player in players" :key="player.id" class="player-box"
               :class="{ 'highlight': player.guessedRight, 'player-answer-submitted': player.answerSubmitted }">

            <font-awesome-icon :icon="['fas', 'caret-down']" v-if="me && me.id === game.owner.id && me.id !== player.id"
                               @click.stop="openDropdown(player)" class="user-dropdown-button"/>
            <div v-if="showDropdown && this.dropdownUser.id === player.id" class="dropdown-menu">
              <p @click.stop="kickPlayer(player)">Kick</p>
              <p @click.stop="transferHost(player)">Host</p>
              <p @click.stop="showDropdown = false">Cancel</p>
            </div>
            <img :src="player.avatar_url" alt="Player's avatar" class="player-avatar" @click.stop="showPlayerInfoModal = true; userpageId = player.id">
              <p class="player-username">
                {{ player.username }}</p>
            <p class="player-title">{{ player.current_title_achievement ? player.current_title_achievement.title_name : "" }}</p>
            <p class="player-points">{{ formatDecimal(player.totalPoints ? player.totalPoints : 0) }}
                <span class="player-totalguess">({{ player.totalGuess ? player.totalGuess : 0 }})</span></p>

            <div v-if="!isGuessing && answers[player.id] && answers[player.id].answer" class="player-answer">
              {{ answers[player.id].answer }}
            </div>

          </div>
        </div>

        <div class="players-container-compact" v-if="compactViewMode">
          <div v-for="player in players" :key="player.id" class="player-box-compact"
               :class="{ 'highlight': player.guessedRight, 'player-answer-submitted': player.answerSubmitted }" @click.stop="showPlayerInfoModal = true; userpageId = player.id">
            <img :src="player.avatar_url" alt="Player's avatar" class="player-avatar-compact">
            <div class="player-info-compact">
              <div class="player-header-compact">
                <p class="player-username-compact">{{ player.username }}</p>
                <p class="player-points-compact">{{ formatDecimal(player.totalPoints ? player.totalPoints : 0) }}
                  <span class="player-totalguess">({{ player.totalGuess ? player.totalGuess : 0 }})</span></p>
              </div>
              <div class="player-answer-compact" v-if="!isGuessing">
                {{ isGuessing ? "" : (answers[player.id] && answers[player.id].answer ? answers[player.id].answer : "") }}
              </div>
            </div>
          </div>
        </div>
      </div>
      <ChatSection :messages="messages" :isConnected="isConnected" :formatTimestamp="formatTimestamp" @send-message="sendChatMessage" />

    </div>
    <div v-if="isLoading" class="loading-indicator">Loading...</div>
    <div v-if="showPlayerInfoModal" class="modal-overlay" @click.stop="showPlayerInfoModal = false">
      <UserPage :playerId="userpageId"></UserPage>
    </div>
  </div>
</template>

<style>
RoomSettingsModal {
  z-index: 100;
}

UserPage {
  z-index: 1000;
}

.modal-overlay {
  cursor: pointer;
  position: fixed;
  width: 100%;
  height: 100%;
  top: 0;
  left: 0;
  background-color: rgba(0, 0, 0, 0.8);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 999;
}

.loading-indicator {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  display: flex;
  justify-content: center;
  align-items: center;
  background-color: rgba(0, 0, 0, 0.8);
  color: white;
  font-size: 3em;
  z-index: 999;
}

.game-container {
  display: flex;
}

.game-content {
  width: 80vw;
  position: relative;
  flex: 3;
}

.game-room-info {
  display: flex;
  justify-content: space-between;
  padding: 10px;
  height: 6vh;
}

.game-room-info-middle {
  display: flex;
  justify-content: center;
  align-items: center;
}

.start-game-button {
  background: var(--color-secondary);
  border: none;
  color: var(--color-text);
  padding: 15px 32px;
  text-align: center;
  text-decoration: none;
  display: inline-block;
  font-size: 16px;
  margin: 4px 2px;
  cursor: pointer;
  border-radius: 4px;
}

.settings-container {
  position: absolute;
  bottom: 32px;
  left: 32px;
  padding: 10px;
  margin-top: 10px;
  border: 1px solid #ccc;
  background-color: var(--color-body);
  z-index: 4;
}

.beatmap-info-container {
  position: absolute;
  top: 7vh;
  right: 20px;
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

.beatmap-info-playcount {
  font-size: 0.6em;
}

.beatmap-info-clickme {
  position: absolute;
  bottom: 4px;
  right: 4px;
  font-size: 0.6em;
  text-align: right;
}

.icon-container {
  background-color: var(--color-secondary);
  border-radius: 5px;
  padding: 4px;
  display: flex;
  gap: 5px;
  justify-content: center;
  justify-items: center;
}

.input-container {
  display: flex;
  justify-content: center;
  align-items: center;
}

.input-icon-wrapper {
  position: relative;
  width: 50%;
}

.check-icon {
  position: absolute;
  right: 10px;
  top: 50%;
  transform: translateY(-50%);
  visibility: hidden;
}

.check-icon.visible {
  visibility: visible;
}

.game-in-progress {
  color: var(--color-text);
  font-size: 2em;
  font-weight: bold;
}

.game-room-info-left, .game-room-info-right {
  display: flex;
  flex-direction: row;
  align-items: center;
  gap: 20px;
}

.game-room-info-left .game-name {
  font-weight: bold;
  font-size: 1.2em;
}

.game-room-info-unranked {
  background-color: var(--color-gameroom);
  border-radius: 5px;
  padding: 5px;
  font-weight: bold;
  font-size: 1.2em;
}

.info-icon {
  color: var(--color-text);
  font-size: 20px;
  margin-right: 10px;
}

.info-text {
  font-size: 16px;
}

.leave-room-button {
  background: none;
  border: none;
  cursor: pointer;
  padding: 0;
  margin-right: 10px;
}

.players-container {
  display: flex;
  flex-direction: row;
  justify-content: center;
  overflow-y: visible;
  overflow-x: auto;
  margin-top: 16px;
  height: 30vh;
  padding-left: 10px;
  padding-right: 10px;
}

.players-container-overflow {
  justify-content: flex-start;
}

.player-box {
  position: relative;
  padding: 10px;
  margin-right: 10px;
  text-align: center;
  width: 150px;
  max-width: 150px;
  min-width: 150px;
  border-radius: 10px;
  background-color: rgba(231, 239, 255, 0.2);
  border: 2px solid transparent;
  overflow: visible;
}

.player-box-compact {
  cursor: pointer;
  display: flex;
  flex-direction: row;
  align-items: center;
  justify-content: flex-start;
  width: 95%;
  height: 3em;
  margin: 5px;
  border-radius: 10px;
  background-color: rgba(231, 239, 255, 0.2);
  border: 2px solid transparent;
}

.player-answer-submitted {
  background-color: rgba(231, 239, 255, 0.66);
}

.player-avatar {
  cursor: pointer;
  width: 100%;
  height: auto;
  border-radius: 10%;
  box-shadow: 0px 0px 15px 5px rgba(0, 0, 0, 0.1);
  border: 3px solid #000000;
  box-sizing: border-box;
}

.player-username{
  margin-top: 10px;
  margin-bottom: 2px;
}

.player-title {
  min-height: 1.2em;
  font-size: 0.8em;
  margin-top: 2px;
  margin-bottom: 2px;
}

.player-info-hover {
  position: fixed;
  width: 200px;
  background-color: var(--color-secondary);
  color: var(--color-text);
  z-index: 1000;
}

.player-points {
  font-size: 1.2em;
  color: var(--color-text);
  margin-top: 0.2em;
  margin-bottom: 0.2em;
}

.player-totalguess {
  font-size: 0.8em;
  color: #999;
}

.player-username {
  font-weight: bold;
  font-size: 1.2em;
  overflow: hidden;
}

.player-answer {
  margin-top: 0.0em;
  font-size: 0.8em;
  color: var(--color-text);
  background-color: var(--color-secondary);
  padding: 5px;
  border-radius: 5px;
  width: 90%;
  overflow: hidden;
  text-overflow: ellipsis;
}

.player-answer-compact {
  display: flex;
  font-size: 1em;
  color: var(--color-text);
  background-color: var(--color-secondary);
  padding: 5px;
  border-radius: 5px;
  overflow: hidden;
  align-items: center;
  text-overflow: ellipsis;
  height: 1.5em;
  max-width: 25vw;
}

.answerSubmitted {
  background-color: var(--color-disabled);
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
  background-color: rgba(49, 49, 49, 1);
  width: 72vh;
  height: 41vh;
  z-index: 1;
}

.question-image-container {
  display: flex;
  justify-content: center;
  align-items: center;
  position: relative;
}

.audio-input-container {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.answer-input {
  width: 100%;
  height: 3em;
  box-sizing: border-box;
  font-size: 1.2em;
  text-align: center;
  margin-top: 8px;
  font-family: 'Sen', serif;
  background-color: var(--color-secondary);
  color: var(--color-text);
}

.highlight {
  background-color: rgba(22, 225, 49, 0.33);
  border: 2px solid rgba(22, 225, 49, 0.8);
}

.autocomplete-dropdown {
  position: absolute;
  background-color: var(--color-secondary);
  border: 1px solid #ccc;
  width: 100%;
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

.difficulty-easy {
  color: #20c020;
}

.difficulty-normal {
  color: #226f9a;
}

.difficulty-hard {
  color: #e7c125;
}

.difficulty-insane {
  color: #e53f3f;
}

.gameroom-difficulty {
  font-weight: bold;
  font-size: 0.8em;
}

.gameroom-settingsIcon {
  cursor: pointer;
  width: 24px;
  height: 24px;
}

.settings-toggles {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 10px;
}

.settings-toggles>svg {
  width: 20px;
  height: 20px;
  margin: 0 10px;
  border-radius: 5px;
  padding: 5px;
  cursor: pointer;
}

.settings-warning {
  position: absolute;
  background-color: #f44336;
  color: white;
  padding: 16px;
  border-radius: 4px;
  z-index: 1;
}

.user-dropdown-button {
  position: absolute;
  right: 18px;
  top: 18px;
  width: 20px;
  height: 20px;
  cursor: pointer;
  border-radius: 4px;
  color: #ffffff;
  background-color: rgba(0, 0, 0, 1);
}

.dropdown-menu {
  background-color: rgba(0, 0, 0, 0.8);
  position: absolute;
  right: 8px;
  box-shadow: 0px 8px 16px 0px rgba(0,0,0,0.2);
  z-index: 4;
}

.dropdown-menu p {
  color: #ffffff;
  padding: 12px 16px;
  text-decoration: none;
  display: block;
}

.dropdown-menu p:hover {
  background-color: #f1f1f1;
}

.showDropdown .dropdown-menu {
  display: block;
}

.icon-report {
  position: absolute;
  right: 0px;
  bottom: 0px;
  width: 24px;
  height: 24px;
  cursor: pointer;
}

.icon-showSettings {
  position: absolute;
  left: 6px;
  bottom: 0px;
  width: 24px;
  height: 24px;
  cursor: pointer;
  background-color: var(--color-secondary);
  border-radius: 5px;
  border: 2px solid var(--color-text);
  z-index: 3;
}

.start-game-button-highlight {
  animation: start-game-button-highlight 2s ease-out;
  border: 5px solid #FFFF00;
}

@keyframes start-game-button-highlight {
  0% { background-color: #FFFF00; }
  100% { background-color: #eaeaea; }
}

.leaderboard {
  position: absolute;
  top: 7vh;
  left: 20px;
  padding: 10px;
  margin-top: 10px;
  border: 1px solid #ccc;
  background-color: var(--color-secondary);
  max-width: 28vh;
  min-width: 28vh;

}

.leaderboard h2 {
  text-align: center;
  margin-bottom: 10px;
}

.leaderboard-player-info {
  cursor: pointer;
  display: flex;
  justify-content: space-between;
  padding: 5px 0;
  margin-bottom: 5px;
  align-items: center;
  box-sizing: border-box;
}

.leaderboard-player-highlight {
  background-color: rgba(22, 225, 49, 0.33);
  border: 2px solid rgba(22, 225, 49, 0.8);
}

.leaderboard-player-rank, .leaderboard-player-name, .leaderboard-player-points {
  flex-basis: 33.33%;
  text-align: center;
}

.leaderboard-player-totalguess {
  font-size: 0.8em;
  color: #999;
}

.stats-percentage {
  font-size: 0.8em;
  color: #999;
}

.button-switch-viewmode {
  cursor: pointer;
  position: absolute;
  right: -36px;
  width: 24px;
  height: 24px;
  top: 50%;
  transform: translateY(-50%);
}

.player-info-compact {
  width: 100%;
  display: flex;
  justify-content: space-between;
  flex-direction: row;
  gap: 5px;
  align-items: center;
  margin-right: 8px;
}

.player-avatar-compact {
  width: 50px;
  height: 50px;
  border-radius: 50%;
  margin-right: 10px;
}

.player-username-compact {
  font-weight: bold;
  font-size: 1.2em;
  margin-bottom: -1.2em;
}

.player-points-compact {
  font-size: 1.2em;
  color: var(--color-text);
  margin-left: auto;
}

.players-container-compact {
  display: grid;
  grid-template-columns: 1fr 1fr;
  grid-auto-rows: 4.5em;
  grid-auto-flow: row;
  overflow-y: auto;
  height: 35vh;
}
</style>