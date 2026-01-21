package com.featureflag.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.featureflag.dto.UpdateWorkspaceFeatureFlagRequest
import com.featureflag.exception.ResourceNotFoundException
import com.featureflag.service.FeatureFlagService
import org.junit.jupiter.api.Test
import org.mockito.Mockito.doNothing
import org.mockito.Mockito.doThrow
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.*

@WebMvcTest(FeatureFlagController::class)
class FeatureFlagControllerWorkspaceTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockBean
    private lateinit var featureFlagService: FeatureFlagService

    @Test
    fun `should enable feature flag for specific workspaces`() {
        val featureFlagId = UUID.randomUUID()
        val workspaceIds = listOf(UUID.randomUUID(), UUID.randomUUID())
        val request = UpdateWorkspaceFeatureFlagRequest(
            workspaceIds = workspaceIds,
            enabled = true
        )

        doNothing().`when`(featureFlagService).updateWorkspaceFeatureFlags(featureFlagId, request)

        mockMvc.perform(
            put("/api/feature-flags/$featureFlagId/workspaces")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
    }

    @Test
    fun `should disable feature flag for specific workspaces`() {
        val featureFlagId = UUID.randomUUID()
        val workspaceIds = listOf(UUID.randomUUID())
        val request = UpdateWorkspaceFeatureFlagRequest(
            workspaceIds = workspaceIds,
            enabled = false
        )

        doNothing().`when`(featureFlagService).updateWorkspaceFeatureFlags(featureFlagId, request)

        mockMvc.perform(
            put("/api/feature-flags/$featureFlagId/workspaces")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
    }

    @Test
    fun `should return 404 when feature flag not found`() {
        val featureFlagId = UUID.randomUUID()
        val workspaceIds = listOf(UUID.randomUUID())
        val request = UpdateWorkspaceFeatureFlagRequest(
            workspaceIds = workspaceIds,
            enabled = true
        )

        doThrow(ResourceNotFoundException("Feature flag not found with id: $featureFlagId"))
            .`when`(featureFlagService).updateWorkspaceFeatureFlags(featureFlagId, request)

        mockMvc.perform(
            put("/api/feature-flags/$featureFlagId/workspaces")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isNotFound)
    }

    @Test
    fun `should return 404 when workspace not found`() {
        val featureFlagId = UUID.randomUUID()
        val workspaceIds = listOf(UUID.randomUUID())
        val request = UpdateWorkspaceFeatureFlagRequest(
            workspaceIds = workspaceIds,
            enabled = true
        )

        doThrow(ResourceNotFoundException("Workspaces not found with ids: $workspaceIds"))
            .`when`(featureFlagService).updateWorkspaceFeatureFlags(featureFlagId, request)

        mockMvc.perform(
            put("/api/feature-flags/$featureFlagId/workspaces")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isNotFound)
    }

    // Test removed: Empty workspace list is now valid in the API
    // @Test
    // fun `should return 400 when workspace list is empty`() { ... }

    @Test
    fun `should return 400 when no workspace-feature flag associations exist`() {
        val featureFlagId = UUID.randomUUID()
        val workspaceIds = listOf(UUID.randomUUID())
        val request = UpdateWorkspaceFeatureFlagRequest(
            workspaceIds = workspaceIds,
            enabled = true
        )

        doThrow(IllegalArgumentException("No workspace-feature flag associations found"))
            .`when`(featureFlagService).updateWorkspaceFeatureFlags(featureFlagId, request)

        mockMvc.perform(
            put("/api/feature-flags/$featureFlagId/workspaces")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isBadRequest)
    }
}
