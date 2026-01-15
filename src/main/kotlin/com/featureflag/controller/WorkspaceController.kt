package com.featureflag.controller

import com.featureflag.dto.WorkspaceDto
import com.featureflag.service.WorkspaceService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.*

@RestController
@RequestMapping("/api/workspaces")
class WorkspaceController(
    private val workspaceService: WorkspaceService
) {

    @GetMapping
    fun getAllWorkspaces(): ResponseEntity<List<WorkspaceDto>> {
        val workspaces = workspaceService.getAllWorkspaces()
        return ResponseEntity.ok(workspaces)
    }

    @GetMapping("/{id}")
    fun getWorkspaceById(@PathVariable id: UUID): ResponseEntity<WorkspaceDto> {
        val workspace = workspaceService.getWorkspaceById(id)
        return ResponseEntity.ok(workspace)
    }

    @GetMapping("/{id}/enabled-feature-flags")
    fun getEnabledFeatureFlags(@PathVariable id: UUID): ResponseEntity<List<com.featureflag.dto.FeatureFlagDto>> {
        val featureFlags = workspaceService.getEnabledFeatureFlagsForWorkspace(id)
        return ResponseEntity.ok(featureFlags)
    }
}
