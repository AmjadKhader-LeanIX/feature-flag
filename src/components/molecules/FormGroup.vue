<template>
  <div class="form-group">
    <!-- Label -->
    <label
      v-if="label"
      :for="inputId"
      :class="labelClasses"
    >
      {{ label }}
      <span v-if="required" class="text-danger-500 ml-1">*</span>
    </label>

    <!-- Description -->
    <p v-if="description" class="text-sm text-neutral-600 mb-2">
      {{ description }}
    </p>

    <!-- Input slot (can be FioriInput, FioriSwitch, select, etc.) -->
    <slot
      :input-id="inputId"
      :has-error="!!error"
      :is-required="required"
    ></slot>

    <!-- Error message -->
    <p v-if="error" class="mt-1.5 text-sm text-danger-600 flex items-center gap-1">
      <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
        <circle cx="12" cy="12" r="10"></circle>
        <line x1="12" x2="12" y1="8" y2="12"></line>
        <line x1="12" x2="12.01" y1="16" y2="16"></line>
      </svg>
      {{ error }}
    </p>

    <!-- Helper text (only shown if no error) -->
    <p v-else-if="helperText" class="mt-1.5 text-sm text-neutral-600">
      {{ helperText }}
    </p>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  label: {
    type: String,
    default: ''
  },
  description: {
    type: String,
    default: ''
  },
  helperText: {
    type: String,
    default: ''
  },
  error: {
    type: String,
    default: ''
  },
  required: {
    type: Boolean,
    default: false
  },
  inputId: {
    type: String,
    default: () => `form-group-${Math.random().toString(36).substr(2, 9)}`
  }
})

const labelClasses = computed(() => {
  return [
    'block text-sm font-medium mb-1.5',
    props.error ? 'text-danger-600' : 'text-neutral-700'
  ]
})
</script>

<style scoped>
.form-group {
  @apply mb-4;
}
</style>
