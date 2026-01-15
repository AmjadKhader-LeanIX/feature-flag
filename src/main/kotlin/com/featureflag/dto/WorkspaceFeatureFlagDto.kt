package com.featureflag.dto

import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import java.util.*

/**
 * Request to enable or disable a feature flag for specific workspaces
 */
data class UpdateWorkspaceFeatureFlagRequest(
    @field:NotEmpty(message = "At least one workspace ID is required")
    val workspaceIds: List<UUID>,
    @field:NotNull(message = "Enabled flag is required")
    val enabled: Boolean
)
