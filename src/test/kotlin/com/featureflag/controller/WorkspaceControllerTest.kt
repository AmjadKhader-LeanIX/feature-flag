package com.featureflag.controller

import com.featureflag.dto.WorkspaceDto
import com.featureflag.exception.ResourceNotFoundException
import com.featureflag.service.WorkspaceService
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.*

@WebMvcTest(WorkspaceController::class)
@ActiveProfiles("test")
class WorkspaceControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockkBean
    private lateinit var workspaceService: WorkspaceService

    @Test
    fun `should get all workspaces`() {
        val workspaces = listOf(
            WorkspaceDto(
                id = UUID.randomUUID(),
                name = "Test Workspace 1",
                type = "Development",
                createdAt = null,
                updatedAt = null
            ),
            WorkspaceDto(
                id = UUID.randomUUID(),
                name = "Test Workspace 2",
                type = "Production",
                createdAt = null,
                updatedAt = null
            )
        )
        every { workspaceService.getAllWorkspaces() } returns workspaces

        mockMvc.perform(get("/api/workspaces"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
    }

    @Test
    fun `should get workspace by id`() {
        val workspaceId = UUID.randomUUID()
        val workspace = WorkspaceDto(
            id = workspaceId,
            name = "Test Workspace",
            type = "Development",
            createdAt = null,
            updatedAt = null
        )
        every { workspaceService.getWorkspaceById(workspaceId) } returns workspace

        mockMvc.perform(get("/api/workspaces/$workspaceId"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.id").value(workspaceId.toString()))
            .andExpect(jsonPath("$.name").value("Test Workspace"))
            .andExpect(jsonPath("$.type").value("Development"))

        verify { workspaceService.getWorkspaceById(workspaceId) }
    }

    @Test
    fun `should return 404 when workspace not found`() {
        val workspaceId = UUID.randomUUID()
        every { workspaceService.getWorkspaceById(workspaceId) } throws ResourceNotFoundException("Workspace not found with id: $workspaceId")

        mockMvc.perform(get("/api/workspaces/$workspaceId"))
            .andExpect(status().isNotFound)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.error").value("Not Found"))
            .andExpect(jsonPath("$.message").value("Workspace not found with id: $workspaceId"))

        verify { workspaceService.getWorkspaceById(workspaceId) }
    }

    @Test
    fun `should return empty list when no workspaces exist`() {
        every { workspaceService.getAllWorkspaces() } returns emptyList()

        mockMvc.perform(get("/api/workspaces"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.length()").value(0))

        verify { workspaceService.getAllWorkspaces() }
    }

    @Test
    fun `should handle workspace with special characters in name`() {
        val workspaceId = UUID.randomUUID()
        val specialName = "Test-Workspace_123 & Co!"
        val workspace = WorkspaceDto(
            id = workspaceId,
            name = specialName,
            type = "Development",
            createdAt = null,
            updatedAt = null
        )
        every { workspaceService.getWorkspaceById(workspaceId) } returns workspace

        mockMvc.perform(get("/api/workspaces/$workspaceId"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value(specialName))

        verify { workspaceService.getWorkspaceById(workspaceId) }
    }

    @Test
    fun `should handle invalid UUID format`() {
        mockMvc.perform(get("/api/workspaces/invalid-uuid"))
            .andExpect(status().isInternalServerError)
            .andExpect(jsonPath("$.status").value(500))
    }

    @Test
    fun `should include proper JSON response structure for getAllWorkspaces`() {
        val workspace1 = WorkspaceDto(
            id = UUID.randomUUID(),
            name = "Workspace 1",
            type = "Development",
            createdAt = null,
            updatedAt = null
        )
        val workspace2 = WorkspaceDto(
            id = UUID.randomUUID(),
            name = "Workspace 2",
            type = "Production",
            createdAt = null,
            updatedAt = null
        )
        every { workspaceService.getAllWorkspaces() } returns listOf(workspace1, workspace2)

        mockMvc.perform(get("/api/workspaces"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].name").value("Workspace 1"))
            .andExpect(jsonPath("$[0].type").value("Development"))
            .andExpect(jsonPath("$[1].name").value("Workspace 2"))
            .andExpect(jsonPath("$[1].type").value("Production"))

        verify { workspaceService.getAllWorkspaces() }
    }
}
