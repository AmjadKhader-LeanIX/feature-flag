package com.featureflag.service

import com.featureflag.dto.CreateFeatureFlagRequest
import com.featureflag.dto.FeatureFlagDto
import com.featureflag.dto.FeatureFlagEvaluationResponse
import com.featureflag.dto.UpdateFeatureFlagRequest
import com.featureflag.entity.FeatureFlag
import com.featureflag.entity.WorkspaceFeatureFlag
import com.featureflag.exception.FeatureFlagEvaluationException
import com.featureflag.exception.ResourceNotFoundException
import com.featureflag.repository.FeatureFlagRepository
import com.featureflag.repository.WorkspaceRepository
import com.featureflag.repository.WorkspaceFeatureFlagRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*
import kotlin.math.abs

@Service
@Transactional
class FeatureFlagService(
    private val featureFlagRepository: FeatureFlagRepository,
    private val workspaceRepository: WorkspaceRepository,
    private val workspaceFeatureFlagRepository: WorkspaceFeatureFlagRepository
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

        val workspaceFeatureFlags = workspaceFeatureFlagRepository.findByWorkspace(workspace)
        return workspaceFeatureFlags.map { it.featureFlag.toDto() }
    }

    fun createFeatureFlag(request: CreateFeatureFlagRequest): FeatureFlagDto {
        if (featureFlagRepository.existsByTeamAndName(request.team, request.name)) {
            throw IllegalArgumentException("Feature flag with name '${request.name}' already exists in this team")
        }

        val featureFlag = FeatureFlag(
            name = request.name,
            description = request.description,
            team = request.team,
            rolloutPercentage = request.rolloutPercentage
        )
        val savedFeatureFlag = featureFlagRepository.save(featureFlag)

        // Step 1: Get all workspaces and create WorkspaceFeatureFlag entries with isEnabled=false
        val allWorkspaces = workspaceRepository.findAll()
        val workspaceFeatureFlags = allWorkspaces.map { workspace ->
            WorkspaceFeatureFlag(
                workspace = workspace,
                featureFlag = savedFeatureFlag,
                isEnabled = false
            )
        }
        val savedWorkspaceFeatureFlags = workspaceFeatureFlagRepository.saveAll(workspaceFeatureFlags)

        // Step 2: Enable percentage-based rollout for the newly created associations
        enableFeatureFlagByPercentage(savedFeatureFlag, savedWorkspaceFeatureFlags, request.rolloutPercentage)

        return savedFeatureFlag.toDto()
    }

    fun updateFeatureFlag(id: UUID, request: UpdateFeatureFlagRequest): FeatureFlagDto {
        val featureFlag = featureFlagRepository.findById(id)
            .orElseThrow { ResourceNotFoundException("Feature flag not found with id: $id") }

        if (featureFlagRepository.existsByTeamAndName(featureFlag.team, request.name) &&
            featureFlag.name != request.name) {
            throw IllegalArgumentException("Feature flag with name '${request.name}' already exists in this team")
        }

        val updatedFeatureFlag = featureFlag.copy(
            name = request.name,
            description = request.description,
            team = request.team,
            rolloutPercentage = request.rolloutPercentage
        )
        val savedFeatureFlag = featureFlagRepository.save(updatedFeatureFlag)
        return savedFeatureFlag.toDto()
    }

    fun deleteFeatureFlag(id: UUID) {
        if (!featureFlagRepository.existsById(id)) {
            throw ResourceNotFoundException("Feature flag not found with id: $id")
        }
        featureFlagRepository.deleteById(id)
    }

    fun searchFeatureFlags(name: String): List<FeatureFlagDto> {
        return featureFlagRepository.findByNameContainingIgnoreCase(name).map { it.toDto() }
    }

    @Transactional(readOnly = true)
    fun evaluateFeatureFlag(featureFlagId: UUID, customerId: UUID): FeatureFlagEvaluationResponse {
        try {
            val featureFlag = featureFlagRepository.findById(featureFlagId)
                .orElseThrow { ResourceNotFoundException("Feature flag not found with id: $featureFlagId") }

            // Use rollout percentage evaluation
            val isEnabled = evaluateRolloutPercentage(customerId, featureFlag.rolloutPercentage)
            return FeatureFlagEvaluationResponse(
                enabled = isEnabled,
                reason = if (isEnabled) "Enabled by rollout percentage (${featureFlag.rolloutPercentage}%)"
                         else "Disabled by rollout percentage (${featureFlag.rolloutPercentage}%)"
            )

        } catch (ex: ResourceNotFoundException) {
            throw ex
        } catch (ex: Exception) {
            throw FeatureFlagEvaluationException("Failed to evaluate feature flag: ${ex.message}")
        }
    }

    private fun evaluateRolloutPercentage(customerId: UUID, rolloutPercentage: Int): Boolean {
        if (rolloutPercentage == 0) return false
        if (rolloutPercentage == 100) return true

        // Use consistent hash-based evaluation
        val hash = abs(customerId.hashCode())
        val bucket = hash % 100
        return bucket < rolloutPercentage
    }

    /**
     * Enables feature flag for workspaces based on the rollout percentage.
     * Uses consistent hash-based distribution to determine which workspaces should be enabled.
     */
    private fun enableFeatureFlagByPercentage(
        featureFlag: FeatureFlag,
        workspaceFeatureFlags: List<WorkspaceFeatureFlag>,
        rolloutPercentage: Int
    ) {
        if (rolloutPercentage == 0) return // No workspaces should be enabled
        if (rolloutPercentage == 100) {
            // Enable all workspaces
            val enabledFlags = workspaceFeatureFlags.map { wff ->
                wff.copy(isEnabled = true)
            }
            workspaceFeatureFlagRepository.saveAll(enabledFlags)
            return
        }

        // For partial rollout, use consistent hash-based selection
        val workspacesToEnable = mutableListOf<WorkspaceFeatureFlag>()

        workspaceFeatureFlags.forEach { workspaceFeatureFlag ->
            val workspaceId = workspaceFeatureFlag.workspace.id!!
            val hash = abs((featureFlag.id.toString() + workspaceId.toString()).hashCode())
            val bucket = hash % 100

            if (bucket < rolloutPercentage) {
                workspacesToEnable.add(workspaceFeatureFlag.copy(isEnabled = true))
            }
        }

        if (workspacesToEnable.isNotEmpty()) {
            workspaceFeatureFlagRepository.saveAll(workspacesToEnable)
        }
    }

    private fun FeatureFlag.toDto(): FeatureFlagDto {
        return FeatureFlagDto(
            id = this.id,
            name = this.name,
            description = this.description,
            team = this.team,
            rolloutPercentage = this.rolloutPercentage
        )
    }
}


