<template>
  <div id="app">
    <!-- <img src="./assets/logo.png"> -->
    <router-view/>
  </div>
</template>

<script>
export default {
  name: 'App'
}
</script>

<style>
#app {
  font-family: 'Avenir', Helvetica, Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  /*text-align: center;*/
  color: #2c3e50;
  /*margin-top: 60px;*/
  width: 100%;
  position: absolute;
  top: 0;
  bottom: 0;
  left: 0;
  background-image: url('./assets/bg.jpg');
  background-repeat: no-repeat;
  background-size: cover;
}
</style>
