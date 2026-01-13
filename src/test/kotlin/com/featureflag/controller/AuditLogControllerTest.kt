package com.featureflag.controller

import com.featureflag.dto.AuditLogDto
import com.featureflag.entity.AuditOperation
import com.featureflag.service.AuditLogService
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDateTime
import java.util.*

@WebMvcTest(AuditLogController::class)
class AuditLogControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockkBean
    private lateinit var auditLogService: AuditLogService

    @Test
    fun `getAllAuditLogs should return all audit logs when no filters provided`() {
        val auditLogs = listOf(
            createAuditLogDto(name = "flag1", operation = AuditOperation.CREATE),
            createAuditLogDto(name = "flag2", operation = AuditOperation.UPDATE)
        )

        every { auditLogService.getAllAuditLogs() } returns auditLogs

        mockMvc.perform(get("/api/audit-logs"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].featureFlagName").value("flag1"))
            .andExpect(jsonPath("$[0].operation").value("CREATE"))
            .andExpect(jsonPath("$[1].featureFlagName").value("flag2"))
            .andExpect(jsonPath("$[1].operation").value("UPDATE"))
    }

    @Test
    fun `getAllAuditLogs should filter by featureFlagId`() {
        val featureFlagId = UUID.randomUUID()
        val auditLogs = listOf(
            createAuditLogDto(featureFlagId = featureFlagId, name = "test-flag", operation = AuditOperation.CREATE)
        )

        every { auditLogService.getAuditLogsByFeatureFlagId(featureFlagId) } returns auditLogs

        mockMvc.perform(get("/api/audit-logs?featureFlagId=$featureFlagId"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].featureFlagId").value(featureFlagId.toString()))
    }

    @Test
    fun `getAllAuditLogs should filter by team`() {
        val team = "test-team"
        val auditLogs = listOf(
            createAuditLogDto(name = "flag1", team = team, operation = AuditOperation.CREATE)
        )

        every { auditLogService.getAuditLogsByTeam(team) } returns auditLogs

        mockMvc.perform(get("/api/audit-logs?team=$team"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].team").value(team))
    }

    @Test
    fun `getAllAuditLogs should filter by operation`() {
        val operation = AuditOperation.UPDATE
        val auditLogs = listOf(
            createAuditLogDto(name = "flag1", operation = operation)
        )

        every { auditLogService.getAuditLogsByOperation(operation) } returns auditLogs

        mockMvc.perform(get("/api/audit-logs?operation=UPDATE"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].operation").value("UPDATE"))
    }

    @Test
    fun `getAllAuditLogs should filter by featureFlagId and operation`() {
        val featureFlagId = UUID.randomUUID()
        val operation = AuditOperation.UPDATE
        val auditLogs = listOf(
            createAuditLogDto(featureFlagId = featureFlagId, name = "test-flag", operation = operation)
        )

        every {
            auditLogService.getAuditLogsByFeatureFlagIdAndOperation(featureFlagId, operation)
        } returns auditLogs

        mockMvc.perform(get("/api/audit-logs?featureFlagId=$featureFlagId&operation=UPDATE"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].featureFlagId").value(featureFlagId.toString()))
            .andExpect(jsonPath("$[0].operation").value("UPDATE"))
    }

    @Test
    fun `getAuditLogsByFeatureFlagId should return audit logs for specific feature flag`() {
        val featureFlagId = UUID.randomUUID()
        val auditLogs = listOf(
            createAuditLogDto(featureFlagId = featureFlagId, name = "test-flag", operation = AuditOperation.CREATE),
            createAuditLogDto(featureFlagId = featureFlagId, name = "test-flag", operation = AuditOperation.UPDATE)
        )

        every { auditLogService.getAuditLogsByFeatureFlagId(featureFlagId) } returns auditLogs

        mockMvc.perform(get("/api/audit-logs/feature-flag/$featureFlagId"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].featureFlagId").value(featureFlagId.toString()))
            .andExpect(jsonPath("$[1].featureFlagId").value(featureFlagId.toString()))
    }

    @Test
    fun `getAuditLogsByTeam should return audit logs for specific team`() {
        val team = "backend-team"
        val auditLogs = listOf(
            createAuditLogDto(name = "flag1", team = team, operation = AuditOperation.CREATE),
            createAuditLogDto(name = "flag2", team = team, operation = AuditOperation.UPDATE)
        )

        every { auditLogService.getAuditLogsByTeam(team) } returns auditLogs

        mockMvc.perform(get("/api/audit-logs/team/$team"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].team").value(team))
            .andExpect(jsonPath("$[1].team").value(team))
    }

    @Test
    fun `getAuditLogsByOperation should return audit logs for specific operation`() {
        val operation = AuditOperation.DELETE
        val auditLogs = listOf(
            createAuditLogDto(name = "flag1", operation = operation),
            createAuditLogDto(name = "flag2", operation = operation)
        )

        every { auditLogService.getAuditLogsByOperation(operation) } returns auditLogs

        mockMvc.perform(get("/api/audit-logs/operation/DELETE"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].operation").value("DELETE"))
            .andExpect(jsonPath("$[1].operation").value("DELETE"))
    }

    @Test
    fun `getAllAuditLogs should return empty list when no audit logs exist`() {
        every { auditLogService.getAllAuditLogs() } returns emptyList()

        mockMvc.perform(get("/api/audit-logs"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(0))
    }

    @Test
    fun `getAuditLogsByFeatureFlagId should return empty list when no logs exist for flag`() {
        val featureFlagId = UUID.randomUUID()
        every { auditLogService.getAuditLogsByFeatureFlagId(featureFlagId) } returns emptyList()

        mockMvc.perform(get("/api/audit-logs/feature-flag/$featureFlagId"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(0))
    }

    @Test
    fun `audit logs should include oldValues and newValues`() {
        val oldValues = mapOf("rolloutPercentage" to 25)
        val newValues = mapOf("rolloutPercentage" to 75)
        val auditLog = AuditLogDto(
            id = UUID.randomUUID(),
            featureFlagId = UUID.randomUUID(),
            featureFlagName = "test-flag",
            operation = AuditOperation.UPDATE,
            team = "test-team",
            oldValues = oldValues,
            newValues = newValues,
            changedBy = "test-user",
            timestamp = LocalDateTime.now()
        )

        every { auditLogService.getAllAuditLogs() } returns listOf(auditLog)

        mockMvc.perform(get("/api/audit-logs"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].oldValues.rolloutPercentage").value(25))
            .andExpect(jsonPath("$[0].newValues.rolloutPercentage").value(75))
    }

    @Test
    fun `audit logs should include changedBy field`() {
        val auditLog = createAuditLogDto(
            name = "test-flag",
            operation = AuditOperation.CREATE,
            changedBy = "john.doe@example.com"
        )

        every { auditLogService.getAllAuditLogs() } returns listOf(auditLog)

        mockMvc.perform(get("/api/audit-logs"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].changedBy").value("john.doe@example.com"))
    }

    @Test
    fun `audit logs should handle null changedBy`() {
        val auditLog = createAuditLogDto(
            name = "test-flag",
            operation = AuditOperation.CREATE,
            changedBy = null
        )

        every { auditLogService.getAllAuditLogs() } returns listOf(auditLog)

        mockMvc.perform(get("/api/audit-logs"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].changedBy").doesNotExist())
    }

    private fun createAuditLogDto(
        featureFlagId: UUID? = UUID.randomUUID(),
        name: String,
        team: String = "test-team",
        operation: AuditOperation,
        changedBy: String? = "test-user"
    ): AuditLogDto {
        return AuditLogDto(
            id = UUID.randomUUID(),
            featureFlagId = featureFlagId,
            featureFlagName = name,
            operation = operation,
            team = team,
            oldValues = if (operation == AuditOperation.DELETE) mapOf("name" to name) else null,
            newValues = if (operation != AuditOperation.DELETE) mapOf("name" to name) else null,
            changedBy = changedBy,
            timestamp = LocalDateTime.now()
        )
    }
}
