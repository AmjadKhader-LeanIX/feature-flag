package com.featureflag.controller

import com.featureflag.dto.CreateFeatureFlagRequest
import com.featureflag.dto.FeatureFlagDto
import com.featureflag.dto.UpdateFeatureFlagRequest
import com.featureflag.dto.UpdateWorkspaceFeatureFlagRequest
import com.featureflag.service.FeatureFlagService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/api/feature-flags")
class FeatureFlagController(
    private val featureFlagService: FeatureFlagService
) {

    @GetMapping
    fun getAllFeatureFlags(): ResponseEntity<List<FeatureFlagDto>> {
        val featureFlags = featureFlagService.getAllFeatureFlags()
        return ResponseEntity.ok(featureFlags)
    }

    @GetMapping("/{id}")
    fun getFeatureFlagById(@PathVariable id: UUID): ResponseEntity<FeatureFlagDto> {
        val featureFlag = featureFlagService.getFeatureFlagById(id)
        return ResponseEntity.ok(featureFlag)
    }

    @GetMapping("/workspace/{workspaceId}")
    fun getFeatureFlagsByWorkspace(@PathVariable workspaceId: UUID): ResponseEntity<List<FeatureFlagDto>> {
        val featureFlags = featureFlagService.getFeatureFlagsByWorkspace(workspaceId)
        return ResponseEntity.ok(featureFlags)
    }

    @GetMapping("/team/{team}")
    fun getFeatureFlagsByTeam(@PathVariable team: String): ResponseEntity<List<FeatureFlagDto>> {
        val featureFlags = featureFlagService.getFeatureFlagsByTeam(team)
        return ResponseEntity.ok(featureFlags)
    }

    @PostMapping
    fun createFeatureFlag(@Valid @RequestBody request: CreateFeatureFlagRequest): ResponseEntity<FeatureFlagDto> {
        val featureFlag = featureFlagService.createFeatureFlag(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(featureFlag)
    }

    @PutMapping("/{id}")
    fun updateFeatureFlag(
        @PathVariable id: UUID,
        @Valid @RequestBody request: UpdateFeatureFlagRequest
    ): ResponseEntity<FeatureFlagDto> {
        val featureFlag = featureFlagService.updateFeatureFlag(id, request)
        return ResponseEntity.ok(featureFlag)
    }

    @DeleteMapping("/{id}")
    fun deleteFeatureFlag(@PathVariable id: UUID): ResponseEntity<Void> {
        featureFlagService.deleteFeatureFlag(id)
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/search")
    fun searchFeatureFlags(@RequestParam name: String): ResponseEntity<List<FeatureFlagDto>> {
        val featureFlags = featureFlagService.searchFeatureFlags(name)
        return ResponseEntity.ok(featureFlags)
    }

    @PutMapping("/{id}/workspaces")
    fun updateWorkspaceFeatureFlags(
        @PathVariable id: UUID,
        @Valid @RequestBody request: UpdateWorkspaceFeatureFlagRequest
    ): ResponseEntity<Void> {
        featureFlagService.updateWorkspaceFeatureFlags(id, request)
        return ResponseEntity.ok().build()
    }

    @GetMapping("/{id}/enabled-workspaces")
    fun getEnabledWorkspaces(
        @PathVariable id: UUID,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "100") size: Int,
        @RequestParam(defaultValue = "false") paginated: Boolean,
        @RequestParam(required = false) search: String?
    ): ResponseEntity<*> {
        return if (paginated) {
            val paginatedWorkspaces = featureFlagService.getEnabledWorkspacesForFeatureFlagPaginated(id, page, size, search)
            ResponseEntity.ok(paginatedWorkspaces)
        } else {
            // Backward compatibility
            val workspaces = featureFlagService.getEnabledWorkspacesForFeatureFlag(id)
            ResponseEntity.ok(workspaces)
        }
    }
}
