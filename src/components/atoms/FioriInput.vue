<template>
  <div class="fiori-input-wrapper">
    <!-- Label -->
    <label
      v-if="label"
      :for="inputId"
      :class="labelClasses"
    >
      {{ label }}
      <span v-if="required" class="text-danger-500 ml-1">*</span>
    </label>

    <!-- Input wrapper with icon -->
    <div class="relative">
      <!-- Left icon -->
      <div v-if="iconLeft" class="absolute left-3 top-1/2 -translate-y-1/2 text-neutral-400">
        <component :is="iconLeft" :size="20" />
      </div>

      <!-- Input field -->
      <input
        :id="inputId"
        :type="type"
        :value="modelValue"
        :placeholder="placeholder"
        :disabled="disabled"
        :readonly="readonly"
        :required="required"
        :maxlength="maxlength"
        :class="inputClasses"
        @input="handleInput"
        @blur="handleBlur"
        @focus="handleFocus"
        @keyup.enter="handleEnter"
      />

      <!-- Right icon or clear button -->
      <div
        v-if="iconRight || (clearable && modelValue)"
        class="absolute right-3 top-1/2 -translate-y-1/2"
      >
        <!-- Clear button -->
        <button
          v-if="clearable && modelValue && !disabled"
          type="button"
          class="text-neutral-400 hover:text-neutral-600 transition-colors"
          @click="handleClear"
          aria-label="Clear input"
        >
          <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <circle cx="12" cy="12" r="10"></circle>
            <path d="m15 9-6 6"></path>
            <path d="m9 9 6 6"></path>
          </svg>
        </button>

        <!-- Right icon -->
        <component
          v-else-if="iconRight"
          :is="iconRight"
          :size="20"
          class="text-neutral-400"
        />
      </div>
    </div>

    <!-- Helper text or error message -->
    <p
      v-if="helperText || error"
      :class="helperTextClasses"
    >
      {{ error || helperText }}
    </p>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'

const props = defineProps({
  modelValue: {
    type: [String, Number],
    default: ''
  },
  type: {
    type: String,
    default: 'text',
    validator: (value) => ['text', 'email', 'password', 'number', 'tel', 'url', 'search'].includes(value)
  },
  label: {
    type: String,
    default: ''
  },
  placeholder: {
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
  state: {
    type: String,
    default: 'default',
    validator: (value) => ['default', 'error', 'success', 'warning'].includes(value)
  },
  size: {
    type: String,
    default: 'medium',
    validator: (value) => ['small', 'medium', 'large'].includes(value)
  },
  disabled: {
    type: Boolean,
    default: false
  },
  readonly: {
    type: Boolean,
    default: false
  },
  required: {
    type: Boolean,
    default: false
  },
  clearable: {
    type: Boolean,
    default: false
  },
  iconLeft: {
    type: [Object, String],
    default: null
  },
  iconRight: {
    type: [Object, String],
    default: null
  },
  maxlength: {
    type: Number,
    default: null
  }
})

const emit = defineEmits(['update:modelValue', 'blur', 'focus', 'enter', 'clear'])

// Generate unique ID for input
const inputId = computed(() => `fiori-input-${Math.random().toString(36).substr(2, 9)}`)

const isFocused = ref(false)

const handleInput = (event) => {
  emit('update:modelValue', event.target.value)
}

const handleBlur = (event) => {
  isFocused.value = false
  emit('blur', event)
}

const handleFocus = (event) => {
  isFocused.value = true
  emit('focus', event)
}

const handleEnter = (event) => {
  emit('enter', event)
}

const handleClear = () => {
  emit('update:modelValue', '')
  emit('clear')
}

// Label classes
const labelClasses = computed(() => {
  return [
    'block text-sm font-medium mb-1.5',
    props.error ? 'text-danger-600' : 'text-neutral-700'
  ]
})

// Size classes
const sizeClasses = computed(() => {
  const sizes = {
    small: 'px-3 py-1.5 text-sm',
    medium: 'px-4 py-2 text-base',
    large: 'px-4 py-3 text-lg'
  }
  return sizes[props.size]
})

// State classes
const stateClasses = computed(() => {
  // Error state takes precedence
  if (props.error || props.state === 'error') {
    return 'border-danger-500 focus:border-danger-500 focus:ring-danger-500'
  }

  const states = {
    default: 'border-neutral-300 focus:border-primary-500 focus:ring-primary-500',
    success: 'border-success-500 focus:border-success-500 focus:ring-success-500',
    warning: 'border-warning-500 focus:border-warning-500 focus:ring-warning-500',
    error: 'border-danger-500 focus:border-danger-500 focus:ring-danger-500'
  }
  return states[props.state]
})

// Input classes
const inputClasses = computed(() => {
  return [
    // Base styles
    'w-full',
    'rounded-fiori-lg',
    'border',
    'transition-all duration-200',
    'focus:outline-none focus:ring-2 focus:ring-offset-0',
    'placeholder:text-neutral-400',

    // Size
    sizeClasses.value,

    // State
    stateClasses.value,

    // Icon padding
    props.iconLeft ? 'pl-10' : '',
    props.iconRight || props.clearable ? 'pr-10' : '',

    // Disabled
    props.disabled ? 'bg-neutral-100 cursor-not-allowed text-neutral-500' : 'bg-white',

    // Readonly
    props.readonly ? 'bg-neutral-50 cursor-default' : ''
  ]
})

// Helper text classes
const helperTextClasses = computed(() => {
  return [
    'mt-1.5 text-sm',
    props.error ? 'text-danger-600' : 'text-neutral-600'
  ]
})
</script>

<style scoped>
.fiori-input-wrapper {
  @apply w-full;
}
</style>
