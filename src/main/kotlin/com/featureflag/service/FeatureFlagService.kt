package com.featureflag.service

import com.featureflag.dto.CreateFeatureFlagRequest
import com.featureflag.dto.FeatureFlagDto
import com.featureflag.dto.UpdateFeatureFlagRequest
import com.featureflag.dto.UpdateWorkspaceFeatureFlagRequest
import com.featureflag.dto.WorkspaceDto
import com.featureflag.entity.FeatureFlag
import com.featureflag.entity.Region
import com.featureflag.entity.Workspace
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
import kotlin.math.roundToInt

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

    fun getFeatureFlagsByTeam(team: String): List<FeatureFlagDto> {
        return featureFlagRepository.findByTeam(team).map { it.toDto() }
    }

    @Transactional
    fun createFeatureFlag(request: CreateFeatureFlagRequest): FeatureFlagDto {
        if (featureFlagRepository.existsByTeamAndName(request.team, request.name)) {
            throw IllegalArgumentException("Feature flag with name '${request.name}' already exists in this team")
        }

        val featureFlag = FeatureFlag(
            name = request.name.lowercase(),
            description = request.description,
            team = request.team.uppercase(),
            rolloutPercentage = request.rolloutPercentage
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

        val updatedFeatureFlag = featureFlag.copy(
            rolloutPercentage = request.rolloutPercentage
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

        // Get count of enabled workspaces BEFORE the change
        val oldEnabledCount = workspaceFeatureFlagRepository.countEnabledByFeatureFlag(featureFlag).toInt()

        // Get currently enabled workspaces to maintain additivity
        val currentlyEnabledAssociations = workspaceFeatureFlagRepository.findByFeatureFlag(featureFlag)
            .filter { it.isEnabled }
        val currentlyEnabledWorkspaceIds = currentlyEnabledAssociations.map { it.workspace.id!! }.toMutableList()

        // Merge new workspace IDs with currently enabled ones (additive behavior)
        val allPriorityWorkspaceIds = (currentlyEnabledWorkspaceIds + request.workspaceIds).distinct().toMutableList()

        // Remove excluded workspaces from priority list
        allPriorityWorkspaceIds.removeAll(request.excludedWorkspaceIds)

        // Only update workspaces if workspace IDs are provided
        if (request.workspaceIds.isNotEmpty()) {
            // Validate all workspaces exist
            val workspaces = workspaceRepository.findAllById(request.workspaceIds)
            if (workspaces.size != request.workspaceIds.size) {
                throw ResourceNotFoundException("One or more workspaces not found")
            }

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

                workspaceFeatureFlagRepository.save(association)
            }
        }

        // Update rollout percentage
        val oldRolloutPercentage = featureFlag.rolloutPercentage
        val newRolloutPercentage = if (request.rolloutPercentage != oldRolloutPercentage) {
            // If targetRegion is specified, apply rollout to that region only
            if (request.targetRegion != null) {
                applyRegionSpecificRolloutWithPriority(featureFlag, request.rolloutPercentage!!, request.targetRegion, allPriorityWorkspaceIds, request.excludedWorkspaceIds)
            } else {
                // Apply rollout globally with priority for manually picked workspaces (includes all previously enabled)
                updateFeatureFlagRolloutWithPriority(featureFlag, request.rolloutPercentage!!, allPriorityWorkspaceIds, request.excludedWorkspaceIds)
            }

            // Recalculate the overall rollout percentage based on what's actually enabled
            recalculateRolloutPercentage(featureFlag)
            val reloadedFlag = featureFlagRepository.findById(featureFlagId)
                .orElseThrow { ResourceNotFoundException("Feature flag not found with id: $featureFlagId") }
            reloadedFlag.rolloutPercentage
        } else {
            // Recalculate rollout percentage based on current enabled state
            recalculateRolloutPercentage(featureFlag)
            val reloadedFlag = featureFlagRepository.findById(featureFlagId)
                .orElseThrow { ResourceNotFoundException("Feature flag not found with id: $featureFlagId") }
            reloadedFlag.rolloutPercentage
        }

        // Reload the feature flag to get the updated state
        val updatedFeatureFlag = featureFlagRepository.findById(featureFlagId)
            .orElseThrow { ResourceNotFoundException("Feature flag not found with id: $featureFlagId") }

        // Get workspace names for audit log
        val pinnedWorkspaceNames = if (request.workspaceIds.isNotEmpty()) {
            workspaceRepository.findAllById(request.workspaceIds).map { it.name }
        } else {
            emptyList()
        }

        val excludedWorkspaceNames = if (request.excludedWorkspaceIds.isNotEmpty()) {
            workspaceRepository.findAllById(request.excludedWorkspaceIds).map { it.name }
        } else {
            emptyList()
        }

        val newEnabledCount = workspaceFeatureFlagRepository.countEnabledByFeatureFlag(featureFlag).toInt()

        // Log the workspace update with old and new enabled counts
        auditLogService.logWorkspaceUpdate(
            oldEnabledCount = oldEnabledCount,
            newEnabledCount = newEnabledCount,
            oldRolloutPercentage = oldRolloutPercentage,
            newRolloutPercentage = newRolloutPercentage,
            featureFlag = updatedFeatureFlag,
            newPinnedWorkspaces = pinnedWorkspaceNames,
            newExcludedWorkspaces = excludedWorkspaceNames,
            targetRegion = request.targetRegion?.name
        )
    }

    /**
     * Recalculates the rollout percentage of a feature flag based on the current enabled state
     * of all workspace associations.
     */
    private fun recalculateRolloutPercentage(featureFlag: FeatureFlag) {
        // Get all workspaces
        val targetWorkspaces = workspaceRepository.findAll()

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

        // Calculate percentage with proper rounding
        val newPercentage = ((enabledCount.toDouble() / targetWorkspaces.size) * 100).roundToInt()

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
        // Load all existing workspace-feature flag associations for this feature flag
        val workspaceFeatureFlagsByRegion = workspaceFeatureFlagRepository.findByFeatureFlag(featureFlag)

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
     * Applies rollout percentage globally while ensuring:
     * 1. Excluded workspaces are always disabled (highest priority)
     * 2. Pinned workspaces are always enabled (unless excluded)
     * 3. Other workspaces follow percentage-based rollout
     *
     * @param featureFlag The feature flag being updated
     * @param percentage The rollout percentage (0-100)
     * @param priorityWorkspaceIds List of workspace IDs that must always be enabled
     * @param excludedWorkspaceIds List of workspace IDs that must always be disabled
     */
    private fun updateFeatureFlagRolloutWithPriority(
        featureFlag: FeatureFlag,
        percentage: Int,
        priorityWorkspaceIds: List<UUID>,
        excludedWorkspaceIds: List<UUID>
    ) {
        val allAssociations = workspaceFeatureFlagRepository.findByFeatureFlag(featureFlag)
        val workspacesToUpdate = mutableListOf<WorkspaceFeatureFlag>()

        // Separate workspaces by category
        val excludedWorkspaces = allAssociations.filter { wff ->
            excludedWorkspaceIds.contains(wff.workspace.id)
        }
        val priorityWorkspaces = allAssociations.filter { wff ->
            priorityWorkspaceIds.contains(wff.workspace.id) && !excludedWorkspaceIds.contains(wff.workspace.id)
        }
        val otherWorkspaces = allAssociations.filter { wff ->
            !priorityWorkspaceIds.contains(wff.workspace.id) && !excludedWorkspaceIds.contains(wff.workspace.id)
        }

        // Always disable excluded workspaces (highest priority)
        excludedWorkspaces.forEach { wff ->
            if (wff.isEnabled) {
                wff.isEnabled = false
                wff.updatedAt = LocalDateTime.now()
                workspacesToUpdate.add(wff)
            }
        }

        // Always enable priority workspaces (unless excluded)
        priorityWorkspaces.forEach { wff ->
            if (!wff.isEnabled) {
                wff.isEnabled = true
                wff.updatedAt = LocalDateTime.now()
                workspacesToUpdate.add(wff)
            }
        }

        // Apply percentage to other workspaces
        if (percentage == 0) {
            // Disable all other workspaces
            otherWorkspaces.forEach { wff ->
                if (wff.isEnabled) {
                    wff.isEnabled = false
                    wff.updatedAt = LocalDateTime.now()
                    workspacesToUpdate.add(wff)
                }
            }
        } else if (percentage == 100) {
            // Enable all other workspaces
            otherWorkspaces.forEach { wff ->
                if (!wff.isEnabled) {
                    wff.isEnabled = true
                    wff.updatedAt = LocalDateTime.now()
                    workspacesToUpdate.add(wff)
                }
            }
        } else {
            // Apply percentage using deterministic hash
            val targetEnabledCount = (otherWorkspaces.size * percentage / 100.0).toInt()
            val sortedOthers = otherWorkspaces.sortedBy { wff ->
                val workspaceId = wff.workspace.id!!
                abs((featureFlag.id.toString() + workspaceId.toString()).hashCode())
            }

            sortedOthers.forEachIndexed { index, wff ->
                val shouldBeEnabled = index < targetEnabledCount
                if (wff.isEnabled != shouldBeEnabled) {
                    wff.isEnabled = shouldBeEnabled
                    wff.updatedAt = LocalDateTime.now()
                    workspacesToUpdate.add(wff)
                }
            }
        }

        if (workspacesToUpdate.isNotEmpty()) {
            workspaceFeatureFlagRepository.saveAll(workspacesToUpdate)
        }
    }

    /**
     * Applies rollout percentage to a specific region while ensuring:
     * 1. Excluded workspaces in the region are always disabled (highest priority)
     * 2. Pinned workspaces in the region are always enabled (unless excluded)
     * 3. Other workspaces in the region follow percentage-based rollout
     *
     * @param featureFlag The feature flag being updated
     * @param percentage The rollout percentage (0-100)
     * @param targetRegion The region to target
     * @param priorityWorkspaceIds List of workspace IDs that must always be enabled
     * @param excludedWorkspaceIds List of workspace IDs that must always be disabled
     */
    private fun applyRegionSpecificRolloutWithPriority(
        featureFlag: FeatureFlag,
        percentage: Int,
        targetRegion: Region,
        priorityWorkspaceIds: List<UUID>,
        excludedWorkspaceIds: List<UUID>
    ) {
        val allAssociations = workspaceFeatureFlagRepository.findByFeatureFlag(featureFlag)

        // Filter workspaces by target region
        val regionWorkspaces = allAssociations.filter {
            it.workspace.region == targetRegion && targetRegion != Region.ALL
        }

        if (regionWorkspaces.isEmpty()) {
            return
        }

        // Separate workspaces in this region by category
        val excludedWorkspacesInRegion = regionWorkspaces.filter { wff ->
            excludedWorkspaceIds.contains(wff.workspace.id)
        }
        val priorityWorkspacesInRegion = regionWorkspaces.filter { wff ->
            priorityWorkspaceIds.contains(wff.workspace.id) && !excludedWorkspaceIds.contains(wff.workspace.id)
        }
        val otherWorkspacesInRegion = regionWorkspaces.filter { wff ->
            !priorityWorkspaceIds.contains(wff.workspace.id) && !excludedWorkspaceIds.contains(wff.workspace.id)
        }

        val workspacesToUpdate = mutableListOf<WorkspaceFeatureFlag>()

        // Always enable priority workspaces in the target region (unless excluded)
        priorityWorkspacesInRegion.forEach { wff ->
            if (!wff.isEnabled) {
                wff.isEnabled = true
                wff.updatedAt = LocalDateTime.now()
                workspacesToUpdate.add(wff)
            }
        }

        // Apply percentage to other workspaces in the region
        if (percentage == 0) {
            // Disable all other workspaces in region (but keep priority enabled)
            otherWorkspacesInRegion.forEach { wff ->
                if (wff.isEnabled) {
                    wff.isEnabled = false
                    wff.updatedAt = LocalDateTime.now()
                    workspacesToUpdate.add(wff)
                }
            }
        } else if (percentage == 100) {
            // Enable all workspaces in region
            otherWorkspacesInRegion.forEach { wff ->
                if (!wff.isEnabled) {
                    wff.isEnabled = true
                    wff.updatedAt = LocalDateTime.now()
                    workspacesToUpdate.add(wff)
                }
            }
        } else {
            // Apply percentage using deterministic hash
            val targetEnabledCount = (otherWorkspacesInRegion.size * percentage / 100.0).toInt()
            val sortedOthers = otherWorkspacesInRegion.sortedBy { wff ->
                val workspaceId = wff.workspace.id!!
                abs((featureFlag.id.toString() + workspaceId.toString()).hashCode())
            }

            sortedOthers.forEachIndexed { index, wff ->
                val shouldBeEnabled = index < targetEnabledCount
                if (wff.isEnabled != shouldBeEnabled) {
                    wff.isEnabled = shouldBeEnabled
                    wff.updatedAt = LocalDateTime.now()
                    workspacesToUpdate.add(wff)
                }
            }

            // Always disable excluded workspaces in the target region (highest priority)
            excludedWorkspacesInRegion.forEach { wff ->
                if (wff.isEnabled) {
                    wff.isEnabled = false
                    wff.updatedAt = LocalDateTime.now()
                    workspacesToUpdate.add(wff)
                }
            }
        }

        if (workspacesToUpdate.isNotEmpty()) {
            workspaceFeatureFlagRepository.saveAll(workspacesToUpdate)
        }
    }

    /**
     * Get paginated workspaces that have this feature flag enabled
     */
    fun getEnabledWorkspacesForFeatureFlagPaginated(featureFlagId: UUID, page: Int = 0, size: Int = 100, searchTerm: String? = null): com.featureflag.dto.PageableResponse<com.featureflag.dto.WorkspaceDto> {
        featureFlagRepository.findById(featureFlagId)
            .orElseThrow { ResourceNotFoundException("Feature flag not found with id: $featureFlagId") }

        val pageable = org.springframework.data.domain.PageRequest.of(page, size, org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.ASC, "workspace.name"))
        val workspaceFlagPage = if (searchTerm.isNullOrBlank()) {
            workspaceFeatureFlagRepository.findEnabledByFeatureFlagId(featureFlagId, pageable)
        } else {
            workspaceFeatureFlagRepository.searchEnabledByFeatureFlagId(featureFlagId, searchTerm, pageable)
        }
        val workspaceDtoPage = workspaceFlagPage.map { wff -> wff.workspace.toDto() }

        return com.featureflag.dto.PageableResponse.of(workspaceDtoPage)
    }

    /**
     * Get count of enabled workspaces grouped by region for a feature flag
     */
    fun getWorkspaceCountsByRegion(featureFlagId: UUID): Map<String, Long> {
        featureFlagRepository.findById(featureFlagId)
            .orElseThrow { ResourceNotFoundException("Feature flag not found with id: $featureFlagId") }

        val regionCounts = workspaceFeatureFlagRepository.countEnabledWorkspacesByRegion(featureFlagId)
        return regionCounts.associate { it.getRegion() to it.getCount() }
    }

    private fun FeatureFlag.toDto(): FeatureFlagDto {
        return FeatureFlagDto(
            id = this.id,
            name = this.name,
            description = this.description,
            team = this.team,
            rolloutPercentage = this.rolloutPercentage,
            createdAt = this.createdAt,
            updatedAt = this.updatedAt
        )
    }

    private fun Workspace.toDto(): WorkspaceDto {
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
