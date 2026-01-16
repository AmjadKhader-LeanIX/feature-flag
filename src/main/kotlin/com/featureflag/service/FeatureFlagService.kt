package com.featureflag.service

import com.featureflag.dto.CreateFeatureFlagRequest
import com.featureflag.dto.FeatureFlagDto
import com.featureflag.dto.UpdateFeatureFlagRequest
import com.featureflag.dto.UpdateWorkspaceFeatureFlagRequest
import com.featureflag.entity.FeatureFlag
import com.featureflag.entity.WorkspaceFeatureFlag
import com.featureflag.exception.ResourceNotFoundException
import com.featureflag.repository.FeatureFlagRepository
import com.featureflag.repository.WorkspaceFeatureFlagRepository
import com.featureflag.repository.WorkspaceRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
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

        val featureFlagRegions = featureFlag.regions.split(",").map { it.trim() }.toList()

        workspaceFeatureFlagRepository.saveAll(
            workspaceRepository.findByRegionIn(featureFlagRegions).map { workspace ->
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
     * Enable or disable a feature flag for specific workspaces by their IDs.
     * This allows manual override of the automatic rollout percentage logic.
     * After updating the workspace flags, automatically recalculates the rollout percentage.
     *
     * @param featureFlagId The ID of the feature flag to update
     * @param request Contains the list of workspace IDs and the enabled flag
     * @throws ResourceNotFoundException if the feature flag or any workspace is not found
     */
    @Transactional
    fun updateWorkspaceFeatureFlags(featureFlagId: UUID, request: UpdateWorkspaceFeatureFlagRequest) {
        // Validate feature flag exists
        val featureFlag = featureFlagRepository.findById(featureFlagId)
            .orElseThrow { ResourceNotFoundException("Feature flag not found with id: $featureFlagId") }

        // Validate all workspaces exist
        val workspaces = workspaceRepository.findAllById(request.workspaceIds)
        if (workspaces.size != request.workspaceIds.size) {
            throw ResourceNotFoundException("One or more workspaces not found")
        }

        // Get count of enabled workspaces BEFORE the change
        val allAssociationsBefore = workspaceFeatureFlagRepository.findByFeatureFlag(featureFlag)
        val oldEnabledCount = allAssociationsBefore.count { it.isEnabled }

        // Find existing associations for these workspaces
        val existingAssociations = workspaceFeatureFlagRepository
            .findByFeatureFlagIdAndWorkspaceIdIn(featureFlagId, request.workspaceIds)

        if (existingAssociations.isEmpty()) {
            throw IllegalArgumentException("No workspace-feature flag associations found for the specified workspaces")
        }

        // Update enabled status for all specified workspaces
        existingAssociations.forEach { association ->
            association.isEnabled = request.enabled
            association.updatedAt = LocalDateTime.now()
        }

        workspaceFeatureFlagRepository.saveAll(existingAssociations)

        // Get count of enabled workspaces AFTER the change
        val allAssociationsAfter = workspaceFeatureFlagRepository.findByFeatureFlag(featureFlag)
        val newEnabledCount = allAssociationsAfter.count { it.isEnabled }

        // Recalculate rollout percentage based on current enabled state
        val oldRolloutPercentage = featureFlag.rolloutPercentage
        recalculateRolloutPercentage(featureFlag)

        // Reload the feature flag to get the updated rollout percentage
        val updatedFeatureFlag = featureFlagRepository.findById(featureFlagId)
            .orElseThrow { ResourceNotFoundException("Feature flag not found with id: $featureFlagId") }

        // Log the workspace update with old and new enabled counts
        auditLogService.logWorkspaceUpdate(
            oldEnabledCount = oldEnabledCount,
            newEnabledCount = newEnabledCount,
            oldRolloutPercentage = oldRolloutPercentage,
            newRolloutPercentage = updatedFeatureFlag.rolloutPercentage,
            featureFlag = updatedFeatureFlag
        )
    }

    /**
     * Recalculates the rollout percentage of a feature flag based on the current enabled state
     * of all workspace associations in the target regions.
     */
    private fun recalculateRolloutPercentage(featureFlag: FeatureFlag) {
        // Parse regions from string
        val featureFlagRegions = featureFlag.regions.split(",").map { it.trim() }

        // Get all workspaces in the feature flag's target regions
        val targetWorkspaces = if (featureFlagRegions.contains("ALL")) {
            workspaceRepository.findAll()
        } else {
            workspaceRepository.findByRegionIn(featureFlagRegions)
        }

        if (targetWorkspaces.isEmpty()) {
            val updatedFlag = featureFlag.copy(
                rolloutPercentage = 0,
                updatedAt = LocalDateTime.now()
            )
            featureFlagRepository.save(updatedFlag)
            return
        }

        // Get all workspace associations for this feature flag
        val allAssociations = workspaceFeatureFlagRepository.findByFeatureFlag(featureFlag)

        // Count how many are enabled
        val enabledCount = allAssociations.count { it.isEnabled }

        // Calculate percentage
        val newPercentage = ((enabledCount.toDouble() / targetWorkspaces.size) * 100).toInt()

        // Update the feature flag's rollout percentage
        val updatedFlag = featureFlag.copy(
            rolloutPercentage = newPercentage,
            updatedAt = LocalDateTime.now()
        )
        featureFlagRepository.save(updatedFlag)
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
        val allWorkspaceFeatureFlags = workspaceFeatureFlagRepository.findByFeatureFlag(featureFlag)

        // Parse the feature flag's regions
        val featureFlagRegions = featureFlag.regions.split(",").map { it.trim() }.toSet()

        // Filter workspaces to only include those whose region matches one of the feature flag's regions
        val workspaceFeatureFlagsByRegion = allWorkspaceFeatureFlags.filter { workspaceFeatureFlag ->
            val workspaceRegion = workspaceFeatureFlag.workspace.region?.toString()
            workspaceRegion != null && featureFlagRegions.contains(workspaceRegion)
        }

        // Case 1: 0% rollout means disable all workspaces
        if (newPercentage == 0) {
            workspaceFeatureFlagsByRegion.forEach { existing ->
                existing.isEnabled = false
                existing.updatedAt = LocalDateTime.now()
            }
            if (workspaceFeatureFlagsByRegion.isNotEmpty()) {
                workspaceFeatureFlagRepository.saveAll(workspaceFeatureFlagsByRegion)
            }
            return
        }

        // Case 2: 100% rollout means enable all workspaces
        if (newPercentage == 100) {
            workspaceFeatureFlagsByRegion.forEach { existing ->
                existing.isEnabled = true
                existing.updatedAt = LocalDateTime.now()
            }
            workspaceFeatureFlagRepository.saveAll(workspaceFeatureFlagsByRegion)
            return
        }

        // For percentage between 1-99, use deterministic ordering with exact count
        // Calculate exact number of workspaces to enable
        val totalWorkspaces = workspaceFeatureFlagsByRegion.size
        val targetEnabledCount = (totalWorkspaces * newPercentage / 100.0).toInt()

        // Create a deterministic ordering of all workspaces using hash
        // This ensures the same workspaces are always selected for the same percentage
        val sortedWorkspaces = workspaceFeatureFlagsByRegion.sortedBy { workspaceFeatureFlag ->
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
                workspaceFeatureFlag.isEnabled = shouldBeEnabled
                workspaceFeatureFlag.updatedAt = LocalDateTime.now()
                workspacesToUpdate.add(workspaceFeatureFlag)
            }
        }

        // Batch update all workspaces that need state changes
        if (workspacesToUpdate.isNotEmpty()) {
            workspaceFeatureFlagRepository.saveAll(workspacesToUpdate)
        }
    }

    /**
     * Get all workspaces that have this feature flag enabled
     */
    fun getEnabledWorkspacesForFeatureFlag(featureFlagId: UUID): List<com.featureflag.dto.WorkspaceDto> {
        val featureFlag = featureFlagRepository.findById(featureFlagId)
            .orElseThrow { ResourceNotFoundException("Feature flag not found with id: $featureFlagId") }

        val enabledAssociations = workspaceFeatureFlagRepository.findByFeatureFlag(featureFlag)
            .filter { it.isEnabled }

        return enabledAssociations.map { it.workspace.toDto() }
    }

    /**
     * Get paginated workspaces that have this feature flag enabled
     */
    fun getEnabledWorkspacesForFeatureFlagPaginated(featureFlagId: UUID, page: Int = 0, size: Int = 100, searchTerm: String? = null): com.featureflag.dto.PageableResponse<com.featureflag.dto.WorkspaceDto> {
        val featureFlag = featureFlagRepository.findById(featureFlagId)
            .orElseThrow { ResourceNotFoundException("Feature flag not found with id: $featureFlagId") }

        val pageable = org.springframework.data.domain.PageRequest.of(page, size, org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.ASC, "workspace.name"))
        val workspaceFlagPage = if (searchTerm.isNullOrBlank()) {
            workspaceFeatureFlagRepository.findEnabledByFeatureFlagId(featureFlagId, pageable)
        } else {
            workspaceFeatureFlagRepository.searchEnabledByFeatureFlagId(featureFlagId, searchTerm, pageable)
        }
        val workspaceDtoPage = workspaceFlagPage.map { it.workspace.toDto() }

        return com.featureflag.dto.PageableResponse.of(workspaceDtoPage)
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

    private fun com.featureflag.entity.Workspace.toDto(): com.featureflag.dto.WorkspaceDto {
        return com.featureflag.dto.WorkspaceDto(
            id = this.id,
            name = this.name,
            type = this.type,
            region = this.region?.toString(),
            createdAt = this.createdAt,
            updatedAt = this.updatedAt
        )
    }
}
