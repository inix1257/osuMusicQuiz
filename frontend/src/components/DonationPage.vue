<script>
export default {
  name: "DonationPage",
}
</script>

<template>
  <div>
    <div>
      <div class="div-links">
        <a href="https://github.com/inix1257/osuMusicQuiz" target="_blank" @click.stop><img src="@/assets/github.svg" alt="Github" class="svg-icon icon-normal"></a>
        <a href="https://osu.ppy.sh/users/2688581" target="_blank" @click.stop><img src="@/assets/osu.png" alt="osu" class="svg-icon icon-normal"></a>
      </div>
      If you like this project, please consider donating to help with the server cost!<br>
      <br>
      <div class="div-links">
        <a href="https://ko-fi.com/luscent" target="_blank" @click.stop><img src="@/assets/kofi.svg" alt="Ko-fi" class="svg-icon icon-kofi">Support</a><br>
        <a href="https://paypal.me/inix1257" target="_blank" @click.stop><img src="@/assets/paypal.svg" alt="PayPal" class="svg-icon icon-paypal">Support</a><br>
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
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 16px;
  border-radius: 8px;
  transition: all 0.3s ease;
}

a:hover {
  background: rgba(255, 102, 170, 0.1);
  box-shadow: 0 0 15px rgba(255, 102, 170, 0.3);
}

.div-links {
  gap: 20px;
  justify-content: center;
  display: flex;
  align-items: center;
}

.svg-icon {
  width: 50px;
  height: 50px;
  transition: filter 0.3s ease;
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