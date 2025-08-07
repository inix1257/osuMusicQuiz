<template>
  <div class="beatmap-request-page">
    <h1>Beatmap Requests</h1>
    <form @submit.prevent="submitRequest">
      <input
        v-model="newRequestId"
        type="text"
        placeholder="Enter beatmap set ID"
        required
        class="themed-input"
      />
      <button type="submit" class="themed-btn">Submit Request</button>
    </form>

    <!-- Search and Sort Area -->
    <div class="search-sort-area">
      <input
        v-model="searchQuery"
        type="text"
        placeholder="Search by title, artist, or creator"
        class="themed-input search-bar"
      />
      <div class="sort-toggles">
        <div class="sort-toggle">
          <span>Sort by Votes</span>
          <button @click="toggleSort('votes')" :class="{ active: sortBy === 'votes' }">
            {{ sortBy === 'votes' ? (sortOrder === 'asc' ? '▲' : '▼') : '↕' }}
          </button>
        </div>
        <div class="sort-toggle">
          <span>Sort by Ranked Date</span>
          <button @click="toggleSort('rankedDate')" :class="{ active: sortBy === 'rankedDate' }">
            {{ sortBy === 'rankedDate' ? (sortOrder === 'asc' ? '▲' : '▼') : '↕' }}
          </button>
        </div>
      </div>
    </div>

    <div class="requests-list">
      <div v-for="(request, idx) in filteredAndSortedRequests" :key="request.id">
        <a :href="`https://osu.ppy.sh/beatmapsets/${request.beatmapSetId}`" target="_blank" class="beatmap-link">
          <div class="request-item" :style="{ backgroundImage: `url(https://assets.ppy.sh/beatmaps/${request.beatmapSetId}/covers/card.jpg)` }">
            <img
              class="beatmap-thumb"
              :src="`https://assets.ppy.sh/beatmaps/${request.beatmapSetId}/covers/list.jpg`"
              :alt="request.title"
            />
            <div class="beatmap-meta">
              <div class="beatmap-title">{{ request.title || `Beatmap #${request.beatmapSetId}` }}</div>
              <div class="beatmap-artist" v-if="request.artist">by {{ request.artist }}</div>
              <div class="beatmap-creator" v-if="request.creator">mapped by {{ request.creator }}</div>
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
    toggleSort(type) {
      if (this.sortBy === type) {
        this.sortOrder = this.sortOrder === 'asc' ? 'desc' : 'asc';
      } else {
        this.sortBy = type;
        this.sortOrder = 'desc';
      }
    },
  },
  mounted() {
    this.fetchRequests();
  },
};
</script>

<style scoped>
.beatmap-request-page {
  max-width: 700px;
  margin: 0 auto;
  padding: 2rem 2rem 0 2rem;
  height: 100vh;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
form {
  display: flex;
  flex-wrap: wrap;
  gap: 1rem;
  margin-bottom: 2rem;
}
.themed-input {
  flex: 1 1 180px;
  padding: 0.5rem;
  border: 1px solid var(--color-disabled);
  border-radius: 4px;
  background: var(--color-bodysecondary);
  color: var(--color-text);
  transition: border 0.2s, background 0.2s;
}
.themed-input:focus {
  outline: none;
  border: 1.5px solid var(--color-primary);
  background: var(--color-body);
}
.themed-btn {
  padding: 0.5rem 1.2rem;
  background: var(--color-primary);
  color: var(--color-text);
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-weight: bold;
  transition: background 0.2s;
}
.themed-btn:hover {
  background: var(--color-secondary);
}
.requests-list {
  margin-top: 2rem;
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  padding-bottom: 8rem;
}

.beatmap-link {
  text-decoration: none;
  color: inherit;
  display: block;
  margin-bottom: 1rem;
}

.beatmap-link:hover {
  text-decoration: none;
}
.request-item {
  display: flex;
  align-items: center;
  gap: 1.5rem;
  border-bottom: 1px solid #eee;
  border-radius: 8px;
  margin-bottom: 1rem;
  box-shadow: 0 1px 4px rgba(0,0,0,0.03);
  color: var(--color-text);
  position: relative;
  background-size: cover;
  background-position: center;
  background-repeat: no-repeat;
  padding: 1rem;
  overflow: hidden;
}

.request-item::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.7);
  z-index: 1;
}

.request-item > * {
  position: relative;
  z-index: 2;
}
.beatmap-thumb {
  width: 90px;
  height: 100%;
  object-fit: cover;
  border-radius: 6px;
  border: 1px solid #ddd;
  background: #eee;
}
.beatmap-meta {
  flex: 1;
  min-width: 0;
  text-align: left;
}
.beatmap-title {
  font-size: 1.1em;
  font-weight: bold;
  color: var(--color-text);
  margin-bottom: 0.2em;
  word-break: break-all;
}
.beatmap-artist,
.beatmap-creator {
  color: var(--color-subtext);
  font-size: 0.98em;
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
  gap: 0.2rem;
  min-width: 40px;
}
.votes button {
  background: none;
  border: none;
  font-size: 1.3em;
  cursor: pointer;
  color: var(--color-primary);
  transition: color 0.15s;
  padding: 0;
  position: relative;
  z-index: 10;
}
.votes button:hover {
  color: var(--color-secondary);
}
.votes .admin-btn {
  background: var(--color-primary);
  color: var(--color-text);
  border: none;
  border-radius: 4px;
  padding: 0.3rem 0.6rem;
  cursor: pointer;
  font-size: 0.8em;
  font-weight: bold;
  transition: background 0.2s;
  margin-top: 0.3rem;
  position: relative;
  z-index: 10;
}
.votes .admin-btn:hover {
  background: var(--color-secondary);
}
.notification-push {
  position: fixed;
  bottom: 20px;
  right: 20px;
  background-color: #e53f3f;
  color: white;
  padding: 10px 18px;
  border-radius: 5px;
  z-index: 1000;
  font-size: 1.1em;
  box-shadow: 0 2px 8px rgba(0,0,0,0.12);
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

.search-sort-area {
  display: flex;
  align-items: center;
  gap: 1.5rem;
  margin-bottom: 1.5rem;
}
.search-bar {
  flex: 2 1 250px;
  min-width: 180px;
}
.sort-toggles {
  display: flex;
  gap: 1.2rem;
}
.sort-toggle {
  display: flex;
  align-items: center;
  gap: 0.4rem;
}
.sort-toggle button {
  background: var(--color-secondary);
  color: var(--color-disabled);
  border: none;
  border-radius: 4px;
  padding: 0.2rem 0.7rem;
  cursor: pointer;
  font-size: 1em;
  font-weight: bold;
  transition: background 0.2s;
}
.sort-toggle button.active,
.sort-toggle button:hover {
  background: var(--color-primary);
  color: var(--color-text);
}
</style> 