<script>
import apiService from "@/api/apiService";
import moment from 'moment';

export default {
  name: "GameLog",
  data() {
    return {
      gameLog: [],
      groupedGameLog: {}
    }
  },
  mounted() {
    this.getGameLog()
  },

  methods: {
    getGameLog() {
      apiService.get('/api/history')
          .then((response) => {
            const grouped = response.data.reduce((acc, history) => {
              const sessionId = history.lobbyHistory.session_id;
              const lobbyName = history.lobbyHistory.lobby_title;
              const time = history.lobbyHistory.time;
              if (!acc[sessionId]) {
                acc[sessionId] = {
                  name: lobbyName,
                  time: time,
                  histories: [],
                  collapsed: true,
                  showIncorrectAnswersOnly: false,
                };
              }
              acc[sessionId].histories.push(history);
              return acc;
            }, {});

            // Compute statistics for each group
            Object.keys(grouped).forEach(sessionId => {
              const stats = this.getStatistics(grouped[sessionId].histories);
              grouped[sessionId].stats = stats; // Store computed statistics in the group object
            });

            const sortedGrouped = Object.fromEntries(
                Object.entries(grouped).sort(([, a], [, b]) => new Date(b.time) - new Date(a.time))
            );

            this.groupedGameLog = sortedGrouped;
          })
    },

    toggleCollapse(sessionId) {
      this.groupedGameLog[sessionId].collapsed = !this.groupedGameLog[sessionId].collapsed;
    },

    toggleShowIncorrectAnswersOnly(sessionId) {
      this.groupedGameLog[sessionId].showIncorrectAnswersOnly = !this.groupedGameLog[sessionId].showIncorrectAnswersOnly;
    },

    formatTime(time) {
      return moment(time).format('YYYY-MM-DD HH:mm:ss'); // Customize the format as needed
    },

    getStatistics(histories) {
      let correctAnswers = 0;
      let totalAnswers = 0;
      let totalTimeTaken = 0;
      let totalPoints = 0;

      histories.forEach(history => {
        totalAnswers++;
        if (history.answer) {
          correctAnswers++;
          totalPoints += history.difficulty_bonus * history.speed_bonus * history.lobbyHistory.poolsize_bonus;
        }
        totalTimeTaken += history.timetaken;
      });

      const averageTimeTaken = totalAnswers > 0 ? totalTimeTaken / totalAnswers : 0;

      return {
        correctAnswers,
        totalAnswers,
        averageTimeTaken,
        totalPoints
      };
    },
  },

  computed: {
    filteredHistories() {
      return (sessionId) => {
        const group = this.groupedGameLog[sessionId];
        if (group.showIncorrectAnswersOnly) {
          return group.histories.filter(history => !history.answer);
        }
        return group.histories;
      };
    }
  }
}
</script>

<template>
  <div class="gamelog-div" v-if="groupedGameLog">
    <div v-for="(group, sessionId) in groupedGameLog" :key="sessionId">
      <h3 @click="toggleCollapse(sessionId)" class="session-id">{{ group.name }} ({{ formatTime(group.time) }})</h3> <!-- Clickable session ID -->
      <div class="stats-container">
        <p>Correct Answers: {{ group.stats.correctAnswers }}/{{ group.stats.totalAnswers }}</p>
        <p>Average Time Taken: {{ group.stats.averageTimeTaken.toFixed(1) }}ms</p>
        <p>Total Points: {{ group.stats.totalPoints.toFixed(2) }}pts</p>
        <label v-if="!group.collapsed">
          <input type="checkbox" @change="toggleShowIncorrectAnswersOnly(sessionId)">
          Show Incorrect Answers Only
        </label>
      </div>
      <table v-if="!group.collapsed">
        <tr>
          <th class="col-index">Index</th>
          <th class="col-main">Beatmap</th>
          <th class="col-sub">Difficulty</th>
          <th class="col-sub">Difficulty Bonus</th>
          <th class="col-sub">Time Taken</th>
          <th class="col-sub">Speed Bonus</th>
          <th class="col-sub">Pool Size Bonus</th>
          <th class="col-sub">Total Points</th>
        </tr>
        <tr v-for="(history, index) in filteredHistories(sessionId)" :key="index" class="history-row">
          <td class="col-index">{{ index + 1 }}</td>
          <td class="col-main" :class="{'answer-true': history.answer, 'answer-false': !history.answer}"><p><a :href="`https://osu.ppy.sh/beatmapsets/${history.beatmap.beatmapset_id}`" target="_blank">{{ history.beatmap.artistAndTitle }}</a></p></td>
          <td class="col-sub">{{ history.difficulty }}</td>
          <td class="col-sub">{{ history.difficulty_bonus.toFixed(2) }}</td>
          <td class="col-sub">{{ history.timetaken }}ms</td>
          <td class="col-sub">{{ history.speed_bonus.toFixed(2) }}</td>
          <td class="col-sub">{{ history.lobbyHistory.poolsize_bonus.toFixed(2) }}</td>
          <td class="col-sub">{{ history.answer ? (history.speed_bonus * history.difficulty_bonus * history.lobbyHistory.poolsize_bonus).toFixed(2) : 0 }}</td>
        </tr>
      </table>
      <hr>
    </div>
  </div>
  <div v-else>
    <p>Loading...</p>
  </div>
</template>

<style scoped>
.gamelog-div {
  background-color: var(--color-body);
  border-radius: 20px;
  margin-top: 20px;
  padding: 20px;
  box-shadow: 0px 10px 20px rgba(0, 0, 0, 0.1);
  overflow-y: auto;
  max-height: 85vh;
}

table {
  background-color: var(--color-secondary);
  border-radius: 10px;
  width: 80%; /* Adjust based on your preference */
  margin-left: auto;
  margin-right: auto;
  border-collapse: collapse; /* Ensures borders are neat */
}

hr {
  margin-top: 20px;
  margin-bottom: 20px;
  background-color: var(--color-text);
  width: 20%;
}

tr {

}

th, td {
  padding: 8px; /* Spacing inside cells */
  text-align: left; /* Align text to the left inside cells */
}

.col-index, .col-time {
  width: 5%; /* Adjust as needed to make these columns narrower */
}

.col-main {
  width: 60%; /* Allows this column to take up most of the space */
}

.col-sub {
  width: 8%; /* Adjust as needed to make these columns narrower */
}

.centered-table {
  margin-left: auto;
  margin-right: auto;
}

.history-row {
  line-height: 0;
}

.session-id {
  cursor: pointer;
  background-color: var(--color-secondary);
  display: inline-block; /* Make the element take only as much space as its content */
  width: auto;
  padding: 12px;
  border-radius: 10px;
}

.answer-true {
  color: rgba(0, 255, 0, 1);
}

.answer-false {
  color: rgba(255, 0, 0, 1);
}
</style>