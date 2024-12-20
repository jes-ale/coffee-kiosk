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
  <div class="h-screen w-full flex flex-col justify-center items-center bg-primary">
    <div class="max-w-md p-8 bg-chocolate-200 rounded-md shadow-md">
      <h2 class="text-2xl font-bold text-chocolate-500">Login</h2>
      <form class="space-y-4">
        <div>
          <label for="user" class="block text-sm font-medium text-chocolate-600">Usuario</label>
          <input id="user" v-model="user"
            class="w-full p-2 text-sm text-chocolate-600 border border-chocolate-400 rounded-md" type="text" />
        </div>
        <div>
          <label for="password" class="block text-sm font-medium text-chocolate-600">Password</label>
          <input id="password" v-model="password"
            class="w-full p-2 text-sm text-chocolate-600 border border-chocolate-400 rounded-md" type="password" />
        </div>
        <div class="flex justify-between space-x-2">
          <button class="bg-chocolate-400 hover:bg-chocolate-500 text-white font-bold py-2 px-4 rounded-md"
            type="button" @click="submit()">
            Login
          </button>
          <button class="bg-chocolate-300 hover:bg-chocolate-400 text-chocolate-700 font-bold py-2 px-4 rounded-md"
            type="button">
            Odoo Backend
          </button>
        </div>
      </form>
    </div>
  </div>
</template>
