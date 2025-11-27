package com.featureflag.dto

import jakarta.validation.constraints.NotBlank
import java.time.LocalDateTime
import java.util.*

data class WorkspaceDto(
    val id: UUID? = null,
    @field:NotBlank(message = "Workspace name is required")
    val name: String,
    val type: String?,
    val createdAt: LocalDateTime? = null,
    val updatedAt: LocalDateTime? = null
)

data class CreateWorkspaceRequest(
    @field:NotBlank(message = "Workspace name is required")
    val name: String,
    val type: String?
)

data class UpdateWorkspaceRequest(
    @field:NotBlank(message = "Workspace name is required")
    val name: String,
    val type: String?
)

