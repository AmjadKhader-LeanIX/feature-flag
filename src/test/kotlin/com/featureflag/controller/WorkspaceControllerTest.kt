package com.featureflag.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.featureflag.dto.CreateWorkspaceRequest
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

@WebMvcTest(WorkspaceController::class)
@ActiveProfiles("test")
class WorkspaceControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Test
    fun `should create workspace successfully`() {
        val request = CreateWorkspaceRequest("Test Workspace", "development")
        val requestJson = objectMapper.writeValueAsString(request)

        mockMvc.perform(
            post("/api/workspaces")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson)
        )
            .andExpect(status().isCreated)
    }

    @Test
    fun `should return bad request for invalid workspace creation`() {
        val request = CreateWorkspaceRequest("", null) // Empty name
        val requestJson = objectMapper.writeValueAsString(request)

        mockMvc.perform(
            post("/api/workspaces")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson)
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `should get all workspaces`() {
        mockMvc.perform(get("/api/workspaces"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
    }


}
