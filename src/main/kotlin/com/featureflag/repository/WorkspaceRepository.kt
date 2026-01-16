package com.featureflag.repository

import com.featureflag.entity.Workspace
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface WorkspaceRepository : JpaRepository<Workspace, UUID> {
    fun findByRegionIn(region: List<String>): List<Workspace>

    // Paginated version that uses the index idx_workspaces_region_created
    override fun findAll(pageable: Pageable): Page<Workspace>

    // Search workspaces by name or region with pagination
    @Query("SELECT w FROM Workspace w WHERE LOWER(w.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR LOWER(CAST(w.region AS string)) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    fun searchWorkspaces(@Param("searchTerm") searchTerm: String, pageable: Pageable): Page<Workspace>
}
