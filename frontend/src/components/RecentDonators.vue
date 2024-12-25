<script>
export default {
  name: "RecentDonators",
  props: {
    donations: {
      type: Array,
      required: true
    }
  }
}
</script>

<template>
  <div class="recent-donators">
    <span class="recent-donators-title-icon"><font-awesome-icon :icon="['fas', 'heart']" /></span>
    <div class="recent-donators-title">
      Recent donations
    </div>
    <div class="player-info-row">
      <a v-for="donation in donations" :key="donation.time" :href="`https://osu.ppy.sh/users/${donation.player.id}`" target="_blank" class="player-info">
        <img :src="donation.player.avatar_url" :alt="donation.player.username" class="player-avatar" :title="donation.player.username">
        <div class="player-username">
          {{ donation.player.username }} - ${{ donation.amount }}
        </div>
      </a>
    </div>
  </div>
</template>

<style scoped>
.recent-donators-title-icon {
  font-size: 1.2em;
  font-weight: bold;
  margin-right: 10px;
}

.recent-donators-title {
  font-size: 1.2em;
  font-weight: bold;
  margin-right: 0.5em;
}

.recent-donators {
  display: flex;
  justify-content: left;
  align-items: center;
  padding: 10px;
  border-radius: 10px;
}

.player-info-row {
  max-width: 100%;
  display: flex;
  flex-wrap: nowrap;
  gap: 10px;
  overflow: hidden;
  white-space: nowrap;
}

.player-info {
  display: flex;
  align-items: center;
  border-radius: 8px;
  border: 2px solid #849ee5;
  padding: 2px 8px;
  flex: 0 0 auto;
}

.player-info:hover {
  background-color: #849ee5;
  color: white;
}

.player-avatar {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  margin-right: 10px;
  transition: 0.3s;
  box-shadow: 0px 10px 10px rgba(0, 0, 0, 0.1);
  border: 0px solid #849ee5;
}

.player-username {
  margin-top: 0;
  font-weight: normal;
  font-size: 1.0em;
}
</style>