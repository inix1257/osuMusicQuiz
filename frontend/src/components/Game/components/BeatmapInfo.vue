<template>
  <div v-if="currentBeatmap && !isGuessing" class="beatmap-info-container">
    <a :href="getBeatmapUrl" target="_blank">
      <h2>Beatmap Information</h2>
      <div class="beatmap-info-inner">
        <p>Artist <span class="beatmap-info-value">{{ currentBeatmap.artist }}</span></p>
        <p>Title <span class="beatmap-info-value">{{ currentBeatmap.title }}</span></p>
        <p>Mapper <span class="beatmap-info-value">{{ currentBeatmap.creator }}</span></p>
        <p>Answer Rate <span class="beatmap-info-value">{{ (getBeatmapStats().guess_rate * 100).toFixed(2) }}%</span> <span class="beatmap-info-playcount">({{ getBeatmapStats().guessed }}/{{ getBeatmapStats().played }})</span></p>
        <p>Difficulty <strong><span class="beatmap-info-difficulty beatmap-info-value" :class="difficultyClass(getBeatmapStats().difficulty)">{{ getBeatmapStats().difficulty }}</span></strong></p>
        <p>Ranked on <span class="beatmap-info-value">{{ formatDate(currentBeatmap.approved_date) }}</span></p>
        <br>
        <p v-if="answers[me.id]">Difficulty Bonus <span
            class="beatmap-info-value">{{ answers[me.id].difficultyBonus.toFixed(2) }}</span></p>
        <p v-if="answers[me.id]">Speed Bonus <span class="beatmap-info-value">{{
            answers[me.id].speedBonus.toFixed(2)
          }}</span></p>
        <p v-if="answers[me.id] && answers[me.id].poolSizeBonus < 1.00">Pool Size Penalty <span
            class="beatmap-info-value">{{ answers[me.id].poolSizeBonus.toFixed(2) }}</span></p>
        <p v-if="answers[me.id] && answers[me.id].poolSizeBonus > 1.00">Pool Size Bonus <span
            class="beatmap-info-value">{{ answers[me.id].poolSizeBonus.toFixed(2) }}</span></p>
        <p v-if="answers[me.id] && answers[me.id].aliasCorrect">Alias Penalty <span class="beatmap-info-value">0.5</span></p>
        <p v-if="answers[me.id]">Total Points <span class="beatmap-info-value">{{
            answers[me.id].totalPoints.toFixed(2)
          }}</span></p>
        <div class="beatmap-info-clickme">
          Click to view the beatmap on osu!
        </div>
      </div>
    </a>
  </div>
</template>

<script>
export default {
  name: 'BeatmapInfo',
  props: {
    currentBeatmap: Object,
    isGuessing: Boolean,
    getBeatmapUrl: String,
    difficultyClass: Function,
    gameMode: String,
    guessMode: String,
    formatDate: Function,
    answers: Object,
    me: Object
  },

  methods: {
    getBeatmapStats() {
      for (let i = 0; i < this.currentBeatmap.beatmapStats.length; i++) {
        if (this.currentBeatmap.beatmapStats[i].guess_mode === this.guessMode) {
          return this.currentBeatmap.beatmapStats[i];
        }
      }
    }
  }
}
</script>

<style scoped>
.beatmap-info-container {
  position: absolute;
  top: 7vh;
  right: 20px;
  padding: 10px;
  margin-top: 10px;
  border: 1px solid #ccc;
  background-color: var(--color-secondary);
  max-width: 28vh;
  min-width: 28vh;
}

.beatmap-info-container a {
  color: inherit;
  text-decoration: none;
}

.beatmap-info-container a:hover {
  color: inherit;
}

.beatmap-info-playcount {
  font-size: 0.6em;
}

.beatmap-info-clickme {
  position: absolute;
  bottom: 4px;
  right: 4px;
  font-size: 0.6em;
  text-align: right;
}

.beatmap-info-inner > p {
  margin: 0.8em;
}

.beatmap-info-value {
  font-weight: bold;
  background: var(--color-disabled);
  border-radius: 5px;
  padding: 0 5px;
}
</style>