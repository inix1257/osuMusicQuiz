<template>
  <div class="announcement">
    <h1>News</h1>
    <div v-for="(announcement, index) in announcements" :key="index" class="announcement-div">
      <div class="announcement-header">
        <div class="announcement-title">{{ announcement.title }}</div>
        <div>{{ formatDate(announcement.date) }}</div>
      </div>
      <div class="announcement-content">{{ announcement.content }}</div>
    </div>
  </div>
</template>

<script>
import axios from 'axios';
import moment from "moment/moment";
import apiService from "@/api/apiService";

export default {
  data() {
    return {
      announcements: [],
    };
  },
  methods: {
    formatDate(date) {
      return moment(date).format('MMMM Do YYYY');
    },
  },

  async created() {
    const response = await apiService.get('/api/announcement');
    this.announcements = response.data;
  },
};
</script>

<style scoped>
.announcement {
  background-color: var(--color-secondary);
  border-radius: 20px;
  padding: 20px;
  box-shadow: 0px 10px 20px rgba(0, 0, 0, 0.1);
  width: 100%;
}

.announcement-header {
  display: flex;
  justify-content: space-between;
}

.announcement-title {
  font-size: 1.2em;
  font-weight: bold;
}

.announcement-div {
  margin-bottom: 30px;
}

.announcement-content {
  text-align: left;
}

h1 {
  font-size: 2em;
  font-weight: bold;
  margin-bottom: 20px;
}

h2 {
  font-size: 1.5em;
  font-weight: bold;
  line-height: 1;
  margin-bottom: 10px;
}

p {
  font-size: 1em;
  color: #666;
  line-height: 0;
  margin-bottom: 10px;
}

h1, h2, p {
  text-align: left;
}
</style>