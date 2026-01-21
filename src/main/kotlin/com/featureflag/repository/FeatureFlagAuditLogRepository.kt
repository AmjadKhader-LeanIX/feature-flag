package com.featureflag.repository

import com.featureflag.entity.AuditOperation
import com.featureflag.entity.FeatureFlagAuditLog
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface FeatureFlagAuditLogRepository : JpaRepository<FeatureFlagAuditLog, UUID> {
    fun findByFeatureFlagIdOrderByTimestampDesc(featureFlagId: UUID): List<FeatureFlagAuditLog>
    fun findByTeamOrderByTimestampDesc(team: String): List<FeatureFlagAuditLog>
    fun findByOperationOrderByTimestampDesc(operation: AuditOperation): List<FeatureFlagAuditLog>
    fun findAllByOrderByTimestampDesc(): List<FeatureFlagAuditLog>
    fun findByFeatureFlagIdAndOperationOrderByTimestampDesc(
        featureFlagId: UUID,
        operation: AuditOperation
    ): List<FeatureFlagAuditLog>

    fun findByFeatureFlagNameContainingIgnoreCaseOrTeamContainingIgnoreCase(
        featureFlagName: String,
        team: String,
        pageable: Pageable
    ): Page<FeatureFlagAuditLog>
}
