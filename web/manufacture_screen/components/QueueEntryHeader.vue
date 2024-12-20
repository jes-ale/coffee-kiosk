<template>
  <div class="flex justify-between text-chocolate-800  items-center w-full p-2 space-x-2">
    <h5 class="text-center w-full  font-bold ">{{ entry.unique_name }}</h5>
    <QueueEntryProductSummary :entry="entry" />
  </div>
</template>

<script setup>
import QueueEntryProductSummary from '~/components/QueueEntryProductSummary.vue';

defineProps({
  entry: Object
});
// :class="[entry.delta < 60000 ? 'bg-rose-200' : 'bg-chocolate-100']"
</script>
