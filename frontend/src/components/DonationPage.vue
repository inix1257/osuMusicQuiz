<script>
export default {
  name: "DonationPage",
}
</script>

<template>
  <div>
    <div>
      <div class="div-links">
        <a href="https://github.com/inix1257/osuMusicQuiz" target="_blank"><img src="@/assets/github.svg" alt="Github" class="svg-icon icon-normal"></a>
        <a href="https://osu.ppy.sh/users/2688581" target="_blank"><img src="@/assets/osu.png" alt="osu" class="svg-icon icon-normal"></a>
      </div>
      If you like this project, please consider donating to help with the server cost!<br>
      <br>
      <div class="div-links">
        <a href="https://paypal.me/inix1257" target="_blank"><img src="@/assets/paypal.svg" alt="PayPal" class="svg-icon icon-paypal"></a><br>
        <a href="https://ko-fi.com/luscent" target="_blank"><img src="@/assets/kofi.svg" alt="Ko-fi" class="svg-icon icon-kofi"></a><br>
      </div>
      <br>
      You can also leave your username in the donation message, and I will add a special title for you in the game!
    </div>
  </div>
</template>

<style scoped>
div {
  font-size: 1.2em;
  color: #fff;
  margin: 20px;
}

a {
  font-weight: bold;
  color: #ff66aa;
}

.div-links {
  display: flex;
  gap: 20px;
  justify-content: center;
}

.svg-icon {
  width: 50px;
  height: 50px;
}

.icon-paypal {
  filter: invert(59%) sepia(100%) saturate(500%) hue-rotate(180deg) brightness(95%) contrast(90%);
}

.icon-kofi {
  filter: invert(39%) sepia(100%) saturate(500%) hue-rotate(320deg) brightness(95%) contrast(90%);
}

.icon-normal {
  filter: invert(100%) sepia(100%) saturate(500%) hue-rotate(180deg) brightness(95%) contrast(90%);
}
</style>