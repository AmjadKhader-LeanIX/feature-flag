<template>
  <form @submit.prevent="handleSubmit" class="feature-flag-form">
    <!-- Name Field -->
    <FormGroup
      label="Name"
      :error="errors.name"
      required
    >
      <FioriInput
        v-model="form.name"
        placeholder="Enter feature flag name"
        :disabled="isEdit || loading"
        :error="errors.name"
        required
        @blur="validateField('name')"
      />
    </FormGroup>

    <!-- Description Field -->
    <FormGroup
      label="Description"
      helper-text="Optional: Add a description for this feature flag"
    >
      <textarea
        v-model="form.description"
        placeholder="Enter description (optional)"
        :disabled="isEdit || loading"
        class="fiori-textarea"
        rows="3"
      ></textarea>
    </FormGroup>

    <!-- Team Field -->
    <FormGroup
      label="Team"
      :error="errors.team"
      required
    >
      <FioriInput
        v-model="form.team"
        placeholder="Enter team name"
        :disabled="isEdit || loading"
        :error="errors.team"
        required
        @blur="validateField('team')"
      />
    </FormGroup>

    <!-- Rollout Percentage (Edit Mode Only) -->
    <FormGroup
      v-if="isEdit"
      label="Rollout Percentage"
      :helper-text="`Current rollout: ${form.rolloutPercentage}%`"
    >
      <div class="rollout-slider">
        <input
          v-model.number="form.rolloutPercentage"
          type="range"
          min="0"
          max="100"
          :disabled="loading"
          class="slider"
        />
        <div class="rollout-display">
          <span class="rollout-value">{{ form.rolloutPercentage }}%</span>
          <div class="rollout-bar">
            <div
              class="rollout-fill"
              :style="{ width: form.rolloutPercentage + '%' }"
              :class="{
                'low': form.rolloutPercentage < 25,
                'medium': form.rolloutPercentage >= 25 && form.rolloutPercentage <= 75,
                'high': form.rolloutPercentage > 75
              }"
            ></div>
          </div>
        </div>
      </div>
    </FormGroup>

    <!-- Form Actions -->
    <div class="form-actions">
      <FioriButton
        type="button"
        variant="secondary"
        @click="handleCancel"
        :disabled="loading"
      >
        Cancel
      </FioriButton>
      <FioriButton
        type="submit"
        variant="primary"
        :loading="loading"
        :disabled="!isFormValid"
      >
        {{ isEdit ? 'Update Flag' : 'Create Flag' }}
      </FioriButton>
    </div>
  </form>
</template>

<script setup>
import { ref, reactive, computed, watch } from 'vue'
import { FioriInput, FioriButton } from '../atoms'
import { FormGroup } from '../molecules'

const props = defineProps({
  featureFlag: {
    type: Object,
    default: null
  },
  isEdit: {
    type: Boolean,
    default: false
  },
  loading: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['submit', 'cancel'])

// Form state
const form = reactive({
  name: '',
  description: '',
  team: '',
  rolloutPercentage: 0
})

// Form errors
const errors = reactive({
  name: '',
  team: ''
})

// Watch for prop changes
watch(() => props.featureFlag, (newFlag) => {
  if (newFlag) {
    form.name = newFlag.name || ''
    form.description = newFlag.description || ''
    form.team = newFlag.team || ''
    form.rolloutPercentage = newFlag.rolloutPercentage || 0
  } else {
    resetForm()
  }
}, { immediate: true })

// Validation
const validateField = (field) => {
  if (field === 'name') {
    if (!form.name || form.name.trim() === '') {
      errors.name = 'Name is required'
    } else if (form.name.length < 3) {
      errors.name = 'Name must be at least 3 characters'
    } else {
      errors.name = ''
    }
  }

  if (field === 'team') {
    if (!form.team || form.team.trim() === '') {
      errors.team = 'Team is required'
    } else {
      errors.team = ''
    }
  }
}

const validateForm = () => {
  validateField('name')
  validateField('team')
  return !errors.name && !errors.team
}

const isFormValid = computed(() => {
  return form.name.trim() !== '' && form.team.trim() !== ''
})

// Methods
const handleSubmit = () => {
  if (!validateForm()) {
    return
  }

  const data = {
    name: form.name.trim(),
    description: form.description?.trim() || null,
    team: form.team.trim(),
    rolloutPercentage: props.isEdit ? form.rolloutPercentage : 0
  }

  emit('submit', data)
}

const handleCancel = () => {
  emit('cancel')
}

const resetForm = () => {
  form.name = ''
  form.description = ''
  form.team = ''
  form.rolloutPercentage = 0
  errors.name = ''
  errors.team = ''
}
</script>

<style scoped>
.feature-flag-form {
  @apply space-y-6;
}

.fiori-textarea {
  @apply w-full px-4 py-2 text-base;
  @apply border border-neutral-300 rounded-fiori-lg;
  @apply transition-all duration-200;
  @apply focus:outline-none focus:ring-2 focus:ring-primary-500 focus:border-primary-500;
  @apply placeholder:text-neutral-400;
  @apply disabled:bg-neutral-100 disabled:cursor-not-allowed;
  resize: vertical;
}

.rollout-slider {
  @apply space-y-3;
}

.slider {
  @apply w-full h-2 rounded-full;
  @apply appearance-none cursor-pointer;
  background: linear-gradient(to right, #e5e7eb 0%, #002a86 100%);
}

.slider::-webkit-slider-thumb {
  @apply appearance-none w-5 h-5 rounded-full bg-primary-700;
  @apply border-2 border-white shadow-fiori-md;
  @apply cursor-pointer;
  @apply hover:scale-110 transition-transform;
}

.slider::-moz-range-thumb {
  @apply w-5 h-5 rounded-full bg-primary-700;
  @apply border-2 border-white shadow-fiori-md;
  @apply cursor-pointer;
}

.slider:disabled {
  @apply opacity-50 cursor-not-allowed;
}

.rollout-display {
  @apply space-y-2;
}

.rollout-value {
  @apply text-2xl font-bold text-primary-700;
}

.rollout-bar {
  @apply h-3 bg-neutral-200 rounded-full overflow-hidden;
}

.rollout-fill {
  @apply h-full transition-all duration-300;
}

.rollout-fill.low {
  @apply bg-danger-500;
}

.rollout-fill.medium {
  @apply bg-warning-500;
}

.rollout-fill.high {
  @apply bg-success-500;
}

.form-actions {
  @apply flex items-center justify-end gap-3;
  @apply pt-4 border-t border-neutral-200;
}
</style>
