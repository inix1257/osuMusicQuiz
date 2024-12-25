<template>
  <div :class="playerBoxClass" @click.stop="showPlayerInfoModal">
    <div class="player-box-hover"></div>
    <div class="dropdown-menu-div">
      <font-awesome-icon :icon="['fas', 'caret-down']" v-if="isOwner && !isCurrentPlayer"
                         @click.stop="openDropdown(player)" class="user-dropdown-button"/>
      <div v-if="showDropdown && dropdownUser.id === player.id" class="dropdown-menu">
        <p @click.stop="transferHost(player)">Host</p>
        <p @click.stop="kickPlayer(player)">Kick</p>
        <p @click.stop="banPlayer(player)">Ban</p>
        <p @click.stop="showDropdown = false">Cancel</p>
      </div>
    </div>

    <img :src="player.avatar_url" alt="Player's avatar" class="player-avatar-compact">
    <div class="player-info-compact">
      <div class="player-header-compact">
        <p class="player-username-compact">{{ player.username }}</p>
        <p class="player-points-compact">{{ formatDecimal(player.totalPoints ? player.totalPoints : 0) }}
          <span class="player-totalguess">({{ player.totalGuess ? player.totalGuess : 0 }})</span></p>
      </div>
    </div>
  </div>
</template>

<script>
import apiService from "@/api/apiService";

export default {
  name: 'PlayerBoxCompact',
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
        'player-answer-submitted': this.player.answerSubmitted,
        'player-box-compact': true,
        'highlight': this.answers[this.player.id] && this.answers[this.player.id].correct && !this.answers[this.player.id].aliasCorrect && !this.isGuessing,
        'highlight-secondary': this.answers[this.player.id] && this.answers[this.player.id].correct && this.answers[this.player.id].aliasCorrect && !this.isGuessing
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
.player-box-compact {
  position: relative;
  display: flex;
  flex-direction: row;
  align-items: center;
  justify-content: flex-start;
  width: 95%;
  height: 3em;
  margin: 5px;
  text-align: left;
  border-radius: 10px;
  background-color: rgba(231, 239, 255, 0.2);
  border: 2px solid transparent;
  overflow: visible;
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

.player-box-compact:hover .player-box-hover {
  opacity: 1;
}

.player-box-compact:hover {
  border: 2px solid #ef3838;
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
  align-content: center;
  text-align: center;
  text-overflow: ellipsis;
  height: 1.5em;
  max-width: 25vw;
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
  width: 2.5em;
  height: 2.5em;
  border: 2px solid #000000;
  border-radius: 50%;
  margin-right: 10px;
}

.player-username-compact {
  font-weight: bold;
  font-size: 1.2em;
  margin: 0;
}

.player-points-compact {
  font-size: 1.2em;
  color: var(--color-text);
  margin: 0;
}

.highlight {
  background-color: rgba(22, 225, 49, 0.33);
  border: 2px solid rgba(22, 225, 49, 0.8);
}

.highlight-secondary {
  background-color: rgba(227, 220, 47, 0.52);
  border: 2px solid rgba(235, 243, 76, 0.8);
}

.user-dropdown-button {
  width: 20px;
  height: 20px;
  cursor: pointer;
  border-radius: 4px;
  color: #ffffff;
  background-color: rgba(0, 0, 0, 1);
}

.dropdown-menu-div {
  position: absolute;
  right: 0.4em;
  top: 50%;
  transform: translateY(-50%);
}

.dropdown-menu {
  background-color: rgba(0, 0, 0, 0.8);
  position: absolute;
  left: 2px;
  box-shadow: 0px 8px 16px 0px rgba(0, 0, 0, 0.2);
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
</style>