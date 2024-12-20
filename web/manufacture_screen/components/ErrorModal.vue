<template>
  <TransitionRoot appear :show="isOpenLog" as="template">

    <TransitionChild as="template" enter="duration-300 ease-out" enter-from="opacity-0" enter-to="opacity-100"
      leave="duration-200 ease-in" leave-from="opacity-100" leave-to="opacity-0">
      <div class="fixed inset-0 bg-black/25" />
    </TransitionChild>


    <!-- Error log modal content -->
    <Dialog :modelValue="isOpenLog" @update:modelValue="$emit('update:isOpenLog')"
      class="relative z-10 bg-beige-100 rounded-lg shadow-md">
      <!-- Modal content -->
      <div class="text-lg font-bold text-gray-700">
        Error Log
      </div>
      <ul class="list-none mb-4">
        <li v-for="(error, index) in errorLog" :key="index" class="text-sm text-gray-700">
          {{ error.timestamp }} - {{ error.origin }} - {{ error.msg }}
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
  isOpenLog: Boolean,
  errorLog: Array<{ timestamp: string; origin: string; msg: string }>,
})

defineEmits(['update:isOpenLog'])
</script>
