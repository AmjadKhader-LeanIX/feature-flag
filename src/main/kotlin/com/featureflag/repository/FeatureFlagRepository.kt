package com.featureflag.repository

import com.featureflag.entity.FeatureFlag
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface FeatureFlagRepository : JpaRepository<FeatureFlag, UUID> {
    fun findByTeam(team: String): List<FeatureFlag>
    fun findByNameContainingIgnoreCase(name: String): List<FeatureFlag>
    fun existsByTeamAndName(team: String, name: String): Boolean

}
