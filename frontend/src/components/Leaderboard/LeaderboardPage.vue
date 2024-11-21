<template>
  <div class="leaderboard-container">
    <div class="leaderboard-div">
      <h1>Leaderboard</h1>
      <Leaderboard :players="totalPlayers" />

    </div>
    <div class="leaderboard-div">
      <h1>Top Donators</h1>
      <DonationLeaderboard :playerInfo="topDonators" />
    </div>
<!--    <div v-if="currentLeaderboard === 1" class="leaderboard-div">-->
<!--      <h1>Donator Leaderboard</h1>-->
<!--      <Leaderboard :players="donatorPlayers" />-->
<!--    </div>-->
<!--    <div v-if="currentLeaderboard === 2" class="leaderboard-div">-->
<!--      <h1>Another Leaderboard</h1>-->
<!--      <Leaderboard :players="anotherPlayers" />-->
<!--    </div>-->
  </div>
</template>

<script>
import apiService from "@/api/apiService";
import Leaderboard from "@/components/Leaderboard/Leaderboard.vue";
import DonationLeaderboard from "@/components/Leaderboard/DonationLeaderboard.vue";

export default {
  components: { Leaderboard, DonationLeaderboard },
  data() {
    return {
      currentLeaderboard: 0,
      intervalId: null,
      totalPlayers: [],
      topDonators: [],
    };
  },
  methods: {
    async fetchLeaderboardData() {
      const response = await apiService.get('/api/leaderboard');
      this.totalPlayers = response.data.topPlayers;
      this.topDonators = response.data.topDonators;

      // const totalResponse = await apiService.get('/api/leaderboard?page=0&limit=5');
      // this.totalPlayers = totalResponse.data.players;

      // const donatorResponse = await apiService.get('/api/leaderboard/donator');
      // this.donatorPlayers = donatorResponse.data.players;
      //
      // const anotherResponse = await apiService.get('/api/leaderboard/another');
      // this.anotherPlayers = anotherResponse.data.players;
    },
    startSlideshow() {
      this.intervalId = setInterval(() => {
        this.currentLeaderboard = (this.currentLeaderboard + 1) % 3;
      }, 5000);
    },
  },
  async created() {
    await this.fetchLeaderboardData();
    // this.startSlideshow();
  },
  beforeUnmount() {
    clearInterval(this.intervalId);
  },
};
</script>

<style scoped>
.leaderboard-container {
  border-radius: 20px;
}

.leaderboard-div {
  background-color: var(--color-secondary);
  border-radius: 20px;
  padding: 1em;
  margin-bottom: 1em;
}

h1 {
  font-size: 1.2em;
  text-align: center;
  margin-top: 0.2em;
  margin-bottom: 1em;
}
</style>