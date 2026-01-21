package com.featureflag.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.featureflag.dto.AuditLogDto
import com.featureflag.entity.AuditOperation
import com.featureflag.entity.FeatureFlag
import com.featureflag.entity.FeatureFlagAuditLog
import com.featureflag.repository.FeatureFlagAuditLogRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import java.util.*

@Service
class AuditLogService(
    private val auditLogRepository: FeatureFlagAuditLogRepository,
    private val objectMapper: ObjectMapper
) {

    fun logCreate(featureFlag: FeatureFlag, changedBy: String? = "system") {
        val newValues = mapOf(
            "Name" to featureFlag.name,
            "Description" to featureFlag.description,
            "Team" to featureFlag.team,
            "Rollout Percentage" to featureFlag.rolloutPercentage
        )

        val auditLog = FeatureFlagAuditLog(
            featureFlagId = featureFlag.id,
            featureFlagName = featureFlag.name,
            operation = AuditOperation.CREATE,
            team = featureFlag.team,
            oldValues = null,
            newValues = objectMapper.writeValueAsString(newValues),
            changedBy = changedBy
        )

        auditLogRepository.save(auditLog)
    }

    fun logUpdate(
        oldRolloutPercentage: Int,
        newRolloutPercentage: Int,
        newFeatureFlag: FeatureFlag,
        changedBy: String? = "system"
    ) {
        val oldValues = mapOf(
            "Rollout Percentage" to oldRolloutPercentage,
        )

        val newValues = mapOf(
            "Rollout Percentage" to newRolloutPercentage,
        )

        val auditLog = FeatureFlagAuditLog(
            featureFlagId = newFeatureFlag.id,
            featureFlagName = newFeatureFlag.name,
            operation = AuditOperation.UPDATE,
            team = newFeatureFlag.team,
            oldValues = objectMapper.writeValueAsString(oldValues),
            newValues = objectMapper.writeValueAsString(newValues),
            changedBy = changedBy
        )

        auditLogRepository.save(auditLog)
    }

    fun logDelete(featureFlag: FeatureFlag, changedBy: String? = "system") {
        val oldValues = mapOf(
            "Name" to featureFlag.name,
            "Description" to featureFlag.description,
            "Team" to featureFlag.team,
            "Rollout Percentage" to featureFlag.rolloutPercentage
        )

        val auditLog = FeatureFlagAuditLog(
            featureFlagId = featureFlag.id,
            featureFlagName = featureFlag.name,
            operation = AuditOperation.DELETE,
            team = featureFlag.team,
            oldValues = objectMapper.writeValueAsString(oldValues),
            newValues = null,
            changedBy = changedBy
        )

        auditLogRepository.save(auditLog)
    }

    fun getAllAuditLogs(): List<AuditLogDto> {
        return auditLogRepository.findAllByOrderByTimestampDesc().map { it.toDto() }
    }

    fun getAllAuditLogsPaginated(page: Int, size: Int, search: String?): Page<AuditLogDto> {
        val pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"))

        val auditLogs = if (search.isNullOrBlank()) {
            auditLogRepository.findAll(pageRequest)
        } else {
            auditLogRepository.findByFeatureFlagNameContainingIgnoreCaseOrTeamContainingIgnoreCase(
                search,
                search,
                pageRequest
            )
        }

        return auditLogs.map { it.toDto() }
    }

    fun getAuditLogsByFeatureFlagId(featureFlagId: UUID): List<AuditLogDto> {
        return auditLogRepository.findByFeatureFlagIdOrderByTimestampDesc(featureFlagId).map { it.toDto() }
    }

    fun logWorkspaceUpdate(
        oldEnabledCount: Int,
        newEnabledCount: Int,
        oldRolloutPercentage: Int,
        newRolloutPercentage: Int,
        featureFlag: FeatureFlag,
        newPinnedWorkspaces: List<String> = emptyList(),
        newExcludedWorkspaces: List<String> = emptyList(),
        targetRegion: String? = null,
        changedBy: String? = "system"
    ) {
        val oldValuesMap: MutableMap<String, Any> = mutableMapOf(
            "Enabled Workspaces" to oldEnabledCount,
            "Rollout Percentage" to oldRolloutPercentage
        )

        val newValuesMap: MutableMap<String, Any> = mutableMapOf(
            "Enabled Workspaces" to newEnabledCount,
            "Rollout Percentage" to newRolloutPercentage
        )

        // Add new pinned workspaces if any
        if (newPinnedWorkspaces.isNotEmpty()) {
            newValuesMap["Manually Enabled Workspaces"] = newPinnedWorkspaces
        }

        // Add new excluded workspaces if any
        if (newExcludedWorkspaces.isNotEmpty()) {
            newValuesMap["Manually Disabled Workspaces"] = newExcludedWorkspaces
        }

        // Add target region if specified
        if (targetRegion != null) {
            newValuesMap["Region"] = targetRegion
        }

        val auditLog = FeatureFlagAuditLog(
            featureFlagId = featureFlag.id,
            featureFlagName = featureFlag.name,
            operation = AuditOperation.UPDATE,
            team = featureFlag.team,
            oldValues = objectMapper.writeValueAsString(oldValuesMap),
            newValues = objectMapper.writeValueAsString(newValuesMap),
            changedBy = changedBy
        )

        auditLogRepository.save(auditLog)
    }

    private fun FeatureFlagAuditLog.toDto(): AuditLogDto {
        return AuditLogDto(
            id = this.id,
            featureFlagId = this.featureFlagId,
            featureFlagName = this.featureFlagName,
            operation = this.operation,
            team = this.team,
            oldValues = this.oldValues?.let {
                objectMapper.readValue(
                    it,
                    object : com.fasterxml.jackson.core.type.TypeReference<Map<String, Any?>>() {})
            },
            newValues = this.newValues?.let {
                objectMapper.readValue(
                    it,
                    object : com.fasterxml.jackson.core.type.TypeReference<Map<String, Any?>>() {})
            },
            changedBy = this.changedBy,
            timestamp = this.timestamp
        )
    }
}
