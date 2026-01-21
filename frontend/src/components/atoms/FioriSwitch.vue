<template>
  <div class="fiori-switch-wrapper">
    <label :class="labelWrapperClasses">
      <!-- Hidden checkbox input -->
      <input
        type="checkbox"
        :checked="modelValue"
        :disabled="disabled"
        class="sr-only"
        @change="handleChange"
      />

      <!-- Switch track -->
      <div :class="trackClasses">
        <!-- Switch knob -->
        <div :class="knobClasses"></div>
      </div>

      <!-- Label text -->
      <span v-if="label" :class="labelTextClasses">
        {{ label }}
      </span>
    </label>

    <!-- Helper text -->
    <p v-if="helperText" class="mt-1 text-sm text-neutral-600">
      {{ helperText }}
    </p>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false
  },
  label: {
    type: String,
    default: ''
  },
  helperText: {
    type: String,
    default: ''
  },
  disabled: {
    type: Boolean,
    default: false
  },
  size: {
    type: String,
    default: 'medium',
    validator: (value) => ['small', 'medium', 'large'].includes(value)
  },
  labelPosition: {
    type: String,
    default: 'right',
    validator: (value) => ['left', 'right'].includes(value)
  }
})

const emit = defineEmits(['update:modelValue', 'change'])

const handleChange = (event) => {
  if (!props.disabled) {
    emit('update:modelValue', event.target.checked)
    emit('change', event.target.checked)
  }
}

// Size configuration
const sizeConfig = computed(() => {
  const configs = {
    small: {
      track: 'w-10 h-5',
      knob: 'w-4 h-4',
      translate: 'translate-x-5'
    },
    medium: {
      track: 'w-12 h-6',
      knob: 'w-5 h-5',
      translate: 'translate-x-6'
    },
    large: {
      track: 'w-14 h-7',
      knob: 'w-6 h-6',
      translate: 'translate-x-7'
    }
  }
  return configs[props.size]
})

// Label wrapper classes
const labelWrapperClasses = computed(() => {
  return [
    'inline-flex items-center gap-3',
    props.disabled ? 'cursor-not-allowed opacity-60' : 'cursor-pointer',
    props.labelPosition === 'left' ? 'flex-row-reverse' : ''
  ]
})

// Track classes
const trackClasses = computed(() => {
  return [
    // Base styles
    'relative inline-flex items-center',
    'rounded-full',
    'transition-all duration-300 ease-in-out',
    'focus-within:ring-2 focus-within:ring-primary-500 focus-within:ring-offset-2',

    // Size
    sizeConfig.value.track,

    // Active state color
    props.modelValue
      ? 'bg-primary-600'
      : 'bg-neutral-300',

    // Hover state
    !props.disabled && (props.modelValue
      ? 'hover:bg-primary-700'
      : 'hover:bg-neutral-400'),

    // Disabled
    props.disabled ? 'opacity-50' : ''
  ]
})

// Knob classes
const knobClasses = computed(() => {
  return [
    // Base styles
    'inline-block',
    'rounded-full',
    'bg-white',
    'shadow-fiori-md',
    'transition-transform duration-300 ease-in-out',
    'ml-0.5',

    // Size
    sizeConfig.value.knob,

    // Position based on checked state
    props.modelValue ? sizeConfig.value.translate : 'translate-x-0'
  ]
})

// Label text classes
const labelTextClasses = computed(() => {
  return [
    'text-sm font-medium select-none',
    props.disabled ? 'text-neutral-400' : 'text-neutral-700'
  ]
})
</script>

<style scoped>
.fiori-switch-wrapper {
  @apply inline-block;
}

/* Screen reader only class */
.sr-only {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  margin: -1px;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
  border-width: 0;
}
</style>
