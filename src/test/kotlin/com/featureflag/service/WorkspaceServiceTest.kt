package com.featureflag.service

import com.featureflag.dto.CreateWorkspaceRequest
import com.featureflag.dto.UpdateWorkspaceRequest
import com.featureflag.entity.Workspace
import com.featureflag.exception.ResourceNotFoundException
import com.featureflag.repository.WorkspaceRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.*
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
        // Given
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

        // When
        val result = workspaceService.getAllWorkspaces()

        // Then
        assertEquals(2, result.size)
        assertEquals("Workspace 1", result[0].name)
        assertEquals("Workspace 2", result[1].name)
        verify { workspaceRepository.findAll() }
    }

    @Test
    fun `should get workspace by id`() {
        // Given
        val workspaceId = UUID.randomUUID()
        val workspace = Workspace(
            id = workspaceId,
            name = "Test Workspace",
            type = "test",
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
        every { workspaceRepository.findById(workspaceId) } returns Optional.of(workspace)

        // When
        val result = workspaceService.getWorkspaceById(workspaceId)

        // Then
        assertEquals(workspaceId, result.id)
        assertEquals("Test Workspace", result.name)
        verify { workspaceRepository.findById(workspaceId) }
    }

    @Test
    fun `should throw exception when workspace not found`() {
        // Given
        val workspaceId = UUID.randomUUID()
        every { workspaceRepository.findById(workspaceId) } returns Optional.empty()

        // When & Then
        assertThrows<ResourceNotFoundException> {
            workspaceService.getWorkspaceById(workspaceId)
        }
        verify { workspaceRepository.findById(workspaceId) }
    }

    @Test
    fun `should create workspace successfully`() {
        // Given
        val request = CreateWorkspaceRequest("New Workspace", "development")
        val savedWorkspace = Workspace(
            id = UUID.randomUUID(),
            name = "New Workspace",
            type = "development",
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
        every { workspaceRepository.existsByName("New Workspace") } returns false
        every { workspaceRepository.save(any()) } returns savedWorkspace

        // When
        val result = workspaceService.createWorkspace(request)

        // Then
        assertEquals("New Workspace", result.name)
        assertNotNull(result.id)
        verify { workspaceRepository.existsByName("New Workspace") }
        verify { workspaceRepository.save(any()) }
    }

    @Test
    fun `should throw exception when creating workspace with duplicate name`() {
        // Given
        val request = CreateWorkspaceRequest("Existing Workspace", "production")
        every { workspaceRepository.existsByName("Existing Workspace") } returns true

        // When & Then
        assertThrows<IllegalArgumentException> {
            workspaceService.createWorkspace(request)
        }
        verify { workspaceRepository.existsByName("Existing Workspace") }

}
    }
