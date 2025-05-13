<script>
import hitcircleImage from '@/assets/osu/hitcircle.png';
import icon_report from "@/assets/siren.png";
import {
  createPixiApp,
  setCurrentTime,
  setCurrentTimeToPreviewPoint,
  setBeatmap,
  setBeatmapBypass,
  setVolume,
  setBackground
} from "@/components/osu/Render/Renderer";

export default {
  name: 'BeatmapRenderer',

  data() {
    return {
      app: null,
      hitcircleImage: hitcircleImage,
      icon_report: icon_report,
      currentTime: 0,
      beatmapId: ''
    }
  },

  methods: {
    async draw() {
      this.app = await createPixiApp();
    },

    updateCurrentTime() {
      setCurrentTime(parseInt(this.currentTime));
    },

    resetCurrentTime() {
      setCurrentTimeToPreviewPoint();
    },

    updateBeatmap(beatmapId) {
      setBeatmap(beatmapId);
    },

    updateBeatmapBypass(beatmapId) {
      setBeatmapBypass(beatmapId);
    },

    setVolume(volume) {
      setVolume(volume);
    },

    async updateBackground(imageUrl) {
      await setBackground(imageUrl);
    }
  },

  mounted() {
    this.draw();
  },

  beforeUnmount() {
    if (this.app) {
      this.app.ticker.stop();
    }
  }
}
</script>

<template>
  <div>
    <div id="pixi-container" class="pixi-container"></div>
  </div>

</template>

<style scoped>
.pixi-container {
  width: 70vh;
  height: 40vh;
  display: flex;
  justify-content: center;
  align-items: center;
  overflow: hidden;
}

.pixi-container canvas {
  width: 70vh;
  height: 40vh;
}
</style>