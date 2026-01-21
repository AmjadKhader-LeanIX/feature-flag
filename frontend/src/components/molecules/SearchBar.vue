<template>
  <div class="search-bar-wrapper">
    <div class="relative">
      <!-- Search icon (left) -->
      <div class="absolute left-3 top-1/2 -translate-y-1/2 text-neutral-400 pointer-events-none">
        <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <circle cx="11" cy="11" r="8"></circle>
          <path d="m21 21-4.3-4.3"></path>
        </svg>
      </div>

      <!-- Search input -->
      <input
        ref="searchInput"
        :value="modelValue"
        :type="type"
        :placeholder="placeholder"
        :disabled="disabled"
        :class="inputClasses"
        @input="handleInput"
        @focus="handleFocus"
        @blur="handleBlur"
        @keyup.enter="handleEnter"
        @keyup.esc="handleEscape"
      />

      <!-- Right section: Loading spinner or clear button -->
      <div class="absolute right-3 top-1/2 -translate-y-1/2 flex items-center gap-2">
        <!-- Loading spinner -->
        <div v-if="loading" class="spinner-sm"></div>

        <!-- Clear button -->
        <button
          v-else-if="modelValue && clearable && !disabled"
          type="button"
          class="text-neutral-400 hover:text-neutral-600 transition-colors"
          @click="handleClear"
          aria-label="Clear search"
        >
          <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <circle cx="12" cy="12" r="10"></circle>
            <path d="m15 9-6 6"></path>
            <path d="m9 9 6 6"></path>
          </svg>
        </button>
      </div>
    </div>

    <!-- Search suggestions (optional) -->
    <div
      v-if="suggestions && suggestions.length > 0 && isFocused"
      class="search-suggestions"
    >
      <div
        v-for="(suggestion, index) in suggestions"
        :key="index"
        class="search-suggestion-item"
        @click="handleSuggestionClick(suggestion)"
      >
        {{ suggestion }}
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'

const props = defineProps({
  modelValue: {
    type: String,
    default: ''
  },
  type: {
    type: String,
    default: 'search'
  },
  placeholder: {
    type: String,
    default: 'Search...'
  },
  disabled: {
    type: Boolean,
    default: false
  },
  loading: {
    type: Boolean,
    default: false
  },
  clearable: {
    type: Boolean,
    default: true
  },
  size: {
    type: String,
    default: 'medium',
    validator: (value) => ['small', 'medium', 'large'].includes(value)
  },
  suggestions: {
    type: Array,
    default: () => []
  }
})

const emit = defineEmits(['update:modelValue', 'search', 'clear', 'focus', 'blur', 'enter', 'escape', 'suggestion-click'])

const searchInput = ref(null)
const isFocused = ref(false)

const handleInput = (event) => {
  emit('update:modelValue', event.target.value)
  emit('search', event.target.value)
}

const handleClear = () => {
  emit('update:modelValue', '')
  emit('clear')
  searchInput.value?.focus()
}

const handleFocus = (event) => {
  isFocused.value = true
  emit('focus', event)
}

const handleBlur = (event) => {
  // Delay to allow suggestion click
  setTimeout(() => {
    isFocused.value = false
    emit('blur', event)
  }, 200)
}

const handleEnter = (event) => {
  emit('enter', event.target.value)
}

const handleEscape = () => {
  searchInput.value?.blur()
  emit('escape')
}

const handleSuggestionClick = (suggestion) => {
  emit('update:modelValue', suggestion)
  emit('suggestion-click', suggestion)
  isFocused.value = false
}

// Size classes
const sizeClasses = computed(() => {
  const sizes = {
    small: 'h-9 text-sm',
    medium: 'h-11 text-base',
    large: 'h-13 text-lg'
  }
  return sizes[props.size]
})

// Input classes
const inputClasses = computed(() => {
  return [
    // Base styles
    'w-full',
    'pl-10 pr-12',
    'rounded-fiori-lg',
    'border border-neutral-300',
    'transition-all duration-200',
    'focus:outline-none focus:ring-2 focus:ring-primary-500 focus:border-primary-500',
    'placeholder:text-neutral-400',

    // Size
    sizeClasses.value,

    // States
    props.disabled ? 'bg-neutral-100 cursor-not-allowed text-neutral-500' : 'bg-white',

    // Loading state
    props.loading ? 'pr-14' : ''
  ]
})
</script>

<style scoped>
.search-bar-wrapper {
  @apply relative w-full;
}

.search-suggestions {
  @apply absolute top-full left-0 right-0 mt-1;
  @apply bg-white border border-neutral-200 rounded-fiori-lg shadow-fiori-lg;
  @apply max-h-64 overflow-y-auto;
  @apply z-50;
  @apply animate-fade-in-scale;
}

.search-suggestion-item {
  @apply px-4 py-2.5;
  @apply text-sm text-neutral-700;
  @apply cursor-pointer;
  @apply transition-colors duration-150;
  @apply hover:bg-primary-50;
  @apply border-b border-neutral-100 last:border-0;
}

.search-suggestion-item:hover {
  @apply text-primary-700;
}
</style>
