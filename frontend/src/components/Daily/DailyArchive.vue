<script>
import apiService from "@/api/apiService";

export default {
  name: "DailyArchive",
  data() {
    return {
      dailyData: [],
      currentMonth: new Date().getUTCMonth(),
      currentYear: new Date().getUTCFullYear(),
      startDate: new Date(Date.UTC(2024, 10, 18)), // November 18, 2024
    };
  },
  methods: {
    async fetchData() {
      const response = await apiService.get('/api/dailyarchive');
      this.dailyData = response.data;
    },
    daysInMonth(month, year) {
      return new Date(Date.UTC(year, month + 1, 0)).getUTCDate();
    },
    isOngoing(day) {
      const dailyId = this.calculateDailyId(day);
      return this.dailyData.some(daily => daily.dailyNumber === dailyId && daily.retryCount !== 5 && !daily.guessed);
    },
    isPlayed(day) {
      const dailyId = this.calculateDailyId(day);
      return this.dailyData.some(daily => daily.dailyNumber === dailyId);
    },
    isFuture(day) {
      const today = new Date(Date.UTC(new Date().getUTCFullYear(), new Date().getUTCMonth(), new Date().getUTCDate()));
      return day > today;
    },
    previousMonth() {
      if (this.currentMonth === this.startDate.getUTCMonth() && this.currentYear === this.startDate.getUTCFullYear()) {
        return;
      }
      if (this.currentMonth === 0) {
        this.currentMonth = 11;
        this.currentYear--;
      } else {
        this.currentMonth--;
      }
    },
    nextMonth() {
      const today = new Date(Date.UTC(new Date().getUTCFullYear(), new Date().getUTCMonth(), new Date().getUTCDate()));
      if (this.currentMonth === today.getUTCMonth() && this.currentYear === today.getUTCFullYear()) {
        return;
      }
      if (this.currentMonth === 11) {
        this.currentMonth = 0;
        this.currentYear++;
      } else {
        this.currentMonth++;
      }
    },
    getDaysArray() {
      const daysArray = [];
      const firstDay = new Date(Date.UTC(this.currentYear, this.currentMonth, 1)).getUTCDay();
      const daysInCurrentMonth = this.daysInMonth(this.currentMonth, this.currentYear);

      // Add empty slots for days of the previous month
      for (let i = 0; i < firstDay; i++) {
        daysArray.push(null);
      }

      // Add days of the current month
      for (let i = 1; i <= daysInCurrentMonth; i++) {
        daysArray.push(new Date(Date.UTC(this.currentYear, this.currentMonth, i)));
      }

      return daysArray;
    },
    calculateDailyId(date) {
      if (!date) return '';
      const timeDiff = date - this.startDate;
      const dailyId = Math.floor(timeDiff / (1000 * 60 * 60 * 24)) + 1;
      return dailyId > 0 ? dailyId : '';
    },

    openDaily(day) {
      if (!day) return;

      if (this.isFuture(day) || !this.calculateDailyId(day)) {
        return;
      }

      this.$emit('open-daily', this.calculateDailyId(day));
    },

    getTodayDaily() {
      const today = new Date(Date.UTC(new Date().getUTCFullYear(), new Date().getUTCMonth(), new Date().getUTCDate()));
      this.openDaily(today);
    }
  },
  async created() {
    await this.fetchData();
  },
}
</script>

<template>
  <div>
    <div class="calendar-nav">
      <button class="nav-button" @click.stop="previousMonth"><font-awesome-icon :icon="['fas', 'caret-left']" /></button>
      <span class="calendar-title">{{ currentMonth + 1 }}/{{ currentYear }}</span>
      <button class="nav-button" @click.stop="nextMonth"><font-awesome-icon :icon="['fas', 'caret-right']" /></button>
    </div>
    <div class="calendar">
      <div class="calendar-header">
        <div>Sun</div>
        <div>Mon</div>
        <div>Tue</div>
        <div>Wed</div>
        <div>Thu</div>
        <div>Fri</div>
        <div>Sat</div>
      </div>
      <div class="calendar-body">
        <div v-for="day in getDaysArray()" :key="day" :class="{'ongoing': isOngoing(day), 'played': isPlayed(day), 'disabled': isFuture(day)}"
             class="calendar-day" @click.stop="openDaily(day)">
          {{ day ? calculateDailyId(day) : '' }}
        </div>
      </div>
    </div>
    <div class="today-button-container">
      <button class="today-button" @click.stop="getTodayDaily">Go to Today's osudle</button>
    </div>
  </div>
</template>

<style scoped>
.calendar-nav {
  display: flex;
  justify-content: space-between;
  margin-bottom: 10px;
}

.nav-button {
  appearance: none;
  background-color: #f0f0f0;
  border: 1px solid #ccc;
  border-radius: 5px;
  padding: 5px 10px;
  cursor: pointer;
}

.nav-button:hover {
  background-color: #e0e0e0;
}

.calendar-title {
  font-weight: bold;
  font-size: 1.2em;
}

.calendar {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 5px;
}

.calendar-header {
  display: contents;
}

.calendar-header div {
  font-weight: bold;
  text-align: center;
}

.calendar-body {
  display: contents;
}

.calendar-day {
  width: 2em;
  height: 2em;
  padding: 10px;
  border: 1px solid #ccc;
  border-radius: 5px;
  text-align: center;
  justify-content: center;
  align-content: center;
}

.played {
  background-color: #adbfb2;
  border-color: #c3e6cb;
}

.ongoing {
  background-color: rgb(236, 236, 119);
  border-color: #ffeeba;
}

.disabled {
  background-color: var(--color-disabled);
  border-color: var(--color-subtext);
  opacity: 0.6;
}

.today-button-container {
  display: flex;
  justify-content: center;
  margin-top: 10px;
}

.today-button {
  font-family: "Sen", "Noto Sans Korean", "serif";
  background-color: #4CAF50;
  color: white;
  border: none;
  border-radius: 5px;
  padding: 10px 20px;
}

.today-button:hover {
  background-color: #45a049;
}
</style>