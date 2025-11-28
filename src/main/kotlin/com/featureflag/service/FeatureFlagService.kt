package com.featureflag.service

import com.featureflag.dto.CreateFeatureFlagRequest
import com.featureflag.dto.FeatureFlagDto
import com.featureflag.dto.UpdateFeatureFlagRequest
import com.featureflag.entity.FeatureFlag
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

        val workspaceFeatureFlags = workspaceFeatureFlagRepository.findByWorkspaceId(workspace.id!!)
        return workspaceFeatureFlags.map { it.featureFlag.toDto() }
    }

    @Transactional
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

        enableFeatureFlagByPercentage(savedFeatureFlag, 0)

        return savedFeatureFlag.toDto()
    }

    @Transactional
    fun updateFeatureFlag(id: UUID, request: UpdateFeatureFlagRequest): FeatureFlagDto {
        val featureFlag = featureFlagRepository.findById(id)
            .orElseThrow { ResourceNotFoundException("Feature flag not found with id: $id") }

        if (featureFlagRepository.existsByTeamAndName(featureFlag.team, request.name) &&
            featureFlag.name != request.name
        ) {
            throw IllegalArgumentException("Feature flag with name '${request.name}' already exists in this team")
        }

        val updatedFeatureFlag = featureFlag.copy(
            name = request.name,
            description = request.description,
            team = request.team,
            rolloutPercentage = request.rolloutPercentage
        )
        val savedFeatureFlag = featureFlagRepository.save(updatedFeatureFlag)

        enableFeatureFlagByPercentage(savedFeatureFlag, request.rolloutPercentage)

        return savedFeatureFlag.toDto()
    }

    @Transactional
    fun deleteFeatureFlag(id: UUID) {
        if (!featureFlagRepository.existsById(id)) {
            throw ResourceNotFoundException("Feature flag not found with id: $id")
        }
        featureFlagRepository.deleteById(id)
    }

    fun searchFeatureFlags(name: String): List<FeatureFlagDto> {
        return featureFlagRepository.findByNameContainingIgnoreCase(name).map { it.toDto() }
    }

    private fun enableFeatureFlagByPercentage(
        featureFlag: FeatureFlag,
        rolloutPercentage: Int
    ) {
        // todo: This logic needs to be improved where it should not change the "enabled" value for the workspace that has it true expect in the cases where we are decreasing the rollout percentage.
        val allWorkspaceFeatureFlags = workspaceFeatureFlagRepository.findByFeatureFlag(featureFlag)

        // First, disable feature flag for all existing workspace-feature flag associations
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

        if (rolloutPercentage == 0) return
        if (rolloutPercentage == 100) {
            val enabledFlags = workspaceFeatureFlagRepository.findByFeatureFlag(featureFlag).map { existing ->
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

        val workspacesToEnable = mutableListOf<WorkspaceFeatureFlag>()

        allWorkspaceFeatureFlags.forEach { workspaceFeatureFlag ->
            val workspaceId = workspaceFeatureFlag.workspace.id!!
            val hash = abs((featureFlag.id.toString() + workspaceId.toString()).hashCode())
            val bucket = hash % 100

            if (bucket < rolloutPercentage) {
                workspacesToEnable.add(
                    WorkspaceFeatureFlag(
                        id = workspaceFeatureFlag.id,
                        workspace = workspaceFeatureFlag.workspace,
                        featureFlag = workspaceFeatureFlag.featureFlag,
                        isEnabled = true
                    )
                )
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
