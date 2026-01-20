package com.featureflag.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.featureflag.dto.AuditLogDto
import com.featureflag.entity.AuditOperation
import com.featureflag.entity.FeatureFlag
import com.featureflag.entity.FeatureFlagAuditLog
import com.featureflag.repository.FeatureFlagAuditLogRepository
import org.springframework.stereotype.Service
import java.util.*

@Service
class AuditLogService(
    private val auditLogRepository: FeatureFlagAuditLogRepository,
    private val objectMapper: ObjectMapper
) {

    fun logCreate(featureFlag: FeatureFlag, changedBy: String? = "system") {
        val newValues = mapOf(
            "name" to featureFlag.name,
            "description" to featureFlag.description,
            "team" to featureFlag.team,
            "rolloutPercentage" to featureFlag.rolloutPercentage
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
            "rolloutPercentage" to oldRolloutPercentage,
        )

        val newValues = mapOf(
            "rolloutPercentage" to newRolloutPercentage,
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
            "name" to featureFlag.name,
            "description" to featureFlag.description,
            "team" to featureFlag.team,
            "rolloutPercentage" to featureFlag.rolloutPercentage
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

    fun getAuditLogsByFeatureFlagId(featureFlagId: UUID): List<AuditLogDto> {
        return auditLogRepository.findByFeatureFlagIdOrderByTimestampDesc(featureFlagId).map { it.toDto() }
    }

    fun getAuditLogsByTeam(team: String): List<AuditLogDto> {
        return auditLogRepository.findByTeamOrderByTimestampDesc(team).map { it.toDto() }
    }

    fun getAuditLogsByOperation(operation: AuditOperation): List<AuditLogDto> {
        return auditLogRepository.findByOperationOrderByTimestampDesc(operation).map { it.toDto() }
    }

    fun getAuditLogsByFeatureFlagIdAndOperation(featureFlagId: UUID, operation: AuditOperation): List<AuditLogDto> {
        return auditLogRepository.findByFeatureFlagIdAndOperationOrderByTimestampDesc(featureFlagId, operation)
            .map { it.toDto() }
    }

    fun logWorkspaceUpdate(
        featureFlagId: UUID?,
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
        // Get previous pinned/excluded workspaces from the last audit log entry
        var oldPinnedWorkspaces: List<String> = emptyList()
        var oldExcludedWorkspaces: List<String> = emptyList()
        var oldTargetRegion: String? = null

        if (featureFlagId != null) {
            val lastAuditLog = auditLogRepository.findByFeatureFlagIdOrderByTimestampDesc(featureFlagId)
                .firstOrNull()

            if (lastAuditLog != null && lastAuditLog.newValues != null) {
                try {
                    val previousNewValues = objectMapper.readValue(
                        lastAuditLog.newValues,
                        object : com.fasterxml.jackson.core.type.TypeReference<Map<String, Any?>>() {}
                    )

                    // Extract old pinned workspaces
                    oldPinnedWorkspaces = when (val pinned = previousNewValues["pinnedWorkspaces"]) {
                        is List<*> -> pinned.filterIsInstance<String>()
                        else -> emptyList()
                    }

                    // Extract old excluded workspaces
                    oldExcludedWorkspaces = when (val excluded = previousNewValues["excludedWorkspaces"]) {
                        is List<*> -> excluded.filterIsInstance<String>()
                        else -> emptyList()
                    }

                    // Extract old target region
                    oldTargetRegion = previousNewValues["targetRegion"] as? String
                } catch (e: Exception) {
                    // If we can't parse the previous values, just continue with empty lists
                }
            }
        }

        val oldValuesMap: MutableMap<String, Any> = mutableMapOf(
            "enabledWorkspaces" to oldEnabledCount,
            "rolloutPercentage" to oldRolloutPercentage
        )

        // Add old pinned workspaces if any existed
        if (oldPinnedWorkspaces.isNotEmpty()) {
            oldValuesMap["pinnedWorkspaces"] = oldPinnedWorkspaces
        }

        // Add old excluded workspaces if any existed
        if (oldExcludedWorkspaces.isNotEmpty()) {
            oldValuesMap["excludedWorkspaces"] = oldExcludedWorkspaces
        }

        // Add old target region if it existed
        if (oldTargetRegion != null) {
            oldValuesMap["targetRegion"] = oldTargetRegion
        }

        val newValuesMap: MutableMap<String, Any> = mutableMapOf(
            "enabledWorkspaces" to newEnabledCount,
            "rolloutPercentage" to newRolloutPercentage
        )

        // Add new pinned workspaces if any
        if (newPinnedWorkspaces.isNotEmpty()) {
            newValuesMap["pinnedWorkspaces"] = newPinnedWorkspaces
        }

        // Add new excluded workspaces if any
        if (newExcludedWorkspaces.isNotEmpty()) {
            newValuesMap["excludedWorkspaces"] = newExcludedWorkspaces
        }

        // Add target region if specified
        if (targetRegion != null) {
            newValuesMap["targetRegion"] = targetRegion
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
