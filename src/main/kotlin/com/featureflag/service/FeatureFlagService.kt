package com.featureflag.service

import com.featureflag.dto.CreateFeatureFlagRequest
import com.featureflag.dto.FeatureFlagDto
import com.featureflag.dto.UpdateFeatureFlagRequest
import com.featureflag.entity.FeatureFlag
import com.featureflag.entity.Region
import com.featureflag.entity.WorkspaceFeatureFlag
import com.featureflag.exception.ResourceNotFoundException
import com.featureflag.repository.FeatureFlagRepository
import com.featureflag.repository.WorkspaceFeatureFlagRepository
import com.featureflag.repository.WorkspaceRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*
import kotlin.math.abs

@Service
class FeatureFlagService(
    private val featureFlagRepository: FeatureFlagRepository,
    private val workspaceRepository: WorkspaceRepository,
    private val workspaceFeatureFlagRepository: WorkspaceFeatureFlagRepository,
    private val auditLogService: AuditLogService
) {

    fun getAllFeatureFlags(): List<FeatureFlagDto> {
        return featureFlagRepository.findAll().map { it.toDto() }
    }

    fun getFeatureFlagById(id: UUID): FeatureFlagDto {
        val featureFlag = featureFlagRepository.findById(id)
            .orElseThrow { ResourceNotFoundException("Feature flag not found with id: $id") }
        return featureFlag.toDto()
    }

    fun getFeatureFlagsByTeam(team: String): List<FeatureFlagDto> {
        return featureFlagRepository.findByTeam(team).map { it.toDto() }
    }

    fun getFeatureFlagsByWorkspace(workspaceId: UUID): List<FeatureFlagDto> {
        val workspace = workspaceRepository.findById(workspaceId)
            .orElseThrow { ResourceNotFoundException("Workspace not found with id: $workspaceId") }

        val workspaceFeatureFlags = workspaceFeatureFlagRepository.findByWorkspaceId(workspace.id!!)
        return workspaceFeatureFlags.map { it.featureFlag.toDto() }
    }

    @Transactional
    fun createFeatureFlag(request: CreateFeatureFlagRequest): FeatureFlagDto {
        if (featureFlagRepository.existsByTeamAndName(request.team, request.name)) {
            throw IllegalArgumentException("Feature flag with name '${request.name}' already exists in this team")
        }

        val regionsString = request.regions.joinToString(",")
        val featureFlag = FeatureFlag(
            name = request.name,
            description = request.description,
            team = request.team,
            rolloutPercentage = request.rolloutPercentage,
            regions = regionsString
        )
        val savedFeatureFlag = featureFlagRepository.save(featureFlag)

        updateFeatureFlagRollout(savedFeatureFlag)

        // Log the creation
        auditLogService.logCreate(savedFeatureFlag)

        return savedFeatureFlag.toDto()
    }

    @Transactional
    fun updateFeatureFlag(id: UUID, request: UpdateFeatureFlagRequest): FeatureFlagDto {
        val featureFlag = featureFlagRepository.findById(id)
            .orElseThrow { ResourceNotFoundException("Feature flag not found with id: $id") }

        val oldRolloutPercentage = featureFlag.rolloutPercentage
        if (featureFlagRepository.existsByTeamAndName(featureFlag.team, request.name) &&
            featureFlag.name != request.name
        ) {
            throw IllegalArgumentException("Feature flag with name '${request.name}' already exists in this team")
        }

        val regionsString = request.regions.joinToString(",")
        val updatedFeatureFlag = featureFlag.copy(
            name = request.name,
            description = request.description,
            team = request.team,
            rolloutPercentage = request.rolloutPercentage,
            regions = regionsString
        )
        val savedFeatureFlag = featureFlagRepository.save(updatedFeatureFlag)

        updateFeatureFlagRollout(savedFeatureFlag)

        // Log the update
        auditLogService.logUpdate(oldRolloutPercentage, savedFeatureFlag.rolloutPercentage, savedFeatureFlag)

        return savedFeatureFlag.toDto()
    }

    @Transactional
    fun deleteFeatureFlag(id: UUID) {
        val featureFlag = featureFlagRepository.findById(id)
            .orElseThrow { ResourceNotFoundException("Feature flag not found with id: $id") }

        // Log the deletion before deleting
        auditLogService.logDelete(featureFlag)

        featureFlagRepository.deleteById(id)
    }

    fun searchFeatureFlags(name: String): List<FeatureFlagDto> {
        return featureFlagRepository.findByNameContainingIgnoreCase(name).map { it.toDto() }
    }

    /**
     * Updates the rollout of a feature flag based on region matching and percentage.
     *
     * Logic:
     * - If feature flag regions contains ALL, apply to all workspaces
     * - Otherwise, only apply to workspaces with matching region (any of the feature flag's regions)
     * - Uses deterministic hashing to assign workspaces to buckets for consistent rollout
     */
    private fun updateFeatureFlagRollout(featureFlag: FeatureFlag) {
        val allWorkspaceFeatureFlags = workspaceFeatureFlagRepository.findByFeatureFlag(featureFlag)
        val workspacesToUpdate = mutableListOf<WorkspaceFeatureFlag>()

        // Parse comma-separated regions string
        val regionsList = featureFlag.regions.split(",").map { it.trim() }

        allWorkspaceFeatureFlags.forEach { workspaceFeatureFlag ->
            val workspace = workspaceFeatureFlag.workspace

            // Determine if this workspace matches any of the feature flag's regions
            val isRegionMatch = when {
                regionsList.contains("ALL") -> true  // ALL flags apply to all workspaces
                workspace.region == null -> false  // Workspace has no region assigned
                else -> regionsList.contains(workspace.region.name)  // Check if workspace region is in feature flag's regions
            }

            // Calculate whether workspace should be enabled
            val shouldBeEnabled = if (isRegionMatch) {
                when (featureFlag.rolloutPercentage) {
                    0 -> false
                    100 -> true
                    else -> {
                        // Use deterministic hashing for percentage-based rollout
                        val hash = abs((featureFlag.id.toString() + workspace.id.toString()).hashCode())
                        val bucket = hash % 100
                        bucket < featureFlag.rolloutPercentage
                    }
                }
            } else {
                false  // Region doesn't match, disable
            }

            // Only update if state needs to change
            if (workspaceFeatureFlag.isEnabled != shouldBeEnabled) {
                workspacesToUpdate.add(
                    WorkspaceFeatureFlag(
                        id = workspaceFeatureFlag.id,
                        workspace = workspaceFeatureFlag.workspace,
                        featureFlag = workspaceFeatureFlag.featureFlag,
                        isEnabled = shouldBeEnabled
                    )
                )
            }
        }

        if (workspacesToUpdate.isNotEmpty()) {
            workspaceFeatureFlagRepository.saveAll(workspacesToUpdate)
        }
    }

    private fun FeatureFlag.toDto(): FeatureFlagDto {
        return FeatureFlagDto(
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
