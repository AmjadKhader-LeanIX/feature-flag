package com.featureflag.service

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
import org.junit.jupiter.api.assertThrows
import java.util.*

class FeatureFlagServiceWorkspaceTest {

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
        auditLogService = mockk()
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

    @Test
    fun `should enable feature flag for specific workspaces`() {
        // Given
        val featureFlagId = UUID.randomUUID()
        val workspace1Id = UUID.randomUUID()
        val workspace2Id = UUID.randomUUID()

        val featureFlag = FeatureFlag(
            id = featureFlagId,
            name = "test-flag",
            description = "Test flag",
            team = "test-team",
            rolloutPercentage = 50,
            regions = "WESTEUROPE"
        )

        val workspace1 = Workspace(
            id = workspace1Id,
            name = "Workspace 1",
            type = "PRODUCTION",
            region = Region.WESTEUROPE
        )

        val workspace2 = Workspace(
            id = workspace2Id,
            name = "Workspace 2",
            type = "PRODUCTION",
            region = Region.WESTEUROPE
        )

        val workspaceFeatureFlag1 = WorkspaceFeatureFlag(
            id = UUID.randomUUID(),
            workspace = workspace1,
            featureFlag = featureFlag,
            isEnabled = false
        )

        val workspaceFeatureFlag2 = WorkspaceFeatureFlag(
            id = UUID.randomUUID(),
            workspace = workspace2,
            featureFlag = featureFlag,
            isEnabled = false
        )

        val request = UpdateWorkspaceFeatureFlagRequest(
            workspaceIds = listOf(workspace1Id, workspace2Id),
            enabled = true
        )

        every { featureFlagRepository.findById(featureFlagId) } returns Optional.of(featureFlag)
        every { workspaceRepository.findAllById(request.workspaceIds) } returns listOf(workspace1, workspace2)
        every {
            workspaceFeatureFlagRepository.findByFeatureFlagIdAndWorkspaceIdIn(featureFlagId, request.workspaceIds)
        } returns listOf(workspaceFeatureFlag1, workspaceFeatureFlag2)
        every { workspaceFeatureFlagRepository.findByFeatureFlag(featureFlag) } returns listOf(workspaceFeatureFlag1, workspaceFeatureFlag2)
        every { workspaceRepository.findByRegionIn(listOf("WESTEUROPE")) } returns listOf(workspace1, workspace2)
        every { workspaceFeatureFlagRepository.saveAll(any<List<WorkspaceFeatureFlag>>()) } returns listOf()
        every { featureFlagRepository.save(any()) } returns featureFlag
        every { auditLogService.logWorkspaceUpdate(any(), any(), any(), any()) } just Runs

        // When
        featureFlagService.updateWorkspaceFeatureFlags(featureFlagId, request)

        // Then
        verify {
            workspaceFeatureFlagRepository.saveAll(
                match<List<WorkspaceFeatureFlag>> { flags ->
                    flags.size == 2 && flags.all { it.isEnabled }
                }
            )
        }
    }

    @Test
    fun `should disable feature flag for specific workspaces`() {
        // Given
        val featureFlagId = UUID.randomUUID()
        val workspaceId = UUID.randomUUID()

        val featureFlag = FeatureFlag(
            id = featureFlagId,
            name = "test-flag",
            description = "Test flag",
            team = "test-team",
            rolloutPercentage = 50,
            regions = "WESTEUROPE"
        )

        val workspace = Workspace(
            id = workspaceId,
            name = "Workspace 1",
            type = "PRODUCTION",
            region = Region.WESTEUROPE
        )

        val workspaceFeatureFlag = WorkspaceFeatureFlag(
            id = UUID.randomUUID(),
            workspace = workspace,
            featureFlag = featureFlag,
            isEnabled = true
        )

        val request = UpdateWorkspaceFeatureFlagRequest(
            workspaceIds = listOf(workspaceId),
            enabled = false
        )

        every { featureFlagRepository.findById(featureFlagId) } returns Optional.of(featureFlag)
        every { workspaceRepository.findAllById(request.workspaceIds) } returns listOf(workspace)
        every {
            workspaceFeatureFlagRepository.findByFeatureFlagIdAndWorkspaceIdIn(featureFlagId, request.workspaceIds)
        } returns listOf(workspaceFeatureFlag)
        every { workspaceFeatureFlagRepository.findByFeatureFlag(featureFlag) } returns listOf(workspaceFeatureFlag)
        every { workspaceRepository.findByRegionIn(listOf("WESTEUROPE")) } returns listOf(workspace)
        every { workspaceFeatureFlagRepository.saveAll(any<List<WorkspaceFeatureFlag>>()) } returns listOf()
        every { featureFlagRepository.save(any()) } returns featureFlag
        every { auditLogService.logWorkspaceUpdate(any(), any(), any(), any()) } just Runs

        // When
        featureFlagService.updateWorkspaceFeatureFlags(featureFlagId, request)

        // Then
        verify {
            workspaceFeatureFlagRepository.saveAll(
                match<List<WorkspaceFeatureFlag>> { flags ->
                    flags.size == 1 && flags.all { !it.isEnabled }
                }
            )
        }
    }

    @Test
    fun `should throw ResourceNotFoundException when feature flag does not exist`() {
        // Given
        val featureFlagId = UUID.randomUUID()
        val request = UpdateWorkspaceFeatureFlagRequest(
            workspaceIds = listOf(UUID.randomUUID()),
            enabled = true
        )

        every { featureFlagRepository.findById(featureFlagId) } returns Optional.empty()

        // When/Then
        val exception = assertThrows<ResourceNotFoundException> {
            featureFlagService.updateWorkspaceFeatureFlags(featureFlagId, request)
        }

        assertEquals("Feature flag not found with id: $featureFlagId", exception.message)
        verify(exactly = 0) { workspaceFeatureFlagRepository.saveAll(any<List<WorkspaceFeatureFlag>>()) }
    }

    @Test
    fun `should throw ResourceNotFoundException when workspace does not exist`() {
        // Given
        val featureFlagId = UUID.randomUUID()
        val workspace1Id = UUID.randomUUID()
        val workspace2Id = UUID.randomUUID()

        val featureFlag = FeatureFlag(
            id = featureFlagId,
            name = "test-flag",
            description = "Test flag",
            team = "test-team",
            rolloutPercentage = 50,
            regions = "WESTEUROPE"
        )

        val workspace1 = Workspace(
            id = workspace1Id,
            name = "Workspace 1",
            type = "PRODUCTION",
            region = Region.WESTEUROPE
        )

        val request = UpdateWorkspaceFeatureFlagRequest(
            workspaceIds = listOf(workspace1Id, workspace2Id),
            enabled = true
        )

        every { featureFlagRepository.findById(featureFlagId) } returns Optional.of(featureFlag)
        every { workspaceRepository.findAllById(request.workspaceIds) } returns listOf(workspace1)

        // When/Then
        val exception = assertThrows<ResourceNotFoundException> {
            featureFlagService.updateWorkspaceFeatureFlags(featureFlagId, request)
        }

        assertEquals("One or more workspaces not found", exception.message)
        verify(exactly = 0) { workspaceFeatureFlagRepository.saveAll(any<List<WorkspaceFeatureFlag>>()) }
    }

    @Test
    fun `should throw IllegalArgumentException when no workspace-feature flag associations exist`() {
        // Given
        val featureFlagId = UUID.randomUUID()
        val workspaceId = UUID.randomUUID()

        val featureFlag = FeatureFlag(
            id = featureFlagId,
            name = "test-flag",
            description = "Test flag",
            team = "test-team",
            rolloutPercentage = 50,
            regions = "EASTUS"
        )

        val workspace = Workspace(
            id = workspaceId,
            name = "Workspace 1",
            type = "PRODUCTION",
            region = Region.WESTEUROPE
        )

        val request = UpdateWorkspaceFeatureFlagRequest(
            workspaceIds = listOf(workspaceId),
            enabled = true
        )

        every { featureFlagRepository.findById(featureFlagId) } returns Optional.of(featureFlag)
        every { workspaceRepository.findAllById(request.workspaceIds) } returns listOf(workspace)
        every { workspaceFeatureFlagRepository.findByFeatureFlag(featureFlag) } returns emptyList()
        every {
            workspaceFeatureFlagRepository.findByFeatureFlagIdAndWorkspaceIdIn(featureFlagId, request.workspaceIds)
        } returns emptyList()

        // When/Then
        val exception = assertThrows<IllegalArgumentException> {
            featureFlagService.updateWorkspaceFeatureFlags(featureFlagId, request)
        }

        assertTrue(exception.message!!.contains("No workspace-feature flag associations found"))
        verify(exactly = 0) { workspaceFeatureFlagRepository.saveAll(any<List<WorkspaceFeatureFlag>>()) }
    }

    @Test
    fun `should update multiple workspaces with mixed initial states`() {
        // Given
        val featureFlagId = UUID.randomUUID()
        val workspace1Id = UUID.randomUUID()
        val workspace2Id = UUID.randomUUID()

        val featureFlag = FeatureFlag(
            id = featureFlagId,
            name = "test-flag",
            description = "Test flag",
            team = "test-team",
            rolloutPercentage = 50,
            regions = "WESTEUROPE"
        )

        val workspace1 = Workspace(
            id = workspace1Id,
            name = "Workspace 1",
            type = "PRODUCTION",
            region = Region.WESTEUROPE
        )

        val workspace2 = Workspace(
            id = workspace2Id,
            name = "Workspace 2",
            type = "PRODUCTION",
            region = Region.WESTEUROPE
        )

        val workspaceFeatureFlag1 = WorkspaceFeatureFlag(
            id = UUID.randomUUID(),
            workspace = workspace1,
            featureFlag = featureFlag,
            isEnabled = true // Already enabled
        )

        val workspaceFeatureFlag2 = WorkspaceFeatureFlag(
            id = UUID.randomUUID(),
            workspace = workspace2,
            featureFlag = featureFlag,
            isEnabled = false // Currently disabled
        )

        val request = UpdateWorkspaceFeatureFlagRequest(
            workspaceIds = listOf(workspace1Id, workspace2Id),
            enabled = true
        )

        every { featureFlagRepository.findById(featureFlagId) } returns Optional.of(featureFlag)
        every { workspaceRepository.findAllById(request.workspaceIds) } returns listOf(workspace1, workspace2)
        every {
            workspaceFeatureFlagRepository.findByFeatureFlagIdAndWorkspaceIdIn(featureFlagId, request.workspaceIds)
        } returns listOf(workspaceFeatureFlag1, workspaceFeatureFlag2)
        every { workspaceFeatureFlagRepository.findByFeatureFlag(featureFlag) } returns listOf(workspaceFeatureFlag1, workspaceFeatureFlag2)
        every { workspaceRepository.findByRegionIn(listOf("WESTEUROPE")) } returns listOf(workspace1, workspace2)
        every { workspaceFeatureFlagRepository.saveAll(any<List<WorkspaceFeatureFlag>>()) } returns listOf()
        every { featureFlagRepository.save(any()) } returns featureFlag
        every { auditLogService.logWorkspaceUpdate(any(), any(), any(), any()) } just Runs

        // When
        featureFlagService.updateWorkspaceFeatureFlags(featureFlagId, request)

        // Then
        verify {
            workspaceFeatureFlagRepository.saveAll(
                match<List<WorkspaceFeatureFlag>> { flags ->
                    flags.size == 2 && flags.all { it.isEnabled }
                }
            )
        }
    }
}
