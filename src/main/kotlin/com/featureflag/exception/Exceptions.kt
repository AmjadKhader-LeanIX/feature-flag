package com.featureflag.exception

class ResourceNotFoundException(message: String) : RuntimeException(message)

class ResourceAlreadyExistsException(message: String) : RuntimeException(message)

class ValidationException(message: String) : RuntimeException(message)

class FeatureFlagEvaluationException(message: String) : RuntimeException(message)
