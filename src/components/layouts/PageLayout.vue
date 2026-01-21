<template>
  <div class="page-layout">
    <!-- Page Header -->
    <div v-if="title || subtitle || $slots.actions" class="page-header">
      <div class="page-header-content">
        <div class="page-title-section">
          <h1 v-if="title" class="page-title">{{ title }}</h1>
          <p v-if="subtitle" class="page-subtitle">{{ subtitle }}</p>
        </div>
        <div v-if="$slots.actions" class="page-actions">
          <slot name="actions"></slot>
        </div>
      </div>
    </div>

    <!-- Page Content -->
    <div :class="contentClasses">
      <!-- Loading state -->
      <div v-if="loading" class="page-loading">
        <div class="spinner-lg"></div>
        <p class="text-neutral-600 mt-4">{{ loadingText }}</p>
      </div>

      <!-- Content slot -->
      <slot v-else></slot>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  title: {
    type: String,
    default: ''
  },
  subtitle: {
    type: String,
    default: ''
  },
  loading: {
    type: Boolean,
    default: false
  },
  loadingText: {
    type: String,
    default: 'Loading...'
  },
  maxWidth: {
    type: String,
    default: '7xl',
    validator: (value) => ['full', '7xl', '6xl', '5xl', '4xl', '3xl', '2xl'].includes(value)
  },
  padding: {
    type: String,
    default: 'default',
    validator: (value) => ['none', 'sm', 'default', 'lg'].includes(value)
  }
})

// Max width classes
const maxWidthClasses = computed(() => {
  if (props.maxWidth === 'full') return 'max-w-full'
  return `max-w-${props.maxWidth}`
})

// Padding classes
const paddingClasses = computed(() => {
  const paddings = {
    none: '',
    sm: 'px-4 py-4',
    default: 'px-6 py-8',
    lg: 'px-8 py-12'
  }
  return paddings[props.padding]
})

// Content classes
const contentClasses = computed(() => [
  'page-content',
  maxWidthClasses.value,
  paddingClasses.value,
  'mx-auto'
])
</script>

<style scoped>
.page-layout {
  @apply min-h-screen bg-neutral-50;
}

.page-header {
  @apply bg-white border-b border-neutral-200;
  @apply shadow-fiori-xs;
}

.page-header-content {
  @apply max-w-7xl mx-auto;
  @apply px-6 py-6;
  @apply flex items-start justify-between gap-6;
  @apply flex-col sm:flex-row;
}

.page-title-section {
  @apply flex-1;
}

.page-title {
  @apply text-3xl font-bold text-neutral-900;
  @apply mb-1;
}

.page-subtitle {
  @apply text-base text-neutral-600;
}

.page-actions {
  @apply flex items-center gap-3;
  @apply flex-wrap;
}

.page-content {
  @apply w-full;
}

.page-loading {
  @apply flex flex-col items-center justify-center;
  @apply min-h-[400px];
}

/* Responsive adjustments */
@media (max-width: 640px) {
  .page-title {
    @apply text-2xl;
  }

  .page-header-content {
    @apply px-4 py-4;
  }
}
</style>
