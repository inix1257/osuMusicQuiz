<template>
  <div>
    <table class="centered-table">
      <thead>
      <tr>
        <th>Rank</th>
        <th></th>
        <th class="username-column">Username</th>
        <th class="points-column"></th>
      </tr>
      </thead>
      <tbody>
      <tr class="spaced-row" v-for="(playerData, index) in playerInfo" :key="index" v-on:click.stop="showUserpage = true; userpageId = playerData.player.id">
        <td>{{ index + 1 }}</td>
        <td><img :src="playerData.player.avatar_url" alt="Profile Picture" class="avatar" /></td>
        <td>{{ playerData.player.username }}</td>
        <td>{{ playerData.totalAmount }}</td>
      </tr>
      </tbody>
    </table>
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
  props: {
    playerInfo: {
      type: Array,
      required: true,
    },
  },
  data() {
    return {
      showUserpage: false,
      userpageId: null,
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
    // const response = await apiService.get('/api/leaderboard');
    //
    // this.players = response.data.players;
  },
};
</script>

<style scoped>
.avatar {
  width: 2em;
  height: 2em;
  border-radius: 50%;
  box-shadow: 0px 5px 10px rgba(0, 0, 0, 0.1);
}

.centered-table {
  margin-left: auto;
  margin-right: auto;
}

.spaced-row td {
  padding: 0 1.2em;
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