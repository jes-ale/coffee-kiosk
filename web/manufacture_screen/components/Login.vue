<script lang="ts" setup>
import { ref } from '../.nuxt/imports'
const emit = defineEmits<{
  (e: 'login', data: { user: string, password: string }): void
}>()
const user = ref('')
const password = ref('')
function submit() {
  let data = {
    user: user.value,
    password: password.value
  }
  emit('login', data)
}
</script>

<template>
  <div class="h-full w-full flex flex-col justify-center items-center bg-primary text-old-copper-950">
    <div class="max-w-md p-8 bg-primary-500 rounded-md shadow-md">
      <h2 class="text-2xl font-bold">Login</h2>
      <form class="space-y-4">
        <div class="">
          <label for="user" class="block text-sm font-medium ">Usuario</label>
          <input id="user" v-model="user"
            class="w-full p-2 text-sm border  rounded-md" type="text" />
        </div>
        <div>
          <label for="password" class="block text-sm font-medium ">Password</label>
          <input id="password" v-model="password"
            class="w-full p-2 text-sm border  rounded-md" type="password" />
        </div>
        <div class="flex justify-between space-x-2">
          <button class="bg-secondary-500 hover:bg-primary-500 text-white font-bold py-2 px-4 rounded-md"
            type="button" @click="submit()">
            Login
          </button>
          <button class="bg-old-copper-200 text-old-copper-900 font-bold py-2 px-4 rounded-md"
            type="button">
            Odoo Backend
          </button>
        </div>
      </form>
    </div>
  </div>
</template>
