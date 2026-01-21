<template>
  <div :class="cardClasses">
    <!-- Card header (optional slot) -->
    <div v-if="$slots.header" class="card-header">
      <slot name="header"></slot>
    </div>

    <!-- Card body (default slot) -->
    <div v-if="$slots.default" :class="bodyClasses">
      <slot></slot>
    </div>

    <!-- Card footer (optional slot) -->
    <div v-if="$slots.footer" class="card-footer">
      <slot name="footer"></slot>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  // Shadow intensity
  shadow: {
    type: String,
    default: 'md',
    validator: (value) => ['none', 'sm', 'md', 'lg', 'xl'].includes(value)
  },

  // Border radius
  rounded: {
    type: String,
    default: '2xl',
    validator: (value) => ['none', 'sm', 'md', 'lg', 'xl', '2xl', '3xl'].includes(value)
  },

  // Hover effect
  hover: {
    type: Boolean,
    default: false
  },

  // Padding
  padding: {
    type: String,
    default: 'default',
    validator: (value) => ['none', 'sm', 'default', 'lg'].includes(value)
  },

  // Border
  border: {
    type: Boolean,
    default: true
  },

  // Background color
  backgroundColor: {
    type: String,
    default: 'white'
  },

  // Clickable card
  clickable: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['click'])

const handleClick = () => {
  if (props.clickable) {
    emit('click')
  }
}

// Shadow classes
const shadowClasses = computed(() => {
  const shadows = {
    none: '',
    sm: 'shadow-fiori-sm',
    md: 'shadow-fiori-md',
    lg: 'shadow-fiori-lg',
    xl: 'shadow-fiori-xl'
  }
  return shadows[props.shadow]
})

// Border radius classes
const roundedClasses = computed(() => {
  if (props.rounded === 'none') return ''

  const rounded = {
    sm: 'rounded-fiori-sm',
    md: 'rounded-fiori-md',
    lg: 'rounded-fiori-lg',
    xl: 'rounded-fiori-xl',
    '2xl': 'rounded-fiori-2xl',
    '3xl': 'rounded-fiori-3xl'
  }
  return rounded[props.rounded]
})

// Padding classes for body
const bodyClasses = computed(() => {
  const paddings = {
    none: '',
    sm: 'p-3',
    default: 'p-6',
    lg: 'p-8'
  }
  return paddings[props.padding]
})

// Combined card classes
const cardClasses = computed(() => {
  return [
    // Base classes
    'fiori-card',
    'transition-all duration-300',

    // Background
    `bg-${props.backgroundColor}`,

    // Shadow
    shadowClasses.value,

    // Border radius
    roundedClasses.value,

    // Border
    props.border ? 'border border-neutral-200' : '',

    // Hover effect
    props.hover ? 'hover-lift cursor-pointer' : '',

    // Clickable
    props.clickable ? 'cursor-pointer' : '',

    // Remove default padding if header/footer exist (they handle their own padding)
    props.$slots?.header || props.$slots?.footer ? 'p-0' : ''
  ]
})
</script>

<style scoped>
.card-header {
  @apply px-6 py-4 border-b border-neutral-200;
}

.card-footer {
  @apply px-6 py-4 border-t border-neutral-200 bg-neutral-50;
}
</style>
