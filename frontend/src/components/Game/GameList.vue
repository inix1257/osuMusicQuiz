<template>
  <div @click="joinGame(gameroom.uuid)" class="gameroom">
    <div class="hover-cover"></div>
    <div class="owner-info">
      <img :src="gameroom.owner.avatar_url" :alt="gameroom.owner.username" class="avatar owner-avatar"
           :title="gameroom.owner.username">
            <div class="gameroom-hostname">{{ gameroom.owner.username }}</div>
    </div>
    <div class="gameroom-info">
      <div class="gameroom-header-extrainfo">
        <font-awesome-icon :icon="['fas', 'lock']" v-if="gameroom.private"/>
        <h2 class="gameroom-unranked" v-if="!gameroom.ranked">Unranked</h2>
        <div v-if="!gameroom.displayMode.includes('AUDIO') && !gameroom.displayMode.includes('PATTERN')" class="gameroom-difficulty-container">
          <font-awesome-icon :icon="['fas', 'volume-xmark']"/>
          <strong>NO AUDIO</strong>
        </div>
        <div v-if="!gameroom.displayMode.includes('BACKGROUND') && !gameroom.displayMode.includes('PATTERN')" class="gameroom-difficulty-container">
          <font-awesome-icon :icon="['fas', 'eye-slash']"/>
          <strong>NO BG</strong>
        </div>
      </div>
      <div class="gameroom-header">
        <h2 class="gameroom-name">{{ gameroom.name }}</h2>
        <div class="gameroom-difficulty-container">
          <div :class="gameroom.playing ? 'playing' : 'idle'">{{ gameroom.playing ? 'In Progress' : 'Idle' }}</div>
        </div>
      </div>
      <div class="gameroom-details">
        <div class="gameroom-difficulty-container">
          <img :src="getModeIcon()" class="mode-icon" :title="gameroom.gameMode" :alt="gameroom.gameMode">
          {{ gameroom.gameMode }} | {{ gameroom.guessMode }}
        </div>
        <div class="gameroom-difficulty-container">
          <div v-for="diff in gameroom.difficulty" :key="diff" :class="difficultyClass(diff)" class="gameroom-difficulty">
            {{ difficultyName(diff) }}
          </div>
        </div>
        <div class="gameroom-difficulty-container">
          <div>{{ gameroom.questionIndex }} / {{ gameroom.totalQuestions }} Maps ({{ gameroom.startYear }} - {{ gameroom.endYear }})</div>
        </div>
        <div class="gameroom-difficulty-container" v-if="gameroom.poolMode && gameroom.poolMode !== 'DEFAULT'">
          <div>{{ poolModeFormat(gameroom.poolMode) }}</div>
        </div>
      </div>
      <div class="player-info-row">
        <div v-for="(player) in filteredPlayers(this.gameroom.players)" :key="player.id" class="player-info">
          <img :src="player.avatar_url" :alt="player.username" class="player-avatar" :title="player.username">
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

import icon_osu from '@/assets/osu/mode-osu.png';
import icon_taiko from '@/assets/osu/mode-taiko.png';
import icon_fruits from '@/assets/osu/mode-fruits.png';
import icon_mania from '@/assets/osu/mode-mania.png';

export default {
  name: 'GameroomPage',
  components: {FontAwesomeIcon},
  data() {
    return {
      modeIcon: '',
      icon_osu: icon_osu,
      icon_taiko: icon_taiko,
      icon_fruits: icon_fruits,
      icon_mania: icon_mania
    };
  },
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
      // Check login status before joining the game, just in case...
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
    },

    getModeIcon() {
      switch (this.gameroom.gameMode) {
        case 'STD':
          return this.icon_osu;
        case 'TAIKO':
          return this.icon_taiko;
        case 'CTB':
          return this.icon_fruits;
        case 'MANIA':
          return this.icon_mania;
      }
    }
  },

};
</script>

<style scoped>
.gameroom {
  position: relative;
  display: flex;
  background: linear-gradient(135deg, var(--color-gameroom) 0%, rgba(255, 255, 255, 0.05) 100%);
  border-radius: 15px;
  box-shadow: 0 8px 25px rgba(0, 0, 0, 0.1);
  padding: 20px;
  margin-bottom: 15px;
  cursor: pointer;
  border: 1px solid rgba(255, 255, 255, 0.1);
  transition: all 0.3s ease;
  overflow: hidden;
}

.gameroom:hover {
  transform: translateY(-3px);
  box-shadow: 0 12px 35px rgba(0, 0, 0, 0.15);
  background: linear-gradient(135deg, var(--color-gameroom) 0%, rgba(255, 255, 255, 0.08) 100%);
  border-color: rgba(96, 165, 250, 0.3);
}

.hover-cover {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  border-radius: 15px;
  background: linear-gradient(135deg, rgba(96, 165, 250, 0.1), rgba(59, 130, 246, 0.05));
  opacity: 0;
  transition: opacity 0.3s ease;
  z-index: 2;
}

.gameroom:hover .hover-cover {
  opacity: 1;
}

.gameroom-header-extrainfo {
  gap: 0.5em;
  display: flex;
  align-items: center;
}

.gameroom-header {
  display: flex;
  align-items: center;
}

.gameroom-hostname {
  background: linear-gradient(135deg, rgba(96, 165, 250, 0.15), rgba(59, 130, 246, 0.1));
  border: 1px solid rgba(96, 165, 250, 0.3);
  border-radius: 10px;
  font-size: 1.1em;
  font-weight: 600;
  padding: 8px 12px;
  color: #60a5fa;
  text-shadow: 0 1px 2px rgba(96, 165, 250, 0.3);
  transition: all 0.3s ease;
}

.gameroom:hover .gameroom-hostname {
  background: linear-gradient(135deg, rgba(96, 165, 250, 0.2), rgba(59, 130, 246, 0.15));
  border-color: rgba(96, 165, 250, 0.5);
  transform: translateY(-1px);
}

.gameroom-difficulty {
  font-size: 1.0em;
  font-weight: bold;
}

.gameroom-difficulty-container {
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.1), rgba(255, 255, 255, 0.05));
  border-radius: 8px;
  padding: 6px 10px;
  display: flex;
  gap: 6px;
  align-items: center;
  border: 1px solid rgba(255, 255, 255, 0.1);
  transition: all 0.3s ease;
  font-size: 0.9em;
  font-weight: 500;
}

.gameroom:hover .gameroom-difficulty-container {
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.15), rgba(255, 255, 255, 0.08));
  border-color: rgba(255, 255, 255, 0.2);
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
  margin: 0.2em;
  font-weight: 700;
  color: var(--color-text);
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.3);
  transition: all 0.3s ease;
}

.gameroom:hover .gameroom-name {
  color: #60a5fa;
}

.gameroom-unranked {
  color: #ef4444;
  font-size: 1.0em;
  margin: 0;
  font-weight: 600;
  text-shadow: 0 1px 2px rgba(239, 68, 68, 0.3);
  background: linear-gradient(45deg, #ef4444, #dc2626);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.gameroom-details {
  display: flex;
  gap: 1em;
  margin-bottom: 10px;
}

.player-info-row {
  display: flex;
  flex-wrap: wrap;
  padding: 10px;
  gap: 10px;
}

.owner-avatar {
  width: 100px;
  height: 100px;
  border-radius: 50%;
  margin-bottom: 8px;
  border: 3px solid rgba(96, 165, 250, 0.3);
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.2);
  transition: all 0.3s ease;
}

.gameroom:hover .owner-avatar {
  border-color: rgba(96, 165, 250, 0.6);
  transform: scale(1.05);
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.3);
}

.player-avatar {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  margin-right: 8px;
  transition: all 0.3s ease;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
  border: 2px solid rgba(255, 255, 255, 0.2);
}

.player-info:hover .player-avatar {
  border-color: rgba(96, 165, 250, 0.4);
  transform: scale(1.1);
}

.player-info {
  display: flex;
  align-items: center;
  border-radius: 10px;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.1), rgba(255, 255, 255, 0.05));
  border: 1px solid rgba(255, 255, 255, 0.2);
  padding: 6px 10px;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.1);
  transition: all 0.3s ease;
}

.player-info:hover {
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.15), rgba(255, 255, 255, 0.08));
  border-color: rgba(255, 255, 255, 0.3);
  transform: translateY(-1px);
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.15);
}

.player-username {
  margin-top: 0;
  font-weight: normal;
  font-size: 1.0em;
}

.playing {
  color: #ef4444;
  font-weight: 600;
  text-shadow: 0 1px 2px rgba(239, 68, 68, 0.3);
  background: linear-gradient(45deg, #ef4444, #dc2626);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.idle {
  color: #60a5fa;
  font-weight: 600;
  text-shadow: 0 1px 2px rgba(96, 165, 250, 0.3);
  background: linear-gradient(45deg, #60a5fa, #3b82f6);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.mode-icon {
  width: 1em;
  height: 1em;
}
</style>