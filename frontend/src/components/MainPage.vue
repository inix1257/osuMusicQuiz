<script>
import { useUserStore} from '@/stores/userStore';
import LeaderboardPage from "@/components/Leaderboard/LeaderboardPage.vue";
import AnnouncementPage from "@/components/AnnouncementPage.vue";
import GameroomPage from "@/components/Game/GameList.vue";
import apiService from "../api/apiService";
import RoomSettingsModal from "@/components/Modal/RoomSettingsModal.vue";


export default {
  name: 'MainPage',
  components: {
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
      isModalOpen: false
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
        return;
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
        <div class="gameroom-upper-container">
          <div class="gameroom-statistics">
            <span class="gameroom-statistics-number">{{ getGameroomCount() }}</span> lobbies, <span
              class="gameroom-statistics-number">{{ getGameroomPlayers() }}</span> players online
          </div>

          <input type="text" v-model="searchTerm" placeholder="Search game rooms by title or player..." class="gameroom-searchinput">

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
        <div class="button-row">
          <button @click="getGamerooms" class="refresh-button">Refresh List</button>
          <button @click="openModal" class="create-game-button">Create Game</button>
        </div>
        <LeaderboardPage />
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
}

.info-div {
  display: flex;
  flex-direction: column;
  align-items: center;
  height: 100%;
  padding-right: 20px;
  gap: 20px;
}

.gameroom-container {
  grid-template-columns: 1fr; /* Create a 1-column grid */
  gap: 20px; /* Add some space between the game rooms */
  margin-top: 20px;
  height: 80vh;
  width: 65vw;
}

.gameroom-list {
  border-radius: 10px;
  padding: 10px;
  height: 100%;
  overflow: auto;
}

.create-game-button {
  border: none;
  outline: none;
  background-color: var(--color-secondary);
  color: var(--color-text);
  padding: 10px 20px;
  font-size: 1em;
  font-family: Sen, Arial, sans-serif;
  cursor: pointer;
  border-radius: 10px;
  transition: background-color 0.3s ease;
}

.create-game-button:hover {
  background-color: #aac9e8;
}

.owner-highlight .avatar {
  border: 2px solid #f0f0f0; /* Add a border to the owner's avatar */
}

.refresh-button {
  border: none;
  outline: none;
  background-color: var(--color-secondary);
  color: var(--color-text);
  padding: 10px 20px;
  font-size: 1em;
  font-family: Sen, Arial, sans-serif;
  cursor: pointer;
  border-radius: 10px;
  transition: background-color 0.3s ease;
}

.refresh-button:hover {
  background-color: #aac9e8;
}

.button-row {
  margin-top: 20px;
  display: flex;
  justify-content: center;
  gap: 20px;
  width: 100%;
}





.gameroom-upper-container {
  display: flex;
  flex-direction: row;
  justify-content: center;
  align-items: center;
  margin-bottom: 8px;
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
  padding: 10px;
  border-radius: 5px;
  margin: 4px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.12), 0 1px 2px rgba(0,0,0,0.24);
  transition: all 0.3s cubic-bezier(.25,.8,.25,1);
  background-color: var(--color-secondary);
}

.gameroom-statistics-number {
  font-weight: bold;
}

.gameroom-searchinput {
  width: 100%;
  padding: 10px;
  border: 1px solid #ccc;
  border-radius: 5px;
  font-size: 16px;
  color: var(--color-text);
  background-color: var(--color-secondary);
  font-family: 'Sen', serif;
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