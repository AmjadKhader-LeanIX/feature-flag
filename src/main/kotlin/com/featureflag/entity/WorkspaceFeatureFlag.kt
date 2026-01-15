package com.featureflag.entity

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.*

@Entity
@Table(name = "workspace_feature_flag")
data class WorkspaceFeatureFlag(
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    val id: UUID? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workspace_id", nullable = false)
    val workspace: Workspace,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "feature_flag_id", nullable = false)
    val featureFlag: FeatureFlag,

    @Column(name = "enabled", nullable = false)
    var isEnabled: Boolean,

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
)
