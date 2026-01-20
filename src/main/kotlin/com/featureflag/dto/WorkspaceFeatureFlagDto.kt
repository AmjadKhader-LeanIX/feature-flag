package com.featureflag.dto

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import java.util.*

/**
 * Request to enable or disable a feature flag for specific workspaces and/or update rollout percentage
 */
data class UpdateWorkspaceFeatureFlagRequest(
    val workspaceIds: List<UUID> = emptyList(),
    @field:NotNull(message = "Enabled flag is required")
    val enabled: Boolean,
    @field:Min(value = 0, message = "Rollout percentage must be between 0 and 100")
    @field:Max(value = 100, message = "Rollout percentage must be between 0 and 100")
    val rolloutPercentage: Int? = null
)
