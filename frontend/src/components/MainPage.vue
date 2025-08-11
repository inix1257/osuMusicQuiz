<script>
import { useUserStore} from '@/stores/userStore';
import LeaderboardPage from "@/components/Leaderboard/LeaderboardPage.vue";
import AnnouncementPage from "@/components/AnnouncementPage.vue";
import GameroomPage from "@/components/Game/GameList.vue";
import apiService from "../api/apiService";
import RoomSettingsModal from "@/components/Modal/RoomSettingsModal.vue";
import RecentDonators from "@/components/RecentDonators.vue";


export default {
  name: 'MainPage',
  components: {
    RecentDonators,
    RoomSettingsModal,
    LeaderboardPage,
    AnnouncementPage,
    GameroomPage
  },
  setup() {
    const userStore = useUserStore();
    return {
      me: userStore.getMe(),
    };
  },
  data() {
    return {
      gamerooms: [],
      userId: "",
      username: "",
      searchTerm: '',
      showPublicRoomsOnly: false,
      sortBy: 'players',
      sortDescending: true,
      isModalOpen: false,
      totalPlayers: [],
      topDonators: [],
      recentDonations: [],
      seasonalPlayers: [],
    };
  },
  methods: {
    openModal() {
      this.isModalOpen = true;

      const token = localStorage.getItem("accessToken");

      if (!token) {
        alert("You need to login to create a game.")
        this.isModalOpen = false;
        this.isLoading = false;
      }
    },

    getGamerooms() {
      apiService.get('/api/gamerooms')
          .then((response) => {
            this.gamerooms = response.data;

            this.gamerooms.sort((a, b) => {
              return new Date(b.creationDate) - new Date(a.creationDate);
            });
          })
          .catch((error) => {
            // handle error
          });
    },

    getGameroomCount() {
      return this.gamerooms.length;
    },

    getGameroomPlayers() {
      var count = 0;

      this.gamerooms.forEach((gameroom) => {
        count += gameroom.players.length;
      });

      return count;
    },
  },

  mounted() {
      this.getGamerooms();

      apiService.get('/api/leaderboard').then((response) => {
        this.totalPlayers = response.data.topPlayers;
        this.topDonators = response.data.topDonators;
        this.seasonalPlayers = response.data.topRecentPlayers;
        this.recentDonations = response.data.recentDonations;
      });
  },

  computed: {
    filteredGamerooms() {
      let rooms = this.gamerooms;

      // Filter rooms by searchTerm
      if (this.searchTerm) {
        const lowerCaseSearchTerm = this.searchTerm.toLowerCase();
        rooms = rooms.filter(room =>
            room.name.toLowerCase().includes(lowerCaseSearchTerm) ||
            room.players.some(player => player.username.toLowerCase().includes(lowerCaseSearchTerm))
        );
      }

      // Filter public rooms if showPublicRoomsOnly is true
      if (this.showPublicRoomsOnly) {
        rooms = rooms.filter(room => !room.private);
      }

      // Sort rooms
      if (this.sortBy === 'players') {
        rooms.sort((a, b) => this.sortDescending ? b.players.length - a.players.length : a.players.length - b.players.length);
      } else if (this.sortBy === 'creationDate') {
        rooms.sort((a, b) => this.sortDescending ? new Date(b.creationDate) - new Date(a.creationDate) : new Date(a.creationDate) - new Date(b.creationDate));
      }

      return rooms;
    },

    gameroomsVisibility() {
      if (useUserStore().getMe().id) {
        return true;
      }else {
        return localStorage.getItem("accessToken") == null;
      }
    },
  },
};
</script>

<template>
  <div id="app">
    <div class="main-grid">
      <div class="gameroom-container">
        <RecentDonators :donations="recentDonations" />

        <div class="gameroom-upper-container">

          <div class="gameroom-statistics">
            <span class="gameroom-statistics-number">{{ getGameroomCount() }}</span> lobbies, <span
              class="gameroom-statistics-number">{{ getGameroomPlayers() }}</span> players
          </div>

          <input type="text" v-model="searchTerm" placeholder="Search game rooms by title or player..." class="gameroom-searchinput">
          <button @click="getGamerooms" class="button-searchbar">
            <font-awesome-icon :icon="['fas', 'arrows-rotate']" />
          </button>
          <button @click="openModal" class="button-searchbar button-create">
            <font-awesome-icon :icon="['fas', 'plus']" />
          </button>



        </div>
        <div class="gameroom-upper-searchoptions-container" v-if="false">
          <div>
            <input type="checkbox" id="publicRooms" v-model="showPublicRoomsOnly">
            <label for="publicRooms">Show Public Rooms Only</label>
          </div>
          <div>
            <select v-model="sortBy" class="dropdown">
              <option value="players">Sort by Players</option>
              <option value="creationDate">Sort by Creation Date</option>
            </select>
          </div>
          <div>
            <input type="checkbox" id="sortDescending" v-model="sortDescending">
            <label for="sortDescending">DESC</label>
          </div>
        </div>
        <div class="gameroom-list" v-if="gameroomsVisibility">
          <GameroomPage v-for="gameroom in filteredGamerooms" :key="gameroom.uuid" :gameroom="gameroom" :me="me"/>
        </div>

      </div>

      <div class="info-div">
        <LeaderboardPage :totalPlayers="totalPlayers" :topDonators="topDonators" :seasonalPlayers="seasonalPlayers" />
      </div>

      <RoomSettingsModal v-if="isModalOpen" actionType="create" @close-modal="isModalOpen = false" />
    </div>
  </div>
</template>

<style scoped>
.main-grid {
  display: grid;
  grid-template-columns: 3fr 1fr;
  height: 80vh;
  gap: 30px;
  padding: 20px;
}

.info-div {
  display: flex;
  flex-direction: column;
  align-items: center;
  height: 100%;
  padding: 20px;
  gap: 20px;
  background: linear-gradient(135deg, var(--color-body) 0%, rgba(255, 255, 255, 0.02) 100%);
  border-radius: 25px;
  box-shadow: 0 15px 35px rgba(0, 0, 0, 0.15);
  border: 1px solid rgba(255, 255, 255, 0.05);
}

.gameroom-container {
  display: flex;
  flex-direction: column;
  gap: 10px;
  height: 80vh;
  width: 65vw;
  padding: 20px;
  background: linear-gradient(135deg, var(--color-body) 0%, rgba(255, 255, 255, 0.02) 100%);
  border-radius: 25px;
  box-shadow: 0 15px 35px rgba(0, 0, 0, 0.15);
  border: 1px solid rgba(255, 255, 255, 0.05);
  overflow: hidden;
}

.gameroom-list {
  border-radius: 15px;
  padding: 15px;
  flex: 1;
  overflow: auto;
  background: linear-gradient(135deg, var(--color-secondary) 0%, rgba(255, 255, 255, 0.05) 100%);
  border: 1px solid rgba(255, 255, 255, 0.1);
  box-shadow: 0 8px 25px rgba(0, 0, 0, 0.1);
  min-height: 0;
}

.button-searchbar {
  border: 1px solid rgba(255, 255, 255, 0.2);
  outline: none;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.1), rgba(255, 255, 255, 0.05));
  color: var(--color-text);
  padding: 12px 16px;
  margin: 0 2px;
  font-size: 1em;
  font-family: Sen, Arial, sans-serif;
  cursor: pointer;
  border-radius: 10px;
  transition: all 0.3s ease;
  font-weight: 500;
  display: flex;
  align-items: center;
  gap: 8px;
}

.button-create {
  background: linear-gradient(135deg, #60a5fa, #3b82f6);
  color: white;
  border-color: rgba(96, 165, 250, 0.4);
  font-weight: 600;
}

.button-searchbar:hover {
  background: linear-gradient(135deg, rgba(96, 165, 250, 0.2), rgba(59, 130, 246, 0.1));
  border-color: rgba(96, 165, 250, 0.4);
  transform: translateY(-2px);
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.15);
  color: #60a5fa;
}

.button-create:hover {
  background: linear-gradient(135deg, #3b82f6, #1d4ed8);
  transform: translateY(-2px);
  box-shadow: 0 4px 15px rgba(96, 165, 250, 0.3);
}

.owner-highlight .avatar {
  border: 2px solid #f0f0f0; /* Add a border to the owner's avatar */
}

.gameroom-upper-container {
  display: flex;
  flex-direction: row;
  justify-content: center;
  align-items: center;
  gap: 15px;
  padding: 12px;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.05) 0%, rgba(255, 255, 255, 0.02) 100%);
  border-radius: 15px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.1);
  flex-shrink: 0;
}

.gameroom-upper-searchoptions-container {
  display: flex;
  flex-direction: row;
  align-items: center;
  gap: 16px;
  margin-bottom: 8px;
}

.gameroom-statistics {
  flex-direction: column;
  justify-content: center;
  width: 20vw;
  padding: 15px 20px;
  border-radius: 12px;
  margin: 4px;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.1), rgba(255, 255, 255, 0.05));
  border: 1px solid rgba(255, 255, 255, 0.15);
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.1);
  transition: all 0.3s ease;
  font-weight: 500;
  text-align: center;
}

.gameroom-statistics:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 25px rgba(0, 0, 0, 0.15);
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.15), rgba(255, 255, 255, 0.08));
}

.gameroom-statistics-number {
  font-weight: 700;
  font-size: 1.2em;
  color: #60a5fa;
  background: linear-gradient(45deg, #60a5fa, #3b82f6);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  text-shadow: 0 1px 2px rgba(96, 165, 250, 0.3);
}

.gameroom-searchinput {
  width: 100%;
  padding: 12px 16px;
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 10px;
  font-size: 16px;
  color: var(--color-text);
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.1), rgba(255, 255, 255, 0.05));
  font-family: 'Sen', serif;
  transition: all 0.3s ease;
  outline: none;
}

.gameroom-searchinput:focus {
  border-color: rgba(96, 165, 250, 0.5);
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.15), rgba(255, 255, 255, 0.08));
  box-shadow: 0 4px 15px rgba(96, 165, 250, 0.2);
  transform: translateY(-1px);
}

.gameroom-searchinput::placeholder {
  color: var(--color-text);
  opacity: 0.6;
}

.dropdown {
  font-family: Sen, Arial, sans-serif;
  padding: 6px;
  border-radius: 5px;
  border: 1px solid #ccc;
  background-color: var(--color-secondary);
  font-size: 16px;
  color: var(--color-text);
}

.dropdown:disabled {
  background-color: var(--color-disabled);
}

</style>