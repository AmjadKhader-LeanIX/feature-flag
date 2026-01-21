package com.featureflag.repository

import com.featureflag.entity.FeatureFlag
import com.featureflag.entity.WorkspaceFeatureFlag
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface WorkspaceFeatureFlagRepository : JpaRepository<WorkspaceFeatureFlag, UUID> {
    @Query("SELECT wff FROM WorkspaceFeatureFlag wff WHERE wff.workspace.id = :workspaceId AND wff.isEnabled = true")
    fun findEnabledByWorkspaceIdWithFeatureFlagPaginated(@Param("workspaceId") workspaceId: UUID, pageable: Pageable): Page<WorkspaceFeatureFlag>

    @Query("SELECT wff FROM WorkspaceFeatureFlag wff WHERE wff.workspace.id = :workspaceId AND wff.isEnabled = true AND (LOWER(wff.featureFlag.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR LOWER(wff.featureFlag.description) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR LOWER(wff.featureFlag.team) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    fun findEnabledByWorkspaceIdWithFeatureFlagAndSearch(@Param("workspaceId") workspaceId: UUID, @Param("searchTerm") searchTerm: String, pageable: Pageable): Page<WorkspaceFeatureFlag>

    fun findByFeatureFlag(featureFlag: FeatureFlag): List<WorkspaceFeatureFlag>
    fun findByFeatureFlagIdAndWorkspaceIdIn(featureFlagId: UUID, workspaceIds: List<UUID>): List<WorkspaceFeatureFlag>

    // Count enabled workspaces for a feature flag
    @Query("SELECT COUNT(wff) FROM WorkspaceFeatureFlag wff WHERE wff.featureFlag = :featureFlag AND wff.isEnabled = true")
    fun countEnabledByFeatureFlag(@Param("featureFlag") featureFlag: FeatureFlag): Long

    // Paginated query for enabled workspaces by feature flag
    @Query("SELECT wff FROM WorkspaceFeatureFlag wff WHERE wff.featureFlag.id = :featureFlagId AND wff.isEnabled = true")
    fun findEnabledByFeatureFlagId(@Param("featureFlagId") featureFlagId: UUID, pageable: Pageable): Page<WorkspaceFeatureFlag>

    // Search enabled workspaces by feature flag with name or region filter
    @Query("SELECT wff FROM WorkspaceFeatureFlag wff WHERE wff.featureFlag.id = :featureFlagId AND wff.isEnabled = true AND (LOWER(wff.workspace.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR LOWER(CAST(wff.workspace.region AS string)) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    fun searchEnabledByFeatureFlagId(@Param("featureFlagId") featureFlagId: UUID, @Param("searchTerm") searchTerm: String, pageable: Pageable): Page<WorkspaceFeatureFlag>

    // Count enabled workspaces by region for a feature flag
    @Query("SELECT w.region as region, COUNT(w) as count FROM WorkspaceFeatureFlag wff JOIN wff.workspace w WHERE wff.featureFlag.id = :featureFlagId AND wff.isEnabled = true AND w.region IS NOT NULL GROUP BY w.region")
    fun countEnabledWorkspacesByRegion(@Param("featureFlagId") featureFlagId: UUID): List<RegionCount>

    interface RegionCount {
        fun getRegion(): String
        fun getCount(): Long
    }
}
