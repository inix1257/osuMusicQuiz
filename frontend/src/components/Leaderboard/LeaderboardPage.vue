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
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.leaderboard-div {
  background: linear-gradient(135deg, var(--color-secondary) 0%, rgba(255, 255, 255, 0.05) 100%);
  border-radius: 20px;
  padding: 25px;
  margin-bottom: 0;
  overflow: hidden;
  position: relative;
  border: 1px solid rgba(255, 255, 255, 0.1);
  box-shadow: 0 8px 25px rgba(0, 0, 0, 0.15);
  transition: all 0.3s ease;
}

.leaderboard-div:hover {
  background: linear-gradient(135deg, var(--color-secondary) 0%, rgba(255, 255, 255, 0.08) 100%);
  border-color: rgba(255, 255, 255, 0.2);
  box-shadow: 0 12px 35px rgba(0, 0, 0, 0.2);
  transform: translateY(-2px);
}

.span-weekly {
  font-size: 0.6em;
  color: var(--color-subtext);
}

h1 {
  font-size: 1.4em;
  font-weight: 700;
  text-align: center;
  margin-top: 0;
  margin-bottom: 1.5em;
  padding-bottom: 1em;
  border-bottom: 2px solid rgba(255, 255, 255, 0.1);
  color: var(--color-text);
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.3);
  letter-spacing: 0.5px;
  position: relative;
}

h1::after {
  content: '';
  position: absolute;
  bottom: -2px;
  left: 50%;
  transform: translateX(-50%);
  width: 60px;
  height: 2px;
  background: linear-gradient(90deg, #60a5fa, #3b82f6);
  border-radius: 1px;
}

.button-container {
  display: flex;
  justify-content: center;
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
}

.full-leaderboard-button {
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.1), rgba(255, 255, 255, 0.05));
  border: 1px solid rgba(255, 255, 255, 0.2);
  color: var(--color-text);
  padding: 12px 24px;
  text-align: center;
  text-decoration: none;
  display: inline-block;
  font-size: 0.9em;
  font-weight: 600;
  font-family: "Sen", "Noto Sans Korean", "serif";
  cursor: pointer;
  border-radius: 10px;
  transition: all 0.3s ease;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.1);
}

.full-leaderboard-button:hover {
  background: linear-gradient(135deg, rgba(96, 165, 250, 0.2), rgba(59, 130, 246, 0.1));
  border-color: rgba(96, 165, 250, 0.4);
  color: #60a5fa;
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(96, 165, 250, 0.3);
}

.fade-enter-active, .fade-leave-active {
  transition: all 0.5s ease;
}

.fade-enter, .fade-leave-to /* .fade-leave-active in <2.1.8 */ {
  opacity: 0;
  transform: translateY(10px);
}

/* Responsive adjustments */
@media (max-width: 768px) {
  .leaderboard-container {
    padding: 15px;
    gap: 15px;
  }
  
  .leaderboard-div {
    padding: 20px;
  }
  
  h1 {
    font-size: 1.2em;
    margin-bottom: 1.2em;
  }
  
  .full-leaderboard-button {
    padding: 10px 20px;
    font-size: 0.85em;
  }
}
</style>