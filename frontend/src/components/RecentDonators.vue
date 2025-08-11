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
  font-size: 1.3em;
  font-weight: bold;
  margin-right: 15px;
  color: #ef4444;
  background: linear-gradient(45deg, #ef4444, #dc2626);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  text-shadow: 0 1px 2px rgba(239, 68, 68, 0.3);
  transition: all 0.3s ease;
}

.recent-donators:hover .recent-donators-title-icon {
  transform: scale(1.1);
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
  padding: 15px 20px;
  border-radius: 15px;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.05) 0%, rgba(255, 255, 255, 0.02) 100%);
  border: 1px solid rgba(255, 255, 255, 0.1);
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.1);
  margin-bottom: 15px;
  transition: all 0.3s ease;
}

.recent-donators:hover {
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.08) 0%, rgba(255, 255, 255, 0.05) 100%);
  border-color: rgba(255, 255, 255, 0.2);
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.15);
}

.player-info-row {
  max-width: 100%;
  display: flex;
  flex-wrap: nowrap;
  gap: 12px;
  overflow: hidden;
  white-space: nowrap;
  align-items: center;
}

.player-info {
  display: flex;
  align-items: center;
  border-radius: 10px;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.1), rgba(255, 255, 255, 0.05));
  border: 1px solid rgba(255, 255, 255, 0.2);
  padding: 8px 12px;
  flex: 0 0 auto;
  transition: all 0.3s ease;
  text-decoration: none;
  color: var(--color-text);
}

.player-info:hover {
  background: linear-gradient(135deg, rgba(239, 68, 68, 0.2), rgba(220, 38, 38, 0.1));
  border-color: rgba(239, 68, 68, 0.4);
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.15);
  color: #ef4444;
}

.player-avatar {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  margin-right: 10px;
  transition: all 0.3s ease;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
  border: 2px solid rgba(255, 255, 255, 0.2);
}

.player-info:hover .player-avatar {
  border-color: rgba(239, 68, 68, 0.4);
  transform: scale(1.1);
  box-shadow: 0 4px 12px rgba(239, 68, 68, 0.3);
}

.player-username {
  margin-top: 0;
  font-weight: 500;
  font-size: 0.95em;
  transition: all 0.3s ease;
}

.player-info:hover .player-username {
  font-weight: 600;
  text-shadow: 0 1px 2px rgba(239, 68, 68, 0.3);
}
</style>