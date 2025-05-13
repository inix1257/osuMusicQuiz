<template>
  <div class="leaderboard-container">
    <div class="leaderboard-div">
      <h1>Weekly Leaderboard</h1>
      <div class="leaderboard-div-inner">
        <Leaderboard v-if="currentLeaderboard === 0" :players="seasonalPlayers"/>
      </div>

      <div class="button-container">
        <button class="full-leaderboard-button" @click="goToFullLeaderboard">Show full leaderboard</button>
      </div>

    </div>
    <div class="leaderboard-div">
      <h1>Top Donators</h1>
      <DonationLeaderboard :playerInfo="topDonators" />
    </div>
  </div>
</template>

<script>
import Leaderboard from "@/components/Leaderboard/Leaderboard.vue";
import DonationLeaderboard from "@/components/Leaderboard/DonationLeaderboard.vue";

export default {
  components: { Leaderboard, DonationLeaderboard },
  props: {
    totalPlayers: {
      type: Array,
      required: true,
    },
    topDonators: {
      type: Array,
      required: true,
    },
    seasonalPlayers: {
      type: Array,
      required: true,
    },
  },
  data() {
    return {
      currentLeaderboard: 0,
      intervalId: null,
    };
  },
  methods: {
    switchLeaderboard() {
      this.currentLeaderboard = (this.currentLeaderboard + 1) % 2;
    },

    goToFullLeaderboard() {
      this.$router.push('/leaderboard');
    },
  },

  async created() {
    // this.intervalId = setInterval(this.switchLeaderboard, 5000);
  },

  beforeUnmount() {
    clearInterval(this.intervalId);
  },
};
</script>

<style scoped>
.leaderboard-container {
  width: 100%; 
  border-radius: 20px;
}

.leaderboard-div {
  background-color: var(--color-secondary);
  border-radius: 20px;
  padding: 1em;
  margin-bottom: 1em;
  overflow: hidden;
  position: relative;
}

.span-weekly {
  font-size: 0.6em;
  color: var(--color-subtext);
}

h1 {
  font-size: 1.2em;
  text-align: center;
  margin-top: 0.2em;
  margin-bottom: 1em;
  padding-bottom: 0.8em;
  border-bottom: 1px solid rgba(128, 128, 128, 0.3);
}

.button-container {
  display: flex;
  justify-content: center;
}

.full-leaderboard-button {
  background-color: transparent;
  border: none;
  color: var(--color-subtext);
  padding: 8px 16px;
  text-align: center;
  text-decoration: none;
  display: inline-block;
  font-size: 0.8em;
  font-family: "Sen", "Noto Sans Korean", "serif";
  cursor: pointer;
  border-radius: 4px;
  transition-duration: 0.4s;
}

.full-leaderboard-button:hover {
  background-color: transparent;
  color: rgba(255, 255, 255, 0.8);
}

.fade-enter-active, .fade-leave-active {
  transition: opacity 0.5s;
}

.fade-enter, .fade-leave-to /* .fade-leave-active in <2.1.8 */ {
  opacity: 0;
}
</style>