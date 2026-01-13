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

        workspaceFeatureFlagRepository.saveAll(
            workspaceRepository.findAll().map { workspace ->
                WorkspaceFeatureFlag(
                    workspace = workspace,
                    featureFlag = savedFeatureFlag,
                    isEnabled = false
                )
            }
        )

        updateFeatureFlagRollout(savedFeatureFlag, request.rolloutPercentage)

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

        updateFeatureFlagRollout(savedFeatureFlag, request.rolloutPercentage)

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
     * Updates the rollout of a feature flag across all workspaces based on the new percentage.
     *
     * This method ensures two critical guarantees:
     * 1. When INCREASING percentage: Previously enabled workspaces stay enabled
     * 2. When DECREASING percentage: Some enabled workspaces are disabled
     *
     * How it works:
     * - Uses deterministic hashing to assign each workspace to a "bucket" (0-99)
     * - The same workspace-feature flag combination always produces the same bucket
     * - Workspaces with bucket < newPercentage are enabled, others are disabled
     *
     * Example with 1000 workspaces:
     * - At 30%: Workspaces in buckets 0-29 (~300 workspaces) are enabled
     * - Increase to 50%: Buckets 0-29 stay enabled + buckets 30-49 get enabled (~500 total)
     * - Decrease to 20%: Only buckets 0-19 stay enabled (~200 total), buckets 20-29 get disabled
     *
     * @param featureFlag The feature flag being updated
     * @param newPercentage The new rollout percentage (0-100)
     */
    private fun updateFeatureFlagRollout(
        featureFlag: FeatureFlag,
        newPercentage: Int
    ) {
        // Load all existing workspace-feature flag associations for this feature flag by region
        val allWorkspaceFeatureFlags =
            if (featureFlag.regions.equals("ALL", ignoreCase = true))
                workspaceFeatureFlagRepository.findByFeatureFlag(featureFlag)
            else
                workspaceFeatureFlagRepository.findByFeatureFlagAndWorkspaceRegion(featureFlag, featureFlag.regions)

        // Case 1: 0% rollout means disable all workspaces
        if (newPercentage == 0) {
            val disabledFlags = allWorkspaceFeatureFlags.map { existing ->
                WorkspaceFeatureFlag(
                    id = existing.id,
                    workspace = existing.workspace,
                    featureFlag = existing.featureFlag,
                    isEnabled = false
                )
            }
            if (disabledFlags.isNotEmpty()) {
                workspaceFeatureFlagRepository.saveAll(disabledFlags)
            }
            return
        }

        // Case 2: 100% rollout means enable all workspaces
        if (newPercentage == 100) {
            val enabledFlags = allWorkspaceFeatureFlags.map { existing ->
                WorkspaceFeatureFlag(
                    id = existing.id,
                    workspace = existing.workspace,
                    featureFlag = existing.featureFlag,
                    isEnabled = true
                )
            }
            workspaceFeatureFlagRepository.saveAll(enabledFlags)
            return
        }

        // For percentage between 1-99, use deterministic ordering with exact count
        // Calculate exact number of workspaces to enable
        val totalWorkspaces = allWorkspaceFeatureFlags.size
        val targetEnabledCount = (totalWorkspaces * newPercentage / 100.0).toInt()

        // Create a deterministic ordering of all workspaces using hash
        // This ensures the same workspaces are always selected for the same percentage
        val sortedWorkspaces = allWorkspaceFeatureFlags.sortedBy { workspaceFeatureFlag ->
            val workspaceId = workspaceFeatureFlag.workspace.id!!
            // Calculate a deterministic hash for this workspace-feature flag combination
            abs((featureFlag.id.toString() + workspaceId.toString()).hashCode())
        }

        val workspacesToUpdate = mutableListOf<WorkspaceFeatureFlag>()

        // Enable exactly targetEnabledCount workspaces (the first ones in sorted order)
        // Disable the rest
        sortedWorkspaces.forEachIndexed { index, workspaceFeatureFlag ->
            val shouldBeEnabled = index < targetEnabledCount

            // Only update workspaces where the state needs to change
            // This ensures:
            // - On increase (e.g., 30% → 50%): First 30% stay enabled, next 20% get enabled
            // - On decrease (e.g., 50% → 30%): First 30% stay enabled, remaining 20% get disabled
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

        // Batch update all workspaces that need state changes
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
