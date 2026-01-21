<template>
  <FioriCard
    :hover="clickable"
    :class="cardClasses"
    @click="handleClick"
  >
    <div class="stat-card-content">
      <!-- Left accent bar -->
      <div v-if="accentColor" :class="accentBarClasses" :style="accentBarStyles"></div>

      <!-- Icon section -->
      <div v-if="icon || $slots.icon" :class="iconSectionClasses" :style="iconSectionStyles">
        <slot name="icon">
          <component :is="icon" :size="iconSize" :class="iconClasses" />
        </slot>
      </div>

      <!-- Content section -->
      <div class="stat-card-body">
        <!-- Value -->
        <div :class="valueClasses">
          {{ formattedValue }}
        </div>

        <!-- Label -->
        <div :class="labelClasses">
          {{ label }}
        </div>

        <!-- Trend indicator (optional) -->
        <div v-if="trend" :class="trendClasses">
          <!-- Trend icon -->
          <svg
            v-if="trend.direction === 'up'"
            xmlns="http://www.w3.org/2000/svg"
            width="16"
            height="16"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
            stroke-linecap="round"
            stroke-linejoin="round"
          >
            <path d="m5 12 7-7 7 7"></path>
            <path d="M12 19V5"></path>
          </svg>
          <svg
            v-else-if="trend.direction === 'down'"
            xmlns="http://www.w3.org/2000/svg"
            width="16"
            height="16"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
            stroke-linecap="round"
            stroke-linejoin="round"
          >
            <path d="M12 5v14"></path>
            <path d="m19 12-7 7-7-7"></path>
          </svg>
          <svg
            v-else
            xmlns="http://www.w3.org/2000/svg"
            width="16"
            height="16"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
            stroke-linecap="round"
            stroke-linejoin="round"
          >
            <path d="M5 12h14"></path>
          </svg>

          <!-- Trend value -->
          <span>{{ trend.value }}</span>
          <span v-if="trend.label" class="trend-label">{{ trend.label }}</span>
        </div>

        <!-- Description (optional) -->
        <p v-if="description" class="stat-card-description">
          {{ description }}
        </p>
      </div>
    </div>
  </FioriCard>
</template>

<script setup>
import { computed } from 'vue'
import FioriCard from '../atoms/FioriCard.vue'

const props = defineProps({
  value: {
    type: [String, Number],
    required: true
  },
  label: {
    type: String,
    required: true
  },
  icon: {
    type: [Object, String],
    default: null
  },
  iconSize: {
    type: Number,
    default: 24
  },
  iconColor: {
    type: String,
    default: 'text-primary-600'
  },
  backgroundColor: {
    type: String,
    default: 'bg-blue-50'
  },
  accentColor: {
    type: String,
    default: null // e.g., '#002a86' or 'bg-primary-600'
  },
  trend: {
    type: Object,
    default: null
    // Example: { direction: 'up', value: '+12%', label: 'vs last month' }
  },
  description: {
    type: String,
    default: ''
  },
  clickable: {
    type: Boolean,
    default: false
  },
  loading: {
    type: Boolean,
    default: false
  },
  formatValue: {
    type: Function,
    default: (value) => value
  }
})

const emit = defineEmits(['click'])

const handleClick = () => {
  if (props.clickable) {
    emit('click')
  }
}

// Formatted value
const formattedValue = computed(() => {
  if (props.loading) {
    return '...'
  }
  return props.formatValue(props.value)
})

// Card classes
const cardClasses = computed(() => [
  'stat-card',
  props.loading ? 'animate-pulse' : ''
])

// Accent bar
const accentBarClasses = computed(() => [
  'stat-card-accent',
  props.accentColor && props.accentColor.startsWith('bg-') ? props.accentColor : ''
])

const accentBarStyles = computed(() => {
  if (props.accentColor && !props.accentColor.startsWith('bg-')) {
    return { backgroundColor: props.accentColor }
  }
  return {}
})

// Icon section
const iconSectionClasses = computed(() => [
  'stat-card-icon',
  props.backgroundColor
])

const iconSectionStyles = computed(() => {
  if (props.backgroundColor && !props.backgroundColor.startsWith('bg-')) {
    return { backgroundColor: props.backgroundColor }
  }
  return {}
})

const iconClasses = computed(() => [
  props.iconColor
])

// Value classes
const valueClasses = computed(() => [
  'stat-card-value',
  props.loading ? 'skeleton skeleton-text' : ''
])

// Label classes
const labelClasses = computed(() => [
  'stat-card-label',
  props.loading ? 'skeleton skeleton-text w-2/3' : ''
])

// Trend classes
const trendClasses = computed(() => {
  if (!props.trend) return []

  const baseClasses = [
    'stat-card-trend',
    'flex items-center gap-1',
    'text-xs font-medium',
    'mt-2'
  ]

  if (props.trend.direction === 'up') {
    return [...baseClasses, 'text-success-600']
  } else if (props.trend.direction === 'down') {
    return [...baseClasses, 'text-danger-600']
  } else {
    return [...baseClasses, 'text-neutral-500']
  }
})
</script>

<style scoped>
.stat-card {
  @apply relative overflow-hidden;
}

.stat-card-content {
  @apply flex items-start gap-4 p-6;
}

.stat-card-accent {
  @apply absolute left-0 top-0 bottom-0 w-1;
}

.stat-card-icon {
  @apply flex items-center justify-center;
  @apply w-14 h-14;
  @apply rounded-fiori-lg;
  @apply flex-shrink-0;
}

.stat-card-body {
  @apply flex-1 min-w-0;
}

.stat-card-value {
  @apply text-3xl font-bold text-neutral-900;
  @apply mb-1;
}

.stat-card-label {
  @apply text-sm font-medium text-neutral-600;
  @apply mb-1;
}

.stat-card-description {
  @apply text-xs text-neutral-500;
  @apply mt-2;
  @apply line-clamp-2;
}

.trend-label {
  @apply text-neutral-500 ml-1;
}
</style>
