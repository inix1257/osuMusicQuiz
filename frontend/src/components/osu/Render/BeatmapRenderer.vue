<script>
import hitcircleImage from '@/assets/siren.png';
import icon_report from "@/assets/siren.png";
import * as PIXI from "pixi.js";
import {Application, Assets, Sprite} from "pixi.js";
import {createPixiApp, resetTimer} from "@/components/osu/Render/Renderer";

export default {
  name: 'BeatmapRenderer',

  data() {
    return {
      app: null,
      hitcircleImage: hitcircleImage,
      icon_report: icon_report,
    }
  },

  methods: {
    async draw() {
      const app = await createPixiApp();
      document.getElementById('pixi-container').appendChild(app.canvas);
      // Draw a rectangle
    },

    resetTimer() {
      resetTimer();
    }
  },

  mounted() {
    // Create a Pixi application and append it to the container
    this.draw();
    // this.draw(this.app);
  }

}
</script>

<template>
  <div id="pixi-container"></div>
</template>

<style scoped>

</style>