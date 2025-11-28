package com.featureflag.service

import com.featureflag.dto.CreateFeatureFlagRequest
import com.featureflag.dto.UpdateFeatureFlagRequest
import com.featureflag.entity.FeatureFlag
import com.featureflag.entity.Workspace
import com.featureflag.entity.WorkspaceFeatureFlag
import com.featureflag.exception.ResourceNotFoundException
import com.featureflag.repository.FeatureFlagRepository
import com.featureflag.repository.WorkspaceFeatureFlagRepository
import com.featureflag.repository.WorkspaceRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.time.LocalDateTime
import java.util.*

class FeatureFlagServiceTest {

    private lateinit var featureFlagRepository: FeatureFlagRepository
    private lateinit var workspaceRepository: WorkspaceRepository
    private lateinit var workspaceFeatureFlagRepository: WorkspaceFeatureFlagRepository
    private lateinit var featureFlagService: FeatureFlagService

    @BeforeEach
    fun setUp() {
        featureFlagRepository = mockk()
        workspaceRepository = mockk()
        workspaceFeatureFlagRepository = mockk()
        featureFlagService = FeatureFlagService(
            featureFlagRepository,
            workspaceRepository,
            workspaceFeatureFlagRepository
        )
    }

    @Test
    fun `should get all feature flags`() {
        val featureFlag1 = createMockFeatureFlag("flag1", "team1")
        val featureFlag2 = createMockFeatureFlag("flag2", "team2")
        every { featureFlagRepository.findAll() } returns listOf(featureFlag1, featureFlag2)

        val result = featureFlagService.getAllFeatureFlags()

        assertEquals(2, result.size)
        assertEquals("flag1", result[0].name)
        assertEquals("flag2", result[1].name)
        verify { featureFlagRepository.findAll() }
    }

    @Test
    fun `should get feature flag by id`() {
        val flagId = UUID.randomUUID()
        val featureFlag = createMockFeatureFlag("test-flag", "test-team", flagId)
        every { featureFlagRepository.findById(flagId) } returns Optional.of(featureFlag)

        val result = featureFlagService.getFeatureFlagById(flagId)

        assertEquals(flagId, result.id)
        assertEquals("test-flag", result.name)
        assertEquals("test-team", result.team)
        verify { featureFlagRepository.findById(flagId) }
    }

    @Test
    fun `should throw exception when feature flag not found by id`() {
        val flagId = UUID.randomUUID()
        every { featureFlagRepository.findById(flagId) } returns Optional.empty()

        assertThrows<ResourceNotFoundException> {
            featureFlagService.getFeatureFlagById(flagId)
        }
        verify { featureFlagRepository.findById(flagId) }
    }

    @Test
    fun `should get feature flags by team`() {
        val team = "team1"
        val featureFlag = createMockFeatureFlag("flag1", team)
        every { featureFlagRepository.findByTeam(team) } returns listOf(featureFlag)

        val result = featureFlagService.getFeatureFlagsByTeam(team)

        assertEquals(1, result.size)
        assertEquals("flag1", result[0].name)
        assertEquals(team, result[0].team)
        verify { featureFlagRepository.findByTeam(team) }
    }

    @Test
    fun `should get feature flags by workspace`() {
        val workspaceId = UUID.randomUUID()
        val workspace = createMockWorkspace(workspaceId)
        val featureFlag = createMockFeatureFlag("flag1", "team1")
        val workspaceFeatureFlag = WorkspaceFeatureFlag(
            id = UUID.randomUUID(),
            workspace = workspace,
            featureFlag = featureFlag,
            isEnabled = true
        )

        every { workspaceRepository.findById(workspaceId) } returns Optional.of(workspace)
        every { workspaceFeatureFlagRepository.findByWorkspaceId(workspaceId) } returns listOf(workspaceFeatureFlag)

        val result = featureFlagService.getFeatureFlagsByWorkspace(workspaceId)

        assertEquals(1, result.size)
        assertEquals("flag1", result[0].name)
        verify { workspaceRepository.findById(workspaceId) }
        verify { workspaceFeatureFlagRepository.findByWorkspaceId(workspaceId) }
    }

    @Test
    fun `should throw exception when workspace not found for get feature flags by workspace`() {
        val workspaceId = UUID.randomUUID()
        every { workspaceRepository.findById(workspaceId) } returns Optional.empty()

        assertThrows<ResourceNotFoundException> {
            featureFlagService.getFeatureFlagsByWorkspace(workspaceId)
        }
        verify { workspaceRepository.findById(workspaceId) }
    }

    @Test
    fun `should create feature flag successfully`() {
        val request = CreateFeatureFlagRequest("new-flag", "description", "team1", 50)
        val savedFeatureFlag = createMockFeatureFlag("new-flag", "team1")

        every { featureFlagRepository.existsByTeamAndName("team1", "new-flag") } returns false
        every { featureFlagRepository.save(any()) } returns savedFeatureFlag
        every { workspaceFeatureFlagRepository.findByFeatureFlag(savedFeatureFlag) } returns emptyList()

        val result = featureFlagService.createFeatureFlag(request)

        assertEquals("new-flag", result.name)
        assertEquals("team1", result.team)
        assertEquals(50, result.rolloutPercentage)
        verify { featureFlagRepository.existsByTeamAndName("team1", "new-flag") }
        verify { featureFlagRepository.save(any()) }
    }

    @Test
    fun `should throw exception when creating feature flag with existing name in same team`() {
        val request = CreateFeatureFlagRequest("existing-flag", "description", "team1", 50)
        every { featureFlagRepository.existsByTeamAndName("team1", "existing-flag") } returns true

        assertThrows<IllegalArgumentException> {
            featureFlagService.createFeatureFlag(request)
        }
        verify { featureFlagRepository.existsByTeamAndName("team1", "existing-flag") }
    }

    @Test
    fun `should update feature flag successfully`() {
        val flagId = UUID.randomUUID()
        val existingFlag = createMockFeatureFlag("old-flag", "team1", flagId)
        val request = UpdateFeatureFlagRequest("new-flag", "new description", "team1", 75)
        val updatedFlag = createMockFeatureFlag("new-flag", "team1", flagId).copy(rolloutPercentage = 75)

        every { featureFlagRepository.findById(flagId) } returns Optional.of(existingFlag)
        every { featureFlagRepository.existsByTeamAndName("team1", "new-flag") } returns false
        every { featureFlagRepository.save(any()) } returns updatedFlag
        every { workspaceFeatureFlagRepository.findByFeatureFlag(any()) } returns emptyList()
        every { workspaceFeatureFlagRepository.saveAll(any<List<WorkspaceFeatureFlag>>()) } returns emptyList()

        val result = featureFlagService.updateFeatureFlag(flagId, request)

        assertEquals("new-flag", result.name)
        assertEquals(75, result.rolloutPercentage)
        verify { featureFlagRepository.findById(flagId) }
        verify { featureFlagRepository.save(any()) }
    }

    @Test
    fun `should throw exception when updating to existing name in same team`() {
        val flagId = UUID.randomUUID()
        val existingFlag = createMockFeatureFlag("old-flag", "team1", flagId)
        val request = UpdateFeatureFlagRequest("existing-flag", "description", "team1", 75)

        every { featureFlagRepository.findById(flagId) } returns Optional.of(existingFlag)
        every { featureFlagRepository.existsByTeamAndName("team1", "existing-flag") } returns true

        assertThrows<IllegalArgumentException> {
            featureFlagService.updateFeatureFlag(flagId, request)
        }
        verify { featureFlagRepository.findById(flagId) }
        verify { featureFlagRepository.existsByTeamAndName("team1", "existing-flag") }
    }

    @Test
    fun `should allow updating feature flag with same name`() {
        val flagId = UUID.randomUUID()
        val existingFlag = createMockFeatureFlag("same-flag", "team1", flagId)
        val request = UpdateFeatureFlagRequest("same-flag", "updated description", "team1", 80)
        val updatedFlag = createMockFeatureFlag("same-flag", "team1", flagId).copy(rolloutPercentage = 80)

        every { featureFlagRepository.findById(flagId) } returns Optional.of(existingFlag)
        every { featureFlagRepository.existsByTeamAndName("team1", "same-flag") } returns true
        every { featureFlagRepository.save(any()) } returns updatedFlag
        every { workspaceFeatureFlagRepository.findByFeatureFlag(any()) } returns emptyList()
        every { workspaceFeatureFlagRepository.saveAll(any<List<WorkspaceFeatureFlag>>()) } returns emptyList()

        val result = featureFlagService.updateFeatureFlag(flagId, request)

        assertEquals("same-flag", result.name)
        assertEquals(80, result.rolloutPercentage)
        verify { featureFlagRepository.findById(flagId) }
        verify { featureFlagRepository.save(any()) }
    }

    @Test
    fun `should throw exception when updating non-existent feature flag`() {
        val flagId = UUID.randomUUID()
        val request = UpdateFeatureFlagRequest("flag", "description", "team1", 50)
        every { featureFlagRepository.findById(flagId) } returns Optional.empty()

        assertThrows<ResourceNotFoundException> {
            featureFlagService.updateFeatureFlag(flagId, request)
        }
        verify { featureFlagRepository.findById(flagId) }
    }

    @Test
    fun `should delete feature flag successfully`() {
        val flagId = UUID.randomUUID()
        every { featureFlagRepository.existsById(flagId) } returns true
        every { featureFlagRepository.deleteById(flagId) } returns Unit

        featureFlagService.deleteFeatureFlag(flagId)

        verify { featureFlagRepository.existsById(flagId) }
        verify { featureFlagRepository.deleteById(flagId) }
    }

    @Test
    fun `should throw exception when deleting non-existent feature flag`() {
        val flagId = UUID.randomUUID()
        every { featureFlagRepository.existsById(flagId) } returns false

        assertThrows<ResourceNotFoundException> {
            featureFlagService.deleteFeatureFlag(flagId)
        }
        verify { featureFlagRepository.existsById(flagId) }
    }

    @Test
    fun `should search feature flags by name`() {
        val searchTerm = "test"
        val featureFlag = createMockFeatureFlag("test-flag", "team1")
        every { featureFlagRepository.findByNameContainingIgnoreCase(searchTerm) } returns listOf(featureFlag)

        val result = featureFlagService.searchFeatureFlags(searchTerm)

        assertEquals(1, result.size)
        assertEquals("test-flag", result[0].name)
        verify { featureFlagRepository.findByNameContainingIgnoreCase(searchTerm) }
    }

    @ParameterizedTest
    @ValueSource(ints = [0, 10, 35, 70, 100])
    fun `should handle rollout percentage boundary values`(rolloutPercentage: Int) {
        if (rolloutPercentage == 0) {
            val request = CreateFeatureFlagRequest("zero-flag", "description", "team1", 0)
            val savedFeatureFlag = createMockFeatureFlag("zero-flag", "team1").copy(rolloutPercentage = 0)

            every { featureFlagRepository.existsByTeamAndName("team1", "zero-flag") } returns false
            every { featureFlagRepository.save(any()) } returns savedFeatureFlag
            every { workspaceFeatureFlagRepository.findByFeatureFlag(savedFeatureFlag) } returns emptyList()

            val result = featureFlagService.createFeatureFlag(request)

            assertEquals(0, result.rolloutPercentage)
            verify { featureFlagRepository.save(any()) }
        } else {
            val flagId = UUID.randomUUID()
            val existingFlag = createMockFeatureFlag("flag", "team1", flagId)
            val request = UpdateFeatureFlagRequest("flag", "description", "team1", 100)
            val updatedFlag = createMockFeatureFlag("flag", "team1", flagId).copy(rolloutPercentage = 100)
            val workspaceFeatureFlag = WorkspaceFeatureFlag(
                id = UUID.randomUUID(),
                workspace = createMockWorkspace(),
                featureFlag = existingFlag,
                isEnabled = false
            )

            every { featureFlagRepository.findById(flagId) } returns Optional.of(existingFlag)
            every { featureFlagRepository.existsByTeamAndName("team1", "flag") } returns true
            every { featureFlagRepository.save(any()) } returns updatedFlag
            every { workspaceFeatureFlagRepository.findByFeatureFlag(any()) } returns listOf(workspaceFeatureFlag)
            every { workspaceFeatureFlagRepository.saveAll(any<List<WorkspaceFeatureFlag>>()) } returns emptyList()

            val result = featureFlagService.updateFeatureFlag(flagId, request)

            assertEquals(100, result.rolloutPercentage)
            verify { workspaceFeatureFlagRepository.saveAll(any<List<WorkspaceFeatureFlag>>()) }
        }
    }

    private fun createMockFeatureFlag(name: String, team: String, id: UUID = UUID.randomUUID()): FeatureFlag {
        return FeatureFlag(
            id = id,
            name = name,
            description = "Test description",
            team = team,
            rolloutPercentage = 50,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
    }

    private fun createMockWorkspace(id: UUID = UUID.randomUUID()): Workspace {
        return Workspace(
            id = id,
            name = "Test Workspace",
            type = "test",
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
    }
}
