<template>
  <div id="app">
    <div id="status-bar">
      <router-link to="/" class="logo">
        <h1>omq</h1>
      </router-link>
      <div>
        osu! Music Quiz
      </div>
      <p class="beatmap-count">
        featuring<span class="beatmap-count-span">{{ beatmapCount }}</span>beatmaps
      </p>
      <a :href="discordInviteLink" target="_blank">
<!--        <img v-bind:src="icon_discord" alt="Discord" class="logo_icon" />-->
        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 640 512" class="logo_icon">
          <path :fill='discordIconColor' d="M524.5 69.8a1.5 1.5 0 0 0 -.8-.7A485.1 485.1 0 0 0 404.1 32a1.8 1.8 0 0 0 -1.9 .9 337.5 337.5 0 0 0 -14.9 30.6 447.8 447.8 0 0 0 -134.4 0 309.5 309.5 0 0 0 -15.1-30.6 a1.9 1.9 0 0 0 -1.9-.9A483.7 483.7 0 0 0 116.1 69.1a1.7 1.7 0 0 0 -.8 .7C39.1 183.7 18.2 294.7 28.4 404.4a2 2 0 0 0 .8 1.4A487.7 487.7 0 0 0 176 479.9a1.9 1.9 0 0 0 2.1-.7A348.2 348.2 0 0 0 208.1 430.4a1.9 1.9 0 0 0 -1-2.6 321.2 321.2 0 0 1 -45.9-21.9 a1.9 1.9 0 0 1 -.2-3.1c3.1-2.3 6.2-4.7 9.1-7.1a1.8 1.8 0 0 1 1.9-.3c96.2 43.9 200.4 43.9 295.5 0a1.8 1.8 0 0 1 1.9 .2c2.9 2.4 6 4.9 9.1 7.2a1.9 1.9 0 0 1 -.2 3.1 301.4 301.4 0 0 1 -45.9 21.8 a1.9 1.9 0 0 0 -1 2.6 391.1 391.1 0 0 0 30 48.8 a1.9 1.9 0 0 0 2.1 .7A486 486 0 0 0 610.7 405.7a1.9 1.9 0 0 0 .8-1.4C623.7 277.6 590.9 167.5 524.5 69.8zM222.5 337.6c-29 0-52.8-26.6-52.8-59.2S193.1 219.1 222.5 219.1c29.7 0 53.3 26.8 52.8 59.2C275.3 311 251.9 337.6 222.5 337.6zm195.4 0c-29 0-52.8-26.6-52.8-59.2S388.4 219.1 417.9 219.1c29.7 0 53.3 26.8 52.8 59.2C470.7 311 447.5 337.6 417.9 337.6z"/>
        </svg>
      </a>
      <div v-if="this.me && this.me.id===adminUserId">
        <router-link to="/debug">Debug</router-link>
        <router-link to="/donate">Donation</router-link>
      </div>
      <a href="#" @click.stop="showHelpPage = true">Help</a>
      <router-link to="/log" v-if="this.me.id">Log</router-link>
      <div class="links"/>
      <font-awesome-icon class="icon-darkmode-toggle" v-if="theme === 'lightMode'" :icon="['fas', 'sun']"
                         @click="toggleTheme"/>
      <font-awesome-icon class="icon-darkmode-toggle" v-else :icon="['fas', 'moon']" @click="toggleTheme"/>

      <div class="meInfo" v-if="accessToken && this.me" @click="showOwnProfile = true">
        <img v-bind:src="me.avatar_url" alt="Profile Picture" class="avatar"/>
        <p class="username">{{ me.username }}</p>
      </div>
      <div class="login-div">
        <button v-if="accessToken && this.me" class="button-icon" @click="logout">
          <font-awesome-icon icon="sign-out-alt"/>
        </button>
        <button class="button-icon" @click="login" v-else>
          Login via osu
          <font-awesome-icon icon="sign-in-alt"/>
        </button>
      </div>
    </div>
    <div class="modal-overlay" v-if="showHelpPage" @click="showHelpPage = false">
      <AboutPage ></AboutPage>
    </div>
    <div class="modal-overlay" v-if="showOwnProfile" @click="showOwnProfile = false">
      <UserPage :playerId="me.id"></UserPage>
    </div>
    <br>
    <router-view></router-view>
  </div>
</template>

<script>
import axios from "axios";
import {useUserStore} from "@/stores/userStore";

import icon_discord from "@/assets/discord.svg";
import icon_github from "@/assets/github.svg";
import {getUserData} from "@/service/authService";
import apiService from "@/api/apiService";
import UserPage from "@/components/UserPage.vue";
import AboutPage from "@/components/AboutPage.vue";
import GameLog from "@/components/GameLog.vue";

export default {
  name: 'App',
  components: {
    GameLog,
    AboutPage,
    UserPage
  },
  setup() {
    const userStore = useUserStore();
    userStore.setMe({});

    return { userStore };
  },
  provide() {
    return {
      isDarkMode: false
    }
  },
  data() {
    return {
      accessToken: localStorage.getItem("accessToken"),
      me: {},
      icon_discord: icon_discord,
      icon_github: icon_github,
      discordInviteLink: "https://discord.gg/FPEcThUyjy",
      beatmapCount: 0,
      playerCount: 0,
      theme: 'darkMode',
      showOwnProfile: false,
      showHelpPage: false,
      showLogPage: false,
      adminUserId: process.env.VUE_APP_OSU_ADMINUSERID
    }
  },
  methods: {
    login() {
      // Redirect the user to the osu! OAuth page
      const URL = "https://osu.ppy.sh/oauth/authorize"
      const CLIENT_ID = `${process.env.VUE_APP_OSU_CLIENT_ID}`;
      const REDIRECT_URI = `${process.env.VUE_APP_OSU_REDIRECT_URI}`;
      const SCOPE = "identify";
      const RESPONSE_TYPE = "code";
      const STATE = "state";

      window.location.href = `${URL}?client_id=${CLIENT_ID}&redirect_uri=${REDIRECT_URI}&response_type=${RESPONSE_TYPE}&scope=${SCOPE}&state=${STATE}`;
    },
    logout() {
      localStorage.removeItem("accessToken");
      localStorage.removeItem("refreshToken");
      location.reload();
    },

    getStatistics() {
      apiService.get(`${process.env.VUE_APP_API_URL}/api/statistics`)
          .then((response) => {
            const data = response.data;
            this.beatmapCount = data;
          })
          .catch((error) => {
            console.error(error);
          });
    },

    toggleTheme() {
      this.theme = this.theme === 'lightMode' ? 'darkMode' : 'lightMode';
      localStorage.setItem('theme', this.theme);
      document.documentElement.setAttribute('data-theme', this.theme)
    }
  },

  async created() {
    const token = localStorage.getItem("accessToken");
    const userStore = useUserStore();

    if (token) {
      try {
        this.me = await getUserData(token);
        userStore.setMe(this.me);
      } catch (error) {
        console.error(error);
      }
    }

    this.getStatistics();
  },

  mounted() {
    let localTheme = localStorage.getItem('theme');
    this.theme = localTheme ? localTheme : 'darkMode';
    document.documentElement.setAttribute('data-theme', this.theme)
  },

  computed: {
    discordIconColor() {
      return this.theme === 'lightMode' ? '#000000' : '#ffffff';
    },
  }
}
</script>

<style>
@import url('https://fonts.googleapis.com/css2?family=Sen:wght@400..800&display=swap');
@import url('https://fonts.googleapis.com/css2?family=Noto+Sans+KR:wght@100..900&display=swap');

body {
  background-color: var(--color-body);
  color: var(--color-text);
}

html[data-theme="darkMode"] {
  --color-body: #3c3c3c;
  --color-text: #ffffff;
  --color-disabled: #474747;
  --color-primary: #95a5d6;
  --color-secondary: #2b2b2e;
  --color-gameroom: #2b2b2e;
  --color-player: rgba(117, 117, 117, 0.5);

}

html[data-theme="lightMode"] {
  --color-body: rgb(204, 207, 215);
  --color-text: #151515;
  --color-disabled: #d3d3d3;
  --color-primary: #95a5d6;
  --color-secondary: #ffffff;
  --color-gameroom: #e5e9f5;
  --color-player: rgba(231, 239, 255, 0.5);
}

html, body {
  margin: 0;
  padding: 0;
  overflow-y: hidden;
}

#app {
  font-family: "Sen", "Noto Sans Korean", "serif";
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  text-align: center;
  color: var(--color-text);
  margin-top: 20px;
  box-sizing: border-box;
  padding: 4px;
  min-height: 100vh;
  width: 100%;
}

#status-bar {
  position: fixed;
  top: 0;
  width: 100%;
  height: 60px;
  left: 0;
  right: 0;
  background-color: var(--color-secondary);
  color: var(--color-text);
  padding: 10px;
  text-align: center;
  border-bottom: 1px solid #dee2e6;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 20px;
}

.logo {
  margin-right: 20px;
  margin-left: 10px;
}

a {
  color: inherit;
  text-decoration: none;
}

.links {
  margin-right: auto;
}

.links a.router-link-active {
  color: inherit;
  text-decoration: none;
  border-bottom: 2px solid #1c2c50;
}

.links a.router-link-active:hover {
  color: inherit;
}

.links a:not(.router-link-active) {
  text-decoration: none;
}

.links a:not(.router-link-active):hover {
  color: inherit;
}

.button-icon {
  color: var(--color-text);
  background: none;
  border: none;
  padding: 0;
  cursor: pointer;
  outline: inherit;
}

#status-bar > div {
  display: flex;
  justify-content: flex-end;
  gap: 20px;
}

.username {
  margin-right: 20px;
}

.avatar {
  width: 50px;
  height: 50px;
  border-radius: 50%;
}

.meInfo {
  cursor: pointer;
  display: flex;
  align-items: center;
  padding-right: 32px;
}

.login-div {
  margin-right: 32px;
}

.logo_icon {
  width: 20px;
  height: 20px;
}

.beatmap-count {
  font-size: 0.8em;
  display: flex;
  align-items: flex-end;
  justify-content: center;

}

.beatmap-count-span {
  font-weight: bold;
  font-size: 1.2em;
  justify-content: center;
  margin-left: 8px;
  margin-right: 8px;
}

.icon-darkmode-toggle {
  color: var(--color-text);
  cursor: pointer;
}

.modal-overlay {
  cursor: pointer;
  position: fixed;
  width: 100%;
  height: 100%;
  top: 0;
  left: 0;
  background-color: rgba(0, 0, 0, 0.8);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 999;
}

.modal-close-btn {
  position: relative;
  background-color: var(--color-primary);
  color: var(--color-text);
  border: none;
  cursor: pointer;
  font-size: 1em;
  border-radius: 10px;
  padding: 10px;
  z-index: 1001;
}
</style>
