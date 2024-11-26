<template>
  <div :class="playerBoxClass">
    <div class="player-box-hover"></div>
    <font-awesome-icon :icon="['fas', 'caret-down']" v-if="isOwner && !isCurrentPlayer"
                       @click.stop="openDropdown(player)" class="user-dropdown-button"/>
    <div v-if="showDropdown && dropdownUser.id === player.id" class="dropdown-menu">
      <p @click.stop="transferHost(player)">Host</p>
      <p @click.stop="kickPlayer(player)">Kick</p>
      <p @click.stop="banPlayer(player)">Ban</p>
      <p @click.stop="showDropdown = false">Cancel</p>
    </div>
    <font-awesome-icon v-if="isGameOwner" class="player-hosticon" :icon="['fas', 'crown']"/>
    <img :src="player.avatar_url" alt="Player's avatar" class="player-avatar"
         @click.stop="showPlayerInfoModal">
    <div class="player-username-div">
      <p class="player-username">{{ player.username }}</p>
    </div>
    <p class="player-title">{{
        player.current_title_achievement ? player.current_title_achievement.title_name : ""
      }}</p>
    <p class="player-points">{{ formatDecimal(player.totalPoints ? player.totalPoints : 0) }}
      <span class="player-totalguess">({{ player.totalGuess ? player.totalGuess : 0 }})</span></p>
    <div v-if="!isGuessing && answers[player.id] && answers[player.id].answer" class="player-answer">
      {{ answers[player.id].answer }}
    </div>
  </div>
</template>

<script>
import apiService from "@/api/apiService";

export default {
  name: 'PlayerBox',
  props: {
    player: Object,
    answers: Object,
    isGuessing: Boolean,
    me: Object,
    game: Object,
  },
  data() {
    return {
      gameId: this.game.id,
      showDropdown: false,
      dropdownUser: {},
      userpageId: null
    }
  },
  methods: {
    showPlayerInfoModal() {
      console.log('showing player info modal');
      this.$emit('showUserPage', this.userpageId);
    },
    kickPlayer(player) {
      this.showDropdown = false;

      if (!confirm("Do you want to kick " + player.username + "?")) {
        return;
      }

      apiService.post('/api/kickPlayer', {
        gameId: this.gameId,
        targetUserId: player.id
      })
    },

    banPlayer(player) {
      this.showDropdown = false;

      if (!confirm("Do you want to ban " + player.username + " from this lobby?\n(This user will not be able to join this lobby again)")) {
        return;
      }

      apiService.post('/api/banPlayer', {
        gameId: this.gameId,
        targetUserId: player.id
      })
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
          .catch(() => {
            alert("An error occurred while transferring the host. Please try again later.")
          });
    },

    openDropdown(player) {
      this.dropdownUser = player;
      this.showDropdown = !this.showDropdown;
    },
    formatDecimal(value) {
      if (value === 0) {
        return '0';
      }
      return value.toFixed(2);
    }
  },
  computed: {
    playerBoxClass() {
      return {
        'highlight': this.answers[this.player.id] && this.answers[this.player.id].correct && !this.answers[this.player.id].aliasCorrect && !this.isGuessing,
        'highlight-secondary': this.answers[this.player.id] && this.answers[this.player.id].correct && this.answers[this.player.id].aliasCorrect && !this.isGuessing,
        'player-answer-submitted': this.player.answerSubmitted,
        'player-box': true
      };
    },
    isOwner() {
      return this.me && this.me.id === this.game.owner.id;
    },
    isCurrentPlayer() {
      return this.me && this.me.id === this.player.id;
    },
    isGameOwner() {
      return this.game.owner.id === this.player.id;
    }
  }
}
</script>

<style scoped>
.player-box {
  position: relative;
  padding: 10px;
  text-align: center;
  width: 150px;
  max-width: 150px;
  min-width: 150px;
  border-radius: 10px;
  background-color: rgba(231, 239, 255, 0.2);
  border: 2px solid transparent;
  overflow: visible;
  box-shadow: 0 0 4px rgba(0, 0, 0, 0.3);
}

.player-box-hover {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  border-radius: 10px;
  background-color: rgba(231, 239, 255, 0.2);
  opacity: 0;
  transition: opacity 0.1s;
  pointer-events: none;
}

.player-box:hover .player-box-hover {
  opacity: 1;
}

.player-box:hover {
  border: 2px solid #ef3838;
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

.player-username-div {
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
}

.player-username{
  font-weight: bold;
  font-size: 1.4em;
  overflow: hidden;
  margin-top: 4px;
  margin-bottom: 2px;
}

.player-hosticon {
  position: absolute;
  top: 16px;
  left: 16px;
  width: 24px;
  height: 24px;
  color: #f1c40f;
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

.player-answer-submitted {
  position: relative;
  background-color: rgba(255, 255, 255, 0.2);
}

.player-answer-submitted::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: rgba(0, 0, 0, 0.3);
  pointer-events: none;
  border-radius: inherit;
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
  padding: 0.4em 1em;
  text-decoration: none;
  display: block;
}

.dropdown-menu p:hover {
  background-color: #f1f1f1;
}

.showDropdown .dropdown-menu {
  display: block;
}

.highlight {
  background-color: rgba(22, 225, 49, 0.33);
  border: 2px solid rgba(22, 225, 49, 0.8);
}

.highlight-secondary {
  background-color: rgba(227, 220, 47, 0.52);
  border: 2px solid rgba(235, 243, 76, 0.8);
}
</style>