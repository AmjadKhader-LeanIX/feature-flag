package com.featureflag.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.featureflag.entity.AuditOperation
import com.featureflag.entity.FeatureFlag
import com.featureflag.entity.FeatureFlagAuditLog
import com.featureflag.repository.FeatureFlagAuditLogRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.*

class AuditLogServiceTest {

    private lateinit var auditLogRepository: FeatureFlagAuditLogRepository
    private lateinit var objectMapper: ObjectMapper
    private lateinit var auditLogService: AuditLogService

    @BeforeEach
    fun setup() {
        auditLogRepository = mockk(relaxed = true)
        objectMapper = ObjectMapper()
        auditLogService = AuditLogService(auditLogRepository, objectMapper)
    }

    @Test
    fun `logCreate should create audit log with correct values`() {
        val featureFlag = FeatureFlag(
            id = UUID.randomUUID(),
            name = "test-flag",
            description = "Test description",
            team = "test-team",
            rolloutPercentage = 0,
            regions = "ALL"
        )

        val auditLogSlot = slot<FeatureFlagAuditLog>()
        every { auditLogRepository.save(capture(auditLogSlot)) } returns mockk()

        auditLogService.logCreate(featureFlag, "test-user")

        verify(exactly = 1) { auditLogRepository.save(any()) }

        val capturedLog = auditLogSlot.captured
        assertEquals(featureFlag.id, capturedLog.featureFlagId)
        assertEquals(featureFlag.name, capturedLog.featureFlagName)
        assertEquals(AuditOperation.CREATE, capturedLog.operation)
        assertEquals(featureFlag.team, capturedLog.team)
        assertNull(capturedLog.oldValues)
        assertNotNull(capturedLog.newValues)
        assertEquals("test-user", capturedLog.changedBy)

        val newValuesMap = objectMapper.readValue(
            capturedLog.newValues,
            object : com.fasterxml.jackson.core.type.TypeReference<Map<String, Any?>>() {}
        )
        assertEquals(featureFlag.name, newValuesMap["name"])
        assertEquals(featureFlag.description, newValuesMap["description"])
        assertEquals(featureFlag.team, newValuesMap["team"])
        assertEquals(featureFlag.rolloutPercentage, newValuesMap["rolloutPercentage"])
    }

    @Test
    fun `logCreate should use system as default changedBy`() {
        val featureFlag = FeatureFlag(
            id = UUID.randomUUID(),
            name = "test-flag",
            description = "Test description",
            team = "test-team",
            rolloutPercentage = 0,
            regions = "ALL"
        )

        val auditLogSlot = slot<FeatureFlagAuditLog>()
        every { auditLogRepository.save(capture(auditLogSlot)) } returns mockk()

        auditLogService.logCreate(featureFlag)

        val capturedLog = auditLogSlot.captured
        assertEquals("system", capturedLog.changedBy)
    }

    @Test
    fun `logUpdate should create audit log with old and new rollout percentages`() {
        val featureFlagId = UUID.randomUUID()
        val featureFlag = FeatureFlag(
            id = featureFlagId,
            name = "test-flag",
            description = "Test description",
            team = "test-team",
            rolloutPercentage = 50,
            regions = "ALL"
        )

        val auditLogSlot = slot<FeatureFlagAuditLog>()
        every { auditLogRepository.save(capture(auditLogSlot)) } returns mockk()

        auditLogService.logUpdate(25, 50, featureFlag, "test-user")

        verify(exactly = 1) { auditLogRepository.save(any()) }

        val capturedLog = auditLogSlot.captured
        assertEquals(featureFlagId, capturedLog.featureFlagId)
        assertEquals(featureFlag.name, capturedLog.featureFlagName)
        assertEquals(AuditOperation.UPDATE, capturedLog.operation)
        assertEquals(featureFlag.team, capturedLog.team)
        assertNotNull(capturedLog.oldValues)
        assertNotNull(capturedLog.newValues)
        assertEquals("test-user", capturedLog.changedBy)

        val oldValuesMap = objectMapper.readValue(
            capturedLog.oldValues,
            object : com.fasterxml.jackson.core.type.TypeReference<Map<String, Any?>>() {}
        )
        assertEquals(25, oldValuesMap["rolloutPercentage"])

        val newValuesMap = objectMapper.readValue(
            capturedLog.newValues,
            object : com.fasterxml.jackson.core.type.TypeReference<Map<String, Any?>>() {}
        )
        assertEquals(50, newValuesMap["rolloutPercentage"])
    }

    @Test
    fun `logDelete should create audit log with old values and null new values`() {
        val featureFlag = FeatureFlag(
            id = UUID.randomUUID(),
            name = "test-flag",
            description = "Test description",
            team = "test-team",
            rolloutPercentage = 75,
            regions = "WESTEUROPE,EASTUS"
        )

        val auditLogSlot = slot<FeatureFlagAuditLog>()
        every { auditLogRepository.save(capture(auditLogSlot)) } returns mockk()

        auditLogService.logDelete(featureFlag, "test-user")

        verify(exactly = 1) { auditLogRepository.save(any()) }

        val capturedLog = auditLogSlot.captured
        assertEquals(featureFlag.id, capturedLog.featureFlagId)
        assertEquals(featureFlag.name, capturedLog.featureFlagName)
        assertEquals(AuditOperation.DELETE, capturedLog.operation)
        assertEquals(featureFlag.team, capturedLog.team)
        assertNotNull(capturedLog.oldValues)
        assertNull(capturedLog.newValues)
        assertEquals("test-user", capturedLog.changedBy)

        val oldValuesMap = objectMapper.readValue(
            capturedLog.oldValues,
            object : com.fasterxml.jackson.core.type.TypeReference<Map<String, Any?>>() {}
        )
        assertEquals(featureFlag.name, oldValuesMap["name"])
        assertEquals(featureFlag.description, oldValuesMap["description"])
        assertEquals(featureFlag.team, oldValuesMap["team"])
        assertEquals(featureFlag.rolloutPercentage, oldValuesMap["rolloutPercentage"])
    }

    @Test
    fun `getAllAuditLogs should return all audit logs ordered by timestamp desc`() {
        val auditLog1 = createAuditLog(
            name = "flag1",
            operation = AuditOperation.CREATE,
            timestamp = LocalDateTime.now().minusHours(2)
        )
        val auditLog2 = createAuditLog(
            name = "flag2",
            operation = AuditOperation.UPDATE,
            timestamp = LocalDateTime.now().minusHours(1)
        )

        every { auditLogRepository.findAllByOrderByTimestampDesc() } returns listOf(auditLog2, auditLog1)

        val result = auditLogService.getAllAuditLogs()

        assertEquals(2, result.size)
        assertEquals("flag2", result[0].featureFlagName)
        assertEquals("flag1", result[1].featureFlagName)
        assertEquals(AuditOperation.UPDATE, result[0].operation)
        assertEquals(AuditOperation.CREATE, result[1].operation)
    }

    @Test
    fun `getAuditLogsByFeatureFlagId should return audit logs for specific feature flag`() {
        val featureFlagId = UUID.randomUUID()
        val auditLog1 = createAuditLog(
            featureFlagId = featureFlagId,
            name = "test-flag",
            operation = AuditOperation.CREATE
        )
        val auditLog2 = createAuditLog(
            featureFlagId = featureFlagId,
            name = "test-flag",
            operation = AuditOperation.UPDATE
        )

        every { auditLogRepository.findByFeatureFlagIdOrderByTimestampDesc(featureFlagId) } returns
            listOf(auditLog2, auditLog1)

        val result = auditLogService.getAuditLogsByFeatureFlagId(featureFlagId)

        assertEquals(2, result.size)
        assertEquals(featureFlagId, result[0].featureFlagId)
        assertEquals(featureFlagId, result[1].featureFlagId)
        assertEquals(AuditOperation.UPDATE, result[0].operation)
        assertEquals(AuditOperation.CREATE, result[1].operation)
    }

    @Test
    fun `getAuditLogsByTeam should return audit logs for specific team`() {
        val team = "test-team"
        val auditLog1 = createAuditLog(team = team, name = "flag1", operation = AuditOperation.CREATE)
        val auditLog2 = createAuditLog(team = team, name = "flag2", operation = AuditOperation.UPDATE)

        every { auditLogRepository.findByTeamOrderByTimestampDesc(team) } returns listOf(auditLog2, auditLog1)

        val result = auditLogService.getAuditLogsByTeam(team)

        assertEquals(2, result.size)
        assertEquals(team, result[0].team)
        assertEquals(team, result[1].team)
    }

    @Test
    fun `getAuditLogsByOperation should return audit logs for specific operation`() {
        val operation = AuditOperation.UPDATE
        val auditLog1 = createAuditLog(name = "flag1", operation = operation)
        val auditLog2 = createAuditLog(name = "flag2", operation = operation)

        every { auditLogRepository.findByOperationOrderByTimestampDesc(operation) } returns
            listOf(auditLog2, auditLog1)

        val result = auditLogService.getAuditLogsByOperation(operation)

        assertEquals(2, result.size)
        assertEquals(operation, result[0].operation)
        assertEquals(operation, result[1].operation)
    }

    @Test
    fun `getAuditLogsByFeatureFlagIdAndOperation should return filtered audit logs`() {
        val featureFlagId = UUID.randomUUID()
        val operation = AuditOperation.UPDATE
        val auditLog = createAuditLog(
            featureFlagId = featureFlagId,
            name = "test-flag",
            operation = operation
        )

        every {
            auditLogRepository.findByFeatureFlagIdAndOperationOrderByTimestampDesc(featureFlagId, operation)
        } returns listOf(auditLog)

        val result = auditLogService.getAuditLogsByFeatureFlagIdAndOperation(featureFlagId, operation)

        assertEquals(1, result.size)
        assertEquals(featureFlagId, result[0].featureFlagId)
        assertEquals(operation, result[0].operation)
    }

    @Test
    fun `toDto should correctly deserialize JSON values`() {
        val newValuesJson = """{"name":"test-flag","team":"test-team","rolloutPercentage":50}"""
        val auditLog = FeatureFlagAuditLog(
            id = UUID.randomUUID(),
            featureFlagId = UUID.randomUUID(),
            featureFlagName = "test-flag",
            operation = AuditOperation.CREATE,
            team = "test-team",
            oldValues = null,
            newValues = newValuesJson,
            changedBy = "test-user",
            timestamp = LocalDateTime.now()
        )

        every { auditLogRepository.findAllByOrderByTimestampDesc() } returns listOf(auditLog)

        val result = auditLogService.getAllAuditLogs()

        assertEquals(1, result.size)
        assertNotNull(result[0].newValues)
        assertEquals("test-flag", result[0].newValues!!["name"])
        assertEquals("test-team", result[0].newValues!!["team"])
        assertEquals(50, result[0].newValues!!["rolloutPercentage"])
        assertNull(result[0].oldValues)
    }

    @Test
    fun `toDto should handle both old and new values`() {
        val oldValuesJson = """{"rolloutPercentage":25}"""
        val newValuesJson = """{"rolloutPercentage":75}"""
        val auditLog = FeatureFlagAuditLog(
            id = UUID.randomUUID(),
            featureFlagId = UUID.randomUUID(),
            featureFlagName = "test-flag",
            operation = AuditOperation.UPDATE,
            team = "test-team",
            oldValues = oldValuesJson,
            newValues = newValuesJson,
            changedBy = "test-user",
            timestamp = LocalDateTime.now()
        )

        every { auditLogRepository.findAllByOrderByTimestampDesc() } returns listOf(auditLog)

        val result = auditLogService.getAllAuditLogs()

        assertEquals(1, result.size)
        assertNotNull(result[0].oldValues)
        assertNotNull(result[0].newValues)
        assertEquals(25, result[0].oldValues!!["rolloutPercentage"])
        assertEquals(75, result[0].newValues!!["rolloutPercentage"])
    }

    @Test
    fun `getAllAuditLogs should return empty list when no logs exist`() {
        every { auditLogRepository.findAllByOrderByTimestampDesc() } returns emptyList()

        val result = auditLogService.getAllAuditLogs()

        assertTrue(result.isEmpty())
    }

    @Test
    fun `getAuditLogsByFeatureFlagId should return empty list when no logs exist for flag`() {
        val featureFlagId = UUID.randomUUID()
        every { auditLogRepository.findByFeatureFlagIdOrderByTimestampDesc(featureFlagId) } returns emptyList()

        val result = auditLogService.getAuditLogsByFeatureFlagId(featureFlagId)

        assertTrue(result.isEmpty())
    }

    private fun createAuditLog(
        featureFlagId: UUID? = UUID.randomUUID(),
        name: String,
        operation: AuditOperation,
        team: String = "test-team",
        timestamp: LocalDateTime = LocalDateTime.now()
    ): FeatureFlagAuditLog {
        val valuesJson = """{"name":"$name","team":"$team"}"""
        return FeatureFlagAuditLog(
            id = UUID.randomUUID(),
            featureFlagId = featureFlagId,
            featureFlagName = name,
            operation = operation,
            team = team,
            oldValues = if (operation == AuditOperation.DELETE) valuesJson else null,
            newValues = if (operation != AuditOperation.DELETE) valuesJson else null,
            changedBy = "test-user",
            timestamp = timestamp
        )
    }
}
