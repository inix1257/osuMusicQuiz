<script>
export default {
  name: 'IngameSettingsPage',
  data() {
    return {
      showModal: false,
      settings: {
        submitEnter: true,
        submitRightArrow: false,
        submitTab: false,
        submitAutoSelect: false,
        chatHighlight: false
      }
    }
  },
  methods: {
    toggleModal() {
      this.showModal = !this.showModal;
    },

    updateSetting(setting, value) {
      this.settings[setting] = value;
      this.$emit('update-settings', this.settings);
      localStorage.setItem('gameSettings', JSON.stringify(this.settings));
    },

    loadSettings() {
      const savedSettings = localStorage.getItem('gameSettings');
      if (savedSettings) {
        this.settings = JSON.parse(savedSettings);
        this.$emit('update-settings', this.settings);
      }
    }
  },

  mounted() {
    this.loadSettings();
  }
}
</script>

<template>
  <div>
    <font-awesome-icon :icon="['fas', 'gear']" class="settings-button" @click="toggleModal"/>
    <div v-if="showModal" class="modal-overlay" @click="toggleModal">
      <div class="modal-content" @click.stop>
        <h2 class="modal-title">Settings</h2>
        <label class="category-label">Input</label>
        <div class="settings-row">
          <label for="submit-enter">Use enter key to submit answer</label>
          <input type="checkbox" id="submit-enter" v-model="settings.submitEnter" @change="updateSetting('submitEnter', settings.submitEnter)">
        </div>
        <div class="settings-row">
          <label for="submit-rightarrow">Use right arrow key to submit answer</label>
          <input type="checkbox" id="submit-rightarrow" v-model="settings.submitRightArrow" @change="updateSetting('submitRightArrow', settings.submitRightArrow)">
        </div>
        <div class="settings-row">
          <label for="submit-tab">Use tab key to submit answer</label>
          <input type="checkbox" id="submit-tab" v-model="settings.submitTab" @change="updateSetting('submitTab', settings.submitTab)">
        </div>
        <div class="settings-row">
          <label for="submit-autoselect">Auto select top result when submitting answer</label>
          <input type="checkbox" id="submit-autoselect" v-model="settings.submitAutoSelect" @change="updateSetting('submitAutoSelect', settings.submitAutoSelect)">
        </div>
<!--        <br>-->
<!--        <label class="category-label">Chat</label>-->
<!--        <div class="settings-row">-->
<!--          <label for="chat-highlight">Highlight my username when mentioned</label>-->
<!--          <input type="checkbox" id="chat-highlight" v-model="settings.chatHighlight" @change="updateSetting('chatHighlight', settings.chatHighlight)">-->
<!--        </div>-->
      </div>
    </div>
  </div>
</template>

<style scoped>
.settings-button {
  position: fixed;
  width: 30px;
  height: 30px;
  left: 20px;
  bottom: 20px;
  padding: 10px 20px;
  background-color: var(--color-secondary);
  border: none;
  border-radius: 5px;
  cursor: pointer;
  color: var(--color-text);
  box-shadow: 0 0 8px rgba(0, 0, 0, 0.5);
}

.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  text-align: left;
  justify-content: center;
  align-items: center;
}

.modal-content {
  background-color: var(--color-body);
  padding: 20px;
  border-radius: 5px;
  width: 40%;
  max-width: 80%;
  position: relative;
}

.modal-title {
  position: absolute;
  top: 20px;
  left: 20px;
  font-size: 2em;
  margin: 0;
}

.category-label {
  font-size: 1.5em;
  margin-top: 40px;
  margin-left: 1em;
  display: block;
}

.settings-row {
  display: flex;
  align-items: center;
  margin: 10px 0 10px 2.5em;
}

.settings-row label {
  flex: 1;
  font-size: 1em;
  cursor: pointer;
}

.settings-row input[type="checkbox"] {
  width: 20px;
  height: 20px;
  margin-left: 10px;
  cursor: pointer;
}
</style>