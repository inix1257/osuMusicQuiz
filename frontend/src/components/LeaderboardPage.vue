<template>
  <div class="leaderboard-container">
    <div v-if="currentLeaderboard === 0" class="leaderboard-div">
      <h1>Total Leaderboard</h1>
      <Leaderboard :players="totalPlayers" />
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
import Leaderboard from "@/components/Leaderboard.vue";

export default {
  components: { Leaderboard },
  data() {
    return {
      currentLeaderboard: 0,
      intervalId: null,
      totalPlayers: [],
      donatorPlayers: [],
      anotherPlayers: [],
    };
  },
  methods: {
    async fetchLeaderboardData() {
      const totalResponse = await apiService.get('/api/leaderboard?page=0&limit=5');
      this.totalPlayers = totalResponse.data.players;

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
  background-color: var(--color-secondary);
  border-radius: 20px;
  padding: 20px;
  box-shadow: 0px 10px 20px rgba(0, 0, 0, 0.1);
}
.leaderboard-div {
  //position: absolute;
  //width: 100%;
  //height: 100%;
  //transition: opacity 1s ease-in-out;
}

h1 {
  text-align: center;
  margin-top: 0.2em;
  margin-bottom: 0;
}
</style>