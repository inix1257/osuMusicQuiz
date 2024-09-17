<template>
  <div @click="joinGame(gameroom.uuid)" class="gameroom">
    <div class="owner-info">
      <img :src="gameroom.owner.avatar_url" :alt="gameroom.owner.username" class="avatar owner-avatar"
           :title="gameroom.owner.username">
            <div class="gameroom-hostname">{{ gameroom.owner.username }}</div>
    </div>
    <div class="gameroom-info">
      <div class="gameroom-header">
        <font-awesome-icon :icon="['fas', 'lock']" v-if="gameroom.private"/>
        <h2 class="gameroom-name">{{ gameroom.name }}</h2>
      </div>
      <div class="gameroom-details">
        <font-awesome-icon :icon="['fas', modeIcon]" class="info-icon"/>
        <div class="gameroom-difficulty-container">
          <div v-for="diff in gameroom.difficulty" :key="diff" :class="difficultyClass(diff)" class="gameroom-difficulty">
            {{ difficultyName(diff) }}
          </div>
        </div>
        <div>Maps: {{ gameroom.questionIndex }} / {{ gameroom.totalQuestions }}</div>
        <div>
          ({{ gameroom.startYear }} ~ {{ gameroom.endYear }})
        </div>
        <div v-if="gameroom.poolMode && gameroom.poolMode !== 'DEFAULT'">{{ poolModeFormat(gameroom.poolMode) }}</div>
        <div :class="gameroom.playing ? 'playing' : 'idle'">{{ gameroom.playing ? 'Playing' : 'Idle' }}</div>
      </div>
      <div class="player-info-row">
        <div v-for="(player) in filteredPlayers(this.gameroom.players)" :key="player.id" class="player-info">
          <img :src="player.avatar_url" :alt="player.username" class="avatar" :title="player.username">
          <div class="player-username">
            {{ player.username }}
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import {FontAwesomeIcon} from "@fortawesome/vue-fontawesome";
import {useUserStore} from "@/stores/userStore";

export default {
  name: 'GameroomPage',
  components: {FontAwesomeIcon},
  props: {
    gameroom: {
      type: Object,
      required: true
    },
    me: {
      type: Object,
      required: true
    }
  },
  methods: {
    joinGame(gameId) {
      // Check login status before joining the game
      if (!useUserStore().getMe().id) {
        alert('You need to login to join the game.');
        return;
      }

      if (!confirm('Do you want to join ' + this.gameroom.name + '?')){
        return;
      }

      this.$router.push({
        name: 'GamePage',
        params: {
          gameId: gameId,
          private: this.gameroom.private
        }
      });
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

    poolModeFormat(poolMode) {
      switch (poolMode) {
        case 'TOUHOU':
          return 'Touhou Only';
        case 'VOCALOID':
          return 'Vocaloid Only';
      }
    },

    filteredPlayers(players) {
      return players.filter(player => player.id !== this.gameroom.owner.id).slice(0, 16);
    }
  },

  computed: {
    modeIcon() {
      switch (this.gameroom.mode) {
        case 'DEFAULT':
          return 'music';
        case 'PATTERN':
          return 'circle';
        default:
          return '';
      }
    },
  }
};
</script>

<style scoped>
.gameroom {
  display: flex;
  background-color: var(--color-gameroom);
  border-radius: 10px;
  border: 3px solid var(--color-gameroom);
  box-shadow: 0px 10px 10px rgba(0, 0, 0, 0.1);
  padding: 20px;
  margin-bottom: 20px;
  cursor: pointer;
  width: 95%;
}

.gameroom-header {
  height: 50%;
  display: flex;
  align-items: baseline;
  gap: 10px;
}

.gameroom-hostname {
  border: 2px solid #849ee5;
  border-radius: 8px;
  font-size: 1.2em;
}

.gameroom-difficulty {
  font-size: 1.0em;
  font-weight: bold;
}

.gameroom-difficulty-container {
  background-color: var(--color-disabled);
  border-radius: 8px;
  padding-left: 8px;
  padding-right: 8px;
  display: flex;
  gap: 6px;
}

.owner-info {
  justify-content: center;
  align-items: center;
}

.gameroom-info {
  display: flex;
  flex-direction: column;
  text-align: left;
  margin-left: 16px;
  flex-grow: 1;
  gap: 2px;
  width: 95%;
}

.gameroom-name {
  font-size: 1.8em;
}

.gameroom-details {
  display: flex;
  gap: 30px;
  margin-bottom: 10px;
}

.player-info-row {
  display: flex;
  flex-wrap: wrap;
  padding: 10px;
  gap: 10px;
}

.avatar {
  width: 25px;
  height: 25px;
  border-radius: 50%;
  margin-right: 10px;
  transition: 0.3s;
  box-shadow: 0px 10px 10px rgba(0, 0, 0, 0.1);
  border: 3px solid #849ee5;
}

.owner-avatar {
  width: 100px;
  height: 100px;
  border-radius: 50%;
  margin-bottom: 4px;
}

.player-info {
  display: flex;
  align-items: center;
}

.player-username {
  margin-top: 0;
  font-weight: normal;
  font-size: 1.0em;
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

.playing {
  color: #dc4040;
}

.idle {
  color: #8585d7;
}
</style>