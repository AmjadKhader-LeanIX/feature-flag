package com.featureflag.dto

import com.featureflag.entity.AuditOperation
import java.time.LocalDateTime
import java.util.*

data class AuditLogDto(
    val id: UUID?,
    val featureFlagId: UUID?,
    val featureFlagName: String,
    val operation: AuditOperation,
    val team: String,
    val oldValues: Map<String, Any?>?,
    val newValues: Map<String, Any?>?,
    val changedBy: String?,
    val timestamp: LocalDateTime
)
