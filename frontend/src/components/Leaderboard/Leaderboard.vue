<template>
  <div>
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
        <td><strong>{{ player.username }}</strong></td>
        <td>{{ formatPoints(player.points) }}</td>
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
    players: {
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

    formatPoints(points) {
      if (points >= 1000) {
        return (points / 1000).toFixed(1) + 'k';
      } else {
        return points.toFixed(1);
      }
    }
  },
};
</script>

<style scoped>
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
</style>