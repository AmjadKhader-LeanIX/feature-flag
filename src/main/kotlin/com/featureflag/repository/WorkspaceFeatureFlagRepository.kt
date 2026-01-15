package com.featureflag.repository

import com.featureflag.entity.FeatureFlag
import com.featureflag.entity.WorkspaceFeatureFlag
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface WorkspaceFeatureFlagRepository : JpaRepository<WorkspaceFeatureFlag, UUID> {
    fun findByWorkspaceId(workspaceId: UUID): List<WorkspaceFeatureFlag>
    fun findByFeatureFlag(featureFlag: FeatureFlag): List<WorkspaceFeatureFlag>
    fun findByFeatureFlagIdAndWorkspaceIdIn(featureFlagId: UUID, workspaceIds: List<UUID>): List<WorkspaceFeatureFlag>
}
