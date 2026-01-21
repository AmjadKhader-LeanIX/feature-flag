import { ref, reactive, computed } from 'vue'

/**
 * Composable for form state management with validation
 *
 * @param {Object} initialValues - Initial form values
 * @param {Object} validationRules - Validation rules for each field
 * @returns {Object} - Form state utilities
 */
export function useFormState(initialValues = {}, validationRules = {}) {
  // Form data
  const formData = reactive({ ...initialValues })

  // Form errors
  const errors = reactive({})

  // Touched fields (for showing errors only after user interaction)
  const touched = reactive({})

  // Form status
  const isSubmitting = ref(false)
  const isValidating = ref(false)
  const isDirty = ref(false)

  /**
   * Validate a single field
   */
  const validateField = (fieldName) => {
    const value = formData[fieldName]
    const rules = validationRules[fieldName]

    if (!rules) {
      delete errors[fieldName]
      return true
    }

    // Required validation
    if (rules.required && !value) {
      errors[fieldName] = rules.requiredMessage || `${fieldName} is required`
      return false
    }

    // Min length validation
    if (rules.minLength && value && value.length < rules.minLength) {
      errors[fieldName] = rules.minLengthMessage || `${fieldName} must be at least ${rules.minLength} characters`
      return false
    }

    // Max length validation
    if (rules.maxLength && value && value.length > rules.maxLength) {
      errors[fieldName] = rules.maxLengthMessage || `${fieldName} must be at most ${rules.maxLength} characters`
      return false
    }

    // Pattern validation (regex)
    if (rules.pattern && value && !rules.pattern.test(value)) {
      errors[fieldName] = rules.patternMessage || `${fieldName} is invalid`
      return false
    }

    // Custom validation function
    if (rules.validator && typeof rules.validator === 'function') {
      const result = rules.validator(value, formData)
      if (result !== true) {
        errors[fieldName] = result || `${fieldName} is invalid`
        return false
      }
    }

    // Clear error if validation passes
    delete errors[fieldName]
    return true
  }

  /**
   * Validate all fields
   */
  const validateForm = () => {
    isValidating.value = true
    let isValid = true

    Object.keys(validationRules).forEach(fieldName => {
      const fieldValid = validateField(fieldName)
      if (!fieldValid) {
        isValid = false
      }
    })

    isValidating.value = false
    return isValid
  }

  /**
   * Set field value
   */
  const setFieldValue = (fieldName, value) => {
    formData[fieldName] = value
    isDirty.value = true

    // Validate on change if field was touched
    if (touched[fieldName]) {
      validateField(fieldName)
    }
  }

  /**
   * Set field touched
   */
  const setFieldTouched = (fieldName, isTouched = true) => {
    touched[fieldName] = isTouched

    // Validate when field is touched
    if (isTouched) {
      validateField(fieldName)
    }
  }

  /**
   * Handle field blur
   */
  const handleBlur = (fieldName) => {
    setFieldTouched(fieldName, true)
  }

  /**
   * Reset form
   */
  const resetForm = () => {
    Object.keys(formData).forEach(key => {
      formData[key] = initialValues[key]
    })
    Object.keys(errors).forEach(key => {
      delete errors[key]
    })
    Object.keys(touched).forEach(key => {
      delete touched[key]
    })
    isDirty.value = false
    isSubmitting.value = false
  }

  /**
   * Set form errors (useful for server-side validation)
   */
  const setErrors = (newErrors) => {
    Object.keys(newErrors).forEach(key => {
      errors[key] = newErrors[key]
    })
  }

  /**
   * Clear all errors
   */
  const clearErrors = () => {
    Object.keys(errors).forEach(key => {
      delete errors[key]
    })
  }

  // Computed properties
  const isValid = computed(() => {
    return Object.keys(errors).length === 0
  })

  const hasErrors = computed(() => {
    return Object.keys(errors).length > 0
  })

  return {
    // State
    formData,
    errors,
    touched,
    isSubmitting,
    isValidating,
    isDirty,

    // Computed
    isValid,
    hasErrors,

    // Methods
    validateField,
    validateForm,
    setFieldValue,
    setFieldTouched,
    handleBlur,
    resetForm,
    setErrors,
    clearErrors
  }
}
