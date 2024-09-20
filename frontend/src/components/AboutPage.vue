<script>
export default {
  name: "AboutPage"
}
</script>

<template>
  <div>
    <h1>osu! Music Quiz</h1>
    <div class="about-answer">
      dev: <a href="https://osu.ppy.sh/users/2688581" target="_blank">Luscent</a><br>
      special thanks: jehlp, TequilaWolf, and everyone who helped with the project<br>
      <br>
      inspired by <a href="https://animemusicquiz.com/" target="_blank">Anime Music Quiz</a>
    </div>
    <hr>
    <div class="about-question">
      Background spoils the title!
    </div>
    <div class="about-answer">
      <img src="../assets/usereport.png" alt=""><br>
      Click the report button at the right bottom of the background image to report the background.
    </div>
    <div class="about-question">
      Which beatmaps will I get to play?
    </div>
    <div class="about-answer">
      Beatmaps with more than a certain number of favorites or playcounts will be automatically imported (the threshold may vary).<br>
      Some beatmaps might be manually handpicked by the dev and a few helpers.
    </div>
    <div class="about-question">
      "Too Many Requests"
    </div>
    <div class="about-answer">
      Either you have sent too many requests to the server, or OMQ got rate limited from osu! server.<br>
      This usually happens when a lot of people try to login at the same time, please wait a few minutes and try again.
    </div>

  </div>
</template>

<style scoped>
div {
  color: #fff;
}

.about-question {
  font-weight: bold;
  margin-top: 3em;
  margin-bottom: 16px;
  font-size: 1.5em;
}

.about-answer {
  margin-bottom: 20px;
  font-size: 1.2em;
}

a {
  font-weight: bold;
  color: #ff66aa;
}
</style>