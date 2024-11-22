import { createApp } from 'vue';
import { createRouter, createWebHistory } from 'vue-router';
import MainPage from './components/MainPage.vue';
import App from "@/App.vue";
import IntroPage from "@/components/IntroPage.vue";
import GamePage from "@/components/Game/GamePage.vue";
import CallBack from "@/CallBack.vue";
import { pinia } from './store';
import { library } from '@fortawesome/fontawesome-svg-core'
import { faSignInAlt, faSignOutAlt } from '@fortawesome/free-solid-svg-icons'
import { fas, faR } from '@fortawesome/free-solid-svg-icons'
import { faM, faI, faC } from '@fortawesome/free-solid-svg-icons'
import { faCheck } from '@fortawesome/free-solid-svg-icons'
import { faDoorOpen } from '@fortawesome/free-solid-svg-icons'
import { faLock } from '@fortawesome/free-solid-svg-icons'
import { faXmark } from "@fortawesome/free-solid-svg-icons";
import { faEye, faEyeSlash } from "@fortawesome/free-solid-svg-icons";
import { faVolumeHigh, faVolumeXmark } from "@fortawesome/free-solid-svg-icons";
import { faGear } from "@fortawesome/free-solid-svg-icons";
import { faCaretUp, faCaretDown } from "@fortawesome/free-solid-svg-icons";
import { faMoon, faSun } from "@fortawesome/free-solid-svg-icons";
import { faMinimize, faMaximize } from "@fortawesome/free-solid-svg-icons";
import { faCrown } from "@fortawesome/free-solid-svg-icons";
import { faShareNodes } from "@fortawesome/free-solid-svg-icons";
import { faPlay, faStop } from "@fortawesome/free-solid-svg-icons";
import { faMagnifyingGlass } from "@fortawesome/free-solid-svg-icons";
import { faHeart } from "@fortawesome/free-solid-svg-icons";
import { FontAwesomeIcon } from '@fortawesome/vue-fontawesome'
import FullLeaderboard from "@/components/FullLeaderboard.vue";
import DebugPage from "@/components/DebugPage.vue";
import UserPage from "@/components/UserPage.vue";
import AboutPage from "@/components/AboutPage.vue";
import GameLog from "@/components/GameLog.vue";

const router = createRouter({
    history: createWebHistory(),
    routes: [
        {
            path: '/',
            component: MainPage,
        },
        {
            path: '/debug',
            name: 'Debug',
            component: DebugPage,
        },
        {
            name: 'GamePage',
            path: '/gamepage/:gameId/:private',
            component: GamePage,
        },
        {
            name: 'Leaderboard',
            path: '/leaderboard',
            component: FullLeaderboard,
        },
        {
            name: 'UserPage',
            path: '/profile',
            component: UserPage
        },
        {
            name: 'AboutPage',
            path: '/help',
            component: AboutPage
        },
        {
            name: 'GameLog',
            path: '/log',
            component: GameLog
        },
        {
            name: 'Callback',
            path: '/callback',
            component: CallBack,
        },
        {
            path: '/*',
            redirect: '/notFound',
        }
    ]
})

library.add(faSignInAlt, faSignOutAlt)
library.add(fas, faR)
library.add(faM, faI, faC)
library.add(faDoorOpen)
library.add(faCheck)
library.add(faLock)
library.add(faXmark)
library.add(faEye, faEyeSlash)
library.add(faVolumeHigh, faVolumeXmark)
library.add(faGear)
library.add(faCaretDown, faCaretUp)
library.add(faMoon, faSun)
library.add(faMinimize, faMaximize)
library.add(faCrown)
library.add(faShareNodes)
library.add(faMagnifyingGlass)
library.add(faPlay, faStop)
library.add(faHeart)

const app = createApp(App);
app.use(router)
    .use(pinia)
    .component('font-awesome-icon', FontAwesomeIcon)
    .mount('#app');
