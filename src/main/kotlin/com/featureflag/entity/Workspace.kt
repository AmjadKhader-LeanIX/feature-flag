package com.featureflag.entity

import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.LocalDateTime
import java.util.*

@Entity
@Table(name = "workspaces")
data class Workspace(
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    val id: UUID? = null,

    @Column(name = "name", nullable = false)
    val name: String,

    @Column(name = "type")
    val type: String?,

    @Enumerated(EnumType.STRING)
    @Column(name = "region")
    val region: Region?,

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime? = null,

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    val updatedAt: LocalDateTime? = null,

    @OneToMany(mappedBy = "workspace", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    val workspaceFeatureFlags: List<WorkspaceFeatureFlag> = emptyList(),
)
