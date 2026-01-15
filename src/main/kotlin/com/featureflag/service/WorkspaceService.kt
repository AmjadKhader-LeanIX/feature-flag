package com.featureflag.service

import com.featureflag.dto.WorkspaceDto
import com.featureflag.entity.Workspace
import com.featureflag.exception.ResourceNotFoundException
import com.featureflag.repository.WorkspaceRepository
import com.featureflag.repository.WorkspaceFeatureFlagRepository
import org.springframework.stereotype.Service
import java.util.*

@Service
class WorkspaceService(
    private val workspaceRepository: WorkspaceRepository,
    private val workspaceFeatureFlagRepository: WorkspaceFeatureFlagRepository
) {

    fun getAllWorkspaces(): List<WorkspaceDto> {
        return workspaceRepository.findAll().map { it.toDto() }
    }

    fun getWorkspaceById(id: UUID): WorkspaceDto {
        val workspace = workspaceRepository.findById(id)
            .orElseThrow { ResourceNotFoundException("Workspace not found with id: $id") }
        return workspace.toDto()
    }

    /**
     * Get all feature flags that are enabled for this workspace
     */
    fun getEnabledFeatureFlagsForWorkspace(workspaceId: UUID): List<com.featureflag.dto.FeatureFlagDto> {
        val workspace = workspaceRepository.findById(workspaceId)
            .orElseThrow { ResourceNotFoundException("Workspace not found with id: $workspaceId") }

        val enabledAssociations = workspaceFeatureFlagRepository.findByWorkspaceId(workspace.id!!)
            .filter { it.isEnabled }

        return enabledAssociations.map { it.featureFlag.toDto() }
    }

    private fun Workspace.toDto(): WorkspaceDto {
        return WorkspaceDto(
            id = this.id,
            name = this.name,
            type = this.type,
            region = this.region?.toString(),
            createdAt = this.createdAt,
            updatedAt = this.updatedAt
        )
    }

    private fun com.featureflag.entity.FeatureFlag.toDto(): com.featureflag.dto.FeatureFlagDto {
        return com.featureflag.dto.FeatureFlagDto(
            id = this.id,
            name = this.name,
            description = this.description,
            team = this.team,
            rolloutPercentage = this.rolloutPercentage,
            regions = this.regions.split(",").map { it.trim() },
            createdAt = this.createdAt,
            updatedAt = this.updatedAt
        )
    }
}
