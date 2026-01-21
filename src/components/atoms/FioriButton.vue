<template>
  <button
    :type="type"
    :disabled="disabled || loading"
    :class="buttonClasses"
    @click="handleClick"
  >
    <!-- Loading spinner -->
    <span v-if="loading" class="spinner-sm mr-2"></span>

    <!-- Icon (left) -->
    <component
      v-if="icon && !iconRight"
      :is="icon"
      :size="iconSize"
      :class="iconClasses"
    />

    <!-- Button text -->
    <span v-if="$slots.default" :class="textClasses">
      <slot></slot>
    </span>

    <!-- Icon (right) -->
    <component
      v-if="icon && iconRight"
      :is="icon"
      :size="iconSize"
      :class="iconClasses"
    />
  </button>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  variant: {
    type: String,
    default: 'primary',
    validator: (value) => ['primary', 'secondary', 'tertiary', 'critical', 'success', 'ghost'].includes(value)
  },
  size: {
    type: String,
    default: 'medium',
    validator: (value) => ['small', 'medium', 'large'].includes(value)
  },
  type: {
    type: String,
    default: 'button',
    validator: (value) => ['button', 'submit', 'reset'].includes(value)
  },
  disabled: {
    type: Boolean,
    default: false
  },
  loading: {
    type: Boolean,
    default: false
  },
  icon: {
    type: [Object, String],
    default: null
  },
  iconRight: {
    type: Boolean,
    default: false
  },
  fullWidth: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['click'])

const handleClick = (event) => {
  if (!props.disabled && !props.loading) {
    emit('click', event)
  }
}

// Size classes
const sizeClasses = computed(() => {
  const sizes = {
    small: 'px-3 py-1.5 text-sm',
    medium: 'px-4 py-2 text-base',
    large: 'px-6 py-3 text-lg'
  }
  return sizes[props.size]
})

// Icon size based on button size
const iconSize = computed(() => {
  const sizes = {
    small: 16,
    medium: 20,
    large: 24
  }
  return sizes[props.size]
})

// Icon classes
const iconClasses = computed(() => {
  const hasText = !!props.$slots?.default
  if (!hasText) return ''

  if (props.iconRight) {
    return 'ml-2'
  }
  return 'mr-2'
})

// Text classes
const textClasses = computed(() => {
  return 'inline-flex items-center'
})

// Variant classes
const variantClasses = computed(() => {
  const variants = {
    primary: 'bg-primary-700 text-white hover:bg-primary-800 active:bg-primary-900 shadow-fiori-md hover:shadow-fiori-lg disabled:bg-neutral-300 disabled:text-neutral-500',
    secondary: 'bg-white text-primary-700 border-2 border-primary-700 hover:bg-primary-50 active:bg-primary-100 disabled:border-neutral-300 disabled:text-neutral-400',
    tertiary: 'bg-transparent text-primary-700 hover:bg-primary-50 active:bg-primary-100 disabled:text-neutral-400',
    critical: 'bg-danger-500 text-white hover:bg-danger-600 active:bg-danger-700 shadow-fiori-md hover:shadow-fiori-lg disabled:bg-neutral-300 disabled:text-neutral-500',
    success: 'bg-success-500 text-white hover:bg-success-600 active:bg-success-700 shadow-fiori-md hover:shadow-fiori-lg disabled:bg-neutral-300 disabled:text-neutral-500',
    ghost: 'bg-transparent text-neutral-700 hover:bg-neutral-100 active:bg-neutral-200 disabled:text-neutral-400'
  }
  return variants[props.variant]
})

// Combined button classes
const buttonClasses = computed(() => {
  return [
    // Base classes
    'inline-flex items-center justify-center',
    'font-medium',
    'rounded-fiori-lg',
    'transition-all duration-200',
    'focus-ring',
    'select-none',

    // Size
    sizeClasses.value,

    // Variant
    variantClasses.value,

    // Full width
    props.fullWidth ? 'w-full' : '',

    // Disabled state
    props.disabled || props.loading ? 'cursor-not-allowed opacity-60' : 'cursor-pointer',

    // Ripple effect (except for ghost)
    props.variant !== 'ghost' && props.variant !== 'tertiary' ? 'button-ripple' : ''
  ]
})
</script>

<style scoped>
/* Additional button-specific styles if needed */
</style>
