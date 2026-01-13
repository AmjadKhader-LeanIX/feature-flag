package com.featureflag.entity

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.*

@Entity
@Table(name = "feature_flag_audit_log")
data class FeatureFlagAuditLog(
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    val id: UUID? = null,

    @Column(name = "feature_flag_id")
    val featureFlagId: UUID?,

    @Column(name = "feature_flag_name", nullable = false)
    val featureFlagName: String,

    @Column(name = "operation", nullable = false)
    @Enumerated(EnumType.STRING)
    val operation: AuditOperation,

    @Column(name = "team", nullable = false)
    val team: String,

    @Column(name = "old_values", columnDefinition = "TEXT")
    val oldValues: String?,

    @Column(name = "new_values", columnDefinition = "TEXT")
    val newValues: String?,

    @Column(name = "changed_by")
    val changedBy: String?,

    @Column(name = "timestamp", nullable = false)
    val timestamp: LocalDateTime = LocalDateTime.now()
)

enum class AuditOperation {
    CREATE,
    UPDATE,
    DELETE
}
