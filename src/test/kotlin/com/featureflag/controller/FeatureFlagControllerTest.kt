package com.featureflag.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.featureflag.dto.CreateFeatureFlagRequest
import com.featureflag.dto.FeatureFlagDto
import com.featureflag.dto.UpdateFeatureFlagRequest
import com.featureflag.exception.ResourceNotFoundException
import com.featureflag.service.FeatureFlagService
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.time.LocalDateTime
import java.util.*

@WebMvcTest(FeatureFlagController::class)
@ActiveProfiles("test")
class FeatureFlagControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockkBean
    private lateinit var featureFlagService: FeatureFlagService

    @Test
    fun `should get all feature flags`() {
        val featureFlags = listOf(
            createMockFeatureFlagDto("flag1", "team1"),
            createMockFeatureFlagDto("flag2", "team2")
        )
        every { featureFlagService.getAllFeatureFlags() } returns featureFlags

        mockMvc.perform(get("/api/feature-flags"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].name").value("flag1"))
            .andExpect(jsonPath("$[1].name").value("flag2"))

        verify { featureFlagService.getAllFeatureFlags() }
    }

    @Test
    fun `should get feature flag by id`() {
        val flagId = UUID.randomUUID()
        val featureFlag = createMockFeatureFlagDto("test-flag", "test-team", flagId)
        every { featureFlagService.getFeatureFlagById(flagId) } returns featureFlag

        mockMvc.perform(get("/api/feature-flags/$flagId"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.id").value(flagId.toString()))
            .andExpect(jsonPath("$.name").value("test-flag"))
            .andExpect(jsonPath("$.team").value("test-team"))

        verify { featureFlagService.getFeatureFlagById(flagId) }
    }

    @Test
    fun `should return 404 when feature flag not found by id`() {
        val flagId = UUID.randomUUID()
        every { featureFlagService.getFeatureFlagById(flagId) } throws ResourceNotFoundException("Feature flag not found with id: $flagId")

        mockMvc.perform(get("/api/feature-flags/$flagId"))
            .andExpect(status().isNotFound)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.error").value("Not Found"))

        verify { featureFlagService.getFeatureFlagById(flagId) }
    }

    @Test
    fun `should return 404 when workspace not found for feature flags`() {
        val workspaceId = UUID.randomUUID()
        every { featureFlagService.getFeatureFlagsByWorkspace(workspaceId) } throws ResourceNotFoundException("Workspace not found with id: $workspaceId")

        mockMvc.perform(get("/api/feature-flags/workspace/$workspaceId"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.status").value(404))

        verify { featureFlagService.getFeatureFlagsByWorkspace(workspaceId) }
    }

    @Test
    fun `should get feature flags by workspace`() {
        val workspaceId = UUID.randomUUID()
        val featureFlags = listOf(createMockFeatureFlagDto("flag1", "team1"))
        every { featureFlagService.getFeatureFlagsByWorkspace(workspaceId) } returns featureFlags

        mockMvc.perform(get("/api/feature-flags/workspace/$workspaceId"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].name").value("flag1"))

        verify { featureFlagService.getFeatureFlagsByWorkspace(workspaceId) }
    }

    @Test
    fun `should get feature flags by team`() {
        val team = "team1"
        val featureFlags = listOf(createMockFeatureFlagDto("flag1", team))
        every { featureFlagService.getFeatureFlagsByTeam(team) } returns featureFlags

        mockMvc.perform(get("/api/feature-flags/team/$team"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].team").value(team))

        verify { featureFlagService.getFeatureFlagsByTeam(team) }
    }

    @Test
    fun `should create feature flag successfully`() {
        val request = CreateFeatureFlagRequest("new-flag", "description", "team1", 50)
        val createdFlag = createMockFeatureFlagDto("new-flag", "team1")
        every { featureFlagService.createFeatureFlag(request) } returns createdFlag

        mockMvc.perform(
            post("/api/feature-flags")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isCreated)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.name").value("new-flag"))
            .andExpect(jsonPath("$.team").value("team1"))
            .andExpect(jsonPath("$.rolloutPercentage").value(50))

        verify { featureFlagService.createFeatureFlag(request) }
    }

    @Test
    fun `should return 400 when creating feature flag with invalid data`() {
        val invalidRequest = CreateFeatureFlagRequest("", null, "", -1)

        mockMvc.perform(
            post("/api/feature-flags")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest))
        )
            .andExpect(status().isBadRequest)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Validation Failed"))
    }

    @Test
    fun `should return 400 when creating feature flag with existing name in team`() {
        val request = CreateFeatureFlagRequest("existing-flag", "description", "team1", 50)
        every { featureFlagService.createFeatureFlag(request) } throws IllegalArgumentException("Feature flag with name 'existing-flag' already exists in this team")

        mockMvc.perform(
            post("/api/feature-flags")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Bad Request"))

        verify { featureFlagService.createFeatureFlag(request) }
    }

    @Test
    fun `should update feature flag successfully`() {
        val flagId = UUID.randomUUID()
        val request = UpdateFeatureFlagRequest("updated-flag", "new description", "team1", 75)
        val updatedFlag = createMockFeatureFlagDto("updated-flag", "team1", flagId).copy(rolloutPercentage = 75)
        every { featureFlagService.updateFeatureFlag(flagId, request) } returns updatedFlag

        mockMvc.perform(
            put("/api/feature-flags/$flagId")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.name").value("updated-flag"))
            .andExpect(jsonPath("$.rolloutPercentage").value(75))

        verify { featureFlagService.updateFeatureFlag(flagId, request) }
    }

    @Test
    fun `should return 404 when updating non-existent feature flag`() {
        val flagId = UUID.randomUUID()
        val request = UpdateFeatureFlagRequest("flag", "description", "team1", 50)
        every { featureFlagService.updateFeatureFlag(flagId, request) } throws ResourceNotFoundException("Feature flag not found with id: $flagId")

        mockMvc.perform(
            put("/api/feature-flags/$flagId")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.status").value(404))

        verify { featureFlagService.updateFeatureFlag(flagId, request) }
    }

    @Test
    fun `should return 400 when updating feature flag with invalid data`() {
        val flagId = UUID.randomUUID()
        val invalidRequest = UpdateFeatureFlagRequest("", null, "", 150)

        mockMvc.perform(
            put("/api/feature-flags/$flagId")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest))
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Validation Failed"))
    }

    @Test
    fun `should delete feature flag successfully`() {
        val flagId = UUID.randomUUID()
        every { featureFlagService.deleteFeatureFlag(flagId) } returns Unit

        mockMvc.perform(delete("/api/feature-flags/$flagId"))
            .andExpect(status().isNoContent)

        verify { featureFlagService.deleteFeatureFlag(flagId) }
    }

    @Test
    fun `should return 404 when deleting non-existent feature flag`() {
        val flagId = UUID.randomUUID()
        every { featureFlagService.deleteFeatureFlag(flagId) } throws ResourceNotFoundException("Feature flag not found with id: $flagId")

        mockMvc.perform(delete("/api/feature-flags/$flagId"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.status").value(404))

        verify { featureFlagService.deleteFeatureFlag(flagId) }
    }

    @Test
    fun `should search feature flags by name`() {
        val searchTerm = "test"
        val featureFlags = listOf(createMockFeatureFlagDto("test-flag", "team1"))
        every { featureFlagService.searchFeatureFlags(searchTerm) } returns featureFlags

        mockMvc.perform(get("/api/feature-flags/search").param("name", searchTerm))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].name").value("test-flag"))

        verify { featureFlagService.searchFeatureFlags(searchTerm) }
    }

    @Test
    fun `should return empty list when searching with no matches`() {
        val searchTerm = "nonexistent"
        every { featureFlagService.searchFeatureFlags(searchTerm) } returns emptyList()

        mockMvc.perform(get("/api/feature-flags/search").param("name", searchTerm))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.length()").value(0))

        verify { featureFlagService.searchFeatureFlags(searchTerm) }
    }

    @ParameterizedTest
    @ValueSource(ints = [0, 100])
    fun `should handle rollout percentage boundary values`(rolloutPercentage: Int) {
        val request = CreateFeatureFlagRequest("${rolloutPercentage}-flag", "description", "team1", rolloutPercentage)
        val createdFlag = createMockFeatureFlagDto("${rolloutPercentage}-flag", "team1").copy(rolloutPercentage = rolloutPercentage)
        every { featureFlagService.createFeatureFlag(request) } returns createdFlag

        mockMvc.perform(
            post("/api/feature-flags")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.rolloutPercentage").value(rolloutPercentage))
    }

    @Test
    fun `should handle special characters in team name`() {
        val team = "team-with-special_chars"
        val featureFlags = listOf(createMockFeatureFlagDto("flag1", team))
        every { featureFlagService.getFeatureFlagsByTeam(team) } returns featureFlags

        mockMvc.perform(get("/api/feature-flags/team/$team"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].team").value(team))

        verify { featureFlagService.getFeatureFlagsByTeam(team) }
    }

    @Test
    fun `should handle empty list responses`() {
        every { featureFlagService.getAllFeatureFlags() } returns emptyList()

        mockMvc.perform(get("/api/feature-flags"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.length()").value(0))

        verify { featureFlagService.getAllFeatureFlags() }
    }

    private fun createMockFeatureFlagDto(name: String, team: String, id: UUID = UUID.randomUUID()): FeatureFlagDto {
        return FeatureFlagDto(
            id = id,
            name = name,
            description = "Test description",
            team = team,
            rolloutPercentage = 50,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
    }
}
