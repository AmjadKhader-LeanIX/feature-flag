package com.featureflag.repository

import com.featureflag.entity.Workspace
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface WorkspaceRepository : JpaRepository<Workspace, UUID> {
    fun findByRegionIn(region: List<String>): List<Workspace>
}
