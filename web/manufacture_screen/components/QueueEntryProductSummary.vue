<template>
  <div class="flex flex-col w-full text-center items-start font-bold ">
    <div v-for="key in Object.keys(productTemplateCount)" :key="key"
      class="w-full flex justify-end items-start font-bold text-center">
      ({{ productTemplateCount[key] }}) {{ key }}
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue';

const props = defineProps({
  entry: Object
});


const productTemplateCount = computed(() => {
  let entry = props.entry

  // Check if entry and entry.item are available
  if (!entry || !Array.isArray(entry.item)) return {};

  // Start reducing the entry.item array to accumulate counts of template names
  return entry.item.reduce((acc, production) => {
    // Extract the template name from the display_name (e.g., "Coffee" from "Coffee (Large, Extra Hot)")
    const templateName = production.product.display_name.split(' (')[0];

    // If templateName isn't in acc yet, initialize its count at 0
    if (!acc[templateName]) {
      acc[templateName] = 0;
    }

    acc[templateName] += 1;

    // Return the updated accumulator for the next iteration
    return acc;
  }, {}); // Initial value for acc is an empty object
});

</script>
