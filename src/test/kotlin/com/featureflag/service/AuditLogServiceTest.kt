package com.featureflag.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.featureflag.entity.AuditOperation
import com.featureflag.entity.FeatureFlag
import com.featureflag.entity.FeatureFlagAuditLog
import com.featureflag.repository.FeatureFlagAuditLogRepository
import io.mockk.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import java.time.LocalDateTime
import java.util.*
import java.util.stream.Stream

class AuditLogServiceTest {

    private lateinit var auditLogRepository: FeatureFlagAuditLogRepository
    private lateinit var objectMapper: ObjectMapper
    private lateinit var auditLogService: AuditLogService

    @BeforeEach
    fun setup() {
        auditLogRepository = mockk()
        objectMapper = ObjectMapper()
        auditLogService = AuditLogService(auditLogRepository, objectMapper)
    }

    @AfterEach
    fun tearDown() {
        clearAllMocks()
    }

    // ==================== logCreate ====================

    @Test
    fun `logCreate should create audit log with all feature flag fields`() {
        val featureFlag = createFeatureFlag("flag1", "team1", 50)
        val auditLogSlot = slot<FeatureFlagAuditLog>()

        every { auditLogRepository.save(capture(auditLogSlot)) } answers { auditLogSlot.captured }

        auditLogService.logCreate(featureFlag, "user@example.com")

        verify { auditLogRepository.save(any()) }
        val capturedLog = auditLogSlot.captured
        assertEquals(featureFlag.id, capturedLog.featureFlagId)
        assertEquals(featureFlag.name, capturedLog.featureFlagName)
        assertEquals(AuditOperation.CREATE, capturedLog.operation)
        assertEquals(featureFlag.team, capturedLog.team)
        assertNull(capturedLog.oldValues)
        assertNotNull(capturedLog.newValues)
        assertEquals("user@example.com", capturedLog.changedBy)
    }

    @ParameterizedTest
    @ValueSource(strings = ["user@example.com", "admin@test.com", "system"])
    fun `logCreate should handle different changedBy values`(changedBy: String) {
        val featureFlag = createFeatureFlag("flag1", "team1", 50)
        val auditLogSlot = slot<FeatureFlagAuditLog>()

        every { auditLogRepository.save(capture(auditLogSlot)) } answers { auditLogSlot.captured }

        auditLogService.logCreate(featureFlag, changedBy)

        assertEquals(changedBy, auditLogSlot.captured.changedBy)
    }

    @Test
    fun `logCreate should use default changedBy when not provided`() {
        val featureFlag = createFeatureFlag("flag1", "team1", 50)
        val auditLogSlot = slot<FeatureFlagAuditLog>()

        every { auditLogRepository.save(capture(auditLogSlot)) } answers { auditLogSlot.captured }

        auditLogService.logCreate(featureFlag)

        assertEquals("system", auditLogSlot.captured.changedBy)
    }

    @Test
    fun `logCreate should serialize newValues correctly`() {
        val featureFlag = createFeatureFlag("test-flag", "backend-team", 75)
        val auditLogSlot = slot<FeatureFlagAuditLog>()

        every { auditLogRepository.save(capture(auditLogSlot)) } answers { auditLogSlot.captured }

        auditLogService.logCreate(featureFlag)

        val capturedLog = auditLogSlot.captured
        assertNotNull(capturedLog.newValues)
        val newValues = objectMapper.readValue(
            capturedLog.newValues,
            object : com.fasterxml.jackson.core.type.TypeReference<Map<String, Any?>>() {}
        )
        assertEquals("test-flag", newValues["Name"])
        assertEquals("backend-team", newValues["Team"])
        assertEquals(75, newValues["Rollout Percentage"])
    }

    // ==================== logUpdate ====================

    @ParameterizedTest
    @MethodSource("rolloutPercentageChanges")
    fun `logUpdate should log rollout percentage changes`(
        oldPercentage: Int,
        newPercentage: Int
    ) {
        val featureFlag = createFeatureFlag("flag1", "team1", newPercentage)
        val auditLogSlot = slot<FeatureFlagAuditLog>()

        every { auditLogRepository.save(capture(auditLogSlot)) } answers { auditLogSlot.captured }

        auditLogService.logUpdate(oldPercentage, newPercentage, featureFlag, "user@example.com")

        verify { auditLogRepository.save(any()) }
        val capturedLog = auditLogSlot.captured
        assertEquals(AuditOperation.UPDATE, capturedLog.operation)
        assertEquals("user@example.com", capturedLog.changedBy)

        val oldValues = objectMapper.readValue(
            capturedLog.oldValues,
            object : com.fasterxml.jackson.core.type.TypeReference<Map<String, Any?>>() {}
        )
        val newValues = objectMapper.readValue(
            capturedLog.newValues,
            object : com.fasterxml.jackson.core.type.TypeReference<Map<String, Any?>>() {}
        )
        assertEquals(oldPercentage, oldValues["Rollout Percentage"])
        assertEquals(newPercentage, newValues["Rollout Percentage"])
    }

    @Test
    fun `logUpdate should use default changedBy when not provided`() {
        val featureFlag = createFeatureFlag("flag1", "team1", 50)
        val auditLogSlot = slot<FeatureFlagAuditLog>()

        every { auditLogRepository.save(capture(auditLogSlot)) } answers { auditLogSlot.captured }

        auditLogService.logUpdate(25, 50, featureFlag)

        assertEquals("system", auditLogSlot.captured.changedBy)
    }

    @Test
    fun `logUpdate should include feature flag metadata`() {
        val featureFlag = createFeatureFlag("test-flag", "backend-team", 100)
        val auditLogSlot = slot<FeatureFlagAuditLog>()

        every { auditLogRepository.save(capture(auditLogSlot)) } answers { auditLogSlot.captured }

        auditLogService.logUpdate(50, 100, featureFlag)

        val capturedLog = auditLogSlot.captured
        assertEquals(featureFlag.id, capturedLog.featureFlagId)
        assertEquals("test-flag", capturedLog.featureFlagName)
        assertEquals("backend-team", capturedLog.team)
    }

    // ==================== logDelete ====================

    @Test
    fun `logDelete should create audit log with all old values`() {
        val featureFlag = createFeatureFlag("flag-to-delete", "team1", 50)
        val auditLogSlot = slot<FeatureFlagAuditLog>()

        every { auditLogRepository.save(capture(auditLogSlot)) } answers { auditLogSlot.captured }

        auditLogService.logDelete(featureFlag, "admin@example.com")

        verify { auditLogRepository.save(any()) }
        val capturedLog = auditLogSlot.captured
        assertEquals(AuditOperation.DELETE, capturedLog.operation)
        assertNotNull(capturedLog.oldValues)
        assertNull(capturedLog.newValues)
        assertEquals("admin@example.com", capturedLog.changedBy)

        val oldValues = objectMapper.readValue(
            capturedLog.oldValues,
            object : com.fasterxml.jackson.core.type.TypeReference<Map<String, Any?>>() {}
        )
        assertEquals("flag-to-delete", oldValues["Name"])
        assertEquals("team1", oldValues["Team"])
        assertEquals(50, oldValues["Rollout Percentage"])
    }

    @Test
    fun `logDelete should use default changedBy when not provided`() {
        val featureFlag = createFeatureFlag("flag1", "team1", 50)
        val auditLogSlot = slot<FeatureFlagAuditLog>()

        every { auditLogRepository.save(capture(auditLogSlot)) } answers { auditLogSlot.captured }

        auditLogService.logDelete(featureFlag)

        assertEquals("system", auditLogSlot.captured.changedBy)
    }

    // ==================== getAllAuditLogs ====================

    @Test
    fun `getAllAuditLogs should return all logs sorted by timestamp descending`() {
        val logs = listOf(
            createAuditLog("flag1", AuditOperation.CREATE),
            createAuditLog("flag2", AuditOperation.UPDATE),
            createAuditLog("flag3", AuditOperation.DELETE)
        )

        every { auditLogRepository.findAllByOrderByTimestampDesc() } returns logs

        val result = auditLogService.getAllAuditLogs()

        assertEquals(3, result.size)
        assertEquals("flag1", result[0].featureFlagName)
        assertEquals(AuditOperation.CREATE, result[0].operation)
        assertEquals("flag2", result[1].featureFlagName)
        assertEquals(AuditOperation.UPDATE, result[1].operation)
        verify { auditLogRepository.findAllByOrderByTimestampDesc() }
    }

    @Test
    fun `getAllAuditLogs should return empty list when no logs exist`() {
        every { auditLogRepository.findAllByOrderByTimestampDesc() } returns emptyList()

        val result = auditLogService.getAllAuditLogs()

        assertTrue(result.isEmpty())
        verify { auditLogRepository.findAllByOrderByTimestampDesc() }
    }

    @ParameterizedTest
    @EnumSource(AuditOperation::class)
    fun `getAllAuditLogs should handle all operation types`(operation: AuditOperation) {
        val logs = listOf(createAuditLog("flag1", operation))

        every { auditLogRepository.findAllByOrderByTimestampDesc() } returns logs

        val result = auditLogService.getAllAuditLogs()

        assertEquals(1, result.size)
        assertEquals(operation, result[0].operation)
    }

    // ==================== getAllAuditLogsPaginated ====================

    @ParameterizedTest
    @MethodSource("paginationParameters")
    fun `getAllAuditLogsPaginated should return paginated results`(page: Int, size: Int, totalElements: Long) {
        val logs = (1..size.coerceAtMost(10)).map { createAuditLog("flag$it", AuditOperation.CREATE) }
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"))
        val logPage = PageImpl(logs, pageable, totalElements)

        every { auditLogRepository.findAll(pageable) } returns logPage

        val result = auditLogService.getAllAuditLogsPaginated(page, size, null)

        assertEquals(logs.size, result.content.size)
        assertEquals(totalElements, result.totalElements)
        verify { auditLogRepository.findAll(pageable) }
    }

    @Test
    fun `getAllAuditLogsPaginated should use search when provided`() {
        val searchTerm = "backend"
        val logs = listOf(createAuditLog("backend-flag", AuditOperation.CREATE))
        val pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "timestamp"))
        val logPage = PageImpl(logs, pageable, 1)

        every {
            auditLogRepository.findByFeatureFlagNameContainingIgnoreCaseOrTeamContainingIgnoreCase(
                searchTerm,
                searchTerm,
                pageable
            )
        } returns logPage

        val result = auditLogService.getAllAuditLogsPaginated(0, 20, searchTerm)

        assertEquals(1, result.content.size)
        verify {
            auditLogRepository.findByFeatureFlagNameContainingIgnoreCaseOrTeamContainingIgnoreCase(
                searchTerm,
                searchTerm,
                pageable
            )
        }
        verify(exactly = 0) { auditLogRepository.findAll(any<org.springframework.data.domain.Pageable>()) }
    }

    @Test
    fun `getAllAuditLogsPaginated should handle blank search term as no search`() {
        val logs = listOf(createAuditLog("flag1", AuditOperation.CREATE))
        val pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "timestamp"))
        val logPage = PageImpl(logs, pageable, 1)

        every { auditLogRepository.findAll(pageable) } returns logPage

        val result = auditLogService.getAllAuditLogsPaginated(0, 20, "   ")

        verify { auditLogRepository.findAll(pageable) }
        verify(exactly = 0) { auditLogRepository.findByFeatureFlagNameContainingIgnoreCaseOrTeamContainingIgnoreCase(any(), any(), any()) }
    }

    @Test
    fun `getAllAuditLogsPaginated should return empty page when no logs match search`() {
        val searchTerm = "nonexistent"
        val pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "timestamp"))
        val emptyPage = PageImpl<FeatureFlagAuditLog>(emptyList(), pageable, 0)

        every {
            auditLogRepository.findByFeatureFlagNameContainingIgnoreCaseOrTeamContainingIgnoreCase(
                searchTerm,
                searchTerm,
                pageable
            )
        } returns emptyPage

        val result = auditLogService.getAllAuditLogsPaginated(0, 20, searchTerm)

        assertEquals(0, result.content.size)
        assertEquals(0, result.totalElements)
    }

    // ==================== getAuditLogsByFeatureFlagId ====================

    @Test
    fun `getAuditLogsByFeatureFlagId should return logs for specific feature flag`() {
        val featureFlagId = UUID.randomUUID()
        val logs = listOf(
            createAuditLog("flag1", AuditOperation.CREATE, featureFlagId),
            createAuditLog("flag1", AuditOperation.UPDATE, featureFlagId)
        )

        every { auditLogRepository.findByFeatureFlagIdOrderByTimestampDesc(featureFlagId) } returns logs

        val result = auditLogService.getAuditLogsByFeatureFlagId(featureFlagId)

        assertEquals(2, result.size)
        assertEquals(featureFlagId, result[0].featureFlagId)
        assertEquals(featureFlagId, result[1].featureFlagId)
        verify { auditLogRepository.findByFeatureFlagIdOrderByTimestampDesc(featureFlagId) }
    }

    @Test
    fun `getAuditLogsByFeatureFlagId should return empty list when no logs exist`() {
        val featureFlagId = UUID.randomUUID()

        every { auditLogRepository.findByFeatureFlagIdOrderByTimestampDesc(featureFlagId) } returns emptyList()

        val result = auditLogService.getAuditLogsByFeatureFlagId(featureFlagId)

        assertTrue(result.isEmpty())
        verify { auditLogRepository.findByFeatureFlagIdOrderByTimestampDesc(featureFlagId) }
    }

    // ==================== logWorkspaceUpdate ====================

    @ParameterizedTest
    @MethodSource("workspaceUpdateParameters")
    fun `logWorkspaceUpdate should log workspace count and rollout changes`(
        oldCount: Int,
        newCount: Int,
        oldRollout: Int,
        newRollout: Int
    ) {
        val featureFlag = createFeatureFlag("flag1", "team1", newRollout)
        val auditLogSlot = slot<FeatureFlagAuditLog>()

        every { auditLogRepository.save(capture(auditLogSlot)) } answers { auditLogSlot.captured }

        auditLogService.logWorkspaceUpdate(
            oldCount,
            newCount,
            oldRollout,
            newRollout,
            featureFlag,
            changedBy = "user@example.com"
        )

        verify { auditLogRepository.save(any()) }
        val capturedLog = auditLogSlot.captured
        assertEquals(AuditOperation.UPDATE, capturedLog.operation)

        val oldValues = objectMapper.readValue(
            capturedLog.oldValues,
            object : com.fasterxml.jackson.core.type.TypeReference<Map<String, Any?>>() {}
        )
        val newValues = objectMapper.readValue(
            capturedLog.newValues,
            object : com.fasterxml.jackson.core.type.TypeReference<Map<String, Any?>>() {}
        )
        assertEquals(oldCount, oldValues["Enabled Workspaces"])
        assertEquals(newCount, newValues["Enabled Workspaces"])
        assertEquals(oldRollout, oldValues["Rollout Percentage"])
        assertEquals(newRollout, newValues["Rollout Percentage"])
    }

    @Test
    fun `logWorkspaceUpdate should include pinned workspaces when provided`() {
        val featureFlag = createFeatureFlag("flag1", "team1", 50)
        val pinnedWorkspaces = listOf("workspace1", "workspace2", "workspace3")
        val auditLogSlot = slot<FeatureFlagAuditLog>()

        every { auditLogRepository.save(capture(auditLogSlot)) } answers { auditLogSlot.captured }

        auditLogService.logWorkspaceUpdate(
            5, 8, 50, 75,
            featureFlag,
            newPinnedWorkspaces = pinnedWorkspaces
        )

        val newValues = objectMapper.readValue(
            auditLogSlot.captured.newValues,
            object : com.fasterxml.jackson.core.type.TypeReference<Map<String, Any?>>() {}
        )
        @Suppress("UNCHECKED_CAST")
        val actualPinned = newValues["Manually Enabled Workspaces"] as List<String>
        assertEquals(3, actualPinned.size)
        assertTrue(actualPinned.containsAll(pinnedWorkspaces))
    }

    @Test
    fun `logWorkspaceUpdate should include excluded workspaces when provided`() {
        val featureFlag = createFeatureFlag("flag1", "team1", 50)
        val excludedWorkspaces = listOf("workspace-a", "workspace-b")
        val auditLogSlot = slot<FeatureFlagAuditLog>()

        every { auditLogRepository.save(capture(auditLogSlot)) } answers { auditLogSlot.captured }

        auditLogService.logWorkspaceUpdate(
            10, 8, 50, 50,
            featureFlag,
            newExcludedWorkspaces = excludedWorkspaces
        )

        val newValues = objectMapper.readValue(
            auditLogSlot.captured.newValues,
            object : com.fasterxml.jackson.core.type.TypeReference<Map<String, Any?>>() {}
        )
        @Suppress("UNCHECKED_CAST")
        val actualExcluded = newValues["Manually Disabled Workspaces"] as List<String>
        assertEquals(2, actualExcluded.size)
        assertTrue(actualExcluded.containsAll(excludedWorkspaces))
    }

    @Test
    fun `logWorkspaceUpdate should include target region when provided`() {
        val featureFlag = createFeatureFlag("flag1", "team1", 50)
        val auditLogSlot = slot<FeatureFlagAuditLog>()

        every { auditLogRepository.save(capture(auditLogSlot)) } answers { auditLogSlot.captured }

        auditLogService.logWorkspaceUpdate(
            5, 10, 50, 75,
            featureFlag,
            targetRegion = "WESTEUROPE"
        )

        val newValues = objectMapper.readValue(
            auditLogSlot.captured.newValues,
            object : com.fasterxml.jackson.core.type.TypeReference<Map<String, Any?>>() {}
        )
        assertEquals("WESTEUROPE", newValues["Region"])
    }

    @Test
    fun `logWorkspaceUpdate should include all optional fields when provided`() {
        val featureFlag = createFeatureFlag("flag1", "team1", 50)
        val pinnedWorkspaces = listOf("workspace1", "workspace2")
        val excludedWorkspaces = listOf("workspace3")
        val auditLogSlot = slot<FeatureFlagAuditLog>()

        every { auditLogRepository.save(capture(auditLogSlot)) } answers { auditLogSlot.captured }

        auditLogService.logWorkspaceUpdate(
            5, 10, 50, 75,
            featureFlag,
            newPinnedWorkspaces = pinnedWorkspaces,
            newExcludedWorkspaces = excludedWorkspaces,
            targetRegion = "EASTUS",
            changedBy = "admin@example.com"
        )

        val capturedLog = auditLogSlot.captured
        assertEquals("admin@example.com", capturedLog.changedBy)

        val newValues = objectMapper.readValue(
            capturedLog.newValues,
            object : com.fasterxml.jackson.core.type.TypeReference<Map<String, Any?>>() {}
        )
        assertNotNull(newValues["Manually Enabled Workspaces"])
        assertNotNull(newValues["Manually Disabled Workspaces"])
        assertEquals("EASTUS", newValues["Region"])
    }

    @Test
    fun `logWorkspaceUpdate should use default changedBy when not provided`() {
        val featureFlag = createFeatureFlag("flag1", "team1", 50)
        val auditLogSlot = slot<FeatureFlagAuditLog>()

        every { auditLogRepository.save(capture(auditLogSlot)) } answers { auditLogSlot.captured }

        auditLogService.logWorkspaceUpdate(5, 10, 50, 75, featureFlag)

        assertEquals("system", auditLogSlot.captured.changedBy)
    }

    @Test
    fun `logWorkspaceUpdate should not include pinned workspaces when empty`() {
        val featureFlag = createFeatureFlag("flag1", "team1", 50)
        val auditLogSlot = slot<FeatureFlagAuditLog>()

        every { auditLogRepository.save(capture(auditLogSlot)) } answers { auditLogSlot.captured }

        auditLogService.logWorkspaceUpdate(
            5, 10, 50, 75,
            featureFlag,
            newPinnedWorkspaces = emptyList()
        )

        val newValues = objectMapper.readValue(
            auditLogSlot.captured.newValues,
            object : com.fasterxml.jackson.core.type.TypeReference<Map<String, Any?>>() {}
        )
        assertFalse(newValues.containsKey("Manually Enabled Workspaces"))
    }

    // ==================== Helper Methods ====================

    private fun createFeatureFlag(
        name: String,
        team: String,
        rollout: Int,
        id: UUID = UUID.randomUUID()
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

    private fun createAuditLog(
        featureFlagName: String,
        operation: AuditOperation,
        featureFlagId: UUID = UUID.randomUUID()
    ): FeatureFlagAuditLog {
        val newValues = when (operation) {
            AuditOperation.CREATE -> mapOf("name" to featureFlagName, "rolloutPercentage" to 50)
            AuditOperation.UPDATE -> mapOf("rolloutPercentage" to 75)
            AuditOperation.DELETE -> null
        }

        val oldValues = when (operation) {
            AuditOperation.DELETE -> mapOf("name" to featureFlagName, "rolloutPercentage" to 50)
            AuditOperation.UPDATE -> mapOf("rolloutPercentage" to 50)
            AuditOperation.CREATE -> null
        }

        return FeatureFlagAuditLog(
            id = UUID.randomUUID(),
            featureFlagId = featureFlagId,
            featureFlagName = featureFlagName,
            operation = operation,
            team = "test-team",
            oldValues = oldValues?.let { objectMapper.writeValueAsString(it) },
            newValues = newValues?.let { objectMapper.writeValueAsString(it) },
            changedBy = "test-user",
            timestamp = LocalDateTime.now()
        )
    }

    companion object {
        @JvmStatic
        fun paginationParameters(): Stream<Arguments> {
            return Stream.of(
                Arguments.of(0, 10, 100L),
                Arguments.of(0, 20, 50L),
                Arguments.of(0, 50, 200L),
                Arguments.of(1, 20, 50L),
                Arguments.of(5, 10, 100L)
            )
        }

        @JvmStatic
        fun rolloutPercentageChanges(): Stream<Arguments> {
            return Stream.of(
                Arguments.of(0, 25),
                Arguments.of(25, 50),
                Arguments.of(50, 75),
                Arguments.of(75, 100),
                Arguments.of(100, 50),
                Arguments.of(50, 0)
            )
        }

        @JvmStatic
        fun workspaceUpdateParameters(): Stream<Arguments> {
            return Stream.of(
                Arguments.of(0, 5, 0, 50),
                Arguments.of(5, 10, 50, 75),
                Arguments.of(10, 15, 75, 100),
                Arguments.of(15, 10, 100, 75),
                Arguments.of(10, 0, 50, 0)
            )
        }
    }
}
