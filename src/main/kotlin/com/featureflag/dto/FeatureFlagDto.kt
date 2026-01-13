package com.featureflag.dto

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.time.LocalDateTime
import java.util.*

data class FeatureFlagDto(
    val id: UUID? = null,
    val name: String,
    val description: String?,
    val team: String,
    val rolloutPercentage: Int,
    val regions: List<String> = listOf("ALL"),
    val createdAt: LocalDateTime? = null,
    val updatedAt: LocalDateTime? = null
)

data class CreateFeatureFlagRequest(
    @field:NotBlank(message = "Feature flag name is required")
    val name: String,
    val description: String?,
    @field:NotBlank(message = "Team is required")
    val team: String,
    val regions: List<String> = listOf("ALL"),
    @field:Min(value = 0, message = "Rollout percentage must be between 0 and 100")
    @field:Max(value = 100, message = "Rollout percentage must be between 0 and 100")
    val rolloutPercentage: Int = 0,
)

data class UpdateFeatureFlagRequest(
    @field:NotBlank(message = "Feature flag name is required")
    val name: String,
    val description: String?,
    @field:NotBlank(message = "Team is required")
    val team: String,
    val regions: List<String> = listOf("ALL"),
    @field:NotNull(message = "Rollout percentage is required")
    @field:Min(value = 0, message = "Rollout percentage must be between 0 and 100")
    @field:Max(value = 100, message = "Rollout percentage must be between 0 and 100")
    var rolloutPercentage: Int,
)
