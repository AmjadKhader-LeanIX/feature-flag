package com.featureflag.controller

import com.featureflag.dto.CreateFeatureFlagRequest
import com.featureflag.dto.FeatureFlagDto
import com.featureflag.dto.FeatureFlagEvaluationResponse
import com.featureflag.dto.UpdateFeatureFlagRequest
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

    @GetMapping("/{id}/check")
    fun evaluateFeatureFlag(
        @PathVariable id: UUID,
        @RequestParam customerId: UUID
    ): ResponseEntity<FeatureFlagEvaluationResponse> {
        val evaluation = featureFlagService.evaluateFeatureFlag(id, customerId)
        return ResponseEntity.ok(evaluation)
    }
}
