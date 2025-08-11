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
import BeatmapInfo from "@/components/Game/components/BeatmapInfo.vue";
import IngameSettingsPage from "@/components/Game/components/IngameSettingsPage.vue";
import PlayerBox from "@/components/Game/components/PlayerBox.vue";
import PlayerBoxCompact from "@/components/Game/components/PlayerBoxCompact.vue";
import BeatmapRenderer from "@/components/osu/Render/BeatmapRenderer.vue";


export default {
  name: 'GamePage',
  components: {
    BeatmapRenderer,
    PlayerBoxCompact,
    PlayerBox, IngameSettingsPage, BeatmapInfo, ChatSection, UserPage, FontAwesomeIcon, RoomSettingsModal},
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
      ownGuessRightAlias: false,
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
      beatmapVolume: 30,
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
      settings: {
        submitEnter: true,
        submitRightArrow: false,
        submitTab: false,
        chatHighlight: false
      },
      // Draggable leaderboard properties
      isDragging: false,
      dragStartX: 0,
      dragStartY: 0,
      leaderboardX: this.loadLeaderboardPosition().x,
      leaderboardY: this.loadLeaderboardPosition().y,
      dragOffsetX: 0,
      dragOffsetY: 0
    };
  },
  methods: {
    initialize() {
      window.addEventListener('beforeunload', this.requestLeaveRoom);

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

        const savedBeatmapVolume = localStorage.getItem("beatmapVolume");
        if (savedBeatmapVolume) {
          this.beatmapVolume = savedBeatmapVolume;
        }
      }, 1000);

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
              player.guessedRightAlias = false;
            });

            this.getPossibleAnswers();
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
          if (!this.game) {
            return;
          }

          // Handle game progress updates
          this.isPlaying = true;
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
          this.ownGuessRightAlias = false;
          if (this.game.displayMode.includes('BACKGROUND')) {
            this.imageSourceBase64 = encryptedBeatmapInfo.base64;
          }

          this.audioSourceBase64 = encryptedBeatmapInfo.base64;

          if (this.game.displayMode.includes('PATTERN')) {
            this.updateBeatmap(encryptedBeatmapInfo.beatmapId);
            this.updateBeatmapBackground('');
            this.imageSourceBase64 = encryptedBeatmapInfo.base64;
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

              player.guessedRight = leaderboardEntry.correct;
              player.guessedRightAlias = leaderboardEntry.aliasCorrect;
              if (player.id === this.me.id && player.guessedRight) {
                this.ownGuessRight = true;
              }
              // Update the point property
              player.totalGuess = leaderboardEntry.totalGuess;
              player.totalPoints = leaderboardEntry.totalPoints;
            } else {
              player.guessedRight = false;
              player.guessedRightAlias = false;
            }
          });

          this.players.sort((a, b) => b.totalPoints - a.totalPoints);
        },
        onAnswer: (message) => {
          // Handle answer updates
          this.currentBeatmap = JSON.parse(message.body);

          if (this.game.guessMode === 'TITLE') {
            this.answerInput = this.currentBeatmap.title;
          } else if (this.game.guessMode === 'ARTIST') {
            this.answerInput = this.currentBeatmap.artist;
          } else if (this.game.guessMode === 'CREATOR') {
            this.answerInput = this.currentBeatmap.creator;
          }

          const audioPlayer = this.$refs.audio;
          if (audioPlayer) {
            audioPlayer.currentTime = 0;
            audioPlayer.volume = this.volume / 100;

            if (!audioPlayer.isPlaying) {
              audioPlayer.play()
            }
          }

          if (this.game.displayMode.includes('PATTERN')) {
            this.resetCurrentTime();
            this.updateBeatmapBackground(this.imageSourceBase64);
            this.answerInput = this.currentBeatmap.title;
          }

          this.players.forEach((player) => {
            player.answerSubmitted = false;
          });

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
      }).then(() => {
        this.isConnected = true;
        this.messages.push({
          systemMessage: true,
          content: "Successfully connected to the game room.",
          timestamp: Date.now(),
        });
      });
    },

    startGame() {
      this.isLoading = true;
      apiService.post('/api/startGame', {
        gameId: this.gameId,
        userId: this.userId
      })
          .then(() => {
          })
          .catch(() => {
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
      if (this.webSocketService) {
        this.webSocketService.disconnect();
        this.webSocketService = null;
      }

      // if (this.$refs.beatmapRenderer) {
      //   this.$refs.beatmapRenderer.clearBeatmap();
      // }

      apiService.post('/api/leaveGame', {
        gameId: this.gameId,
        userId: this.me.id,
      })
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
      const date = new Date(timestamp);

      const localTimestamp = date.toLocaleString('en-US', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit',
        hour12: false
      });

      return moment(localTimestamp, 'MM/DD/YYYY, HH:mm:ss').format('h:mm:ss a');
    },

    formatDate(date) {
      return moment(date).format('MMMM Do YYYY');
    },

    formatDecimal(value) {
      if (value === 0) {
        return '0';
      }
      return value.toFixed(2);
    },

    selectAutocompleteOption(index) {
      if (!this.isGuessing) {
        return;
      }

      if (index === -1) {
        if (this.settings.submitAutoSelect) {
          this.answerInput = this.autocompleteOptions[0];
        }
      } else {
        this.answerInput = this.autocompleteOptions[index];
      }

      this.autocompleteOptions = [];
      this.selectedOptionIndex = -1; 

      this.submitAnswer()
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
        case 'EXTRA':
          return 'difficulty-extra';
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
        case 'EXTRA':
          return 'X';
        default:
          return '';
      }
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
              this.getPossibleAnswers();

              this.initialize();

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
            throw new Error("An error occurred while joining the game.");
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
    },

    resetInactivityTimer() {
      clearTimeout(this.inactivityTimer);
      this.showInactivityWarning = false;
      this.inactivityTimer = setTimeout(() => {
        alert("You have been flagged as inactive. Click OK if you are still here.")
        this.resetInactivityTimer();
      }, 60000 * 9); // 9 minutes
    },

    setInputPlaceholder() {
      if (this.game.guessMode === 'TITLE') {
        this.answerInputPlaceHolder = "Guess the title of the beatmap!";
      } else if (this.game.guessMode === 'ARTIST') {
        this.answerInputPlaceHolder = "Guess the artist of the beatmap!";
      } else if (this.game.guessMode === 'CREATOR') {
        this.answerInputPlaceHolder = "Guess the mapper of the beatmap!";
      }
    },

    getPossibleAnswers() {
      apiService.get(`${process.env.VUE_APP_API_URL}/api/possibleAnswers?gamemode=${this.game.gameMode}&guessmode=${this.game.guessMode}`, {})
          .then((response) => {
            this.possibleAnswers = response.data;
          })
    },

    updateSettings(newSettings) {
      this.settings = newSettings;
    },

    handleKeyPress(event) {
      if (this.settings.submitEnter && event.key === 'Enter') {
        this.selectAutocompleteOption(this.selectedOptionIndex);
      } else if (this.settings.submitRightArrow && event.key === 'ArrowRight') {
        this.selectAutocompleteOption(this.selectedOptionIndex);
      } else if (this.settings.submitTab && event.key === 'Tab') {
        this.selectAutocompleteOption(this.selectedOptionIndex);
      }
    },

    updateBeatmap(encodedBeatmapId) {
      this.$refs.beatmapRenderer.updateBeatmap(encodedBeatmapId);
    },

    updateBeatmapBackground(encodedBeatmapSetId) {
      this.$refs.beatmapRenderer.updateBackground(encodedBeatmapSetId);
    },

    resetCurrentTime() {
      this.$refs.beatmapRenderer.resetCurrentTime();
    },

    // Draggable leaderboard methods
    startDrag(event) {
      this.isDragging = true;
      this.dragStartX = event.clientX;
      this.dragStartY = event.clientY;
      this.dragOffsetX = this.dragStartX - this.leaderboardX;
      this.dragOffsetY = this.dragStartY - this.leaderboardY;
      document.addEventListener('mousemove', this.onDrag);
      document.addEventListener('mouseup', this.stopDrag);
      event.preventDefault();
    },

    onDrag(event) {
      if (!this.isDragging) return;
      
      // Calculate the new position based on mouse movement from the start position
      const deltaX = event.clientX - this.dragStartX;
      const deltaY = event.clientY - this.dragStartY;
      
      const newX = this.leaderboardX + deltaX;
      const newY = this.leaderboardY + deltaY;
      
      // Get leaderboard dimensions (approximate values)
      const leaderboardWidth = 320; // 28vh converted to approximate pixels
      const leaderboardHeight = 400; // Approximate height based on content
      
      // Constrain to viewport bounds with padding
      const padding = 20; // Keep some padding from screen edges
      const statusBarHeight = 80; // Account for status bar at the top
      const maxX = window.innerWidth - leaderboardWidth - padding;
      const maxY = window.innerHeight - leaderboardHeight - padding;
      
      // Ensure leaderboard stays within viewport, accounting for status bar
      this.leaderboardX = Math.max(padding, Math.min(newX, maxX));
      this.leaderboardY = Math.max(statusBarHeight + padding, Math.min(newY, maxY));
      
      // Update the start position for the next frame
      this.dragStartX = event.clientX;
      this.dragStartY = event.clientY;
    },

    stopDrag() {
      this.isDragging = false;
      document.removeEventListener('mousemove', this.onDrag);
      document.removeEventListener('mouseup', this.stopDrag);
      
      // Ensure leaderboard is still within bounds after drag ends
      this.ensureLeaderboardInBounds();
      
      // Save the final position
      this.saveLeaderboardPosition();
    },

    ensureLeaderboardInBounds() {
      const leaderboardWidth = 320;
      const leaderboardHeight = 400;
      const padding = 20;
      
      const maxX = window.innerWidth - leaderboardWidth - padding;
      const maxY = window.innerHeight - leaderboardHeight - padding;
      
      // If leaderboard is outside bounds, reset to safe position
      if (this.leaderboardX < padding || this.leaderboardX > maxX || 
          this.leaderboardY < padding || this.leaderboardY > maxY) {
        this.leaderboardX = padding;
        this.leaderboardY = padding;
      }
      
      // Save the position after ensuring it's in bounds
      this.saveLeaderboardPosition();
    },

    saveLeaderboardPosition() {
      const position = {
        x: this.leaderboardX,
        y: this.leaderboardY
      };
      localStorage.setItem('leaderboardPosition', JSON.stringify(position));
    },

    loadLeaderboardPosition() {
      try {
        const savedPosition = localStorage.getItem('leaderboardPosition');
        if (savedPosition) {
          const position = JSON.parse(savedPosition);
          // Validate the saved position is reasonable
          if (typeof position.x === 'number' && typeof position.y === 'number' &&
              position.x >= 0 && position.y >= 0 && 
              position.x < window.innerWidth && position.y < window.innerHeight) {
            return position;
          }
        }
      } catch (error) {
        console.warn('Failed to load leaderboard position:', error);
      }
      
      // Return default position if no valid saved position
      return { x: 20, y: 50 };
    },

    handleWindowResize() {
      // Ensure leaderboard is in bounds after resize
      this.ensureLeaderboardInBounds();
      
      // Save the adjusted position
      this.saveLeaderboardPosition();
    },
  },

  async mounted() {
    this.gameId = this.$route.params.gameId;
    var isPrivate = this.$route.params.private;
    var roompw = localStorage.getItem("roompw");

    if (isPrivate === "true" && roompw === null) {
      this.password = prompt("Please enter the password to join the game.");
      localStorage.setItem("roompw", this.password);
    }

    try{
      await this.joinGame(roompw, false);
    } catch (e) {
      this.$router.push('/');
    }

    // Add window resize listener to keep leaderboard in bounds
    window.addEventListener('resize', this.handleWindowResize);
  },

  beforeUnmount() {
    window.removeEventListener('beforeunload', this.requestLeaveRoom);
    window.removeEventListener('resize', this.handleWindowResize);

    clearInterval(this.countdownInterval)
    clearInterval(this.inactivityTimer)
    
    // Clean up drag event listeners
    document.removeEventListener('mousemove', this.onDrag);
    document.removeEventListener('mouseup', this.stopDrag);
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
          // Ensure audio is loaded before playing
          audioPlayer.load();
          if (newVal) {
            if (this.game.displayMode.includes('PATTERN') && this.isGuessing
            || (!this.game.displayMode.includes('PATTERN') && !this.game.displayMode.includes('AUDIO') && this.isGuessing) ) {
              audioPlayer.volume = 0;
            } else {
              // Handle autoplay promise
              const playPromise = audioPlayer.play();
              if (playPromise !== undefined) {
                playPromise.catch(error => {
                  // Autoplay was prevented, show a message to the user
                  this.messages.push({
                    systemMessage: true,
                    content: "Click anywhere on the page to enable audio playback.",
                    timestamp: Date.now(),
                  });
                  // Add click listener to enable audio
                  const enableAudio = () => {
                    audioPlayer.play();
                    document.removeEventListener('click', enableAudio);
                  };
                  document.addEventListener('click', enableAudio);
                });
              }
            }
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
      let beatmapRenderer = this.$refs.beatmapRenderer;
      if (beatmapRenderer) {
        beatmapRenderer.setVolume(this.beatmapVolume / 100 * 0.3);
      }

      if (this.isGuessing && this.game.displayMode.includes('PATTERN')) {
        return;
      }

      let audioPlayer = this.$refs.audio;

      if (audioPlayer) {
        this.volume = newVal;
        audioPlayer.volume = newVal / 100;
        localStorage.setItem("volume", newVal);
      }
    },

    beatmapVolume(newVal) {
      let beatmapRenderer = this.$refs.beatmapRenderer;
      if (beatmapRenderer) {
        beatmapRenderer.setVolume(newVal / 100 * 0.3);
        localStorage.setItem("beatmapVolume", newVal);
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
            <div class="game-info-container">
              <div class="game-name icon-container">{{ this.game.name }}</div>
              <div class="game-details">
                <div class="game-host-info">
                  <font-awesome-icon :icon="['fas', 'crown']" class="host-icon" />
                  <span class="host-text">{{ this.game.owner.username }}</span>
                </div>
                <div class="game-players-info">
                  <font-awesome-icon :icon="['fas', 'users']" class="players-icon" />
                  <span class="players-text">{{ players.length }} players</span>
                </div>
              </div>
              <div v-if="!this.game.ranked" class="game-room-info-unranked">Unranked</div>
            </div>
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
            <div class="icon-text-container">
              <div class="icon-container">
                <div class="gameroom-difficulty">{{ this.game.gameMode }}</div>
              </div>
              <div class="info-text">Mode</div>
            </div>
            <div class="icon-text-container">
              <div class="icon-container">
                <div class="gameroom-difficulty">{{ this.game.guessMode }}</div>
              </div>
              <div class="info-text">Guess</div>
            </div>
            <div class="icon-text-container">
              <div class="icon-container">{{ this.game.questionIndex }}/{{ this.game.totalQuestions }}</div>
              <div class="info-text">Maps</div>
            </div>
          </div>
        </div>

        <RoomSettingsModal actionType="update" v-if="this.game && isRoomSettingsModalOpen" v-on:toggleRoomSettingsModal="toggleRoomSettingsModal" :game="game" @close-modal="isRoomSettingsModalOpen = false" />

        <div v-if="this.game">
          <div class="question-image-container">
            <div class="question-image-cover" v-if="!toggleImage || ((!this.game.displayMode.includes('BACKGROUND') && !this.game.displayMode.includes('PATTERN')) && isGuessing)"></div>

            <div class="image-wrapper">
              <BeatmapRenderer v-if="this.game && (this.game.displayMode.includes('PATTERN'))" class="beatmap-renderer" ref="beatmapRenderer"/>
              <img :src="'/image/' + this.imageSourceBase64" alt="" class="question-image" v-else>
              <img :src="icon_report" alt="" class="icon-report" v-on:click="toggleReportDropdown" v-if="isPlaying"/>
              <font-awesome-icon :icon="['fas', 'volume-high']" class="icon-showSettings" @click="showUserSettings = !showUserSettings"/>
              <div class="settings-container" v-if="showUserSettings">
                <div class="volume-control">
                  <font-awesome-icon :icon="['fas', 'volume-high']" class="volume-icon"/>
                  <input type="range" min="0" max="100" value="50" class="slider" id="volume" v-model="volume" :disabled="!toggleAudio"/>
                </div>
                <div class="volume-control">
                  <font-awesome-icon :icon="['fas', 'play']" class="volume-icon"/>
                  <input type="range" min="0" max="100" value="50" class="slider" id="beatmapVolume" v-model="beatmapVolume"/>
                </div>
                <div class="settings-toggles">
                  <font-awesome-icon :icon="['fas', 'eye']" v-if="toggleImage" v-on:click="toggleImage = !toggleImage"/>
                  <font-awesome-icon :icon="['fas', 'eye-slash']" v-else v-on:click="toggleImage = !toggleImage"/>
                  <font-awesome-icon :icon="['fas', 'volume-high']" v-if="toggleAudio" v-on:click="toggleAudioVolume"/>
                  <font-awesome-icon :icon="['fas', 'volume-xmark']" v-else v-on:click="toggleAudioVolume"/>
                </div>
              </div>
              <div v-if="showReportDropdown" class="report-dropdown">
                <p v-on:click="reportBeatmap('MISSING_RESOURCE')">missing mp3/bg</p>
                <p v-on:click="reportBeatmap('BG_SPOILER')">bg spoils title</p>
              </div>
            </div>
          </div>

          <div class="audio-input-container">
            <audio hidden ref="audio" v-if="this.game" id="audioPlayer" preload="auto">
              <source :src="'/audio/' + this.audioSourceBase64" type="audio/mpeg">
              Your browser does not support the audio element.
            </audio>
          </div>

          <div class="input-container">
            <div class="input-icon-wrapper">
              <input type="text"
                     :class="{ answerSubmitted: answerSubmitted, highlight: (answers[me.id] && answers[me.id].correct && !answers[me.id].aliasCorrect && !isGuessing), 'highlight-secondary': (answers[me.id] && answers[me.id].aliasCorrect && !isGuessing) }"
                     v-model="answerInput" :placeholder="answerInputPlaceHolder"
                     :readonly="!isGuessing"
                     v-on:keydown.up.prevent="selectedOptionIndex > 0 ? selectedOptionIndex-- : selectedOptionIndex = autocompleteOptions.length - 1"
                     v-on:keydown.down.prevent="selectedOptionIndex < autocompleteOptions.length - 1 ? selectedOptionIndex++ : selectedOptionIndex = 0"
                     @keydown="handleKeyPress"
                     v-on:focus="inputFocused = true" class="answer-input">
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

          <BeatmapInfo
              v-if="currentBeatmap && !isGuessing"
              :currentBeatmap="currentBeatmap"
              :isGuessing="isGuessing"
              :getBeatmapUrl="getBeatmapUrl"
              :gameMode="this.game.gameMode"
              :guessMode="this.game.guessMode"
              :difficultyClass="difficultyClass"
              :formatDate="formatDate"
              :answers="answers"
              :me="me"
              class="beatmap-info"
          />

          <div class="leaderboard" 
               :style="{ left: leaderboardX + 'px', top: leaderboardY + 'px' }"
               :class="{ 'dragging': isDragging }">
            <div class="leaderboard-header" @mousedown="startDrag">
              <h2>Leaderboard</h2>
              <div class="drag-handle">
                <font-awesome-icon :icon="['fas', 'grip-vertical']" />
              </div>
            </div>
            <div v-for="(player, index) in sortedPlayers" :key="player.id" class="leaderboard-player-info" :class="{ 'leaderboard-player-highlight': (answers[player.id] && answers[player.id].correct && !answers[player.id].aliasCorrect && !isGuessing), 'highlight-secondary': (answers[player.id] && answers[player.id].correct && answers[player.id].aliasCorrect && !isGuessing) }" @click.stop="showPlayerInfoModal = true; userpageId = player.id">
              <div class="leaderboard-player-rank">#{{ index + 1 }}</div>
              <div class="leaderboard-player-name">{{ player.username }}</div>
              <div class="leaderboard-player-points">{{ formatDecimal(player.totalPoints ? player.totalPoints : 0) }} <span class="leaderboard-player-totalguess">({{ player.totalGuess ? player.totalGuess : 0 }})</span></div>
            </div>
          </div>
        </div>

        <div class="players-container" v-if="!compactViewMode" :class="{'players-container-overflow': players.length > 8}">

          <PlayerBox
              v-for="player in players"
              :key="player.id"
              :player="player"
              :answers="answers"
              :isGuessing="isGuessing"
              :me="me"
              :game="game"
              @showUserPage="showPlayerInfoModal = true; userpageId = player.id;"
          />
        </div>

        <div class="players-container-compact" v-if="compactViewMode">
          <PlayerBoxCompact
              v-for="player in players"
              :key="player.id"
              :player="player"
              :answers="answers"
              :isGuessing="isGuessing"
              :me="me"
              :game="game"
              @showUserPage="showPlayerInfoModal = true; userpageId = player.id"
          />
        </div>
      </div>
      <ChatSection :messages="messages" :isConnected="isConnected" :formatTimestamp="formatTimestamp" @send-message="sendChatMessage" />

    </div>
    <div v-if="isLoading" class="loading-indicator">Loading...</div>
    <div v-if="showPlayerInfoModal" class="modal-overlay" @click.stop="showPlayerInfoModal = false">
      <UserPage :playerId="userpageId"></UserPage>
    </div>
    <IngameSettingsPage @update-settings="updateSettings"/>
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
  background: linear-gradient(135deg, rgba(0, 0, 0, 0.9), rgba(0, 0, 0, 0.7));
  backdrop-filter: blur(10px);
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
  background: linear-gradient(135deg, rgba(0, 0, 0, 0.9), rgba(0, 0, 0, 0.7));
  backdrop-filter: blur(10px);
  color: white;
  font-size: 3em;
  z-index: 999;
}

.game-container {
  display: flex;
  min-height: 100vh;
  padding: 20px;
  gap: 20px;
}

.game-content {
  width: 82vw;
  max-width: 82vw;
  position: relative;
  flex: 3;
  background: linear-gradient(135deg, var(--color-secondary) 0%, rgba(255, 255, 255, 0.05) 100%);
  border-radius: 20px;
  padding: 25px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  box-shadow: 0 8px 25px rgba(0, 0, 0, 0.15);
}

.game-room-info {
  display: flex;
  justify-content: space-between;
  padding: 20px;
  height: 6vh;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.1), rgba(255, 255, 255, 0.05));
  border-radius: 15px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.1);
  margin-bottom: 20px;
}

.game-room-info-middle {
  display: flex;
  justify-content: center;
  align-items: center;
}

.start-game-button {
  background: linear-gradient(135deg, rgba(96, 165, 250, 0.8), rgba(59, 130, 246, 0.8));
  border: 1px solid rgba(255, 255, 255, 0.2);
  color: white;
  padding: 12px 24px;
  text-align: center;
  text-decoration: none;
  display: inline-block;
  font-size: 16px;
  font-weight: 600;
  margin: 4px 2px;
  cursor: pointer;
  border-radius: 10px;
  transition: all 0.3s ease;
  box-shadow: 0 4px 15px rgba(96, 165, 250, 0.3);
}

.start-game-button:hover {
  background: linear-gradient(135deg, rgba(59, 130, 246, 0.9), rgba(37, 99, 235, 0.9));
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(96, 165, 250, 0.4);
  border-color: rgba(96, 165, 250, 0.4);
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
  min-width: 200px;
}

.icon-container {
  font-size: 1.2em;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.1), rgba(255, 255, 255, 0.05));
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 10px;
  padding: 8px 12px;
  display: flex;
  gap: 5px;
  justify-content: center;
  align-items: center;
  transition: all 0.3s ease;
}

.icon-container:hover {
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.15), rgba(255, 255, 255, 0.08));
  border-color: rgba(255, 255, 255, 0.3);
  transform: translateY(-1px);
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.1);
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

.game-info-container {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.game-room-info-left .game-name {
  font-weight: bold;
  font-size: 1.2em;
}

.game-details {
  display: flex;
  flex-direction: column;
  gap: 4px;
  margin-left: 8px;
}

.game-host-info, .game-players-info {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 0.9em;
  color: var(--color-text);
  opacity: 0.8;
  transition: all 0.3s ease;
}

.game-host-info:hover, .game-players-info:hover {
  opacity: 1;
  transform: translateX(2px);
}

.host-icon {
  color: #fbbf24;
  font-size: 0.8em;
}

.players-icon {
  color: #60a5fa;
  font-size: 0.8em;
}

.host-text, .players-text {
  font-weight: 500;
}

.game-room-info-left .game-name {
  font-weight: bold;
  font-size: 1.2em;
}

.game-room-info-unranked {
  background: linear-gradient(135deg, rgba(239, 68, 68, 0.8), rgba(220, 38, 38, 0.8));
  border: 1px solid rgba(239, 68, 68, 0.4);
  border-radius: 8px;
  padding: 6px 12px;
  font-weight: bold;
  font-size: 1.2em;
  color: white;
  box-shadow: 0 2px 8px rgba(239, 68, 68, 0.3);
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
  background: linear-gradient(135deg, rgba(239, 68, 68, 0.8), rgba(220, 38, 38, 0.8));
  border: 1px solid rgba(255, 255, 255, 0.2);
  color: white;
  cursor: pointer;
  padding: 8px 12px;
  margin-right: 10px;
  border-radius: 8px;
  transition: all 0.3s ease;
  box-shadow: 0 2px 8px rgba(239, 68, 68, 0.3);
}

.leave-room-button:hover {
  background: linear-gradient(135deg, rgba(220, 38, 38, 0.9), rgba(185, 28, 28, 0.9));
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(239, 68, 68, 0.4);
  border-color: rgba(239, 68, 68, 0.4);
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
  gap: 10px;
}

.players-container-overflow {
  justify-content: flex-start;
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

.player-totalguess {
  font-size: 0.8em;
  color: #999;
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

.player-answer-submitted {
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
  border: 3px solid rgba(255, 255, 255, 0.2);
  border-radius: 15px;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.05), rgba(255, 255, 255, 0.02));
  display: flex;
  justify-content: center;
  align-items: center;
  text-align: center;
  box-shadow: 0 8px 25px rgba(0, 0, 0, 0.15);
  transition: all 0.3s ease;
}

.question-image:hover {
  border-color: rgba(255, 255, 255, 0.3);
  box-shadow: 0 12px 35px rgba(0, 0, 0, 0.2);
  transform: translateY(-2px);
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

.beatmap-renderer {
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
  font-family: 'Exo 2', 'Sen', serif;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.1), rgba(255, 255, 255, 0.05));
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 10px;
  color: var(--color-text);
  transition: all 0.3s ease;
  outline: none;
}

.answer-input:focus {
  border-color: rgba(96, 165, 250, 0.5);
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.15), rgba(255, 255, 255, 0.08));
  box-shadow: 0 4px 15px rgba(96, 165, 250, 0.2);
  transform: translateY(-1px);
}

.highlight {
  background-color: rgba(22, 225, 49, 0.33);
  border: 2px solid rgba(22, 225, 49, 0.8);
}

.highlight-secondary {
  background-color: rgba(227, 220, 47, 0.52);
  border: 2px solid rgba(235, 243, 76, 0.8);
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
  font-family: 'Exo 2', 'Sen', serif;
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

.report-dropdown {
  background-color: rgba(0, 0, 0, 0.8);
  position: absolute;
  right: 8px;
  box-shadow: 0px 8px 16px 0px rgba(0,0,0,0.2);
  z-index: 4;
}

.report-dropdown p {
  color: #ffffff;
  padding: 12px 16px;
  text-decoration: none;
  display: block;
}

.report-dropdown p:hover {
  background-color: #f1f1f1;
}

.report-dropdown {
  display: block;
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
  padding: 4px;
  width: 24px;
  height: 24px;
  cursor: pointer;
  background-color: var(--color-secondary);
  border-radius: 5px;
  border: 1px solid var(--color-text);
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
  position: fixed;
  top: 7vh;
  left: 20px;
  padding: 0;
  margin-top: 10px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.1), rgba(255, 255, 255, 0.05));
  border-radius: 15px;
  max-width: 28vh;
  min-width: 28vh;
  box-shadow: 0 8px 25px rgba(0, 0, 0, 0.15);
  z-index: 100;
  user-select: none;
  transition: box-shadow 0.3s ease;
}

.leaderboard.dragging {
  box-shadow: 0 12px 35px rgba(0, 0, 0, 0.25);
  cursor: grabbing;
}

.leaderboard-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 15px 20px;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.15), rgba(255, 255, 255, 0.08));
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 15px 15px 0 0;
  cursor: grab;
  transition: all 0.3s ease;
}

.leaderboard-header:hover {
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.2), rgba(255, 255, 255, 0.12));
}

.leaderboard-header:active {
  cursor: grabbing;
}

.drag-handle {
  color: rgba(255, 255, 255, 0.6);
  font-size: 0.9em;
  transition: color 0.3s ease;
}

.leaderboard-header:hover .drag-handle {
  color: rgba(255, 255, 255, 0.8);
}

.leaderboard h2 {
  text-align: center;
  margin: 0;
  font-size: 1.3em;
  font-weight: 700;
  color: var(--color-text);
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.3);
  letter-spacing: 0.5px;
}

.leaderboard-player-info {
  cursor: pointer;
  display: flex;
  justify-content: space-between;
  padding: 8px 12px;
  margin: 8px 20px;
  align-items: center;
  box-sizing: border-box;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.05), rgba(255, 255, 255, 0.02));
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 10px;
  transition: all 0.3s ease;
}

.leaderboard-player-info:hover {
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.1), rgba(255, 255, 255, 0.05));
  border-color: rgba(255, 255, 255, 0.2);
  transform: translateY(-1px);
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.1);
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

.players-container-compact {
  flex-direction: row;
  justify-content: center;
  overflow-x: auto;
  margin-top: 16px;
  display: grid;
  grid-template-columns: 1fr 1fr 1fr 1fr;
  grid-auto-rows: 4em;
  grid-auto-flow: row;
  overflow-y: auto;
  height: 35vh;
}

.volume-control {
  display: flex;
  flex-direction: row;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
}

.volume-icon {
  width: 16px;
  height: 16px;
  color: var(--color-text);
}

.volume-control label {
  color: var(--color-text);
  font-size: 0.9em;
}

@media (max-width: 768px) {
  .game-container {
    flex-direction: row;
    overflow-x: hidden;
    overflow-y: auto;
    height: 100vh;
  }

  .game-content {
    width: 100%;
    max-width: 100%;
  }

  .players-container, .players-container-compact {
    flex-direction: row;
    height: auto;
    overflow-x: visible;
  }

  .leaderboard {
    display: none;
    position: static;
    width: 100%;
    max-width: 100%;
    margin-top: 10px;
  }

  .question-image {
    width: 100%;
    height: auto;
  }

  .question-image-container {
    width: 100%;
    height: auto;
  }

  .input-icon-wrapper {
    width: 100%;
  }

  .answer-input {
    width: 100%;
  }

  .game-room-info {
    flex-direction: row;
    align-items: flex-start;
  }

  .game-room-info-left, .game-room-info-right {
    flex-direction: row;
    align-items: flex-start;
    gap: 10px;
  }

  .game-room-info-middle {
    margin-top: 10px;
  }

  .beatmap-info {
    display: none;
  }
}
</style>