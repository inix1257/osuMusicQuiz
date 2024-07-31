<script>
export default {
  name: 'CallBack',
  components: {
  },
  data() {
    return {
    }
  },
  methods: {

  },
  mounted() {
    localStorage.setItem("code", this.$route.query.code);
  }
}
</script>

<template>
<div>
<p>Hello</p>
</div>
</template>

<style scoped>

</style>