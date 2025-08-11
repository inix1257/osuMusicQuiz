<template>
  <div class="beatmap-request-page">
    <h1>Request New Beatmaps</h1>
    <p class="page-description">Submit beatmapset ID to request the beatmap for addition to OMQ. Vote on existing requests to help prioritize which beatmaps should be added next.</p>

    <div class="request-toolbar">
      <form @submit.prevent="submitRequest" class="inline-form">
        <input
          v-model="newRequestId"
          type="text"
          placeholder="Enter beatmap set ID"
          required
          class="themed-input compact-input"
        />
        <button type="submit" class="themed-btn compact-btn">Submit</button>
      </form>

      <div class="toolbar-spacer"></div>

      <div class="inline-search">
        <input
          v-model="searchQuery"
          type="text"
          placeholder="Search by title, artist, or creator"
          class="themed-input compact-input search-bar"
        />
        <div class="sort-controls compact-controls">
          <div class="sort-dropdown">
            <select v-model="sortBy" @change="updateSortOrder" class="sort-select compact-select">
              <option value="votes">Sort by Votes</option>
              <option value="rankedDate">Sort by Ranked Date</option>
              <option value="playCount">Sort by Play Count</option>
              <option value="favouriteCount">Sort by Favourite Count</option>
            </select>
          </div>
          <button @click="toggleSortOrder" class="sort-order-btn compact-order" :class="{ active: true }">
            {{ sortOrder === 'asc' ? '▲' : '▼' }}
          </button>
        </div>
      </div>
    </div>

    <div class="requests-list">
      <div v-for="(request, idx) in filteredAndSortedRequests" :key="request.id">
        <a :href="`https://osu.ppy.sh/beatmapsets/${request.beatmapSetId}`" target="_blank" class="beatmap-link">
          <div class="request-item" :style="{ '--bg-image': `url(https://assets.ppy.sh/beatmaps/${request.beatmapSetId}/covers/card.jpg)` }">
            <img
              class="beatmap-thumb"
              :src="`https://assets.ppy.sh/beatmaps/${request.beatmapSetId}/covers/list.jpg`"
              :alt="request.title"
            />
            <div class="beatmap-meta">
              <div class="beatmap-title">{{ request.title || `Beatmap #${request.beatmapSetId}` }}</div>
              <div class="beatmap-artist" v-if="request.artist">by {{ request.artist }}</div>
              <div class="beatmap-creator" v-if="request.creator">mapped by {{ request.creator }}</div>
              <div class="beatmap-stats" v-if="request.playCount !== undefined || request.favouriteCount !== undefined || request.rankedDate">
                <span v-if="request.playCount !== undefined" class="stat-item">
                  <span class="icon">▶</span> {{ request.playCount.toLocaleString() }}
                </span>
                <span v-if="request.favouriteCount !== undefined" class="stat-item">
                  <span class="icon">❤</span> {{ request.favouriteCount.toLocaleString() }}
                </span>
                <span v-if="request.rankedDate" class="stat-item">
                  <span class="icon">📅</span> {{ formatDate(request.rankedDate) }}
                </span>
              </div>
              <!-- <div class="submitter">Requested by <b>{{ request.submitterName }}</b></div> -->
            </div>
            <div class="votes">
              <button @click.stop.prevent="upvote(idx)">▲</button>
              <span>{{ request.votes }}</span>
              <button v-if="isAdmin" @click.stop.prevent="addBeatmap(request.beatmapSetId)" class="admin-btn">
                Add
              </button>
            </div>
          </div>
        </a>
      </div>
    </div>
    <div class="notification-push" v-if="showNotification">
      {{ notificationMessage }}
    </div>
  </div>
</template>

<script>
import apiService from "@/api/apiService";
import { useUserStore } from "@/stores/userStore";

export default {
  name: "BeatmapRequestPage",
  setup() {
    const userStore = useUserStore();
    return {
      me: userStore.getMe(),
    };
  },
  data() {
    return {
      newRequestId: "",
      requests: [],
      loading: false,
      notificationMessage: "",
      showNotification: false,
      searchQuery: "",
      sortBy: 'votes', // or 'rankedDate'
      sortOrder: 'desc', // or 'asc'
    };
  },
  computed: {
    isAdmin() {
      if (!this.me.achievements) {
        return false;
      }
      return this.me.achievements.some(achievement => achievement.id === 2);
    },
    filteredAndSortedRequests() {
      // Filter
      let filtered = this.requests.filter(req => {
        const q = this.searchQuery.trim().toLowerCase();
        if (!q) return true;
        return (
          (req.title && req.title.toLowerCase().includes(q)) ||
          (req.artist && req.artist.toLowerCase().includes(q)) ||
          (req.creator && req.creator.toLowerCase().includes(q))
        );
      });
      // Sort
      if (this.sortBy === 'votes') {
        filtered = filtered.slice().sort((a, b) => {
          if (this.sortOrder === 'asc') return a.votes - b.votes;
          else return b.votes - a.votes;
        });
      } else if (this.sortBy === 'rankedDate') {
        filtered = filtered.slice().sort((a, b) => {
          const aDate = a.rankedDate ? new Date(a.rankedDate) : new Date(0);
          const bDate = b.rankedDate ? new Date(b.rankedDate) : new Date(0);
          if (this.sortOrder === 'asc') return aDate - bDate;
          else return bDate - aDate;
        });
      } else if (this.sortBy === 'playCount') {
        filtered = filtered.slice().sort((a, b) => {
          const aCount = a.playCount || 0;
          const bCount = b.playCount || 0;
          if (this.sortOrder === 'asc') return aCount - bCount;
          else return bCount - aCount;
        });
      } else if (this.sortBy === 'favouriteCount') {
        filtered = filtered.slice().sort((a, b) => {
          const aCount = a.favouriteCount || 0;
          const bCount = b.favouriteCount || 0;
          if (this.sortOrder === 'asc') return aCount - bCount;
          else return bCount - aCount;
        });
      }
      return filtered;
    },
  },
  methods: {
    async fetchRequests() {
      this.loading = true;
      try {
        const res = await apiService.get("/api/beatmap-request");
        this.requests = res.data;
      } catch (e) {
        this.showNotif("Failed to load requests.");
      }
      this.loading = false;
    },
    async submitRequest() {
      if (!this.newRequestId.trim()) return;
      
      let beatmapSetId = this.newRequestId.trim();
      
      // Check if input is a URL and extract beatmapset ID
      if (beatmapSetId.includes('osu.ppy.sh/beatmapsets/')) {
        const urlMatch = beatmapSetId.match(/osu\.ppy\.sh\/beatmapsets\/(\d+)/);
        if (urlMatch) {
          beatmapSetId = urlMatch[1];
        } else {
          this.showNotif("Invalid beatmap URL format.");
          return;
        }
      }
      
      try {
        await apiService.post("/api/beatmap-request?beatmapSetId=" + beatmapSetId);
        this.newRequestId = "";
        await this.fetchRequests();
        this.showNotif("Request submitted!");
      } catch (e) {
        this.showNotif(e?.response?.data?.error || "Failed to submit request.");
      }
    },
    async upvote(idx) {
      const req = this.requests[idx];
      try {
        await apiService.post(`/api/beatmap-request/${req.id}/vote`);
        await this.fetchRequests();
        this.showNotif("Voted!");
      } catch (e) {
        this.showNotif(e?.response?.data?.error || "Failed to vote.");
      }
    },
    async addBeatmap(beatmapSetId) {
      try {
        await apiService.post("/api/addBeatmap", {
          beatmapsetId: beatmapSetId,
          mode: ""
        });
        this.showNotif("Beatmap added successfully!");
        await this.fetchRequests();
      } catch (e) {
        this.showNotif("Failed to add beatmap. It might already exist.");
      }
    },
    showNotif(msg) {
      this.notificationMessage = msg;
      this.showNotification = true;
      setTimeout(() => {
        this.showNotification = false;
      }, 3000);
    },
    updateSortOrder() {
      // Keep the current sort order when changing sort type
      this.sortOrder = this.sortOrder || 'desc';
    },
    toggleSortOrder() {
      this.sortOrder = this.sortOrder === 'asc' ? 'desc' : 'asc';
    },
    formatDate(dateString) {
      if (!dateString) return '';
      const date = new Date(dateString);
      return date.toLocaleDateString('en-US', { 
        year: 'numeric', 
        month: 'short', 
        day: 'numeric' 
      });
    },
  },
  mounted() {
    this.fetchRequests();
  },
};
</script>

<style scoped>
.beatmap-request-page {
  max-width: 1200px;
  margin: 0 auto;
  padding: 2rem;
  height: 100vh;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background: linear-gradient(135deg, rgba(30, 41, 59, 0.8) 0%, rgba(15, 23, 42, 0.9) 100%);
  border-radius: 16px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.3);
}

.beatmap-request-page h1 {
  font-size: 2.5em;
  font-weight: 700;
  text-align: center;
  margin-bottom: 2rem;
  background: linear-gradient(135deg, #60a5fa 0%, #3b82f6 100%);
  background-clip: text;
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  position: relative;
}

.beatmap-request-page h1::after {
  content: '';
  position: absolute;
  bottom: -8px;
  left: 50%;
  transform: translateX(-50%);
  width: 80px;
  height: 3px;
  background: linear-gradient(90deg, #60a5fa, #3b82f6);
  border-radius: 2px;
}

.page-description {
  text-align: center;
  color: rgba(255, 255, 255, 0.8);
  font-size: 1.1em;
  margin-bottom: 1rem;
  line-height: 1.6;
  max-width: 800px;
  margin-left: auto;
  margin-right: auto;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.2);
}

/* Compact single-row toolbar */
.request-toolbar {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.6rem 0.75rem;
  border-radius: 12px;
  background: linear-gradient(135deg, rgba(51, 65, 85, 0.6) 0%, rgba(30, 41, 59, 0.8) 100%);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.2);
  border: 1px solid rgba(96, 165, 250, 0.2);
  margin-bottom: 1rem;
}
.inline-form {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}
.inline-search {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}
.toolbar-spacer {
  flex: 1;
}

/* Make all controls compact */
.compact-input {
  padding: 0.4rem 0.6rem !important;
  font-size: 0.85em !important;
  border-radius: 6px !important;
}
.compact-btn {
  padding: 0.4rem 0.8rem !important;
  font-size: 0.85em !important;
  border-radius: 6px !important;
}
.compact-controls {
  padding: 0.3rem 0.4rem !important;
  gap: 0.4rem !important;
}
.compact-select {
  padding: 0.3rem 0.5rem !important;
  font-size: 0.8em !important;
}
.compact-order {
  padding: 0.3rem 0.5rem !important;
  font-size: 0.95em !important;
}

/* Remove old sections spacing since merged into toolbar */
.search-sort-area {
  display: none;
}

/* Existing styles for inputs/buttons to keep theme */
.themed-input {
  flex: 1 1 180px;
  padding: 0.5rem 0.8rem;
  border: 1px solid rgba(96, 165, 250, 0.3);
  border-radius: 6px;
  background: linear-gradient(135deg, rgba(30, 41, 59, 0.8) 0%, rgba(15, 23, 42, 0.9) 100%);
  color: var(--color-text);
  font-size: 0.9em;
  transition: all 0.3s ease;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.themed-input:focus {
  outline: none;
  border: 2px solid #60a5fa;
  background: linear-gradient(135deg, rgba(30, 41, 59, 0.9) 0%, rgba(15, 23, 42, 1) 100%);
  box-shadow: 0 4px 16px rgba(96, 165, 250, 0.3);
  transform: translateY(-1px);
}
.themed-btn {
  padding: 0.5rem 1.2rem;
  background: linear-gradient(135deg, #60a5fa 0%, #3b82f6 100%);
  color: white;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-weight: 600;
  font-size: 0.9em;
  transition: all 0.3s ease;
  box-shadow: 0 4px 12px rgba(96, 165, 250, 0.3);
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.1);
}

.themed-btn:hover {
  background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(96, 165, 250, 0.4);
}

.themed-btn:active {
  transform: translateY(0);
  box-shadow: 0 2px 8px rgba(96, 165, 250, 0.3);
}

.requests-list {
  margin-top: 1rem;
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  padding-bottom: 8rem;
  padding-right: 0.5rem;
}

.requests-list::-webkit-scrollbar {
  width: 8px;
}

.requests-list::-webkit-scrollbar-track {
  background: rgba(30, 41, 59, 0.3);
  border-radius: 4px;
}

.requests-list::-webkit-scrollbar-thumb {
  background: linear-gradient(135deg, #60a5fa, #3b82f6);
  border-radius: 4px;
}

.requests-list::-webkit-scrollbar-thumb:hover {
  background: linear-gradient(135deg, #3b82f6, #2563eb);
}

.beatmap-link {
  text-decoration: none;
  color: inherit;
  display: block;
  margin-bottom: 1.5rem;
  transition: all 0.3s ease;
}

.beatmap-link:hover {
  text-decoration: none;
  transform: translateY(-2px);
}
.request-item {
  display: flex;
  align-items: center;
  gap: 1.2rem;
  border-radius: 12px;
  margin-bottom: 0.8rem;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.2);
  color: var(--color-text);
  position: relative;
  background: none;
  padding: 1rem;
  overflow: hidden;
  transition: all 0.3s ease;
  border: 1px solid rgba(96, 165, 250, 0.1);
}

.request-item::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-image: var(--bg-image);
  background-size: cover;
  background-position: center;
  background-repeat: no-repeat;
  filter: blur(8px);
  z-index: 0;
}

.request-item::after {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(135deg, rgba(0, 0, 0, 0.9) 0%, rgba(0, 0, 0, 0.8) 100%);
  z-index: 1;
}

.request-item::after {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(135deg, rgba(0, 0, 0, 0.85) 0%, rgba(0, 0, 0, 0.7) 100%);
  z-index: 1;
}

.request-item > * {
  position: relative;
  z-index: 2;
}

.request-item:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.3);
  border-color: rgba(96, 165, 250, 0.3);
}
.beatmap-thumb {
  width: 80px;
  height: 80px;
  object-fit: cover;
  border-radius: 8px;
  border: 2px solid rgba(96, 165, 250, 0.3);
  background: linear-gradient(135deg, rgba(30, 41, 59, 0.8) 0%, rgba(15, 23, 42, 0.9) 100%);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.2);
  transition: all 0.3s ease;
}

.beatmap-thumb:hover {
  transform: scale(1.05);
  border-color: rgba(96, 165, 250, 0.6);
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.3);
}
.beatmap-meta {
  flex: 1;
  min-width: 0;
  text-align: left;
  padding: 0.3rem 0;
}
.beatmap-title {
  font-size: 1.1em;
  font-weight: 700;
  color: white;
  margin-bottom: 0.2em;
  word-break: break-all;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.3);
  letter-spacing: 0.5px;
}
.beatmap-artist,
.beatmap-creator {
  color: rgba(255, 255, 255, 0.9);
  font-size: 0.9em;
  font-weight: 500;
  margin-bottom: 0.15em;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.2);
}
.beatmap-stats {
  display: flex;
  gap: 1rem;
  margin-top: 0.3rem;
  flex-wrap: wrap;
}
.stat-item {
  color: rgba(255, 255, 255, 0.8);
  font-size: 0.85em;
  display: flex;
  align-items: center;
  gap: 0.3rem;
  opacity: 1;
  background: rgba(96, 165, 250, 0.2);
  padding: 0.25rem 0.5rem;
  border-radius: 6px;
  backdrop-filter: blur(4px);
  transition: all 0.3s ease;
}

.stat-item:hover {
  background: rgba(96, 165, 250, 0.3);
  transform: translateY(-1px);
}

.stat-item .icon {
  font-size: 1em;
  color: #60a5fa;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.2);
}
.submitter {
  color: var(--color-disabled);
  font-size: 0.95em;
  margin-top: 0.2em;
}
.votes {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.4rem;
  min-width: 45px;
  background: rgba(96, 165, 250, 0.1);
  padding: 0.8rem 0.6rem;
  border-radius: 8px;
  backdrop-filter: blur(4px);
  border: 1px solid rgba(96, 165, 250, 0.2);
}
.votes button {
  background: none;
  border: none;
  font-size: 1.3em;
  cursor: pointer;
  color: #60a5fa;
  transition: all 0.3s ease;
  padding: 0.15rem;
  position: relative;
  z-index: 10;
  border-radius: 4px;
}

.votes button:hover {
  color: #3b82f6;
  transform: scale(1.2);
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.3);
}

.votes span {
  font-weight: 600;
  font-size: 1em;
  color: white;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.2);
}
.votes .admin-btn {
  background: linear-gradient(135deg, #10b981 0%, #059669 100%);
  color: white;
  border: none;
  border-radius: 6px;
  padding: 0.3rem 0.6rem;
  cursor: pointer;
  font-size: 0.8em;
  font-weight: 600;
  transition: all 0.3s ease;
  margin-top: 0.2rem;
  position: relative;
  z-index: 10;
  box-shadow: 0 2px 8px rgba(16, 185, 129, 0.3);
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.1);
}

.votes .admin-btn:hover {
  background: linear-gradient(135deg, #059669 0%, #047857 100%);
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(16, 185, 129, 0.4);
}
.notification-push {
  position: fixed;
  bottom: 20px;
  right: 20px;
  background: linear-gradient(135deg, #ef4444 0%, #dc2626 100%);
  color: white;
  padding: 12px 20px;
  border-radius: 8px;
  z-index: 1000;
  font-size: 1.1em;
  font-weight: 600;
  box-shadow: 0 8px 24px rgba(239, 68, 68, 0.3);
  backdrop-filter: blur(8px);
  border: 1px solid rgba(239, 68, 68, 0.2);
  animation: slideIn 0.3s ease-out;
}

@keyframes slideIn {
  from {
    transform: translateX(100%);
    opacity: 0;
  }
  to {
    transform: translateX(0);
    opacity: 1;
  }
}

.admin-actions {
  margin-left: 1rem;
}

.admin-btn {
  background: var(--color-primary);
  color: var(--color-text);
  border: none;
  border-radius: 4px;
  padding: 0.5rem 1rem;
  cursor: pointer;
  font-size: 0.9em;
  transition: background 0.2s;
}

.admin-btn:hover {
  background: var(--color-secondary);
}

/* Keep but unused now (merged into toolbar); could be removed later */
.search-sort-area {
  display: none;
}
.search-bar {
  flex: 2 1 250px;
  min-width: 220px;
}
.sort-controls {
  display: flex;
  align-items: center;
  gap: 0.8rem;
  background: rgba(96, 165, 250, 0.1);
  padding: 0.5rem;
  border-radius: 8px;
  border: 1px solid rgba(96, 165, 250, 0.2);
}
.sort-dropdown {
  position: relative;
}
.sort-select {
  background: linear-gradient(135deg, rgba(30, 41, 59, 0.8) 0%, rgba(15, 23, 42, 0.9) 100%);
  color: var(--color-text);
  border: 1px solid rgba(96, 165, 250, 0.3);
  border-radius: 6px;
  padding: 0.4rem 0.6rem;
  cursor: pointer;
  font-size: 0.85em;
  font-weight: 600;
  transition: all 0.3s ease;
  min-width: 140px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.sort-select:focus {
  outline: none;
  border: 2px solid #60a5fa;
  background: linear-gradient(135deg, rgba(30, 41, 59, 0.9) 0%, rgba(15, 23, 42, 1) 100%);
  box-shadow: 0 4px 16px rgba(96, 165, 250, 0.3);
}
.sort-order-btn {
  background: linear-gradient(135deg, rgba(30, 41, 59, 0.8) 0%, rgba(15, 23, 42, 0.9) 100%);
  color: #60a5fa;
  border: 1px solid rgba(96, 165, 250, 0.3);
  border-radius: 6px;
  padding: 0.4rem 0.6rem;
  cursor: pointer;
  font-size: 1em;
  font-weight: 600;
  transition: all 0.3s ease;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  min-width: 40px;
}

.sort-order-btn:hover {
  background: linear-gradient(135deg, rgba(96, 165, 250, 0.2) 0%, rgba(59, 130, 246, 0.3) 100%);
  border-color: rgba(96, 165, 250, 0.6);
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(96, 165, 250, 0.3);
}

.sort-order-btn.active {
  background: linear-gradient(135deg, rgba(96, 165, 250, 0.3) 0%, rgba(59, 130, 246, 0.4) 100%);
  border-color: rgba(96, 165, 250, 0.8);
  color: white;
}
</style> 