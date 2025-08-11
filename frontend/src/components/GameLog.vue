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

    handleImageError(event) {
      // Hide the image if it fails to load
      event.target.style.display = 'none';
    },

    openBeatmap(beatmapsetId) {
      // Open beatmap in new tab
      window.open(`https://osu.ppy.sh/beatmapsets/${beatmapsetId}`, '_blank');
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
      <div @click="toggleCollapse(sessionId)" class="lobby-header">
        <div class="lobby-info">
          <h3 class="session-id">
            <span class="lobby-name">{{ group.name }}</span>
            <span class="lobby-date">({{ formatTime(group.time) }})</span>
          </h3>
        </div>
        <div class="stats-container">
          <div class="stat-item">
            <span class="stat-label">Correct Answers:</span>
            <span class="stat-value correct">{{ group.stats.correctAnswers }}/{{ group.stats.totalAnswers }}</span>
          </div>
          <div class="stat-item">
            <span class="stat-label">Average Time:</span>
            <span class="stat-value time">{{ group.stats.averageTimeTaken.toFixed(1) }}ms</span>
          </div>
          <div class="stat-item">
            <span class="stat-label">Total Points:</span>
            <span class="stat-value points">{{ group.stats.totalPoints.toFixed(2) }}pts</span>
          </div>
          <div class="stat-accuracy">
            <span class="accuracy-label">Accuracy:</span>
            <span class="accuracy-value">{{ group.stats.totalAnswers > 0 ? ((group.stats.correctAnswers / group.stats.totalAnswers) * 100).toFixed(1) : 0 }}%</span>
          </div>
        </div>
      </div>
      <div v-if="!group.collapsed" class="filter-controls">
        <label class="modern-checkbox">
          <input type="checkbox" @change="toggleShowIncorrectAnswersOnly(sessionId)">
          <span class="checkmark"></span>
          <span class="checkbox-label">Show Incorrect Answers Only</span>
        </label>
      </div>
      <table v-if="!group.collapsed">
        <thead>
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
        </thead>
        <tbody>
          <tr v-for="(history, index) in filteredHistories(sessionId)" :key="index" class="history-row" @click="openBeatmap(history.beatmap.beatmapset_id)" style="cursor: pointer;">
            <td class="col-index">{{ index + 1 }}</td>
            <td class="col-main">
              <div class="beatmap-info">
                <img class="beatmap-thumbnail" :src="`https://assets.ppy.sh/beatmaps/${history.beatmap.beatmapset_id}/covers/list.jpg`" :alt="history.beatmap.artistAndTitle" @error="handleImageError">
                <div class="beatmap-details">
                  <div class="beatmap-title" :class="{'answer-true': history.answer && history.gametype === 'TITLE', 'answer-false': !history.answer && history.gametype === 'TITLE'}">
                    <span>{{ history.beatmap.title }}</span>
                  </div>
                  <div class="beatmap-artist" :class="{'answer-true': history.answer && history.gametype === 'ARTIST', 'answer-false': !history.answer && history.gametype === 'ARTIST'}">{{ history.beatmap.artist }}</div>
                  <div class="beatmap-creator" :class="{'answer-true': history.answer && history.gametype === 'CREATOR', 'answer-false': !history.answer && history.gametype === 'CREATOR'}">by {{ history.beatmap.creator }}</div>
                </div>
              </div>
            </td>
            <td class="col-sub">{{ history.difficulty }}</td>
            <td class="col-sub">{{ history.difficulty_bonus.toFixed(2) }}</td>
            <td class="col-sub">{{ history.timetaken }}ms</td>
            <td class="col-sub">{{ history.speed_bonus.toFixed(2) }}</td>
            <td class="col-sub">{{ history.lobbyHistory.poolsize_bonus.toFixed(2) }}</td>
            <td class="col-sub" :class="{'answer-true': history.answer, 'answer-false': !history.answer}">{{ history.answer ? (history.speed_bonus * history.difficulty_bonus * history.lobbyHistory.poolsize_bonus).toFixed(2) : 0 }}</td>
          </tr>
        </tbody>
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
  background: linear-gradient(135deg, var(--color-body) 0%, rgba(255, 255, 255, 0.02) 100%);
  border-radius: 25px;
  margin-top: 20px;
  padding: 30px;
  box-shadow: 0 15px 35px rgba(0, 0, 0, 0.15);
  overflow-y: auto;
  max-height: 85vh;
  border: 1px solid rgba(255, 255, 255, 0.05);
}

table {
  background: linear-gradient(135deg, var(--color-secondary) 0%, rgba(255, 255, 255, 0.05) 100%);
  border-radius: 15px;
  width: 95%;
  margin: 20px auto;
  border-collapse: collapse;
  box-shadow: 0 8px 25px rgba(0, 0, 0, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.1);
  overflow: hidden;
}

hr {
  margin: 30px auto;
  background: linear-gradient(90deg, transparent, var(--color-text), transparent);
  height: 2px;
  width: 60%;
  border: none;
  border-radius: 1px;
  opacity: 0.3;
}

tr {

}

th, td {
  padding: 8px 12px;
  text-align: left;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

th {
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.1) 0%, rgba(255, 255, 255, 0.05) 100%);
  font-weight: 600;
  font-size: 0.95em;
  color: var(--color-text);
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

td {
  background: rgba(255, 255, 255, 0.02);
  transition: background-color 0.2s ease;
}

tr:hover td {
  background: rgba(255, 255, 255, 0.05);
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
  line-height: 1.2;
  transition: all 0.2s ease;
  cursor: pointer;
}

.history-row:hover {
  transform: scale(1.01);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  background: rgba(96, 165, 250, 0.05) !important;
}

.lobby-header {
  cursor: pointer;
  background: linear-gradient(135deg, var(--color-secondary) 0%, rgba(255, 255, 255, 0.05) 100%);
  border-radius: 15px;
  padding: 20px;
  margin: 15px 0;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.1);
  transition: all 0.3s ease;
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.lobby-header:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 25px rgba(0, 0, 0, 0.15);
  background: linear-gradient(135deg, var(--color-secondary) 0%, rgba(255, 255, 255, 0.08) 100%);
}

.lobby-info {
  display: flex;
  justify-content: flex-start;
}

.session-id {
  font-size: 1.3em;
  font-weight: 700;
  color: var(--color-text);
  transition: all 0.3s ease;
  margin: 0;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.3);
  letter-spacing: 0.5px;
  position: relative;
  padding-bottom: 8px;
}

.session-id::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 0;
  width: 0;
  height: 2px;
  background: linear-gradient(90deg, #60a5fa, #3b82f6);
  transition: width 0.3s ease;
  border-radius: 1px;
}

.lobby-header:hover .session-id::after {
  width: 100%;
}

.lobby-name {
  font-weight: 700;
  color: var(--color-text);
}

.lobby-date {
  font-weight: 400;
  color: var(--color-text);
  opacity: 0.6;
  font-size: 0.9em;
  margin-left: 8px;
}

.answer-true {
  color: #4ade80 !important;
  font-weight: 600;
  text-shadow: 0 1px 2px rgba(74, 222, 128, 0.3);
}

.answer-false {
  color: #ef4444 !important;
  font-weight: 600;
  text-shadow: 0 1px 2px rgba(239, 68, 68, 0.3);
}

.answer-true.beatmap-artist {
  color: #4ade80 !important;
}

.answer-false.beatmap-artist {
  color: #ef4444 !important;
}

.answer-true.beatmap-creator {
  color: #4ade80 !important;
}

.answer-false.beatmap-creator {
  color: #ef4444 !important;
}

/* Stats Container Styling */
.stats-container {
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.05) 0%, rgba(255, 255, 255, 0.02) 100%);
  border-radius: 12px;
  padding: 15px;
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 15px;
  border: 1px solid rgba(255, 255, 255, 0.08);
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  padding: 15px;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 10px;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.stat-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.15);
}

.stat-label {
  font-size: 0.9em;
  color: var(--color-text);
  opacity: 0.8;
  margin-bottom: 8px;
  font-weight: 500;
}

.stat-value {
  font-size: 1.8em;
  font-weight: bold;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.3);
}

.stat-value.correct {
  color: #4ade80;
  background: linear-gradient(45deg, #4ade80, #22c55e);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.stat-value.time {
  color: #60a5fa;
  background: linear-gradient(45deg, #60a5fa, #3b82f6);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.stat-value.points {
  color: #fbbf24;
  background: linear-gradient(45deg, #fbbf24, #f59e0b);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.stat-accuracy {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  padding: 15px;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 10px;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.stat-accuracy:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.15);
}

.accuracy-label {
  font-size: 0.9em;
  color: var(--color-text);
  opacity: 0.8;
  margin-bottom: 8px;
  font-weight: 500;
}

.accuracy-value {
  font-size: 2.2em;
  font-weight: bold;
  color: #a855f7;
  background: linear-gradient(45deg, #a855f7, #8b5cf6);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.3);
}

.filter-controls {
  display: flex;
  justify-content: center;
  margin: 20px 0;
  padding: 15px;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.05) 0%, rgba(255, 255, 255, 0.02) 100%);
  border-radius: 12px;
  border: 1px solid rgba(255, 255, 255, 0.1);
}

.modern-checkbox {
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
  padding: 8px 16px;
  border-radius: 8px;
  transition: all 0.2s ease;
  position: relative;
}

.modern-checkbox:hover {
  background: rgba(255, 255, 255, 0.05);
  transform: translateY(-1px);
}

.modern-checkbox input[type="checkbox"] {
  position: absolute;
  opacity: 0;
  cursor: pointer;
  height: 0;
  width: 0;
}

.checkmark {
  height: 20px;
  width: 20px;
  background: rgba(255, 255, 255, 0.1);
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-radius: 4px;
  position: relative;
  transition: all 0.2s ease;
  flex-shrink: 0;
}

.modern-checkbox:hover .checkmark {
  border-color: rgba(255, 255, 255, 0.5);
  background: rgba(255, 255, 255, 0.15);
}

.modern-checkbox input:checked ~ .checkmark {
  background: linear-gradient(135deg, #4ade80, #22c55e);
  border-color: #4ade80;
  box-shadow: 0 2px 8px rgba(74, 222, 128, 0.3);
}

.checkmark:after {
  content: "";
  position: absolute;
  display: none;
  left: 6px;
  top: 2px;
  width: 4px;
  height: 8px;
  border: solid white;
  border-width: 0 2px 2px 0;
  transform: rotate(45deg);
}

.modern-checkbox input:checked ~ .checkmark:after {
  display: block;
}

.checkbox-label {
  font-size: 0.95em;
  font-weight: 500;
  color: var(--color-text);
  user-select: none;
}

/* Beatmap info styling */
.beatmap-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.beatmap-thumbnail {
  width: 40px;
  height: 40px;
  border-radius: 6px;
  object-fit: cover;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
  transition: transform 0.2s ease;
  flex-shrink: 0;
}

.beatmap-thumbnail:hover {
  transform: scale(1.1);
}

.beatmap-details {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.beatmap-title {
  font-weight: 600;
  font-size: 0.95em;
}

.beatmap-title span {
  transition: all 0.2s ease;
  padding: 2px 4px;
  border-radius: 4px;
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* Override colors for answer states */
.answer-true .beatmap-title span {
  color: #4ade80 !important;
}

.answer-false .beatmap-title span {
  color: #ef4444 !important;
}

.beatmap-artist {
  font-size: 0.85em;
  color: var(--color-text);
  opacity: 0.8;
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.beatmap-creator {
  font-size: 0.8em;
  color: var(--color-text);
  opacity: 0.6;
  font-weight: 400;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* Loading state styling */
.gamelog-div p {
  text-align: center;
  font-size: 1.1em;
  color: var(--color-text);
  opacity: 0.8;
  padding: 20px;
}

/* Responsive adjustments */
@media (max-width: 768px) {
  .stats-container {
    grid-template-columns: 1fr;
    gap: 10px;
    padding: 15px;
  }
  
  .stat-value {
    font-size: 1.5em;
  }
  
  .accuracy-value {
    font-size: 1.8em;
  }
  
  .gamelog-div {
    padding: 20px;
    margin-top: 10px;
  }
  
  table {
    width: 100%;
    font-size: 0.9em;
  }
  
  th, td {
    padding: 6px 8px;
  }
  
  .lobby-header {
    padding: 15px;
    gap: 10px;
  }
  
  .session-id {
    font-size: 1.1em;
    padding: 10px 14px;
  }
}
</style>