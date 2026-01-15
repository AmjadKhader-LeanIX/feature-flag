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
            "rolloutPercentage" to featureFlag.rolloutPercentage,
            "regions" to featureFlag.regions
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
            "rolloutPercentage" to featureFlag.rolloutPercentage,
            "regions" to featureFlag.regions
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
        oldEnabledCount: Int,
        newEnabledCount: Int,
        oldRolloutPercentage: Int,
        newRolloutPercentage: Int,
        featureFlag: FeatureFlag,
        changedBy: String? = "system"
    ) {
        val oldValues = mapOf(
            "enabledWorkspaces" to oldEnabledCount,
            "rolloutPercentage" to oldRolloutPercentage
        )

        val newValues = mapOf(
            "enabledWorkspaces" to newEnabledCount,
            "rolloutPercentage" to newRolloutPercentage
        )

        val auditLog = FeatureFlagAuditLog(
            featureFlagId = featureFlag.id,
            featureFlagName = featureFlag.name,
            operation = AuditOperation.UPDATE,
            team = featureFlag.team,
            oldValues = objectMapper.writeValueAsString(oldValues),
            newValues = objectMapper.writeValueAsString(newValues),
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
