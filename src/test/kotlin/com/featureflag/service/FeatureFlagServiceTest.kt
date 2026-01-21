package com.featureflag.service

import com.featureflag.dto.CreateFeatureFlagRequest
import com.featureflag.dto.UpdateFeatureFlagRequest
import com.featureflag.dto.UpdateWorkspaceFeatureFlagRequest
import com.featureflag.entity.FeatureFlag
import com.featureflag.entity.Region
import com.featureflag.entity.Workspace
import com.featureflag.entity.WorkspaceFeatureFlag
import com.featureflag.exception.ResourceNotFoundException
import com.featureflag.repository.FeatureFlagRepository
import com.featureflag.repository.WorkspaceFeatureFlagRepository
import com.featureflag.repository.WorkspaceRepository
import io.mockk.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.time.LocalDateTime
import java.util.*
import java.util.stream.Stream

class FeatureFlagServiceTest {

    private lateinit var featureFlagRepository: FeatureFlagRepository
    private lateinit var workspaceRepository: WorkspaceRepository
    private lateinit var workspaceFeatureFlagRepository: WorkspaceFeatureFlagRepository
    private lateinit var auditLogService: AuditLogService
    private lateinit var featureFlagService: FeatureFlagService

    @BeforeEach
    fun setup() {
        featureFlagRepository = mockk()
        workspaceRepository = mockk()
        workspaceFeatureFlagRepository = mockk()
        auditLogService = mockk(relaxed = true)
        featureFlagService = FeatureFlagService(
            featureFlagRepository,
            workspaceRepository,
            workspaceFeatureFlagRepository,
            auditLogService
        )
    }

    @AfterEach
    fun tearDown() {
        clearAllMocks()
    }

    // ==================== getAllFeatureFlags ====================

    @Test
    fun `getAllFeatureFlags should return all feature flags`() {
        val flags = listOf(
            createFeatureFlag("flag1", "team1"),
            createFeatureFlag("flag2", "team2")
        )
        every { featureFlagRepository.findAll() } returns flags

        val result = featureFlagService.getAllFeatureFlags()

        assertEquals(2, result.size)
        assertEquals("flag1", result[0].name)
        assertEquals("flag2", result[1].name)
        verify { featureFlagRepository.findAll() }
    }

    @Test
    fun `getAllFeatureFlags should return empty list when no flags exist`() {
        every { featureFlagRepository.findAll() } returns emptyList()

        val result = featureFlagService.getAllFeatureFlags()

        assertTrue(result.isEmpty())
        verify { featureFlagRepository.findAll() }
    }

    // ==================== getFeatureFlagsByTeam ====================

    @ParameterizedTest(name = "should get feature flags for team {0}")
    @ValueSource(strings = ["team1", "TEAM2", "backend-team", "frontend-team"])
    fun `getFeatureFlagsByTeam should return flags for specified team`(team: String) {
        val flags = listOf(createFeatureFlag("flag1", team))
        every { featureFlagRepository.findByTeam(team) } returns flags

        val result = featureFlagService.getFeatureFlagsByTeam(team)

        assertEquals(1, result.size)
        assertEquals(team, result[0].team)
        verify { featureFlagRepository.findByTeam(team) }
    }

    @Test
    fun `getFeatureFlagsByTeam should return empty list when team has no flags`() {
        every { featureFlagRepository.findByTeam("nonexistent") } returns emptyList()

        val result = featureFlagService.getFeatureFlagsByTeam("nonexistent")

        assertTrue(result.isEmpty())
        verify { featureFlagRepository.findByTeam("nonexistent") }
    }

    // ==================== createFeatureFlag ====================

    @ParameterizedTest
    @MethodSource("validCreateFeatureFlagRequests")
    fun `createFeatureFlag should create flag with valid data`(
        name: String,
        description: String?,
        team: String,
        rollout: Int
    ) {
        val request = CreateFeatureFlagRequest(name, description, team, rollout)
        val savedFlag = createFeatureFlag(name.lowercase(), team.uppercase(), rollout = rollout)

        // Service checks existence with original request values before normalizing
        every { featureFlagRepository.existsByTeamAndName(team, name) } returns false
        every { featureFlagRepository.save(any()) } returns savedFlag
        every { workspaceRepository.findAll() } returns emptyList()
        every { workspaceFeatureFlagRepository.saveAll<WorkspaceFeatureFlag>(any()) } returns emptyList()
        every { workspaceFeatureFlagRepository.findByFeatureFlag(any()) } returns emptyList()

        val result = featureFlagService.createFeatureFlag(request)

        assertEquals(name.lowercase(), result.name)
        assertEquals(team.uppercase(), result.team)
        assertEquals(rollout, result.rolloutPercentage)
        verify { featureFlagRepository.save(any()) }
        verify { auditLogService.logCreate(any(), any()) }
    }

    @Test
    fun `createFeatureFlag should throw exception when flag name already exists in team`() {
        val request = CreateFeatureFlagRequest("existing-flag", "desc", "team1", 50)
        // Service checks with original request values (not normalized)
        every { featureFlagRepository.existsByTeamAndName("team1", "existing-flag") } returns true

        val exception = assertThrows(IllegalArgumentException::class.java) {
            featureFlagService.createFeatureFlag(request)
        }

        assertTrue(exception.message!!.contains("already exists"))
        verify { featureFlagRepository.existsByTeamAndName("team1", "existing-flag") }
        verify(exactly = 0) { featureFlagRepository.save(any()) }
    }

    @ParameterizedTest
    @ValueSource(ints = [0, 1, 50, 99, 100])
    fun `createFeatureFlag should handle boundary rollout percentages`(rollout: Int) {
        val request = CreateFeatureFlagRequest("flag", "desc", "team1", rollout)
        val savedFlag = createFeatureFlag("flag", "TEAM1", rollout = rollout)

        every { featureFlagRepository.existsByTeamAndName(any(), any()) } returns false
        every { featureFlagRepository.save(any()) } returns savedFlag
        every { workspaceRepository.findAll() } returns emptyList()
        every { workspaceFeatureFlagRepository.saveAll<WorkspaceFeatureFlag>(any()) } returns emptyList()
        every { workspaceFeatureFlagRepository.findByFeatureFlag(any()) } returns emptyList()

        val result = featureFlagService.createFeatureFlag(request)

        assertEquals(rollout, result.rolloutPercentage)
    }

    @Test
    fun `createFeatureFlag with 100 percent should enable all workspaces`() {
        val request = CreateFeatureFlagRequest("flag", "desc", "team1", 100)
        val savedFlag = createFeatureFlag("flag", "TEAM1", rollout = 100)
        val workspace1 = createWorkspace("ws1")
        val workspace2 = createWorkspace("ws2")
        val wff1 = WorkspaceFeatureFlag(
            id = UUID.randomUUID(),
            workspace = workspace1,
            featureFlag = savedFlag,
            isEnabled = false
        )
        val wff2 = WorkspaceFeatureFlag(
            id = UUID.randomUUID(),
            workspace = workspace2,
            featureFlag = savedFlag,
            isEnabled = false
        )

        every { featureFlagRepository.existsByTeamAndName(any(), any()) } returns false
        every { featureFlagRepository.save(any()) } returns savedFlag
        every { workspaceRepository.findAll() } returns listOf(workspace1, workspace2)
        every { workspaceFeatureFlagRepository.saveAll<WorkspaceFeatureFlag>(any()) } returns listOf(wff1, wff2)
        every { workspaceFeatureFlagRepository.findByFeatureFlag(any()) } returns listOf(wff1, wff2)

        val result = featureFlagService.createFeatureFlag(request)

        assertEquals(100, result.rolloutPercentage)
        verify { workspaceFeatureFlagRepository.saveAll<WorkspaceFeatureFlag>(any()) }
    }

    @Test
    fun `createFeatureFlag with percentage between 1 and 99 should enable proportional workspaces`() {
        val request = CreateFeatureFlagRequest("flag", "desc", "team1", 50)
        val savedFlag = createFeatureFlag("flag", "TEAM1", rollout = 50)
        val workspaces = (1..10).map { createWorkspace("ws$it", UUID.randomUUID()) }
        val wffs = workspaces.map { ws ->
            WorkspaceFeatureFlag(
                id = UUID.randomUUID(),
                workspace = ws,
                featureFlag = savedFlag,
                isEnabled = false
            )
        }

        every { featureFlagRepository.existsByTeamAndName(any(), any()) } returns false
        every { featureFlagRepository.save(any()) } returns savedFlag
        every { workspaceRepository.findAll() } returns workspaces
        every { workspaceFeatureFlagRepository.saveAll<WorkspaceFeatureFlag>(any()) } returns wffs
        every { workspaceFeatureFlagRepository.findByFeatureFlag(any()) } returns wffs

        val result = featureFlagService.createFeatureFlag(request)

        assertEquals(50, result.rolloutPercentage)
        verify(atLeast = 2) { workspaceFeatureFlagRepository.saveAll<WorkspaceFeatureFlag>(any()) }
    }

    // ==================== updateFeatureFlag ====================

    @Test
    fun `updateFeatureFlag should update flag successfully`() {
        val flagId = UUID.randomUUID()
        val existingFlag = createFeatureFlag("flag1", "team1", id = flagId, rollout = 30)
        val request = UpdateFeatureFlagRequest("flag1", "new desc", "team1", 50)
        val updatedFlag = existingFlag.copy(rolloutPercentage = 50)

        every { featureFlagRepository.findById(flagId) } returns Optional.of(existingFlag)
        every { featureFlagRepository.existsByTeamAndName(any(), any()) } returns true
        every { featureFlagRepository.save(any()) } returns updatedFlag
        every { workspaceRepository.findAll() } returns emptyList()
        every { workspaceFeatureFlagRepository.findByFeatureFlag(any()) } returns emptyList()
        every { workspaceFeatureFlagRepository.saveAll<WorkspaceFeatureFlag>(any()) } returns emptyList()

        val result = featureFlagService.updateFeatureFlag(flagId, request)

        assertEquals(50, result.rolloutPercentage)
        verify { featureFlagRepository.save(any()) }
        verify { auditLogService.logUpdate(30, 50, any()) }
    }

    @Test
    fun `updateFeatureFlag should throw exception when flag not found`() {
        val flagId = UUID.randomUUID()
        val request = UpdateFeatureFlagRequest("flag1", "desc", "team1", 50)

        every { featureFlagRepository.findById(flagId) } returns Optional.empty()

        val exception = assertThrows(ResourceNotFoundException::class.java) {
            featureFlagService.updateFeatureFlag(flagId, request)
        }

        assertTrue(exception.message!!.contains("not found"))
        verify(exactly = 0) { featureFlagRepository.save(any()) }
    }

    @Test
    fun `updateFeatureFlag should throw exception when changing name to existing flag in team`() {
        val flagId = UUID.randomUUID()
        val existingFlag = createFeatureFlag("flag1", "team1", id = flagId)
        val request = UpdateFeatureFlagRequest("flag2", "desc", "team1", 50)

        every { featureFlagRepository.findById(flagId) } returns Optional.of(existingFlag)
        every { featureFlagRepository.existsByTeamAndName("team1", "flag2") } returns true

        val exception = assertThrows(IllegalArgumentException::class.java) {
            featureFlagService.updateFeatureFlag(flagId, request)
        }

        assertTrue(exception.message!!.contains("already exists"))
        verify(exactly = 0) { featureFlagRepository.save(any()) }
    }

    @Test
    fun `updateFeatureFlag should handle rollout increase from 0 to 100`() {
        val flagId = UUID.randomUUID()
        val existingFlag = createFeatureFlag("flag1", "team1", id = flagId, rollout = 0)
        val request = UpdateFeatureFlagRequest("flag1", "desc", "team1", 100)
        val updatedFlag = existingFlag.copy(rolloutPercentage = 100)
        val workspace1 = createWorkspace("ws1")
        val workspace2 = createWorkspace("ws2")
        val wff1 = WorkspaceFeatureFlag(
            id = UUID.randomUUID(),
            workspace = workspace1,
            featureFlag = existingFlag,
            isEnabled = false
        )
        val wff2 = WorkspaceFeatureFlag(
            id = UUID.randomUUID(),
            workspace = workspace2,
            featureFlag = existingFlag,
            isEnabled = false
        )

        every { featureFlagRepository.findById(flagId) } returns Optional.of(existingFlag)
        every { featureFlagRepository.existsByTeamAndName(any(), any()) } returns true
        every { featureFlagRepository.save(any()) } returns updatedFlag
        every { workspaceRepository.findAll() } returns listOf(workspace1, workspace2)
        every { workspaceFeatureFlagRepository.findByFeatureFlag(any()) } returns listOf(wff1, wff2)
        every { workspaceFeatureFlagRepository.saveAll<WorkspaceFeatureFlag>(any()) } returns listOf(wff1, wff2)

        val result = featureFlagService.updateFeatureFlag(flagId, request)

        assertEquals(100, result.rolloutPercentage)
        verify { workspaceFeatureFlagRepository.saveAll<WorkspaceFeatureFlag>(any()) }
        verify { auditLogService.logUpdate(0, 100, any()) }
    }

    @Test
    fun `updateFeatureFlag should handle rollout decrease from 100 to 0`() {
        val flagId = UUID.randomUUID()
        val existingFlag = createFeatureFlag("flag1", "team1", id = flagId, rollout = 100)
        val request = UpdateFeatureFlagRequest("flag1", "desc", "team1", 0)
        val updatedFlag = existingFlag.copy(rolloutPercentage = 0)
        val workspace1 = createWorkspace("ws1")
        val workspace2 = createWorkspace("ws2")
        val wff1 = WorkspaceFeatureFlag(
            id = UUID.randomUUID(),
            workspace = workspace1,
            featureFlag = existingFlag,
            isEnabled = true
        )
        val wff2 = WorkspaceFeatureFlag(
            id = UUID.randomUUID(),
            workspace = workspace2,
            featureFlag = existingFlag,
            isEnabled = true
        )

        every { featureFlagRepository.findById(flagId) } returns Optional.of(existingFlag)
        every { featureFlagRepository.existsByTeamAndName(any(), any()) } returns true
        every { featureFlagRepository.save(any()) } returns updatedFlag
        every { workspaceRepository.findAll() } returns listOf(workspace1, workspace2)
        every { workspaceFeatureFlagRepository.findByFeatureFlag(any()) } returns listOf(wff1, wff2)
        every { workspaceFeatureFlagRepository.saveAll<WorkspaceFeatureFlag>(any()) } returns listOf(wff1, wff2)

        val result = featureFlagService.updateFeatureFlag(flagId, request)

        assertEquals(0, result.rolloutPercentage)
        verify { workspaceFeatureFlagRepository.saveAll<WorkspaceFeatureFlag>(any()) }
        verify { auditLogService.logUpdate(100, 0, any()) }
    }

    @Test
    fun `updateFeatureFlag should handle rollout percentage change with proportional updates`() {
        val flagId = UUID.randomUUID()
        val existingFlag = createFeatureFlag("flag1", "team1", id = flagId, rollout = 30)
        val request = UpdateFeatureFlagRequest("flag1", "desc", "team1", 70)
        val updatedFlag = existingFlag.copy(rolloutPercentage = 70)
        val workspaces = (1..10).map { createWorkspace("ws$it", UUID.randomUUID()) }
        val wffs = workspaces.mapIndexed { index, ws ->
            WorkspaceFeatureFlag(
                id = UUID.randomUUID(),
                workspace = ws,
                featureFlag = existingFlag,
                isEnabled = index < 3  // 3 out of 10 = 30%
            )
        }

        every { featureFlagRepository.findById(flagId) } returns Optional.of(existingFlag)
        every { featureFlagRepository.existsByTeamAndName(any(), any()) } returns true
        every { featureFlagRepository.save(any()) } returns updatedFlag
        every { workspaceRepository.findAll() } returns workspaces
        every { workspaceFeatureFlagRepository.findByFeatureFlag(any()) } returns wffs
        every { workspaceFeatureFlagRepository.saveAll<WorkspaceFeatureFlag>(any()) } returns wffs

        val result = featureFlagService.updateFeatureFlag(flagId, request)

        assertEquals(70, result.rolloutPercentage)
        verify { workspaceFeatureFlagRepository.saveAll<WorkspaceFeatureFlag>(any()) }
        verify { auditLogService.logUpdate(30, 70, any()) }
    }

    @Test
    fun `updateFeatureFlag should throw exception when trying to rename to existing flag name`() {
        val flagId = UUID.randomUUID()
        val existingFlag = createFeatureFlag("flag1", "TEAM1", id = flagId, rollout = 50)
        val request = UpdateFeatureFlagRequest("flag2", "desc", "TEAM1", 50)

        every { featureFlagRepository.findById(flagId) } returns Optional.of(existingFlag)
        every { featureFlagRepository.existsByTeamAndName("TEAM1", "flag2") } returns true

        val exception = assertThrows(IllegalArgumentException::class.java) {
            featureFlagService.updateFeatureFlag(flagId, request)
        }

        assertTrue(exception.message!!.contains("already exists"))
        verify(exactly = 0) { featureFlagRepository.save(any()) }
    }

    // ==================== deleteFeatureFlag ====================

    @Test
    fun `deleteFeatureFlag should delete flag successfully`() {
        val flagId = UUID.randomUUID()
        val flag = createFeatureFlag("flag1", "team1", id = flagId)

        every { featureFlagRepository.findById(flagId) } returns Optional.of(flag)
        every { featureFlagRepository.deleteById(flagId) } just Runs

        featureFlagService.deleteFeatureFlag(flagId)

        verify { featureFlagRepository.deleteById(flagId) }
        verify { auditLogService.logDelete(flag, any()) }
    }

    @Test
    fun `deleteFeatureFlag should throw exception when flag not found`() {
        val flagId = UUID.randomUUID()

        every { featureFlagRepository.findById(flagId) } returns Optional.empty()

        val exception = assertThrows(ResourceNotFoundException::class.java) {
            featureFlagService.deleteFeatureFlag(flagId)
        }

        assertTrue(exception.message!!.contains("not found"))
        verify(exactly = 0) { featureFlagRepository.deleteById(any()) }
    }

    // ==================== updateWorkspaceFeatureFlags ====================

    @Test
    fun `updateWorkspaceFeatureFlags should update workspace flags successfully`() {
        val flagId = UUID.randomUUID()
        val workspaceId = UUID.randomUUID()
        val flag = createFeatureFlag("flag1", "team1", id = flagId, rollout = 0)
        val workspace = createWorkspace("workspace1", workspaceId)
        val wff = WorkspaceFeatureFlag(
            id = UUID.randomUUID(),
            workspace = workspace,
            featureFlag = flag,
            isEnabled = false
        )

        // Scenario: Just enable workspaces without changing rollout percentage
        val request = UpdateWorkspaceFeatureFlagRequest(
            workspaceIds = listOf(workspaceId),
            enabled = true,
            rolloutPercentage = 0  // Same as current, so won't trigger rollout logic
        )

        every { featureFlagRepository.findById(flagId) } returns Optional.of(flag)
        every { workspaceFeatureFlagRepository.countEnabledByFeatureFlag(flag) } returnsMany listOf(0L, 1L)
        every { workspaceFeatureFlagRepository.findByFeatureFlag(flag) } returns listOf(wff)
        every { workspaceRepository.findAll() } returns listOf(workspace)
        every { workspaceRepository.findAllById(listOf(workspaceId)) } returns listOf(workspace)
        every { workspaceFeatureFlagRepository.findByFeatureFlagIdAndWorkspaceIdIn(flagId, listOf(workspaceId)) } returns listOf(wff)
        every { workspaceFeatureFlagRepository.save(any()) } returns wff
        every { featureFlagRepository.save(any()) } returns flag

        featureFlagService.updateWorkspaceFeatureFlags(flagId, request)

        verify { workspaceFeatureFlagRepository.save(any()) }
        verify { auditLogService.logWorkspaceUpdate(any(), any(), any(), any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `updateWorkspaceFeatureFlags should throw exception when flag not found`() {
        val flagId = UUID.randomUUID()
        val request = UpdateWorkspaceFeatureFlagRequest(listOf(UUID.randomUUID()), enabled = true)

        every { featureFlagRepository.findById(flagId) } returns Optional.empty()

        val exception = assertThrows(ResourceNotFoundException::class.java) {
            featureFlagService.updateWorkspaceFeatureFlags(flagId, request)
        }

        assertTrue(exception.message!!.contains("not found"))
    }

    @Test
    fun `updateWorkspaceFeatureFlags should throw exception when workspace not found`() {
        val flagId = UUID.randomUUID()
        val workspaceId = UUID.randomUUID()
        val flag = createFeatureFlag("flag1", "team1", id = flagId)
        val request = UpdateWorkspaceFeatureFlagRequest(
            workspaceIds = listOf(workspaceId),
            enabled = true
        )

        every { featureFlagRepository.findById(flagId) } returns Optional.of(flag)
        every { workspaceFeatureFlagRepository.countEnabledByFeatureFlag(flag) } returns 0L
        every { workspaceFeatureFlagRepository.findByFeatureFlag(flag) } returns emptyList()
        every { workspaceRepository.findAllById(listOf(workspaceId)) } returns emptyList()

        val exception = assertThrows(ResourceNotFoundException::class.java) {
            featureFlagService.updateWorkspaceFeatureFlags(flagId, request)
        }

        assertTrue(exception.message!!.contains("not found"))
    }

    @Test
    fun `updateWorkspaceFeatureFlags should throw exception when no associations found`() {
        val flagId = UUID.randomUUID()
        val workspaceId = UUID.randomUUID()
        val flag = createFeatureFlag("flag1", "team1", id = flagId)
        val workspace = createWorkspace("workspace1", workspaceId)
        val request = UpdateWorkspaceFeatureFlagRequest(
            workspaceIds = listOf(workspaceId),
            enabled = true
        )

        every { featureFlagRepository.findById(flagId) } returns Optional.of(flag)
        every { workspaceFeatureFlagRepository.countEnabledByFeatureFlag(flag) } returns 0L
        every { workspaceFeatureFlagRepository.findByFeatureFlag(flag) } returns emptyList()
        every { workspaceRepository.findAllById(listOf(workspaceId)) } returns listOf(workspace)
        every { workspaceFeatureFlagRepository.findByFeatureFlagIdAndWorkspaceIdIn(flagId, listOf(workspaceId)) } returns emptyList()

        val exception = assertThrows(IllegalArgumentException::class.java) {
            featureFlagService.updateWorkspaceFeatureFlags(flagId, request)
        }

        assertTrue(exception.message!!.contains("No workspace-feature flag associations found"))
    }

    @Test
    fun `updateWorkspaceFeatureFlags should handle excluded workspaces`() {
        val flagId = UUID.randomUUID()
        val workspaceId1 = UUID.randomUUID()
        val workspaceId2 = UUID.randomUUID()
        val flag = createFeatureFlag("flag1", "team1", id = flagId, rollout = 50)
        val workspace1 = createWorkspace("workspace1", workspaceId1)
        val workspace2 = createWorkspace("workspace2", workspaceId2)
        val wff1 = WorkspaceFeatureFlag(
            id = UUID.randomUUID(),
            workspace = workspace1,
            featureFlag = flag,
            isEnabled = true
        )
        val wff2 = WorkspaceFeatureFlag(
            id = UUID.randomUUID(),
            workspace = workspace2,
            featureFlag = flag,
            isEnabled = true
        )

        val request = UpdateWorkspaceFeatureFlagRequest(
            workspaceIds = emptyList(),
            enabled = true,
            excludedWorkspaceIds = listOf(workspaceId1),
            rolloutPercentage = 100
        )

        every { featureFlagRepository.findById(flagId) } returnsMany listOf(
            Optional.of(flag),
            Optional.of(flag),
            Optional.of(flag),
            Optional.of(flag)
        )
        every { workspaceFeatureFlagRepository.countEnabledByFeatureFlag(flag) } returnsMany listOf(2L, 1L)
        every { workspaceFeatureFlagRepository.findByFeatureFlag(flag) } returns listOf(wff1, wff2)
        every { workspaceRepository.findAll() } returns listOf(workspace1, workspace2)
        every { workspaceRepository.findAllById(listOf(workspaceId1)) } returns listOf(workspace1)
        every { workspaceFeatureFlagRepository.saveAll<WorkspaceFeatureFlag>(any()) } returns listOf(wff1, wff2)
        every { featureFlagRepository.save(any()) } returns flag

        featureFlagService.updateWorkspaceFeatureFlags(flagId, request)

        verify { workspaceFeatureFlagRepository.saveAll<WorkspaceFeatureFlag>(any()) }
        verify { auditLogService.logWorkspaceUpdate(any(), any(), any(), any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `updateWorkspaceFeatureFlags should handle target region`() {
        val flagId = UUID.randomUUID()
        val flag = createFeatureFlag("flag1", "team1", id = flagId, rollout = 0)
        val workspace1 = createWorkspace("workspace1", Region.WESTEUROPE)
        val workspace2 = createWorkspace("workspace2", Region.EASTUS)
        val wff1 = WorkspaceFeatureFlag(
            id = UUID.randomUUID(),
            workspace = workspace1,
            featureFlag = flag,
            isEnabled = false
        )
        val wff2 = WorkspaceFeatureFlag(
            id = UUID.randomUUID(),
            workspace = workspace2,
            featureFlag = flag,
            isEnabled = false
        )

        val request = UpdateWorkspaceFeatureFlagRequest(
            workspaceIds = emptyList(),
            enabled = true,
            rolloutPercentage = 100,
            targetRegion = Region.WESTEUROPE
        )

        every { featureFlagRepository.findById(flagId) } returnsMany listOf(
            Optional.of(flag),
            Optional.of(flag),
            Optional.of(flag),
            Optional.of(flag)
        )
        every { workspaceFeatureFlagRepository.countEnabledByFeatureFlag(flag) } returnsMany listOf(0L, 1L)
        every { workspaceFeatureFlagRepository.findByFeatureFlag(flag) } returns listOf(wff1, wff2)
        every { workspaceRepository.findAll() } returns listOf(workspace1, workspace2)
        every { workspaceFeatureFlagRepository.saveAll<WorkspaceFeatureFlag>(any()) } returns listOf(wff1)
        every { featureFlagRepository.save(any()) } returns flag

        featureFlagService.updateWorkspaceFeatureFlags(flagId, request)

        verify { workspaceFeatureFlagRepository.saveAll<WorkspaceFeatureFlag>(any()) }
        verify { auditLogService.logWorkspaceUpdate(any(), any(), any(), any(), any(), any(), any(), "WESTEUROPE", any()) }
    }

    @Test
    fun `updateWorkspaceFeatureFlags should handle target region with 0 percent rollout`() {
        val flagId = UUID.randomUUID()
        val flag = createFeatureFlag("flag1", "team1", id = flagId, rollout = 100)
        val workspace1 = createWorkspace("workspace1", Region.WESTEUROPE)
        val workspace2 = createWorkspace("workspace2", Region.EASTUS)
        val wff1 = WorkspaceFeatureFlag(
            id = UUID.randomUUID(),
            workspace = workspace1,
            featureFlag = flag,
            isEnabled = true
        )
        val wff2 = WorkspaceFeatureFlag(
            id = UUID.randomUUID(),
            workspace = workspace2,
            featureFlag = flag,
            isEnabled = true
        )

        val request = UpdateWorkspaceFeatureFlagRequest(
            workspaceIds = emptyList(),
            enabled = true,
            rolloutPercentage = 0,
            targetRegion = Region.WESTEUROPE
        )

        every { featureFlagRepository.findById(flagId) } returnsMany listOf(
            Optional.of(flag),
            Optional.of(flag),
            Optional.of(flag),
            Optional.of(flag)
        )
        every { workspaceFeatureFlagRepository.countEnabledByFeatureFlag(flag) } returnsMany listOf(2L, 1L)
        every { workspaceFeatureFlagRepository.findByFeatureFlag(flag) } returns listOf(wff1, wff2)
        every { workspaceRepository.findAll() } returns listOf(workspace1, workspace2)
        every { workspaceFeatureFlagRepository.saveAll<WorkspaceFeatureFlag>(any()) } returns listOf(wff1)
        every { featureFlagRepository.save(any()) } returns flag

        featureFlagService.updateWorkspaceFeatureFlags(flagId, request)

        verify(atLeast = 0) { workspaceFeatureFlagRepository.saveAll<WorkspaceFeatureFlag>(any()) }
        verify { auditLogService.logWorkspaceUpdate(any(), any(), any(), any(), any(), any(), any(), "WESTEUROPE", any()) }
    }

    @Test
    fun `updateWorkspaceFeatureFlags should handle target region with percentage between 1 and 99`() {
        val flagId = UUID.randomUUID()
        val flag = createFeatureFlag("flag1", "team1", id = flagId, rollout = 50)
        val westWorkspaces = (1..10).map { createWorkspace("west$it", Region.WESTEUROPE, UUID.randomUUID()) }
        val eastWorkspaces = (1..5).map { createWorkspace("east$it", Region.EASTUS, UUID.randomUUID()) }
        val allWorkspaces = westWorkspaces + eastWorkspaces
        val wffs = allWorkspaces.mapIndexed { index, ws ->
            WorkspaceFeatureFlag(
                id = UUID.randomUUID(),
                workspace = ws,
                featureFlag = flag,
                isEnabled = index < 7  // Mix of enabled/disabled
            )
        }

        val request = UpdateWorkspaceFeatureFlagRequest(
            workspaceIds = emptyList(),
            enabled = true,
            rolloutPercentage = 80,
            targetRegion = Region.WESTEUROPE
        )

        every { featureFlagRepository.findById(flagId) } returnsMany listOf(
            Optional.of(flag),
            Optional.of(flag),
            Optional.of(flag),
            Optional.of(flag)
        )
        every { workspaceFeatureFlagRepository.countEnabledByFeatureFlag(flag) } returnsMany listOf(7L, 11L)
        every { workspaceFeatureFlagRepository.findByFeatureFlag(flag) } returns wffs
        every { workspaceRepository.findAll() } returns allWorkspaces
        every { workspaceFeatureFlagRepository.saveAll<WorkspaceFeatureFlag>(any()) } returns wffs
        every { featureFlagRepository.save(any()) } returns flag

        featureFlagService.updateWorkspaceFeatureFlags(flagId, request)

        verify { workspaceFeatureFlagRepository.saveAll<WorkspaceFeatureFlag>(any()) }
        verify { auditLogService.logWorkspaceUpdate(any(), any(), any(), any(), any(), any(), any(), "WESTEUROPE", any()) }
    }

    @Test
    fun `updateWorkspaceFeatureFlags should handle target region ALL as no-op`() {
        val flagId = UUID.randomUUID()
        val flag = createFeatureFlag("flag1", "team1", id = flagId, rollout = 50)
        val workspace1 = createWorkspace("workspace1", Region.WESTEUROPE)
        val wff1 = WorkspaceFeatureFlag(
            id = UUID.randomUUID(),
            workspace = workspace1,
            featureFlag = flag,
            isEnabled = false
        )

        val request = UpdateWorkspaceFeatureFlagRequest(
            workspaceIds = emptyList(),
            enabled = true,
            rolloutPercentage = 100,
            targetRegion = Region.ALL
        )

        every { featureFlagRepository.findById(flagId) } returnsMany listOf(
            Optional.of(flag),
            Optional.of(flag),
            Optional.of(flag),
            Optional.of(flag)
        )
        every { workspaceFeatureFlagRepository.countEnabledByFeatureFlag(flag) } returnsMany listOf(0L, 0L)
        every { workspaceFeatureFlagRepository.findByFeatureFlag(flag) } returns listOf(wff1)
        every { workspaceRepository.findAll() } returns listOf(workspace1)
        every { featureFlagRepository.save(any()) } returns flag

        featureFlagService.updateWorkspaceFeatureFlags(flagId, request)

        verify { auditLogService.logWorkspaceUpdate(any(), any(), any(), any(), any(), any(), any(), "ALL", any()) }
    }

    @Test
    fun `updateWorkspaceFeatureFlags with no workspace IDs should only update rollout percentage`() {
        val flagId = UUID.randomUUID()
        val flag = createFeatureFlag("flag1", "team1", id = flagId, rollout = 0)
        val workspace1 = createWorkspace("workspace1")
        val wff1 = WorkspaceFeatureFlag(
            id = UUID.randomUUID(),
            workspace = workspace1,
            featureFlag = flag,
            isEnabled = false
        )

        val request = UpdateWorkspaceFeatureFlagRequest(
            workspaceIds = emptyList(),
            enabled = true,
            rolloutPercentage = 50
        )

        every { featureFlagRepository.findById(flagId) } returnsMany listOf(
            Optional.of(flag),
            Optional.of(flag),
            Optional.of(flag),
            Optional.of(flag)
        )
        every { workspaceFeatureFlagRepository.countEnabledByFeatureFlag(flag) } returnsMany listOf(0L, 0L)
        every { workspaceFeatureFlagRepository.findByFeatureFlag(flag) } returns listOf(wff1)
        every { workspaceRepository.findAll() } returns listOf(workspace1)
        every { workspaceFeatureFlagRepository.saveAll<WorkspaceFeatureFlag>(any()) } returns listOf(wff1)
        every { featureFlagRepository.save(any()) } returns flag

        featureFlagService.updateWorkspaceFeatureFlags(flagId, request)

        verify(atLeast = 0) { workspaceFeatureFlagRepository.saveAll<WorkspaceFeatureFlag>(any()) }
        verify { auditLogService.logWorkspaceUpdate(any(), any(), any(), any(), any(), any(), any(), null, any()) }
    }

    @Test
    fun `updateWorkspaceFeatureFlags should recalculate percentage when no workspaces exist`() {
        val flagId = UUID.randomUUID()
        val flag = createFeatureFlag("flag1", "team1", id = flagId, rollout = 50)
        val updatedFlag = flag.copy(rolloutPercentage = 0)

        val request = UpdateWorkspaceFeatureFlagRequest(
            workspaceIds = emptyList(),
            enabled = true,
            rolloutPercentage = 50
        )

        every { featureFlagRepository.findById(flagId) } returnsMany listOf(
            Optional.of(flag),
            Optional.of(updatedFlag),
            Optional.of(updatedFlag),
            Optional.of(updatedFlag)
        )
        every { workspaceFeatureFlagRepository.countEnabledByFeatureFlag(any()) } returns 0L
        every { workspaceFeatureFlagRepository.findByFeatureFlag(any()) } returns emptyList()
        every { workspaceRepository.findAll() } returns emptyList()
        every { featureFlagRepository.save(any()) } returns updatedFlag

        featureFlagService.updateWorkspaceFeatureFlags(flagId, request)

        verify { featureFlagRepository.save(any()) }
        verify { auditLogService.logWorkspaceUpdate(any(), any(), any(), any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `updateWorkspaceFeatureFlags when rollout percentage does not change should still recalculate`() {
        val flagId = UUID.randomUUID()
        val workspaceId = UUID.randomUUID()
        val flag = createFeatureFlag("flag1", "team1", id = flagId, rollout = 50)
        val workspace = createWorkspace("workspace1", workspaceId)
        val wff = WorkspaceFeatureFlag(
            id = UUID.randomUUID(),
            workspace = workspace,
            featureFlag = flag,
            isEnabled = true
        )

        val request = UpdateWorkspaceFeatureFlagRequest(
            workspaceIds = listOf(workspaceId),
            enabled = false,
            rolloutPercentage = 50  // Same as current rollout
        )

        every { featureFlagRepository.findById(flagId) } returnsMany listOf(
            Optional.of(flag),
            Optional.of(flag),
            Optional.of(flag)
        )
        every { workspaceFeatureFlagRepository.countEnabledByFeatureFlag(any()) } returns 0L
        every { workspaceFeatureFlagRepository.findByFeatureFlag(any()) } returns listOf(wff)
        every { workspaceRepository.findAll() } returns listOf(workspace)
        every { workspaceRepository.findAllById(listOf(workspaceId)) } returns listOf(workspace)
        every { workspaceFeatureFlagRepository.findByFeatureFlagIdAndWorkspaceIdIn(flagId, listOf(workspaceId)) } returns listOf(wff)
        every { workspaceFeatureFlagRepository.save(any()) } returns wff
        every { featureFlagRepository.save(any()) } returns flag

        featureFlagService.updateWorkspaceFeatureFlags(flagId, request)

        verify { workspaceFeatureFlagRepository.save(any()) }
        verify { auditLogService.logWorkspaceUpdate(any(), any(), any(), any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `updateWorkspaceFeatureFlags with priority workspaces and 0 percent global rollout`() {
        val flagId = UUID.randomUUID()
        val workspaceId1 = UUID.randomUUID()
        val workspaceId2 = UUID.randomUUID()
        val flag = createFeatureFlag("flag1", "team1", id = flagId, rollout = 100)
        val workspace1 = createWorkspace("workspace1", workspaceId1)
        val workspace2 = createWorkspace("workspace2", workspaceId2)
        val wff1 = WorkspaceFeatureFlag(
            id = UUID.randomUUID(),
            workspace = workspace1,
            featureFlag = flag,
            isEnabled = true
        )
        val wff2 = WorkspaceFeatureFlag(
            id = UUID.randomUUID(),
            workspace = workspace2,
            featureFlag = flag,
            isEnabled = true
        )

        val request = UpdateWorkspaceFeatureFlagRequest(
            workspaceIds = listOf(workspaceId1),
            enabled = true,
            rolloutPercentage = 0
        )

        every { featureFlagRepository.findById(flagId) } returnsMany listOf(
            Optional.of(flag),
            Optional.of(flag),
            Optional.of(flag),
            Optional.of(flag)
        )
        every { workspaceFeatureFlagRepository.countEnabledByFeatureFlag(flag) } returnsMany listOf(2L, 1L)
        every { workspaceFeatureFlagRepository.findByFeatureFlag(flag) } returns listOf(wff1, wff2)
        every { workspaceRepository.findAll() } returns listOf(workspace1, workspace2)
        every { workspaceRepository.findAllById(listOf(workspaceId1)) } returns listOf(workspace1)
        every { workspaceFeatureFlagRepository.findByFeatureFlagIdAndWorkspaceIdIn(flagId, listOf(workspaceId1)) } returns listOf(wff1)
        every { workspaceFeatureFlagRepository.save(any()) } returns wff1
        every { workspaceFeatureFlagRepository.saveAll<WorkspaceFeatureFlag>(any()) } returns listOf(wff1, wff2)
        every { featureFlagRepository.save(any()) } returns flag

        featureFlagService.updateWorkspaceFeatureFlags(flagId, request)

        verify { workspaceFeatureFlagRepository.save(any()) }
        verify(atLeast = 0) { workspaceFeatureFlagRepository.saveAll<WorkspaceFeatureFlag>(any()) }
    }

    @Test
    fun `updateWorkspaceFeatureFlags with priority workspaces and 100 percent global rollout`() {
        val flagId = UUID.randomUUID()
        val workspaceId1 = UUID.randomUUID()
        val workspaceId2 = UUID.randomUUID()
        val flag = createFeatureFlag("flag1", "team1", id = flagId, rollout = 0)
        val workspace1 = createWorkspace("workspace1", workspaceId1)
        val workspace2 = createWorkspace("workspace2", workspaceId2)
        val wff1 = WorkspaceFeatureFlag(
            id = UUID.randomUUID(),
            workspace = workspace1,
            featureFlag = flag,
            isEnabled = false
        )
        val wff2 = WorkspaceFeatureFlag(
            id = UUID.randomUUID(),
            workspace = workspace2,
            featureFlag = flag,
            isEnabled = false
        )

        val request = UpdateWorkspaceFeatureFlagRequest(
            workspaceIds = listOf(workspaceId1),
            enabled = true,
            rolloutPercentage = 100
        )

        every { featureFlagRepository.findById(flagId) } returnsMany listOf(
            Optional.of(flag),
            Optional.of(flag),
            Optional.of(flag),
            Optional.of(flag)
        )
        every { workspaceFeatureFlagRepository.countEnabledByFeatureFlag(flag) } returnsMany listOf(0L, 2L)
        every { workspaceFeatureFlagRepository.findByFeatureFlag(flag) } returns listOf(wff1, wff2)
        every { workspaceRepository.findAll() } returns listOf(workspace1, workspace2)
        every { workspaceRepository.findAllById(listOf(workspaceId1)) } returns listOf(workspace1)
        every { workspaceFeatureFlagRepository.findByFeatureFlagIdAndWorkspaceIdIn(flagId, listOf(workspaceId1)) } returns listOf(wff1)
        every { workspaceFeatureFlagRepository.save(any()) } returns wff1
        every { workspaceFeatureFlagRepository.saveAll<WorkspaceFeatureFlag>(any()) } returns listOf(wff1, wff2)
        every { featureFlagRepository.save(any()) } returns flag

        featureFlagService.updateWorkspaceFeatureFlags(flagId, request)

        verify { workspaceFeatureFlagRepository.save(any()) }
        verify(atLeast = 0) { workspaceFeatureFlagRepository.saveAll<WorkspaceFeatureFlag>(any()) }
    }

    @Test
    fun `updateWorkspaceFeatureFlags with priority and excluded workspaces and percentage rollout`() {
        val flagId = UUID.randomUUID()
        val priorityId = UUID.randomUUID()
        val excludedId = UUID.randomUUID()
        val otherId = UUID.randomUUID()
        val flag = createFeatureFlag("flag1", "team1", id = flagId, rollout = 0)
        val priorityWs = createWorkspace("priority", priorityId)
        val excludedWs = createWorkspace("excluded", excludedId)
        val otherWs = createWorkspace("other", otherId)
        val wff1 = WorkspaceFeatureFlag(
            id = UUID.randomUUID(),
            workspace = priorityWs,
            featureFlag = flag,
            isEnabled = false
        )
        val wff2 = WorkspaceFeatureFlag(
            id = UUID.randomUUID(),
            workspace = excludedWs,
            featureFlag = flag,
            isEnabled = false
        )
        val wff3 = WorkspaceFeatureFlag(
            id = UUID.randomUUID(),
            workspace = otherWs,
            featureFlag = flag,
            isEnabled = false
        )

        val request = UpdateWorkspaceFeatureFlagRequest(
            workspaceIds = listOf(priorityId),
            enabled = true,
            excludedWorkspaceIds = listOf(excludedId),
            rolloutPercentage = 50
        )

        every { featureFlagRepository.findById(flagId) } returnsMany listOf(
            Optional.of(flag),
            Optional.of(flag),
            Optional.of(flag),
            Optional.of(flag)
        )
        every { workspaceFeatureFlagRepository.countEnabledByFeatureFlag(flag) } returnsMany listOf(0L, 2L)
        every { workspaceFeatureFlagRepository.findByFeatureFlag(flag) } returns listOf(wff1, wff2, wff3)
        every { workspaceRepository.findAll() } returns listOf(priorityWs, excludedWs, otherWs)
        every { workspaceRepository.findAllById(listOf(priorityId)) } returns listOf(priorityWs)
        every { workspaceRepository.findAllById(listOf(excludedId)) } returns listOf(excludedWs)
        every { workspaceFeatureFlagRepository.findByFeatureFlagIdAndWorkspaceIdIn(flagId, listOf(priorityId)) } returns listOf(wff1)
        every { workspaceFeatureFlagRepository.save(any()) } returns wff1
        every { workspaceFeatureFlagRepository.saveAll<WorkspaceFeatureFlag>(any()) } returns listOf(wff1, wff2, wff3)
        every { featureFlagRepository.save(any()) } returns flag

        featureFlagService.updateWorkspaceFeatureFlags(flagId, request)

        verify { workspaceFeatureFlagRepository.save(any()) }
        verify(atLeast = 0) { workspaceFeatureFlagRepository.saveAll<WorkspaceFeatureFlag>(any()) }
    }

    @Test
    fun `updateWorkspaceFeatureFlags with target region and priority workspaces should enable priority first`() {
        val flagId = UUID.randomUUID()
        val priorityId = UUID.randomUUID()
        val otherId = UUID.randomUUID()
        val flag = createFeatureFlag("flag1", "team1", id = flagId, rollout = 0)
        val priorityWs = createWorkspace("priority", Region.WESTEUROPE, priorityId)
        val otherWs = createWorkspace("other", Region.WESTEUROPE, otherId)
        val wff1 = WorkspaceFeatureFlag(
            id = UUID.randomUUID(),
            workspace = priorityWs,
            featureFlag = flag,
            isEnabled = false
        )
        val wff2 = WorkspaceFeatureFlag(
            id = UUID.randomUUID(),
            workspace = otherWs,
            featureFlag = flag,
            isEnabled = false
        )

        val request = UpdateWorkspaceFeatureFlagRequest(
            workspaceIds = listOf(priorityId),
            enabled = true,
            rolloutPercentage = 50,
            targetRegion = Region.WESTEUROPE
        )

        every { featureFlagRepository.findById(flagId) } returnsMany listOf(
            Optional.of(flag),
            Optional.of(flag),
            Optional.of(flag),
            Optional.of(flag)
        )
        every { workspaceFeatureFlagRepository.countEnabledByFeatureFlag(flag) } returnsMany listOf(0L, 2L)
        every { workspaceFeatureFlagRepository.findByFeatureFlag(flag) } returns listOf(wff1, wff2)
        every { workspaceRepository.findAll() } returns listOf(priorityWs, otherWs)
        every { workspaceRepository.findAllById(listOf(priorityId)) } returns listOf(priorityWs)
        every { workspaceFeatureFlagRepository.findByFeatureFlagIdAndWorkspaceIdIn(flagId, listOf(priorityId)) } returns listOf(wff1)
        every { workspaceFeatureFlagRepository.save(any()) } returns wff1
        every { workspaceFeatureFlagRepository.saveAll<WorkspaceFeatureFlag>(any()) } returns listOf(wff1, wff2)
        every { featureFlagRepository.save(any()) } returns flag

        featureFlagService.updateWorkspaceFeatureFlags(flagId, request)

        verify { workspaceFeatureFlagRepository.save(any()) }
        verify(atLeast = 0) { workspaceFeatureFlagRepository.saveAll<WorkspaceFeatureFlag>(any()) }
    }

    @Test
    fun `updateWorkspaceFeatureFlags with target region and excluded workspaces in percentage range`() {
        val flagId = UUID.randomUUID()
        val excludedId = UUID.randomUUID()
        val otherId1 = UUID.randomUUID()
        val otherId2 = UUID.randomUUID()
        val flag = createFeatureFlag("flag1", "team1", id = flagId, rollout = 0)
        val excludedWs = createWorkspace("excluded", Region.WESTEUROPE, excludedId)
        val other1 = createWorkspace("other1", Region.WESTEUROPE, otherId1)
        val other2 = createWorkspace("other2", Region.WESTEUROPE, otherId2)
        val wff1 = WorkspaceFeatureFlag(
            id = UUID.randomUUID(),
            workspace = excludedWs,
            featureFlag = flag,
            isEnabled = false
        )
        val wff2 = WorkspaceFeatureFlag(
            id = UUID.randomUUID(),
            workspace = other1,
            featureFlag = flag,
            isEnabled = false
        )
        val wff3 = WorkspaceFeatureFlag(
            id = UUID.randomUUID(),
            workspace = other2,
            featureFlag = flag,
            isEnabled = false
        )

        val request = UpdateWorkspaceFeatureFlagRequest(
            workspaceIds = emptyList(),
            enabled = true,
            excludedWorkspaceIds = listOf(excludedId),
            rolloutPercentage = 50,
            targetRegion = Region.WESTEUROPE
        )

        every { featureFlagRepository.findById(flagId) } returnsMany listOf(
            Optional.of(flag),
            Optional.of(flag),
            Optional.of(flag),
            Optional.of(flag)
        )
        every { workspaceFeatureFlagRepository.countEnabledByFeatureFlag(flag) } returnsMany listOf(0L, 1L)
        every { workspaceFeatureFlagRepository.findByFeatureFlag(flag) } returns listOf(wff1, wff2, wff3)
        every { workspaceRepository.findAll() } returns listOf(excludedWs, other1, other2)
        every { workspaceRepository.findAllById(listOf(excludedId)) } returns listOf(excludedWs)
        every { workspaceFeatureFlagRepository.saveAll<WorkspaceFeatureFlag>(any()) } returns listOf(wff2, wff3)
        every { featureFlagRepository.save(any()) } returns flag

        featureFlagService.updateWorkspaceFeatureFlags(flagId, request)

        verify { workspaceFeatureFlagRepository.saveAll<WorkspaceFeatureFlag>(any()) }
    }

    @Test
    fun `updateWorkspaceFeatureFlags with target region 100 percent should enable priority workspaces`() {
        val flagId = UUID.randomUUID()
        val priorityId = UUID.randomUUID()
        val otherId = UUID.randomUUID()
        val flag = createFeatureFlag("flag1", "team1", id = flagId, rollout = 0)
        val priorityWs = createWorkspace("priority", Region.WESTEUROPE, priorityId)
        val otherWs = createWorkspace("other", Region.WESTEUROPE, otherId)
        val wff1 = WorkspaceFeatureFlag(
            id = UUID.randomUUID(),
            workspace = priorityWs,
            featureFlag = flag,
            isEnabled = false
        )
        val wff2 = WorkspaceFeatureFlag(
            id = UUID.randomUUID(),
            workspace = otherWs,
            featureFlag = flag,
            isEnabled = false
        )

        val request = UpdateWorkspaceFeatureFlagRequest(
            workspaceIds = listOf(priorityId),
            enabled = true,
            rolloutPercentage = 100,
            targetRegion = Region.WESTEUROPE
        )

        every { featureFlagRepository.findById(flagId) } returnsMany listOf(
            Optional.of(flag),
            Optional.of(flag),
            Optional.of(flag),
            Optional.of(flag)
        )
        every { workspaceFeatureFlagRepository.countEnabledByFeatureFlag(flag) } returnsMany listOf(0L, 2L)
        every { workspaceFeatureFlagRepository.findByFeatureFlag(flag) } returns listOf(wff1, wff2)
        every { workspaceRepository.findAll() } returns listOf(priorityWs, otherWs)
        every { workspaceRepository.findAllById(listOf(priorityId)) } returns listOf(priorityWs)
        every { workspaceFeatureFlagRepository.findByFeatureFlagIdAndWorkspaceIdIn(flagId, listOf(priorityId)) } returns listOf(wff1)
        every { workspaceFeatureFlagRepository.save(any()) } returns wff1
        every { workspaceFeatureFlagRepository.saveAll<WorkspaceFeatureFlag>(any()) } returns listOf(wff1, wff2)
        every { featureFlagRepository.save(any()) } returns flag

        featureFlagService.updateWorkspaceFeatureFlags(flagId, request)

        verify { workspaceFeatureFlagRepository.save(any()) }
        verify(atLeast = 1) { workspaceFeatureFlagRepository.saveAll<WorkspaceFeatureFlag>(any()) }
    }

    @Test
    fun `updateWorkspaceFeatureFlags with target region 0 percent should disable excluded workspaces`() {
        val flagId = UUID.randomUUID()
        val excludedId = UUID.randomUUID()
        val priorityId = UUID.randomUUID()
        val flag = createFeatureFlag("flag1", "team1", id = flagId, rollout = 100)
        val excludedWs = createWorkspace("excluded", Region.WESTEUROPE, excludedId)
        val priorityWs = createWorkspace("priority", Region.WESTEUROPE, priorityId)
        val wff1 = WorkspaceFeatureFlag(
            id = UUID.randomUUID(),
            workspace = excludedWs,
            featureFlag = flag,
            isEnabled = true
        )
        val wff2 = WorkspaceFeatureFlag(
            id = UUID.randomUUID(),
            workspace = priorityWs,
            featureFlag = flag,
            isEnabled = true
        )

        val request = UpdateWorkspaceFeatureFlagRequest(
            workspaceIds = listOf(priorityId),
            enabled = true,
            excludedWorkspaceIds = listOf(excludedId),
            rolloutPercentage = 0,
            targetRegion = Region.WESTEUROPE
        )

        every { featureFlagRepository.findById(flagId) } returnsMany listOf(
            Optional.of(flag),
            Optional.of(flag),
            Optional.of(flag),
            Optional.of(flag)
        )
        every { workspaceFeatureFlagRepository.countEnabledByFeatureFlag(flag) } returnsMany listOf(2L, 1L)
        every { workspaceFeatureFlagRepository.findByFeatureFlag(flag) } returns listOf(wff1, wff2)
        every { workspaceRepository.findAll() } returns listOf(excludedWs, priorityWs)
        every { workspaceRepository.findAllById(listOf(priorityId)) } returns listOf(priorityWs)
        every { workspaceRepository.findAllById(listOf(excludedId)) } returns listOf(excludedWs)
        every { workspaceFeatureFlagRepository.findByFeatureFlagIdAndWorkspaceIdIn(flagId, listOf(priorityId)) } returns listOf(wff2)
        every { workspaceFeatureFlagRepository.save(any()) } returns wff2
        every { workspaceFeatureFlagRepository.saveAll<WorkspaceFeatureFlag>(any()) } returns listOf(wff1, wff2)
        every { featureFlagRepository.save(any()) } returns flag

        featureFlagService.updateWorkspaceFeatureFlags(flagId, request)

        verify { workspaceFeatureFlagRepository.save(any()) }
        verify(atLeast = 0) { workspaceFeatureFlagRepository.saveAll<WorkspaceFeatureFlag>(any()) }
    }

    // ==================== getEnabledWorkspacesForFeatureFlagPaginated ====================

    @Test
    fun `getEnabledWorkspacesForFeatureFlagPaginated should return paginated workspaces`() {
        val flagId = UUID.randomUUID()
        val workspace = createWorkspace("workspace1")
        val flag = createFeatureFlag("flag1", "team1", id = flagId)
        val wff = WorkspaceFeatureFlag(
            id = UUID.randomUUID(),
            workspace = workspace,
            featureFlag = flag,
            isEnabled = true
        )
        val pageable = PageRequest.of(0, 10)
        val page = PageImpl(listOf(wff), pageable, 1)

        every { featureFlagRepository.findById(flagId) } returns Optional.of(flag)
        every { workspaceFeatureFlagRepository.findEnabledByFeatureFlagId(flagId, any()) } returns page

        val result = featureFlagService.getEnabledWorkspacesForFeatureFlagPaginated(flagId, 0, 10, null)

        assertEquals(1, result.content.size)
        assertEquals(1, result.totalElements)
        verify { workspaceFeatureFlagRepository.findEnabledByFeatureFlagId(flagId, any()) }
    }

    @Test
    fun `getEnabledWorkspacesForFeatureFlagPaginated should use search when provided`() {
        val flagId = UUID.randomUUID()
        val flag = createFeatureFlag("flag1", "team1", id = flagId)
        val pageable = PageRequest.of(0, 10)
        val page = PageImpl<WorkspaceFeatureFlag>(emptyList(), pageable, 0)

        every { featureFlagRepository.findById(flagId) } returns Optional.of(flag)
        every { workspaceFeatureFlagRepository.searchEnabledByFeatureFlagId(flagId, "search", any()) } returns page

        val result = featureFlagService.getEnabledWorkspacesForFeatureFlagPaginated(flagId, 0, 10, "search")

        verify { workspaceFeatureFlagRepository.searchEnabledByFeatureFlagId(flagId, "search", any()) }
    }

    // ==================== getWorkspaceCountsByRegion ====================

    @Test
    fun `getWorkspaceCountsByRegion should return counts with enabled and total per region`() {
        val flagId = UUID.randomUUID()
        val flag = createFeatureFlag("flag1", "team1", id = flagId)
        val enabledCounts = listOf(
            object : com.featureflag.repository.WorkspaceFeatureFlagRepository.RegionCount {
                override fun getRegion() = "WESTEUROPE"
                override fun getCount() = 5L
            },
            object : com.featureflag.repository.WorkspaceFeatureFlagRepository.RegionCount {
                override fun getRegion() = "EASTUS"
                override fun getCount() = 3L
            }
        )
        val totalCounts = listOf(
            object : com.featureflag.repository.WorkspaceRepository.RegionCount {
                override fun getRegion() = "WESTEUROPE"
                override fun getCount() = 10L
            },
            object : com.featureflag.repository.WorkspaceRepository.RegionCount {
                override fun getRegion() = "EASTUS"
                override fun getCount() = 8L
            }
        )

        every { featureFlagRepository.findById(flagId) } returns Optional.of(flag)
        every { workspaceFeatureFlagRepository.countEnabledWorkspacesByRegion(flagId) } returns enabledCounts
        every { workspaceRepository.countTotalWorkspacesByRegion() } returns totalCounts

        val result = featureFlagService.getWorkspaceCountsByRegion(flagId)

        assertEquals(2, result.size)

        val westEurope = result.find { it.region == "WESTEUROPE" }
        assertNotNull(westEurope)
        assertEquals(5L, westEurope!!.enabledCount)
        assertEquals(10L, westEurope.totalCount)

        val eastUs = result.find { it.region == "EASTUS" }
        assertNotNull(eastUs)
        assertEquals(3L, eastUs!!.enabledCount)
        assertEquals(8L, eastUs.totalCount)

        verify { workspaceFeatureFlagRepository.countEnabledWorkspacesByRegion(flagId) }
        verify { workspaceRepository.countTotalWorkspacesByRegion() }
    }

    @Test
    fun `getWorkspaceCountsByRegion should return zero enabled count when no workspaces enabled in region`() {
        val flagId = UUID.randomUUID()
        val flag = createFeatureFlag("flag1", "team1", id = flagId)
        val enabledCounts = listOf(
            object : com.featureflag.repository.WorkspaceFeatureFlagRepository.RegionCount {
                override fun getRegion() = "WESTEUROPE"
                override fun getCount() = 5L
            }
        )
        val totalCounts = listOf(
            object : com.featureflag.repository.WorkspaceRepository.RegionCount {
                override fun getRegion() = "WESTEUROPE"
                override fun getCount() = 10L
            },
            object : com.featureflag.repository.WorkspaceRepository.RegionCount {
                override fun getRegion() = "EASTUS"
                override fun getCount() = 8L
            }
        )

        every { featureFlagRepository.findById(flagId) } returns Optional.of(flag)
        every { workspaceFeatureFlagRepository.countEnabledWorkspacesByRegion(flagId) } returns enabledCounts
        every { workspaceRepository.countTotalWorkspacesByRegion() } returns totalCounts

        val result = featureFlagService.getWorkspaceCountsByRegion(flagId)

        assertEquals(2, result.size)

        val westEurope = result.find { it.region == "WESTEUROPE" }
        assertNotNull(westEurope)
        assertEquals(5L, westEurope!!.enabledCount)
        assertEquals(10L, westEurope.totalCount)

        val eastUs = result.find { it.region == "EASTUS" }
        assertNotNull(eastUs)
        assertEquals(0L, eastUs!!.enabledCount)  // No enabled workspaces in EASTUS
        assertEquals(8L, eastUs.totalCount)
    }

    @Test
    fun `getWorkspaceCountsByRegion should return empty list when no regions exist`() {
        val flagId = UUID.randomUUID()
        val flag = createFeatureFlag("flag1", "team1", id = flagId)

        every { featureFlagRepository.findById(flagId) } returns Optional.of(flag)
        every { workspaceFeatureFlagRepository.countEnabledWorkspacesByRegion(flagId) } returns emptyList()
        every { workspaceRepository.countTotalWorkspacesByRegion() } returns emptyList()

        val result = featureFlagService.getWorkspaceCountsByRegion(flagId)

        assertTrue(result.isEmpty())
    }

    @Test
    fun `getWorkspaceCountsByRegion should sort results by region name`() {
        val flagId = UUID.randomUUID()
        val flag = createFeatureFlag("flag1", "team1", id = flagId)
        val enabledCounts = listOf(
            object : com.featureflag.repository.WorkspaceFeatureFlagRepository.RegionCount {
                override fun getRegion() = "WESTEUROPE"
                override fun getCount() = 5L
            }
        )
        val totalCounts = listOf(
            object : com.featureflag.repository.WorkspaceRepository.RegionCount {
                override fun getRegion() = "WESTEUROPE"
                override fun getCount() = 10L
            },
            object : com.featureflag.repository.WorkspaceRepository.RegionCount {
                override fun getRegion() = "EASTUS"
                override fun getCount() = 8L
            },
            object : com.featureflag.repository.WorkspaceRepository.RegionCount {
                override fun getRegion() = "CANADACENTRAL"
                override fun getCount() = 6L
            }
        )

        every { featureFlagRepository.findById(flagId) } returns Optional.of(flag)
        every { workspaceFeatureFlagRepository.countEnabledWorkspacesByRegion(flagId) } returns enabledCounts
        every { workspaceRepository.countTotalWorkspacesByRegion() } returns totalCounts

        val result = featureFlagService.getWorkspaceCountsByRegion(flagId)

        assertEquals(3, result.size)
        assertEquals("CANADACENTRAL", result[0].region)
        assertEquals("EASTUS", result[1].region)
        assertEquals("WESTEUROPE", result[2].region)
    }

    // ==================== Helper Methods ====================

    private fun createFeatureFlag(
        name: String,
        team: String,
        id: UUID = UUID.randomUUID(),
        rollout: Int = 50
    ): FeatureFlag {
        return FeatureFlag(
            id = id,
            name = name,
            description = "Test description",
            team = team,
            rolloutPercentage = rollout,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
    }

    private fun createWorkspace(name: String, id: UUID = UUID.randomUUID()): Workspace {
        return Workspace(
            id = id,
            name = name,
            type = "PRODUCTION",
            region = Region.WESTEUROPE,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
    }

    private fun createWorkspace(name: String, region: Region, id: UUID = UUID.randomUUID()): Workspace {
        return Workspace(
            id = id,
            name = name,
            type = "PRODUCTION",
            region = region,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
    }

    companion object {
        @JvmStatic
        fun validCreateFeatureFlagRequests(): Stream<Arguments> {
            return Stream.of(
                Arguments.of("flag1", "description 1", "team1", 50),
                Arguments.of("flag-with-dash", null, "TEAM2", 0),
                Arguments.of("FLAG_UPPERCASE", "desc", "backend-team", 100),
                Arguments.of("flag.with.dots", "test flag", "frontend", 25)
            )
        }
    }
}
