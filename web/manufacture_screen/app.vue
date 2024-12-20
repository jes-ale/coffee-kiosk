<template>
  <div class="h-screen w-screen bg-chocolate-100">
    <!-- Main app content -->
    <div v-if="isLogin" class="flex flex-col h-full w-full bg-coffee-200 px-2 py-1">

      <div :class="[isPaused ? 'bg-neutral-400' : 'bg-chocolate-400']"
        class="shadow-xl absolute z-100  p-2 flex flex-col items-center justify-center font-bold border-t border-r border-black text-white text-xl  bottom-0 left-0">
        <span> {{ computedQueueEntries.length }} / {{ productionQueueKeys.length }}</span>
      </div>

      <div v-if="computedQueueEntries.length !== 0" class="mt-4">

        <TransitionRoot :show="computedQueueEntries.length !== 0" enter="transition-opacity duration-75"
          enter-from="opacity-0" enter-to="opacity-100" leave="transition-opacity duration-150" leave-from="opacity-100"
          leave-to="opacity-0">

          <Swiper :modules="modules" :slides-per-view="3" :space-between="10" navigation
            :scrollbar="{ draggable: true }" :pagination="{ clickable: true }"
            class="flex w-full h-auto cursor-pointer">

            <SwiperSlide v-for="key of computedQueueEntries" :key="key"
              class="flex flex-col w-full h-auto p-4 text-center font-bold text-lg">

              <div class="rounded border border-black shadow-md flex flex-col items-center w-full">
                <QueueEntryHeader :entry="getQueueItem(key) || {
                  origin: 'n/a', unique_name: '', item: [], totalDelta: 0
                }" />
                <QueueEntryItems :entry="getQueueItem(key) || {
                  origin: 'n/a', unique_name: '', item: [], totalDelta: 0
                }" />
              </div>
            </SwiperSlide>
          </Swiper>
        </TransitionRoot>
      </div>

      <div v-else class=" text-center text-lg py-32 font-bold text-coffee-400">
        No hay órdenes
      </div>

    </div>

    <div class="h-full w-full" v-else>
      <button type="button" @click="isOpenRemoteURL = !isOpenRemoteURL"
        class="px-8 w-full font-bold text-neutral-800 bg-chocolate-200 border-b border-black">
        {{ baseURL }}
      </button>
      <Login @login="login" />
    </div>

    <!-- Modals -->
    <RemoteURLModal :isOpenRemoteURL="isOpenRemoteURL" :allowedRemoteURLs="allowedRemoteURLs"
      @update:isOpenRemoteURL="isOpenRemoteURL = $event" @update-base-url="updateBaseURL" />

    <ErrorModal :isOpenLog="isOpenLog" :errorLog="errorLog" @update:isOpenLog="isOpenLog = $event" />

    <!-- <CacheModal :isOpen="isOpen" :productionCache="productionCache" @update:isOpen="isOpen = $event"
      @sync-caches="syncCaches" /> -->

  </div>
</template>

<script lang="ts" setup>
import { ref, useFetch, onBeforeUnmount, onNuxtReady } from './.nuxt/imports'
import { Swiper, SwiperSlide } from 'swiper/vue'
import Login from '~/components/Login.vue'
import RemoteURLModal from '~/components/RemoteURLModal.vue'
import CacheModal from '~/components/CacheModal.vue'
import ErrorModal from '~/components/ErrorModal.vue'
import QueueEntryHeader from '~/components/QueueEntryHeader.vue';
import QueueEntryItems from '~/components/QueueEntryItems.vue';
import { Navigation, Pagination, Scrollbar, Autoplay, Parallax, EffectCreative } from 'swiper'
import 'swiper/css'
import {
  TransitionRoot,
  TransitionChild,
  Dialog,
  DialogPanel,
  DialogTitle,
} from '@headlessui/vue'

import { useWebSocket } from '@/composables/useWebSocket';

interface Production {
  id: number
  custom_uid: string
  origin_unique_name: string
  production_delta: number
  shown: true
  display_name: string
  state: string
  priority: string
  origin: string
  component: {
    id: number
    display_name: string
    qty: number
  }[]
  product: {
    id: number
    display_name: string
  }
}

interface Products {
  id: number
  categ: string
  pos_categ: string
  display_name: string
  pos_production: boolean
}

interface ProductionEntry {
  origin: string;
  unique_name: string;
  item: Production[];
  totalDelta: number;
}

// Plugin/Framework declarations and assignments
provideHeadlessUseId(() => useId())
const runtimeConfig = useRuntimeConfig()
const modules = [Navigation, Pagination, Scrollbar, Autoplay, Parallax, EffectCreative]

// Initializations 
// - Reactive data
const isLogin = ref(false)
const isLoading = ref(false)
const isPaused = ref(false)
const isOpen = ref(false)
const isOpenLog = ref(false)
const isOpenRemoteURL = ref(false)
const intervalActive = ref(false)
const intervalId = ref(null);
const intervalTime = ref(1000)

// - Configurable Settings 
const allowedRemoteURLs = ref([
  'coffee1.quadrosoluciones.mx',
  'coffee1-test.quadrosoluciones.mx',
  'coffee2-test.quadrosoluciones.mx',
  'coffee3.quadrosoluciones.mx',
  '192.168.2.71:5000',
  '192.168.2.71:5001',
  '192.168.2.71:5002',
  '192.168.2.71:5003',
  'localhost:5000',
  'localhost:5001',
  'localhost:5002',
  'localhost:5003',
  'localhost:8443'
])
const allowedRemoteIndex = ref(0)
const baseURL = ref('')

// - Authorization
const auth = ref<{ jwt_secret: string; jwt_refresh: string; jwt_expires: string }>({
  jwt_secret: '',
  jwt_refresh: '',
  jwt_expires: '',
})

// - Analytics and monitoring
const errorLog = ref<{ timestamp: string; origin: string; msg: string }[]>([])
const successLog = ref<{ timestamp: string; origin: string; msg: string }[]>([])

// - In memory data
const productionQueue = ref<Map<string, ProductionEntry>>(new Map());
const productionQueueKeys = ref<string[]>([]);

// - Custom Hooks
const { connect, disconnect } = useWebSocket(handleMessage);

// Computed fields
const computedQueueEntries = computed(() => {
  return productionQueueKeys.value.slice(0, 3); // Solo mantener las primeras 3 entradas
});

function getQueueItem(key) {
  try {
    return productionQueue.value.get(key)
  } catch (e) {
    console.error(e)
    return null
  }
}

// Handlers 
function updateBaseURL(newAllowedRemoteIndex) {
  allowedRemoteIndex.value = newAllowedRemoteIndex
  if (allowedRemoteIndex.value >= 0 && allowedRemoteIndex.value < allowedRemoteURLs.value.length) {
    baseURL.value = allowedRemoteURLs.value[allowedRemoteIndex.value];
  }
  isOpenRemoteURL.value = false
}

// Async Handlers
async function login(body: { user: string, password: string }) {
  isLoading.value = true
  try {
    baseURL.value = allowedRemoteURLs.value[allowedRemoteIndex.value]
    const response = await $fetch<{ token: string }>(`https://${baseURL.value}/login`, {
      method: "POST",
      headers: {
        "Accept": "*",
        "Content-Type": "application/json",
      },
      body: JSON.stringify({ user: body.user, password: body.password })
    })

    if (!response) throw new Error("JWT Token not found")
    if (response.token.length < 12) throw new Error("Invalid JWT Token")
    auth.value.jwt_secret = response.token
    isLogin.value = true
    connect(`wss://${baseURL.value}/updates`)
    fetchQueueWaitRoom()
  } catch (e) {
    console.error(e)
    alert(e)
  } finally {
    isLoading.value = false
  }
}

async function apiCall<T>(url: string, method: string, body?: object): Promise<{ data: T, error: any }> {
  isLoading.value = true
  try {
    baseURL.value = allowedRemoteURLs.value[allowedRemoteIndex.value]
    const headers = {
      "Accept": "*",
      "Authorization": `Bearer ${auth.value.jwt_secret}`,
    }
    if (method === "POST") {
      headers["Content-Type"] = "application/json"
    }
    const response = await $fetch<T>(`https://${url}`, { method, headers, body: body ? JSON.stringify(body) : undefined })
    // Do something to all responses...
    return response
  } catch (e) {
    console.error(e)
  } finally {
    isLoading.value = false
  }
}

// Clears the stuck mrp orders from the api queue
async function fetchQueueWaitRoom() {
  let isValid = true
  do {
    let productionResponse = await apiCall<Production[]>(`${baseURL.value}/popProductionQueue`, "GET")
    if (productionResponse.error?.statusCode === 401) isLogin.value = false

    if (!productionResponse || !Array.isArray(productionResponse))
      // throw new Error("Invalid Pop Production Queue Response.")
      continue

    if (productionResponse.length === 0) {
      isValid = false
    }
    else {
      // productionResponse = productionResponse.reverse();

      const key = productionResponse[0].origin;
      if (!productionQueueKeys.value.includes(key)) productionQueueKeys.value.push(key);

      productionQueue.value.set(key, {
        origin: productionResponse[0].origin,
        unique_name: productionResponse[0].origin_unique_name,
        item: productionResponse,
        delta: 0
      });
    }
  } while (isValid)
}


// Pops 1 item from the queue and caches it locally
async function fetchNextMrpProduction() {
  let productionResponse = await apiCall<Production[]>(`${baseURL.value}/popProductionQueue`, "GET")
  if (productionResponse.error?.statusCode === 401) isLogin.value = false

  if (!productionResponse || !Array.isArray(productionResponse) || !productionResponse.length > 0)
    throw new Error("Invalid Pop Production Queue Response.")

  // productionResponse = productionResponse.reverse();

  const key = productionResponse[0].origin;

  if (!productionQueueKeys.value.includes(key)) productionQueueKeys.value.push(key);

  productionQueue.value.set(key, {
    origin: productionResponse[0].origin,
    unique_name: productionResponse[0].origin_unique_name,
    item: productionResponse,
    delta: 0
  });
}

async function syncCaches() {
  const cacheResponse = await apiCall<{ [key: string]: Production[] }>(`${baseURL.value}/getProductionCache`, "GET");
  if (!cacheResponse) return;
}

async function markAsDone(production: Production[]) {
  for (let prod of production) {
    const response = await apiCall<number>(`${baseURL.value}/setDoneProduction`, "POST", { id: prod.custom_uid });
  }
}

async function handleMessage(message: string) {
  if (!isPaused.value && isLogin.value) {
    //console.info('New WebSocket message from /updates ;', message)
    //console.info(productionQueueKeys);
    await fetchNextMrpProduction()
  }
}

async function checkIntervalDone() {
  const itemsToMarkAsDone = [];

  const activeKeys = productionQueueKeys.value.slice(0, 3);

  for (const key of activeKeys) {
    const entry = getQueueItem(key);
    if (!entry.item) continue;

    const itemsToRemove = [];
    const shownItems = entry.item.filter(prod => prod.shown);

    // Process each shown item
    for (const production of shownItems) {
      if (production.production_delta <= 1000) {
        itemsToRemove.push(production);
        itemsToMarkAsDone.push(production);
        production.shown = false;
      } else {
        production.production_delta -= 1000; // Update individual delta
      }
    }

    // Remove expired items from the queue
    entry.item = entry.item.filter(prod => !itemsToRemove.includes(prod));

    // Remove the queue item if all its items are removed
    if (entry.item.length === 0) {
      // Eliminar de la lista de claves
      const index = productionQueueKeys.value.indexOf(entry.origin);
      if (index !== -1) {
        productionQueueKeys.value.splice(index, 1); // Eliminar la clave del arreglo
      }
      // Eliminar del Map
      productionQueue.value.delete(entry.origin);
      continue;
    }

    // Ensure a maximum of 3 items are shown
    const currentShownCount = entry.item.filter(prod => prod.shown).length;
    if (currentShownCount < 3) {
      const itemsToShow = entry.item
        .filter(prod => !prod.shown)
        .slice(0, 3 - currentShownCount);
      for (const prod of itemsToShow) {
        prod.shown = true;
      }
    }
  }

  await markAsDone(itemsToMarkAsDone);
}

// Life cycle hooks
onNuxtReady(() => {
  baseURL.value = allowedRemoteURLs.value[allowedRemoteIndex.value];
  intervalId.value = setInterval(checkIntervalDone, 1000);
})

onBeforeUnmount(() => {
  clearInterval(intervalId.value);
  disconnect();
})

</script>


<style>
.bg-chocolate-100 {
  background-color: #F5F5DC;
  /* Dry Beige */
}

.bg-chocolate-200 {
  background-color: #F2C464;
  /* Soft Cream */
}

.bg-chocolate-300 {
  background-color: #F0F0F0;
  /* Soft Light */
}

.bg-chocolate-400 {
  background-color: #964B00;
  /* Warm Brown */
}

.bg-chocolate-500 {
  background-color: #786C3B;
  /* Dark Chocolate */
}

.bg-chocolate-600 {
  background-color: #452B1F;
  /* Espresso */
}

.bg-chocolate-700 {
  background-color: #452B1F;
  /* Espresso */
}

.bg-chocolate-800 {
  background-color: #452B1F;
  /* Espresso */
}

.bg-chocolate-900 {
  background-color: #452B1F;
  /* Espresso */
}

.text-chocolate-100 {
  color: #F5F5DC;
  /* Dry Beige */
}

.text-chocolate-200 {
  color: #F2C464;
  /* Soft Cream */
}

.text-chocolate-300 {
  color: #F0F0F0;
  /* Soft Light */
}

.text-chocolate-400 {
  color: #964B00;
  /* Warm Brown */
}

.text-chocolate-500 {
  color: #786C3B;
  /* Dark Chocolate */
}

.text-chocolate-600 {
  color: #452B1F;
  /* Espresso */
}

.text-chocolate-700 {
  color: #452B1F;
  /* Espresso */
}

.text-chocolate-800 {
  color: #452B1F;
  /* Espresso */
}

.text-chocolate-900 {
  color: #452B1F;
  /* Espresso */
}
</style>
