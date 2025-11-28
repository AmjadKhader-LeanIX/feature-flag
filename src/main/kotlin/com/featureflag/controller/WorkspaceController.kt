package com.featureflag.controller

import com.featureflag.dto.UpdateWorkspaceRequest
import com.featureflag.dto.WorkspaceDto
import com.featureflag.service.WorkspaceService
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
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
}
