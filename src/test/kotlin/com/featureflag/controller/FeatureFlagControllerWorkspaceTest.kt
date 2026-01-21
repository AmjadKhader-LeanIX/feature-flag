package com.featureflag.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.featureflag.dto.RegionWorkspaceCountDto
import com.featureflag.dto.UpdateWorkspaceFeatureFlagRequest
import com.featureflag.exception.ResourceNotFoundException
import com.featureflag.service.FeatureFlagService
import org.junit.jupiter.api.Test
import org.mockito.Mockito.doNothing
import org.mockito.Mockito.doThrow
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
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

    @Test
    fun `should return workspace counts by region with enabled and total counts`() {
        val featureFlagId = UUID.randomUUID()
        val regionCounts = listOf(
            RegionWorkspaceCountDto("WESTEUROPE", 100, 150),
            RegionWorkspaceCountDto("EASTUS", 50, 75),
            RegionWorkspaceCountDto("CANADACENTRAL", 25, 30)
        )

        `when`(featureFlagService.getWorkspaceCountsByRegion(featureFlagId)).thenReturn(regionCounts)

        mockMvc.perform(
            get("/api/feature-flags/$featureFlagId/workspace-counts-by-region")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].region").value("WESTEUROPE"))
            .andExpect(jsonPath("$[0].enabledCount").value(100))
            .andExpect(jsonPath("$[0].totalCount").value(150))
            .andExpect(jsonPath("$[1].region").value("EASTUS"))
            .andExpect(jsonPath("$[1].enabledCount").value(50))
            .andExpect(jsonPath("$[1].totalCount").value(75))
            .andExpect(jsonPath("$[2].region").value("CANADACENTRAL"))
            .andExpect(jsonPath("$[2].enabledCount").value(25))
            .andExpect(jsonPath("$[2].totalCount").value(30))
    }

    @Test
    fun `should return empty list when no regions have workspaces`() {
        val featureFlagId = UUID.randomUUID()

        `when`(featureFlagService.getWorkspaceCountsByRegion(featureFlagId)).thenReturn(emptyList())

        mockMvc.perform(
            get("/api/feature-flags/$featureFlagId/workspace-counts-by-region")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$").isEmpty)
    }

    @Test
    fun `should return 404 when feature flag not found for region counts`() {
        val featureFlagId = UUID.randomUUID()

        `when`(featureFlagService.getWorkspaceCountsByRegion(featureFlagId))
            .thenThrow(ResourceNotFoundException("Feature flag not found with id: $featureFlagId"))

        mockMvc.perform(
            get("/api/feature-flags/$featureFlagId/workspace-counts-by-region")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isNotFound)
    }
}
