<template>
  <TransitionRoot :show="isOpenRemoteURL">
    <Dialog :modelValue="isOpenRemoteURL" @update:modelValue="$emit('update:isOpenRemoteURL')"
      class="relative z-10 bg-chocolate-100 rounded-lg shadow-md">

      <TransitionChild enter="duration-300 ease-out" enter-from="opacity-0" enter-to="opacity-100"
        leave="duration-200 ease-in" leave-from="opacity-100" leave-to="opacity-0">
        <div class="fixed inset-0 bg-black/25" />
      </TransitionChild>

      <div class="fixed w-full inset-0 overflow-y-auto">
        <div class="flex min-h-full items-center justify-center p-4 text-center">
          <TransitionChild as="template" enter="duration-300 ease-out" enter-from="opacity-0 scale-95"
            enter-to="opacity-100 scale-100" leave="duration-200 ease-in" leave-from="opacity-100 scale-100"
            leave-to="opacity-0 scale-95">
            <DialogPanel
              class="w-full transform overflow-hidden rounded-2xl bg-chocolate-200 p-6 text-left align-middle shadow-xl transition-all">
              <!-- Modal content -->
              <select v-model="localAllowedRemoteIndex"
                class="w-full p-2 text-sm font-medium text-chocolate-600 bg-chocolate-300 border border-chocolate-400 rounded-lg shadow-sm hover:shadow-md">
                <option v-for="(url, index) in allowedRemoteURLs" :key="index" :value="index">{{ url }}</option>
              </select>
              <div class="w-full flex justify-between space-x-2 pt-4">
                <button type="button" @click="$emit('update-base-url', localAllowedRemoteIndex)"
                  class="w-full bg-chocolate-400 hover:bg-chocolate-500 text-white font-bold py-2 px-4 rounded">
                  Aceptar
                </button>
                <button type="button" @click="$emit('update:isOpenRemoteURL', false)"
                  class="w-full bg-chocolate-300 hover:bg-chocolate-400 text-chocolate-700 font-bold py-2 px-4 rounded">
                  Cancelar
                </button>
              </div>
            </DialogPanel>
          </TransitionChild>
        </div>
      </div>
    </Dialog>
  </TransitionRoot>
</template>

<script lang="ts" setup>
import { ref, watch, toRefs } from 'vue'
import {
  TransitionRoot,
  TransitionChild,
  Dialog,
  DialogPanel,
  DialogTitle,
} from '@headlessui/vue'

const props = defineProps({
  isOpenRemoteURL: Boolean,
  allowedRemoteURLs: Array<string>,
  allowedRemoteIndex: Number,
})

const { isOpenRemoteURL, allowedRemoteURLs } = toRefs(props)

const localAllowedRemoteIndex = ref(0)

defineEmits(['update:isOpenRemoteURL', 'update-base-url'])
</script>
