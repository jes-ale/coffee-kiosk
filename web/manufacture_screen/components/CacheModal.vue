<template>
  <TransitionRoot appear :show="isOpen" as="div">

    <TransitionChild as="template" enter="duration-300 ease-out" enter-from="opacity-0" enter-to="opacity-100"
      leave="duration-200 ease-in" leave-from="opacity-100" leave-to="opacity-0">
      <div class="fixed inset-0 bg-black/25" />
    </TransitionChild>


    <!-- Cache modal content -->
    <Dialog as="div" :modelValue="isOpen" @update:modelValue="$emit('update:isOpen')"
      class="relative z-10 bg-beige-100 rounded-lg shadow-md">
      <!-- Modal content -->
      <button type="button" @click="$emit('sync-caches')"
        class="bg-emerald-600 hover:bg-emerald-700 text-white font-bold py-2 px-4 rounded">
        Actualizar
      </button>
      <button type="button" @click="$emit('update:isOpen', false)"
        class="bg-gray-200 hover:bg-gray-300 text-gray-700 font-bold py-2 px-4 rounded">
        Cerrar
      </button>
      <!-- Cache list -->
      <ul class="list-none mb-4">
        <li v-for="(cache, index) in productionCache" :key="index" class="text-sm text-gray-700">
          {{ cache.key }} - {{ cache.value }}
        </li>
      </ul>
    </Dialog>
  </TransitionRoot>
</template>

<script lang="ts" setup>
import {
  TransitionRoot,
  TransitionChild,
  Dialog,
  DialogPanel,
  DialogTitle,
} from '@headlessui/vue'

defineProps({
  isOpen: Boolean,
  productionCache: Object<{ [key: string]: Production[] }>,
})

defineEmits(['update:isOpen', 'sync-caches'])
</script>
