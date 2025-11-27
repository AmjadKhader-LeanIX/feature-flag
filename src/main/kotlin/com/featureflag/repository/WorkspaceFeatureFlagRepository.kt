package com.featureflag.repository

import com.featureflag.entity.FeatureFlag
import com.featureflag.entity.Workspace
import com.featureflag.entity.WorkspaceFeatureFlag
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface WorkspaceFeatureFlagRepository : JpaRepository<WorkspaceFeatureFlag, UUID> {
    fun findByWorkspace(workspace: Workspace): List<WorkspaceFeatureFlag>
    fun findByFeatureFlag(featureFlag: FeatureFlag): List<WorkspaceFeatureFlag>
    fun findByWorkspaceAndFeatureFlag(workspace: Workspace, featureFlag: FeatureFlag): Optional<WorkspaceFeatureFlag>
    fun findByWorkspaceAndIsEnabled(workspace: Workspace, isEnabled: Boolean): List<WorkspaceFeatureFlag>
    fun existsByWorkspaceAndFeatureFlag(workspace: Workspace, featureFlag: FeatureFlag): Boolean
    fun deleteByWorkspaceAndFeatureFlag(workspace: Workspace, featureFlag: FeatureFlag)
}


