package com.featureflag.controller

import com.featureflag.dto.PageableResponse
import com.featureflag.dto.WorkspaceDto
import com.featureflag.service.WorkspaceService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.*

@RestController
@RequestMapping("/api/workspaces")
class WorkspaceController(
    private val workspaceService: WorkspaceService
) {

    @GetMapping
    fun getAllWorkspaces(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(defaultValue = "false") paginated: Boolean,
        @RequestParam(required = false) search: String?
    ): ResponseEntity<*> {
        return if (paginated) {
            val paginatedWorkspaces = workspaceService.getAllWorkspacesPaginated(page, size, search)
            ResponseEntity.ok(paginatedWorkspaces)
        } else {
            // Backward compatibility: return all workspaces when paginated=false
            val workspaces = workspaceService.getAllWorkspaces()
            ResponseEntity.ok(workspaces)
        }
    }

    @GetMapping("/{id}")
    fun getWorkspaceById(@PathVariable id: UUID): ResponseEntity<WorkspaceDto> {
        val workspace = workspaceService.getWorkspaceById(id)
        return ResponseEntity.ok(workspace)
    }

    @GetMapping("/{id}/enabled-feature-flags")
    fun getEnabledFeatureFlags(
        @PathVariable id: UUID,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "100") size: Int,
        @RequestParam(defaultValue = "false") paginated: Boolean,
        @RequestParam(required = false) search: String?
    ): ResponseEntity<*> {
        return if (paginated) {
            val paginatedFeatureFlags = workspaceService.getEnabledFeatureFlagsForWorkspacePaginated(id, page, size, search)
            ResponseEntity.ok(paginatedFeatureFlags)
        } else {
            // Backward compatibility
            val featureFlags = workspaceService.getEnabledFeatureFlagsForWorkspace(id)
            ResponseEntity.ok(featureFlags)
        }
    }
}
