package com.featureflag.entity

import jakarta.persistence.*
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.LocalDateTime
import java.util.*

@Entity
@Table(name = "feature_flag")
data class FeatureFlag(
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    val id: UUID? = null,

    @Column(name = "name", nullable = false)
    val name: String,

    @Column(name = "description")
    val description: String?,

    @Column(name = "team", nullable = false)
    val team: String,

    @Column(name = "rollout_percentage", nullable = false)
    @Min(0)
    @Max(100)
    val rolloutPercentage: Int,

    @Enumerated(EnumType.STRING)
    @Column(name = "region", nullable = false)
    val region: Region = Region.ALL,

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime? = null,

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    val updatedAt: LocalDateTime? = null
)
