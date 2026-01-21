package com.featureflag.service

import com.featureflag.entity.FeatureFlag
import com.featureflag.entity.Region
import com.featureflag.entity.Workspace
import com.featureflag.entity.WorkspaceFeatureFlag
import com.featureflag.exception.ResourceNotFoundException
import com.featureflag.repository.WorkspaceFeatureFlagRepository
import com.featureflag.repository.WorkspaceRepository
import io.mockk.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import java.time.LocalDateTime
import java.util.*
import java.util.stream.Stream

class WorkspaceServiceTest {

    private lateinit var workspaceRepository: WorkspaceRepository
    private lateinit var workspaceFeatureFlagRepository: WorkspaceFeatureFlagRepository
    private lateinit var workspaceService: WorkspaceService

    @BeforeEach
    fun setup() {
        workspaceRepository = mockk()
        workspaceFeatureFlagRepository = mockk()
        workspaceService = WorkspaceService(
            workspaceRepository,
            workspaceFeatureFlagRepository
        )
    }

    @AfterEach
    fun tearDown() {
        clearAllMocks()
    }

    // ==================== getAllWorkspaces ====================

    @Test
    fun `getAllWorkspaces should return all workspaces`() {
        val workspaces = listOf(
            createWorkspace("workspace1", Region.WESTEUROPE),
            createWorkspace("workspace2", Region.EASTUS),
            createWorkspace("workspace3", Region.CANADACENTRAL)
        )

        every { workspaceRepository.findAll() } returns workspaces

        val result = workspaceService.getAllWorkspaces()

        assertEquals(3, result.size)
        assertEquals("workspace1", result[0].name)
        assertEquals("WESTEUROPE", result[0].region)
        assertEquals("workspace2", result[1].name)
        assertEquals("EASTUS", result[1].region)
        verify { workspaceRepository.findAll() }
    }

    @Test
    fun `getAllWorkspaces should return empty list when no workspaces exist`() {
        every { workspaceRepository.findAll() } returns emptyList()

        val result = workspaceService.getAllWorkspaces()

        assertTrue(result.isEmpty())
        verify { workspaceRepository.findAll() }
    }

    @ParameterizedTest
    @EnumSource(Region::class)
    fun `getAllWorkspaces should handle all region types`(region: Region) {
        val workspaces = listOf(createWorkspace("workspace", region))
        every { workspaceRepository.findAll() } returns workspaces

        val result = workspaceService.getAllWorkspaces()

        assertEquals(1, result.size)
        assertEquals(region.toString(), result[0].region)
    }

    // ==================== getAllWorkspacesPaginated ====================

    @ParameterizedTest
    @MethodSource("paginationParameters")
    fun `getAllWorkspacesPaginated should return paginated results`(page: Int, size: Int, totalElements: Long) {
        val workspaces = (1..size.coerceAtMost(10)).map { createWorkspace("workspace$it") }
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        val workspacePage = PageImpl(workspaces, pageable, totalElements)

        every { workspaceRepository.findAll(pageable) } returns workspacePage

        val result = workspaceService.getAllWorkspacesPaginated(page, size, null)

        assertEquals(workspaces.size, result.content.size)
        assertEquals(totalElements, result.totalElements)
        assertEquals(page, result.pageNumber)
        assertEquals(size, result.pageSize)
        verify { workspaceRepository.findAll(pageable) }
    }

    @Test
    fun `getAllWorkspacesPaginated should use search when provided`() {
        val searchTerm = "test"
        val workspaces = listOf(createWorkspace("test-workspace"))
        val pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"))
        val workspacePage = PageImpl(workspaces, pageable, 1)

        every { workspaceRepository.searchWorkspaces(searchTerm, pageable) } returns workspacePage

        val result = workspaceService.getAllWorkspacesPaginated(0, 20, searchTerm)

        assertEquals(1, result.content.size)
        verify { workspaceRepository.searchWorkspaces(searchTerm, pageable) }
        verify(exactly = 0) { workspaceRepository.findAll(any<org.springframework.data.domain.Pageable>()) }
    }

    @Test
    fun `getAllWorkspacesPaginated should return empty page when no workspaces match search`() {
        val searchTerm = "nonexistent"
        val pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"))
        val emptyPage = PageImpl<Workspace>(emptyList(), pageable, 0)

        every { workspaceRepository.searchWorkspaces(searchTerm, pageable) } returns emptyPage

        val result = workspaceService.getAllWorkspacesPaginated(0, 20, searchTerm)

        assertEquals(0, result.content.size)
        assertEquals(0, result.totalElements)
    }

    @Test
    fun `getAllWorkspacesPaginated should handle blank search term as no search`() {
        val workspaces = listOf(createWorkspace("workspace1"))
        val pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"))
        val workspacePage = PageImpl(workspaces, pageable, 1)

        every { workspaceRepository.findAll(pageable) } returns workspacePage

        val result = workspaceService.getAllWorkspacesPaginated(0, 20, "   ")

        verify { workspaceRepository.findAll(pageable) }
        verify(exactly = 0) { workspaceRepository.searchWorkspaces(any(), any()) }
    }

    // ==================== getEnabledFeatureFlagsForWorkspacePaginated ====================

    @Test
    fun `getEnabledFeatureFlagsForWorkspacePaginated should return enabled flags for workspace`() {
        val workspaceId = UUID.randomUUID()
        val workspace = createWorkspace("workspace1", Region.WESTEUROPE, workspaceId)
        val flag = createFeatureFlag("flag1", "team1")
        val wff = WorkspaceFeatureFlag(
            id = UUID.randomUUID(),
            workspace = workspace,
            featureFlag = flag,
            isEnabled = true,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
        val pageable = PageRequest.of(0, 100, Sort.by(Sort.Direction.ASC, "featureFlag.name"))
        val page = PageImpl(listOf(wff), pageable, 1)

        every { workspaceRepository.findById(workspaceId) } returns Optional.of(workspace)
        every { workspaceFeatureFlagRepository.findEnabledByWorkspaceIdWithFeatureFlagPaginated(workspaceId, any()) } returns page

        val result = workspaceService.getEnabledFeatureFlagsForWorkspacePaginated(workspaceId, 0, 100, null)

        assertEquals(1, result.content.size)
        assertEquals("flag1", result.content[0].name)
        verify { workspaceFeatureFlagRepository.findEnabledByWorkspaceIdWithFeatureFlagPaginated(workspaceId, any()) }
    }

    @Test
    fun `getEnabledFeatureFlagsForWorkspacePaginated should throw exception when workspace not found`() {
        val workspaceId = UUID.randomUUID()

        every { workspaceRepository.findById(workspaceId) } returns Optional.empty()

        val exception = assertThrows(ResourceNotFoundException::class.java) {
            workspaceService.getEnabledFeatureFlagsForWorkspacePaginated(workspaceId, 0, 100, null)
        }

        assertTrue(exception.message!!.contains("not found"))
        verify(exactly = 0) { workspaceFeatureFlagRepository.findEnabledByWorkspaceIdWithFeatureFlagPaginated(any(), any()) }
    }

    @Test
    fun `getEnabledFeatureFlagsForWorkspacePaginated should use search when provided`() {
        val workspaceId = UUID.randomUUID()
        val workspace = createWorkspace("workspace1", Region.WESTEUROPE, workspaceId)
        val searchTerm = "flag"
        val pageable = PageRequest.of(0, 100, Sort.by(Sort.Direction.ASC, "featureFlag.name"))
        val page = PageImpl<WorkspaceFeatureFlag>(emptyList(), pageable, 0)

        every { workspaceRepository.findById(workspaceId) } returns Optional.of(workspace)
        every { workspaceFeatureFlagRepository.findEnabledByWorkspaceIdWithFeatureFlagAndSearch(workspaceId, searchTerm, any()) } returns page

        val result = workspaceService.getEnabledFeatureFlagsForWorkspacePaginated(workspaceId, 0, 100, searchTerm)

        verify { workspaceFeatureFlagRepository.findEnabledByWorkspaceIdWithFeatureFlagAndSearch(workspaceId, searchTerm, any()) }
        verify(exactly = 0) { workspaceFeatureFlagRepository.findEnabledByWorkspaceIdWithFeatureFlagPaginated(any(), any()) }
    }

    @Test
    fun `getEnabledFeatureFlagsForWorkspacePaginated should return empty page when no flags enabled`() {
        val workspaceId = UUID.randomUUID()
        val workspace = createWorkspace("workspace1", Region.WESTEUROPE, workspaceId)
        val pageable = PageRequest.of(0, 100, Sort.by(Sort.Direction.ASC, "featureFlag.name"))
        val emptyPage = PageImpl<WorkspaceFeatureFlag>(emptyList(), pageable, 0)

        every { workspaceRepository.findById(workspaceId) } returns Optional.of(workspace)
        every { workspaceFeatureFlagRepository.findEnabledByWorkspaceIdWithFeatureFlagPaginated(workspaceId, any()) } returns emptyPage

        val result = workspaceService.getEnabledFeatureFlagsForWorkspacePaginated(workspaceId, 0, 100, null)

        assertEquals(0, result.content.size)
        assertEquals(0, result.totalElements)
    }

    // ==================== Helper Methods ====================

    private fun createWorkspace(
        name: String,
        region: Region = Region.WESTEUROPE,
        id: UUID = UUID.randomUUID()
    ): Workspace {
        return Workspace(
            id = id,
            name = name,
            type = "PRODUCTION",
            region = region,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
    }

    private fun createFeatureFlag(
        name: String,
        team: String,
        id: UUID = UUID.randomUUID()
    ): FeatureFlag {
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

    companion object {
        @JvmStatic
        fun paginationParameters(): Stream<Arguments> {
            return Stream.of(
                Arguments.of(0, 10, 100L),
                Arguments.of(0, 20, 50L),
                Arguments.of(0, 50, 200L),
                Arguments.of(0, 100, 150L),
                Arguments.of(1, 20, 50L),
                Arguments.of(5, 10, 100L)
            )
        }
    }
}
