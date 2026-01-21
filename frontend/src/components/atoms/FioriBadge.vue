<template>
  <span :class="badgeClasses">
    <!-- Icon (left) -->
    <component
      v-if="icon && !iconRight"
      :is="icon"
      :size="iconSize"
      :class="iconClasses"
    />

    <!-- Badge text -->
    <span>{{ text }}</span>

    <!-- Icon (right) -->
    <component
      v-if="icon && iconRight"
      :is="icon"
      :size="iconSize"
      :class="iconClasses"
    />

    <!-- Close button (if closable) -->
    <button
      v-if="closable"
      type="button"
      class="ml-1.5 -mr-1 hover:opacity-70 transition-opacity"
      @click="handleClose"
      aria-label="Close badge"
    >
      <svg xmlns="http://www.w3.org/2000/svg" :width="iconSize" :height="iconSize" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
        <path d="M18 6 6 18"></path>
        <path d="m6 6 12 12"></path>
      </svg>
    </button>
  </span>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  text: {
    type: [String, Number],
    required: true
  },
  variant: {
    type: String,
    default: 'neutral',
    validator: (value) => ['primary', 'success', 'warning', 'danger', 'info', 'neutral'].includes(value)
  },
  size: {
    type: String,
    default: 'medium',
    validator: (value) => ['small', 'medium', 'large'].includes(value)
  },
  icon: {
    type: [Object, String],
    default: null
  },
  iconRight: {
    type: Boolean,
    default: false
  },
  closable: {
    type: Boolean,
    default: false
  },
  pulse: {
    type: Boolean,
    default: false
  },
  outlined: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['close'])

const handleClose = () => {
  emit('close')
}

// Size classes
const sizeClasses = computed(() => {
  const sizes = {
    small: 'px-2 py-0.5 text-xs',
    medium: 'px-3 py-1 text-sm',
    large: 'px-4 py-1.5 text-base'
  }
  return sizes[props.size]
})

// Icon size based on badge size
const iconSize = computed(() => {
  const sizes = {
    small: 12,
    medium: 14,
    large: 16
  }
  return sizes[props.size]
})

// Icon classes
const iconClasses = computed(() => {
  if (!props.icon) return ''

  if (props.iconRight) {
    return 'ml-1.5 -mr-0.5'
  }
  return 'mr-1.5 -ml-0.5'
})

// Variant classes
const variantClasses = computed(() => {
  if (props.outlined) {
    const outlinedVariants = {
      primary: 'bg-primary-50 text-primary-700 border border-primary-300',
      success: 'bg-success-50 text-success-700 border border-success-300',
      warning: 'bg-warning-50 text-warning-700 border border-warning-300',
      danger: 'bg-danger-50 text-danger-700 border border-danger-300',
      info: 'bg-info-50 text-info-700 border border-info-300',
      neutral: 'bg-neutral-50 text-neutral-700 border border-neutral-300'
    }
    return outlinedVariants[props.variant]
  }

  const solidVariants = {
    primary: 'bg-primary-600 text-white',
    success: 'bg-success-500 text-white',
    warning: 'bg-warning-500 text-white',
    danger: 'bg-danger-500 text-white',
    info: 'bg-info-500 text-white',
    neutral: 'bg-neutral-200 text-neutral-700'
  }
  return solidVariants[props.variant]
})

// Combined badge classes
const badgeClasses = computed(() => {
  return [
    // Base classes
    'inline-flex items-center',
    'font-medium',
    'rounded-fiori-md',
    'whitespace-nowrap',
    'select-none',

    // Size
    sizeClasses.value,

    // Variant
    variantClasses.value,

    // Pulse effect
    props.pulse ? 'badge-pulse' : '',

    // Animation
    'transition-all duration-200'
  ]
})
</script>

<style scoped>
/* Badge-specific styles */
</style>
