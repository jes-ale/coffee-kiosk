<template>
  <ul class="list-none w-full mb-0">
    <li v-for="production in entry.item" :key="production.custom_uid"
      class="py-1 px-3 border-t border-neutral-500 flex items-center w-full" v-show="production.shown">
      <div class="flex flex-col justify-between w-full text-2_5xl">
        <span class="flex items-center w-full ">
          <span class="h-auto w-[30%] rounded-full flex items-center justify-center border border-neutral-800 text-xl"
            :class="getTimeClass(production.production_delta)">
            {{ formatTime(production.production_delta) }}
          </span>
          <span class="ml-2 w-full text-left text-old-copper-950">
            {{ production.product.display_name }}
          </span>
        </span>
        <ul class="list-none mb-0 grid grid-cols-2 gap-x-4 text-lg">
          <li v-for="extra in production.component" :key="extra.display_name"
            class="text-color-extras text-center leading-none">
            <span v-if="extra.qty > 0">- {{ extra.display_name }} ({{ extra.qty }})</span>
          </li>
        </ul>
      </div>
    </li>
  </ul>
</template>


<script setup>
defineProps({
  entry: Object
});

function getTimeClass(delta) {
  if (delta < 60000) {
    // Menos de 1 minuto
    return 'timer-ending';
  } else if (delta < 120000) {
    // Menos de 2 minutos
    return 'bg-amber-500 text-white';
  }
  return 'bg-emerald-600 text-white';
}

function formatTime(ms) {
  const hours = Math.floor(ms / (1000 * 60 * 60));
  const minutes = Math.floor((ms % (1000 * 60 * 60)) / (1000 * 60));
  const seconds = Math.floor((ms % (1000 * 60)) / 1000);

  let timeString = '';
  if (hours > 0) timeString += `${hours}h `;
  if (minutes > 0) timeString += `${minutes}m `;
  timeString += `${seconds}s`; // Siempre mostramos los segundos

  return timeString.trim();
}
</script>
