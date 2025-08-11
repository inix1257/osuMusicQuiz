<script>
import axios from 'axios';
import apiService from "@/api/apiService";
import UserPage from "@/components/UserPage.vue";

export default {
  components: {UserPage},
  data() {
    return {
      players: [],
      page: 0,
      maxPage: 0,
      showUserpage: false,
      userpageId: null,
    };
  },
  methods: {
    goBack() {
      this.$router.go(-1);
    },

    nextPage() {
      this.players = [];
      if (this.page < this.maxPage - 1) {
        this.page++;
        this.fetchData();
      }
    },

    previousPage() {
      this.players = [];
      if (this.page > 0) {
        this.page--;
        this.fetchData();
      }
    },

    async fetchData() {
      const response = await apiService.get('/api/fullleaderboard?page=' + this.page + '&limit=50');
      this.players = response.data.topPlayers;
      this.maxPage = Math.ceil(response.data.totalItems / 50);
    }
  },
  async created() {
    await this.fetchData();
  },


};
</script>

<template>
  <div class="leaderboard-div">
    <button class="back-button" @click="goBack">Back</button>
    <h1>OMQ Leaderboard</h1>
    <div class="leaderboard-content">
      <table class="centered-table">
        <thead>
        <tr>
          <th>Rank</th>
          <th></th>
          <th class="username-column">Username</th>
          <th class="points-column">Points</th>
        </tr>
        </thead>
        <tbody>
        <tr class="spaced-row" v-for="(player, index) in players" :key="index" v-on:click.stop="showUserpage = true; userpageId = player.id">
          <td>{{ index + 1 + (page * 50) }}</td>
          <td><img :src="player.avatar_url" alt="Profile Picture" class="avatar" /></td>
          <td>{{ player.username }}</td>
          <td>{{ player.points }}</td>
        </tr>
        </tbody>
      </table>
    </div>
    <button class="page-button" @click="previousPage">&lt;</button>
    <span class="page-count">{{ this.page + 1 }}</span>
    <button class="page-button" @click="nextPage">&gt;</button>
    <div v-if="showUserpage" class="leaderboard-modal-overlay" @click.stop="showUserpage = false">
      <div class="leaderboard-modal-content" @click.stop>
        <UserPage :playerId="userpageId"></UserPage>
      </div>
    </div>
  </div>
</template>

<style scoped>
.leaderboard-div {
  position: relative;
  background-color: var(--color-secondary);
  border-radius: 20px;
  padding: 20px;
  box-shadow: 0px 10px 20px rgba(0, 0, 0, 0.1);
  width: 100%;
  height: 100%;
}

.leaderboard-content {
  height: 70vh;
  overflow-y: auto;
}

.avatar {
  width: 50px;
  height: 50px;
  border-radius: 50%;
  box-shadow: 0px 5px 10px rgba(0, 0, 0, 0.1);
}

.centered-table {
  margin-left: auto;
  margin-right: auto;
}

.spaced-row td {
  padding: 0 30px;
}

.username-column {
  min-width: 200px;
}

.points-column {
  min-width: 100px;
}

.spaced-row {
  cursor: pointer;
}

.back-button {
  position: absolute;
  top: 20px;
  left: 20px;
  background-color: var(--color-primary);
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
  transition-duration: 0.4s;
}

.back-button:hover {
  background-color: white;
  color: black;
}

.page-button {
  background-color: var(--color-primary);
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
  transition-duration: 0.4s;
}

.page-button:hover {
  background-color: white;
  color: black;
}

.page-count {
  font-size: 16px;
  margin: 16px
}

.leaderboard-modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  background: linear-gradient(135deg, rgba(0, 0, 0, 0.9), rgba(0, 0, 0, 0.7));
  backdrop-filter: blur(10px);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 9999;
}

.leaderboard-modal-content {
  max-width: 90vw;
  max-height: 90vh;
  overflow: auto;
}

</style>