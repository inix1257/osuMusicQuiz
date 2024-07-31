// src/stores/userStore.js
import { defineStore } from 'pinia';

export const useUserStore = defineStore({
    id: 'user',
    state: () => ({
        me: {},
    }),
    actions: {
        setMe(me) {
            this.me = me;
        },
        getMe() {
            return this.me;
        }
    },
});