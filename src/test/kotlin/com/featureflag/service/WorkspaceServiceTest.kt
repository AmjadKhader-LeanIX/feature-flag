package com.featureflag.service

import com.featureflag.entity.Workspace
import com.featureflag.exception.ResourceNotFoundException
import com.featureflag.repository.WorkspaceRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDateTime
import java.util.*

class WorkspaceServiceTest {

    private lateinit var workspaceRepository: WorkspaceRepository
    private lateinit var workspaceService: WorkspaceService

    @BeforeEach
    fun setUp() {
        workspaceRepository = mockk()
        workspaceService = WorkspaceService(workspaceRepository)
    }

    @Test
    fun `should get all workspaces`() {
        val workspace1 = Workspace(
            id = UUID.randomUUID(),
            name = "Workspace 1",
            type = "development",
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
        val workspace2 = Workspace(
            id = UUID.randomUUID(),
            name = "Workspace 2",
            type = "production",
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
        every { workspaceRepository.findAll() } returns listOf(workspace1, workspace2)

        val result = workspaceService.getAllWorkspaces()

        assertEquals(2, result.size)
        assertEquals("Workspace 1", result[0].name)
        assertEquals("Workspace 2", result[1].name)
        verify { workspaceRepository.findAll() }
    }

    @Test
    fun `should get workspace by id`() {
        val workspaceId = UUID.randomUUID()
        val workspace = Workspace(
            id = workspaceId,
            name = "Test Workspace",
            type = "test",
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
        every { workspaceRepository.findById(workspaceId) } returns Optional.of(workspace)

        val result = workspaceService.getWorkspaceById(workspaceId)

        assertEquals(workspaceId, result.id)
        assertEquals("Test Workspace", result.name)
        verify { workspaceRepository.findById(workspaceId) }
    }

    @Test
    fun `should throw exception when workspace not found`() {
        val workspaceId = UUID.randomUUID()
        every { workspaceRepository.findById(workspaceId) } returns Optional.empty()

        assertThrows<ResourceNotFoundException> {
            workspaceService.getWorkspaceById(workspaceId)
        }
        verify { workspaceRepository.findById(workspaceId) }
    }

    @Test
    fun `should return empty list when no workspaces exist`() {
        every { workspaceRepository.findAll() } returns emptyList()

        val result = workspaceService.getAllWorkspaces()

        assertEquals(0, result.size)
        verify { workspaceRepository.findAll() }
    }

    @Test
    fun `should handle workspace with null description`() {
        val workspaceId = UUID.randomUUID()
        val workspace = Workspace(
            id = workspaceId,
            name = "Test Workspace",
            type = "test",
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
        every { workspaceRepository.findById(workspaceId) } returns Optional.of(workspace)

        val result = workspaceService.getWorkspaceById(workspaceId)

        assertEquals(workspaceId, result.id)
        assertEquals("Test Workspace", result.name)
        verify { workspaceRepository.findById(workspaceId) }
    }

    @Test
    fun `should handle workspace with special characters in name`() {
        val workspaceId = UUID.randomUUID()
        val specialName = "Test-Workspace_123 & Co!"
        val workspace = Workspace(
            id = workspaceId,
            name = specialName,
            type = "test",
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
        every { workspaceRepository.findById(workspaceId) } returns Optional.of(workspace)

        val result = workspaceService.getWorkspaceById(workspaceId)

        assertEquals(specialName, result.name)
        verify { workspaceRepository.findById(workspaceId) }
    }

    @Test
    fun `should verify error message content for not found exception`() {
        val workspaceId = UUID.randomUUID()
        every { workspaceRepository.findById(workspaceId) } returns Optional.empty()

        val exception = assertThrows<ResourceNotFoundException> {
            workspaceService.getWorkspaceById(workspaceId)
        }
        assertEquals("Workspace not found with id: $workspaceId", exception.message)
        verify { workspaceRepository.findById(workspaceId) }
    }
}
