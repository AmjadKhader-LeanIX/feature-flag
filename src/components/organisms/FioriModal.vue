<template>
  <!-- Modal overlay -->
  <Teleport to="body">
    <Transition name="modal-fade">
      <div
        v-if="visible"
        class="modal-overlay"
        @click="handleOverlayClick"
      >
        <!-- Modal container -->
        <Transition name="modal-scale">
          <div
            v-if="visible"
            :class="modalClasses"
            @click.stop
            role="dialog"
            aria-modal="true"
            :aria-labelledby="titleId"
          >
            <!-- Modal header -->
            <div class="modal-header">
              <h2 :id="titleId" class="modal-title">
                <slot name="title">{{ title }}</slot>
              </h2>
              <button
                v-if="closable"
                type="button"
                class="modal-close"
                @click="handleClose"
                aria-label="Close modal"
              >
                <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M18 6 6 18"></path>
                  <path d="m6 6 12 12"></path>
                </svg>
              </button>
            </div>

            <!-- Modal body -->
            <div :class="bodyClasses">
              <slot></slot>
            </div>

            <!-- Modal footer (optional) -->
            <div v-if="$slots.footer" class="modal-footer">
              <slot name="footer"></slot>
            </div>
          </div>
        </Transition>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup>
import { computed, watch, onMounted, onUnmounted } from 'vue'

const props = defineProps({
  visible: {
    type: Boolean,
    required: true
  },
  title: {
    type: String,
    default: ''
  },
  size: {
    type: String,
    default: 'md',
    validator: (value) => ['sm', 'md', 'lg', 'xl', 'full'].includes(value)
  },
  closable: {
    type: Boolean,
    default: true
  },
  closeOnOverlay: {
    type: Boolean,
    default: true
  },
  closeOnEsc: {
    type: Boolean,
    default: true
  },
  scrollable: {
    type: Boolean,
    default: true
  },
  padding: {
    type: String,
    default: 'default',
    validator: (value) => ['none', 'sm', 'default', 'lg'].includes(value)
  }
})

const emit = defineEmits(['close', 'open'])

// Generate unique ID for accessibility
const titleId = computed(() => `modal-title-${Math.random().toString(36).substr(2, 9)}`)

// Size classes
const sizeClasses = computed(() => {
  const sizes = {
    sm: 'max-w-md',
    md: 'max-w-2xl',
    lg: 'max-w-4xl',
    xl: 'max-w-6xl',
    full: 'max-w-full mx-4'
  }
  return sizes[props.size]
})

// Body padding classes
const bodyPaddingClasses = computed(() => {
  const paddings = {
    none: 'p-0',
    sm: 'p-3',
    default: 'p-6',
    lg: 'p-8'
  }
  return paddings[props.padding]
})

// Modal classes
const modalClasses = computed(() => [
  'modal-container',
  'relative',
  'w-full',
  sizeClasses.value,
  'bg-white',
  'rounded-fiori-2xl',
  'shadow-fiori-2xl',
  'my-8',
  'mx-auto',
  'max-h-[90vh]',
  'flex flex-col'
])

// Body classes
const bodyClasses = computed(() => [
  'modal-body',
  bodyPaddingClasses.value,
  props.scrollable ? 'overflow-y-auto flex-1' : ''
])

// Methods
const handleClose = () => {
  if (props.closable) {
    emit('close')
  }
}

const handleOverlayClick = () => {
  if (props.closeOnOverlay) {
    handleClose()
  }
}

const handleEscKey = (event) => {
  if (event.key === 'Escape' && props.visible && props.closeOnEsc) {
    handleClose()
  }
}

// Lock body scroll when modal is open
watch(() => props.visible, (newValue) => {
  if (newValue) {
    document.body.style.overflow = 'hidden'
    emit('open')
  } else {
    document.body.style.overflow = ''
  }
})

// Keyboard event listener
onMounted(() => {
  document.addEventListener('keydown', handleEscKey)
})

onUnmounted(() => {
  document.removeEventListener('keydown', handleEscKey)
  document.body.style.overflow = ''
})
</script>

<style scoped>
.modal-overlay {
  @apply fixed inset-0 z-50;
  @apply bg-neutral-900/50;
  @apply backdrop-blur-sm;
  @apply flex items-start justify-center;
  @apply overflow-y-auto;
  @apply p-4;
}

.modal-container {
  @apply animate-fade-in-scale;
}

.modal-header {
  @apply flex items-center justify-between;
  @apply px-6 py-4;
  @apply border-b border-neutral-200;
  @apply flex-shrink-0;
}

.modal-title {
  @apply text-xl font-semibold text-neutral-900;
  @apply pr-8;
}

.modal-close {
  @apply p-1;
  @apply rounded-fiori-md;
  @apply text-neutral-500 hover:text-neutral-700 hover:bg-neutral-100;
  @apply transition-all duration-200;
  @apply focus:outline-none focus:ring-2 focus:ring-primary-500 focus:ring-offset-1;
}

.modal-body {
  @apply text-neutral-700;
}

.modal-footer {
  @apply px-6 py-4;
  @apply border-t border-neutral-200;
  @apply bg-neutral-50;
  @apply flex items-center justify-end gap-3;
  @apply flex-shrink-0;
}

/* Transition animations */
.modal-fade-enter-active,
.modal-fade-leave-active {
  @apply transition-opacity duration-300;
}

.modal-fade-enter-from,
.modal-fade-leave-to {
  @apply opacity-0;
}

.modal-scale-enter-active,
.modal-scale-leave-active {
  @apply transition-all duration-300;
}

.modal-scale-enter-from,
.modal-scale-leave-to {
  @apply opacity-0 scale-95;
}
</style>
