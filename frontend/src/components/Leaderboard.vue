<template>
  <div class="leaderboard-div">
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
        <td>{{ index + 1 }}</td>
        <td><img :src="player.avatar_url" alt="Profile Picture" class="avatar" /></td>
        <td>{{ player.username }}</td>
        <td>{{ player.points }}</td>
      </tr>

      </tbody>
    </table>
    <div class="button-container">
      <button class="full-leaderboard-button" @click="goToFullLeaderboard">Show Full Leaderboard</button>
    </div>
    <div class="modal-overlay" v-if="showUserpage" @click.stop="showUserpage = false">
      <UserPage :playerId="userpageId"></UserPage>
    </div>
  </div>
</template>

<script>
import apiService from "@/api/apiService";
import UserPage from "@/components/UserPage.vue";

export default {
  components: {UserPage},
  data() {
    return {
      showUserpage: false,
      userpageId: null,
      players: [],
    };
  },
  methods: {
    openUserpage(id) {
      window.open(`https://osu.ppy.sh/users/${id}`, '_blank')
    },

    goToFullLeaderboard() {
      this.$router.push('/leaderboard');
    }
  },
  async created() {
    const response = await apiService.get('/api/leaderboard?page=0&limit=5');

    this.players = response.data.players;
  },
};
</script>

<style scoped>
.leaderboard-div {
  border-radius: 20px;
  padding: 20px;
}

.avatar {
  width: 40px;
  height: 40px;
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

.button-container {
  display: flex;
  justify-content: right;
}

.full-leaderboard-button {
  background-color: var(--color-primary);
  border: none;
  color: white;
  padding: 8px 16px;
  text-align: center;
  text-decoration: none;
  display: inline-block;
  font-size: 0.8em;
  cursor: pointer;
  border-radius: 4px;
  transition-duration: 0.4s;
}

.full-leaderboard-button:hover {
  background-color: white;
  color: black;
}
</style>