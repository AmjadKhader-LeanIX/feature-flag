package com.featureflag.service

import com.featureflag.dto.CreateFeatureFlagRequest
import com.featureflag.dto.UpdateFeatureFlagRequest
import com.featureflag.entity.FeatureFlag
import com.featureflag.entity.Region
import com.featureflag.entity.Workspace
import com.featureflag.entity.WorkspaceFeatureFlag
import com.featureflag.exception.ResourceNotFoundException
import com.featureflag.repository.FeatureFlagRepository
import com.featureflag.repository.WorkspaceFeatureFlagRepository
import com.featureflag.repository.WorkspaceRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.time.LocalDateTime
import java.util.*

class FeatureFlagServiceTest {

    private lateinit var featureFlagRepository: FeatureFlagRepository
    private lateinit var workspaceRepository: WorkspaceRepository
    private lateinit var workspaceFeatureFlagRepository: WorkspaceFeatureFlagRepository
    private lateinit var auditLogService: AuditLogService
    private lateinit var featureFlagService: FeatureFlagService

    @BeforeEach
    fun setUp() {
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

    @Test
    fun `should get all feature flags`() {
        val featureFlag1 = createMockFeatureFlag("flag1", "team1")
        val featureFlag2 = createMockFeatureFlag("flag2", "team2")
        every { featureFlagRepository.findAll() } returns listOf(featureFlag1, featureFlag2)

        val result = featureFlagService.getAllFeatureFlags()

        assertEquals(2, result.size)
        assertEquals("flag1", result[0].name)
        assertEquals("flag2", result[1].name)
        verify { featureFlagRepository.findAll() }
    }

    @Test
    fun `should get feature flag by id`() {
        val flagId = UUID.randomUUID()
        val featureFlag = createMockFeatureFlag("test-flag", "test-team", flagId)
        every { featureFlagRepository.findById(flagId) } returns Optional.of(featureFlag)

        val result = featureFlagService.getFeatureFlagById(flagId)

        assertEquals(flagId, result.id)
        assertEquals("test-flag", result.name)
        assertEquals("test-team", result.team)
        verify { featureFlagRepository.findById(flagId) }
    }

    @Test
    fun `should throw exception when feature flag not found by id`() {
        val flagId = UUID.randomUUID()
        every { featureFlagRepository.findById(flagId) } returns Optional.empty()

        assertThrows<ResourceNotFoundException> {
            featureFlagService.getFeatureFlagById(flagId)
        }
        verify { featureFlagRepository.findById(flagId) }
    }

    @Test
    fun `should get feature flags by team`() {
        val team = "team1"
        val featureFlag = createMockFeatureFlag("flag1", team)
        every { featureFlagRepository.findByTeam(team) } returns listOf(featureFlag)

        val result = featureFlagService.getFeatureFlagsByTeam(team)

        assertEquals(1, result.size)
        assertEquals("flag1", result[0].name)
        assertEquals(team, result[0].team)
        verify { featureFlagRepository.findByTeam(team) }
    }

    @Test
    fun `should get feature flags by workspace`() {
        val workspaceId = UUID.randomUUID()
        val workspace = createMockWorkspace(workspaceId)
        val featureFlag = createMockFeatureFlag("flag1", "team1")
        val workspaceFeatureFlag = WorkspaceFeatureFlag(
            id = UUID.randomUUID(),
            workspace = workspace,
            featureFlag = featureFlag,
            isEnabled = true
        )

        every { workspaceRepository.findById(workspaceId) } returns Optional.of(workspace)
        every { workspaceFeatureFlagRepository.findByWorkspaceId(workspaceId) } returns listOf(workspaceFeatureFlag)

        val result = featureFlagService.getFeatureFlagsByWorkspace(workspaceId)

        assertEquals(1, result.size)
        assertEquals("flag1", result[0].name)
        verify { workspaceRepository.findById(workspaceId) }
        verify { workspaceFeatureFlagRepository.findByWorkspaceId(workspaceId) }
    }

    @Test
    fun `should throw exception when workspace not found for get feature flags by workspace`() {
        val workspaceId = UUID.randomUUID()
        every { workspaceRepository.findById(workspaceId) } returns Optional.empty()

        assertThrows<ResourceNotFoundException> {
            featureFlagService.getFeatureFlagsByWorkspace(workspaceId)
        }
        verify { workspaceRepository.findById(workspaceId) }
    }

    @Test
    fun `should create feature flag successfully`() {
        val request = CreateFeatureFlagRequest("new-flag", "description", "team1", listOf("ALL"), 50)
        val savedFeatureFlag = createMockFeatureFlag("new-flag", "team1")

        every { featureFlagRepository.existsByTeamAndName("team1", "new-flag") } returns false
        every { featureFlagRepository.save(any()) } returns savedFeatureFlag
        every { workspaceRepository.findByRegionIn(listOf("ALL")) } returns emptyList()
        every { workspaceFeatureFlagRepository.saveAll(any<List<WorkspaceFeatureFlag>>()) } returns emptyList()
        every { workspaceFeatureFlagRepository.findByFeatureFlag(savedFeatureFlag) } returns emptyList()

        val result = featureFlagService.createFeatureFlag(request)

        assertEquals("new-flag", result.name)
        assertEquals("team1", result.team)
        assertEquals(50, result.rolloutPercentage)
        verify { featureFlagRepository.existsByTeamAndName("team1", "new-flag") }
        verify { featureFlagRepository.save(any()) }
        verify { workspaceRepository.findByRegionIn(listOf("ALL")) }
    }

    @ParameterizedTest
    @org.junit.jupiter.params.provider.CsvSource(
        "WESTEUROPE, 2",
        "EASTUS, 2",
        "CANADACENTRAL, 2"
    )
    fun `should create feature flag with specific region`(regionName: String, expectedWorkspaceCount: Int) {
        val region = Region.valueOf(regionName)
        val request = CreateFeatureFlagRequest("region-flag", "description", "team1", listOf(regionName), 50)
        val savedFeatureFlag = createMockFeatureFlag("region-flag", "team1").copy(regions = regionName)
        val workspace1 = createMockWorkspace(UUID.randomUUID(), region)
        val workspace2 = createMockWorkspace(UUID.randomUUID(), region)

        every { featureFlagRepository.existsByTeamAndName("team1", "region-flag") } returns false
        every { featureFlagRepository.save(any()) } returns savedFeatureFlag
        every { workspaceRepository.findByRegionIn(listOf(regionName)) } returns listOf(workspace1, workspace2)
        every { workspaceFeatureFlagRepository.saveAll(any<List<WorkspaceFeatureFlag>>()) } returns emptyList()
        every { workspaceFeatureFlagRepository.findByFeatureFlag(savedFeatureFlag) } returns emptyList()

        val result = featureFlagService.createFeatureFlag(request)

        assertEquals("region-flag", result.name)
        assertEquals(listOf(regionName), result.regions)
        verify { workspaceRepository.findByRegionIn(listOf(regionName)) }
        verify(exactly = 1) { workspaceFeatureFlagRepository.saveAll(any<List<WorkspaceFeatureFlag>>()) }
    }

    @ParameterizedTest
    @org.junit.jupiter.params.provider.CsvSource(
        "WESTEUROPE;EASTUS, 3",
        "WESTEUROPE;CANADACENTRAL, 3",
        "EASTUS;JAPANEAST, 3"
    )
    fun `should create feature flag with multiple regions`(regionsStr: String, expectedWorkspaceCount: Int) {
        val regionsList = regionsStr.split(";")
        val regions = regionsList.map { Region.valueOf(it) }
        val request = CreateFeatureFlagRequest("multi-region-flag", "description", "team1", regionsList, 50)
        val savedFeatureFlag =
            createMockFeatureFlag("multi-region-flag", "team1").copy(regions = regionsStr.replace(";", ","))

        val workspaces = listOf<Workspace>(
            createMockWorkspace(UUID.randomUUID(), regions[0]),
            createMockWorkspace(UUID.randomUUID(), regions[1]),
            createMockWorkspace(UUID.randomUUID(), regions[1])
        )

        every { featureFlagRepository.existsByTeamAndName("team1", "multi-region-flag") } returns false
        every { featureFlagRepository.save(any()) } returns savedFeatureFlag
        every { workspaceRepository.findByRegionIn(regionsList) } returns workspaces
        every { workspaceFeatureFlagRepository.saveAll(any<List<WorkspaceFeatureFlag>>()) } returns emptyList()
        every { workspaceFeatureFlagRepository.findByFeatureFlag(savedFeatureFlag) } returns emptyList()

        val result = featureFlagService.createFeatureFlag(request)

        assertEquals("multi-region-flag", result.name)
        assertEquals(regionsList, result.regions)
        verify { workspaceRepository.findByRegionIn(regionsList) }
        verify(exactly = 1) { workspaceFeatureFlagRepository.saveAll(any<List<WorkspaceFeatureFlag>>()) }
    }

    @Test
    fun `should throw exception when creating feature flag with existing name in same team`() {
        val request = CreateFeatureFlagRequest("existing-flag", "description", "team1", listOf("ALL"), 50)
        every { featureFlagRepository.existsByTeamAndName("team1", "existing-flag") } returns true

        assertThrows<IllegalArgumentException> {
            featureFlagService.createFeatureFlag(request)
        }
        verify { featureFlagRepository.existsByTeamAndName("team1", "existing-flag") }
    }

    @Test
    fun `should update feature flag successfully`() {
        val flagId = UUID.randomUUID()
        val existingFlag = createMockFeatureFlag("old-flag", "team1", flagId)
        val request = UpdateFeatureFlagRequest("new-flag", "new description", "team1", listOf("ALL"), 75)
        val updatedFlag = createMockFeatureFlag("new-flag", "team1", flagId).copy(rolloutPercentage = 75)

        every { featureFlagRepository.findById(flagId) } returns Optional.of(existingFlag)
        every { featureFlagRepository.existsByTeamAndName("team1", "new-flag") } returns false
        every { featureFlagRepository.save(any()) } returns updatedFlag
        every { workspaceFeatureFlagRepository.findByFeatureFlag(any()) } returns emptyList()
        every { workspaceFeatureFlagRepository.saveAll(any<List<WorkspaceFeatureFlag>>()) } returns emptyList()

        val result = featureFlagService.updateFeatureFlag(flagId, request)

        assertEquals("new-flag", result.name)
        assertEquals(75, result.rolloutPercentage)
        verify { featureFlagRepository.findById(flagId) }
        verify { featureFlagRepository.save(any()) }
    }

    @Test
    fun `should throw exception when updating to existing name in same team`() {
        val flagId = UUID.randomUUID()
        val existingFlag = createMockFeatureFlag("old-flag", "team1", flagId)
        val request = UpdateFeatureFlagRequest("existing-flag", "description", "team1", listOf("ALL"), 75)

        every { featureFlagRepository.findById(flagId) } returns Optional.of(existingFlag)
        every { featureFlagRepository.existsByTeamAndName("team1", "existing-flag") } returns true

        assertThrows<IllegalArgumentException> {
            featureFlagService.updateFeatureFlag(flagId, request)
        }
        verify { featureFlagRepository.findById(flagId) }
        verify { featureFlagRepository.existsByTeamAndName("team1", "existing-flag") }
    }

    @Test
    fun `should allow updating feature flag with same name`() {
        val flagId = UUID.randomUUID()
        val existingFlag = createMockFeatureFlag("same-flag", "team1", flagId)
        val request = UpdateFeatureFlagRequest("same-flag", "updated description", "team1", listOf("ALL"), 80)
        val updatedFlag = createMockFeatureFlag("same-flag", "team1", flagId).copy(rolloutPercentage = 80)

        every { featureFlagRepository.findById(flagId) } returns Optional.of(existingFlag)
        every { featureFlagRepository.existsByTeamAndName("team1", "same-flag") } returns true
        every { featureFlagRepository.save(any()) } returns updatedFlag
        every { workspaceFeatureFlagRepository.findByFeatureFlag(any()) } returns emptyList()
        every { workspaceFeatureFlagRepository.saveAll(any<List<WorkspaceFeatureFlag>>()) } returns emptyList()

        val result = featureFlagService.updateFeatureFlag(flagId, request)

        assertEquals("same-flag", result.name)
        assertEquals(80, result.rolloutPercentage)
        verify { featureFlagRepository.findById(flagId) }
        verify { featureFlagRepository.save(any()) }
    }

    @Test
    fun `should throw exception when updating non-existent feature flag`() {
        val flagId = UUID.randomUUID()
        val request = UpdateFeatureFlagRequest("flag", "description", "team1", listOf("ALL"), 50)
        every { featureFlagRepository.findById(flagId) } returns Optional.empty()

        assertThrows<ResourceNotFoundException> {
            featureFlagService.updateFeatureFlag(flagId, request)
        }
        verify { featureFlagRepository.findById(flagId) }
    }

    @Test
    fun `should delete feature flag successfully`() {
        val flagId = UUID.randomUUID()
        val featureFlag = createMockFeatureFlag("test-flag", "team1", flagId)
        every { featureFlagRepository.findById(flagId) } returns Optional.of(featureFlag)
        every { featureFlagRepository.deleteById(flagId) } returns Unit

        featureFlagService.deleteFeatureFlag(flagId)

        verify { featureFlagRepository.findById(flagId) }
        verify { featureFlagRepository.deleteById(flagId) }
    }

    @Test
    fun `should throw exception when deleting non-existent feature flag`() {
        val flagId = UUID.randomUUID()
        every { featureFlagRepository.findById(flagId) } returns Optional.empty()

        assertThrows<ResourceNotFoundException> {
            featureFlagService.deleteFeatureFlag(flagId)
        }
        verify { featureFlagRepository.findById(flagId) }
    }

    @Test
    fun `should search feature flags by name`() {
        val searchTerm = "test"
        val featureFlag = createMockFeatureFlag("test-flag", "team1")
        every { featureFlagRepository.findByNameContainingIgnoreCase(searchTerm) } returns listOf(featureFlag)

        val result = featureFlagService.searchFeatureFlags(searchTerm)

        assertEquals(1, result.size)
        assertEquals("test-flag", result[0].name)
        verify { featureFlagRepository.findByNameContainingIgnoreCase(searchTerm) }
    }

    @ParameterizedTest
    @ValueSource(ints = [0, 10, 35, 70, 100])
    fun `should handle rollout percentage boundary values`(rolloutPercentage: Int) {
        if (rolloutPercentage == 0) {
            val request = CreateFeatureFlagRequest("zero-flag", "description", "team1", listOf("ALL"), 0)
            val savedFeatureFlag = createMockFeatureFlag("zero-flag", "team1").copy(rolloutPercentage = 0)

            every { featureFlagRepository.existsByTeamAndName("team1", "zero-flag") } returns false
            every { featureFlagRepository.save(any()) } returns savedFeatureFlag
            every { workspaceRepository.findByRegionIn(listOf("ALL")) } returns emptyList()
            every { workspaceFeatureFlagRepository.saveAll(any<List<WorkspaceFeatureFlag>>()) } returns emptyList()
            every { workspaceFeatureFlagRepository.findByFeatureFlag(savedFeatureFlag) } returns emptyList()

            val result = featureFlagService.createFeatureFlag(request)

            assertEquals(0, result.rolloutPercentage)
            verify { featureFlagRepository.save(any()) }
        } else {
            val flagId = UUID.randomUUID()
            val existingFlag = createMockFeatureFlag("flag", "team1", flagId)
            val request = UpdateFeatureFlagRequest("flag", "description", "team1", listOf("ALL"), 100)
            val updatedFlag = createMockFeatureFlag("flag", "team1", flagId).copy(rolloutPercentage = 100)
            val workspaceFeatureFlag = WorkspaceFeatureFlag(
                id = UUID.randomUUID(),
                workspace = createMockWorkspace(),
                featureFlag = existingFlag,
                isEnabled = false
            )

            every { featureFlagRepository.findById(flagId) } returns Optional.of(existingFlag)
            every { featureFlagRepository.existsByTeamAndName("team1", "flag") } returns true
            every { featureFlagRepository.save(any()) } returns updatedFlag
            every { workspaceFeatureFlagRepository.findByFeatureFlag(any()) } returns listOf(workspaceFeatureFlag)
            every { workspaceFeatureFlagRepository.saveAll(any<List<WorkspaceFeatureFlag>>()) } returns emptyList()

            val result = featureFlagService.updateFeatureFlag(flagId, request)

            assertEquals(100, result.rolloutPercentage)
            verify { workspaceFeatureFlagRepository.saveAll(any<List<WorkspaceFeatureFlag>>()) }
        }
    }

    @ParameterizedTest
    @org.junit.jupiter.params.provider.CsvSource(
        "0, 100, ALL",
        "100, 0, ALL",
        "20, 50, ALL",
        "50, 20, ALL",
        "0, 100, WESTEUROPE",
        "100, 0, WESTEUROPE",
        "20, 50, WESTEUROPE;EASTUS",
        "50, 20, WESTEUROPE;EASTUS;CANADACENTRAL"
    )
    fun `should handle rollout percentage updates correctly`(
        oldPercentage: Int,
        newPercentage: Int,
        regionsStr: String
    ) {
        val flagId = UUID.randomUUID()
        val regionsList = regionsStr.split(";")
        val regions = regionsList.map { Region.valueOf(it) }
        val existingFlag = createMockFeatureFlag("flag", "team1", flagId).copy(
            rolloutPercentage = oldPercentage,
            regions = regionsStr.replace(";", ",")
        )

        // Create workspaces: some in matching regions, some not
        val matchingWorkspaces = (0..4).map { createMockWorkspace(UUID.randomUUID(), regions[0]) }
        val nonMatchingWorkspaces = (0..2).map { createMockWorkspace(UUID.randomUUID(), Region.JAPANEAST) }
        val allWorkspaces = matchingWorkspaces + nonMatchingWorkspaces

        // Sort workspaces deterministically the same way the service does
        val sortedWorkspaces = allWorkspaces.sortedBy { workspace ->
            kotlin.math.abs((existingFlag.id.toString() + workspace.id.toString()).hashCode())
        }

        // Calculate how many should be enabled at old percentage using exact count
        val totalWorkspaces = allWorkspaces.size
        val oldTargetCount = (totalWorkspaces * oldPercentage / 100.0).toInt()

        val workspaceFeatureFlags = sortedWorkspaces.mapIndexed { index, workspace ->
            val shouldBeEnabledAtOld = index < oldTargetCount

            WorkspaceFeatureFlag(
                id = UUID.randomUUID(),
                workspace = workspace,
                featureFlag = existingFlag,
                isEnabled = shouldBeEnabledAtOld
            )
        }

        val originalEnabledWorkspaceIds = workspaceFeatureFlags.filter { it.isEnabled }.map { it.workspace.id }.toSet()

        val request = UpdateFeatureFlagRequest("flag", "description", "team1", regionsList, newPercentage)
        val updatedFlag = existingFlag.copy(rolloutPercentage = newPercentage)

        every { featureFlagRepository.findById(flagId) } returns Optional.of(existingFlag)
        every { featureFlagRepository.existsByTeamAndName("team1", "flag") } returns true
        every { featureFlagRepository.save(any()) } returns updatedFlag
        every { workspaceFeatureFlagRepository.findByFeatureFlag(any()) } returns workspaceFeatureFlags

        val savedWorkspaces = mutableListOf<List<WorkspaceFeatureFlag>>()
        every { workspaceFeatureFlagRepository.saveAll(capture(savedWorkspaces)) } returns emptyList()

        val result = featureFlagService.updateFeatureFlag(flagId, request)

        assertEquals(newPercentage, result.rolloutPercentage, "Returned DTO should have new percentage")

        verify(atMost = 1) { workspaceFeatureFlagRepository.saveAll(any<List<WorkspaceFeatureFlag>>()) }

        if (savedWorkspaces.isNotEmpty()) {
            val saved = savedWorkspaces.first()

            // Verify that only workspaces in matching regions were updated
            val regionSet = regions.toSet()
            assert(saved.all { it.workspace.region in regionSet }) {
                "Only workspaces in matching regions should be updated. Found regions: ${saved.map { it.workspace.region }}"
            }
            when {
                newPercentage == 0 -> {
                    assert(saved.all { !it.isEnabled }) { "All changed workspaces should be disabled at 0%" }
                }
                newPercentage == 100 -> {
                    assert(saved.all { it.isEnabled }) { "All changed workspaces should be enabled at 100%" }
                }
                newPercentage > oldPercentage -> {
                    val nowDisabled =
                        saved.filter { !it.isEnabled && originalEnabledWorkspaceIds.contains(it.workspace.id) }
                    assert(nowDisabled.isEmpty()) {
                        "When increasing percentage, no originally enabled workspaces should be disabled. Found: ${nowDisabled.map { it.workspace.id }}"
                    }
                }

                newPercentage < oldPercentage -> {
                    val wronglyEnabled =
                        saved.filter { it.isEnabled && !originalEnabledWorkspaceIds.contains(it.workspace.id) }
                    assert(wronglyEnabled.isEmpty()) {
                        "When decreasing percentage, no new workspaces should be enabled. Found: ${wronglyEnabled.map { it.workspace.id }}"
                    }
                }
            }
        }
    }

    @Test
    fun `should only rollout to workspaces in matching regions`() {
        val flagId = UUID.randomUUID()
        val existingFlag = createMockFeatureFlag("region-flag", "team1", flagId).copy(
            regions = "WESTEUROPE,EASTUS",
            rolloutPercentage = 0
        )

        val westEuropeWs1 = createMockWorkspace(UUID.randomUUID(), Region.WESTEUROPE)
        val westEuropeWs2 = createMockWorkspace(UUID.randomUUID(), Region.WESTEUROPE)
        val eastUsWs = createMockWorkspace(UUID.randomUUID(), Region.EASTUS)
        val canadaWs = createMockWorkspace(UUID.randomUUID(), Region.CANADACENTRAL)
        val japanWs = createMockWorkspace(UUID.randomUUID(), Region.JAPANEAST)

        val allWorkspaceFeatureFlags = listOf(
            WorkspaceFeatureFlag(UUID.randomUUID(), westEuropeWs1, existingFlag, false),
            WorkspaceFeatureFlag(UUID.randomUUID(), westEuropeWs2, existingFlag, false),
            WorkspaceFeatureFlag(UUID.randomUUID(), eastUsWs, existingFlag, false),
            WorkspaceFeatureFlag(UUID.randomUUID(), canadaWs, existingFlag, false),
            WorkspaceFeatureFlag(UUID.randomUUID(), japanWs, existingFlag, false)
        )

        val request =
            UpdateFeatureFlagRequest("region-flag", "description", "team1", listOf("WESTEUROPE", "EASTUS"), 100)
        val updatedFlag = existingFlag.copy(rolloutPercentage = 100)

        every { featureFlagRepository.findById(flagId) } returns Optional.of(existingFlag)
        every { featureFlagRepository.existsByTeamAndName("team1", "region-flag") } returns true
        every { featureFlagRepository.save(any()) } returns updatedFlag
        every { workspaceFeatureFlagRepository.findByFeatureFlag(any()) } returns allWorkspaceFeatureFlags

        val savedWorkspaces = mutableListOf<List<WorkspaceFeatureFlag>>()
        every { workspaceFeatureFlagRepository.saveAll(capture(savedWorkspaces)) } returns emptyList()

        val result = featureFlagService.updateFeatureFlag(flagId, request)

        assertEquals(100, result.rolloutPercentage)
        verify { workspaceFeatureFlagRepository.saveAll(any<List<WorkspaceFeatureFlag>>()) }

        val saved = savedWorkspaces.first()
        assertEquals(3, saved.size, "Only 3 workspaces (2 WESTEUROPE + 1 EASTUS) should be updated")
        assert(saved.all { it.isEnabled }) { "All matching region workspaces should be enabled" }
        assert(saved.none { it.workspace.id == canadaWs.id }) { "CANADACENTRAL workspace should not be updated" }
        assert(saved.none { it.workspace.id == japanWs.id }) { "JAPANEAST workspace should not be updated" }
    }

    @Test
    fun `should filter by region when decreasing rollout percentage`() {
        val flagId = UUID.randomUUID()
        val existingFlag = createMockFeatureFlag("region-flag", "team1", flagId).copy(
            regions = "WESTEUROPE",
            rolloutPercentage = 100
        )

        val westEuropeWs1 = createMockWorkspace(UUID.randomUUID(), Region.WESTEUROPE)
        val westEuropeWs2 = createMockWorkspace(UUID.randomUUID(), Region.WESTEUROPE)
        val westEuropeWs3 = createMockWorkspace(UUID.randomUUID(), Region.WESTEUROPE)
        val eastUsWs = createMockWorkspace(UUID.randomUUID(), Region.EASTUS)

        val allWorkspaceFeatureFlags = listOf(
            WorkspaceFeatureFlag(UUID.randomUUID(), westEuropeWs1, existingFlag, true),
            WorkspaceFeatureFlag(UUID.randomUUID(), westEuropeWs2, existingFlag, true),
            WorkspaceFeatureFlag(UUID.randomUUID(), westEuropeWs3, existingFlag, true),
            WorkspaceFeatureFlag(UUID.randomUUID(), eastUsWs, existingFlag, true)
        )

        val request = UpdateFeatureFlagRequest("region-flag", "description", "team1", listOf("WESTEUROPE"), 50)
        val updatedFlag = existingFlag.copy(rolloutPercentage = 50)

        every { featureFlagRepository.findById(flagId) } returns Optional.of(existingFlag)
        every { featureFlagRepository.existsByTeamAndName("team1", "region-flag") } returns true
        every { featureFlagRepository.save(any()) } returns updatedFlag
        every { workspaceFeatureFlagRepository.findByFeatureFlag(any()) } returns allWorkspaceFeatureFlags

        val savedWorkspaces = mutableListOf<List<WorkspaceFeatureFlag>>()
        every { workspaceFeatureFlagRepository.saveAll(capture(savedWorkspaces)) } returns emptyList()

        val result = featureFlagService.updateFeatureFlag(flagId, request)

        assertEquals(50, result.rolloutPercentage)

        val saved = savedWorkspaces.first()
        assert(saved.none { it.workspace.id == eastUsWs.id }) { "EASTUS workspace should not be touched" }
        assert(saved.all { it.workspace.region == Region.WESTEUROPE }) { "Only WESTEUROPE workspaces should be updated" }
    }

    @Test
    fun `should handle multi-region rollout with mixed percentages`() {
        val flagId = UUID.randomUUID()
        val existingFlag = createMockFeatureFlag("multi-region-flag", "team1", flagId).copy(
            regions = "WESTEUROPE,EASTUS,CANADACENTRAL",
            rolloutPercentage = 0
        )

        val westEuropeWs1 = createMockWorkspace(UUID.randomUUID(), Region.WESTEUROPE)
        val westEuropeWs2 = createMockWorkspace(UUID.randomUUID(), Region.WESTEUROPE)
        val eastUsWs1 = createMockWorkspace(UUID.randomUUID(), Region.EASTUS)
        val eastUsWs2 = createMockWorkspace(UUID.randomUUID(), Region.EASTUS)
        val canadaWs1 = createMockWorkspace(UUID.randomUUID(), Region.CANADACENTRAL)
        val canadaWs2 = createMockWorkspace(UUID.randomUUID(), Region.CANADACENTRAL)
        val japanWs1 = createMockWorkspace(UUID.randomUUID(), Region.JAPANEAST)
        val japanWs2 = createMockWorkspace(UUID.randomUUID(), Region.JAPANEAST)

        val allWorkspaceFeatureFlags = listOf(
            WorkspaceFeatureFlag(UUID.randomUUID(), westEuropeWs1, existingFlag, false),
            WorkspaceFeatureFlag(UUID.randomUUID(), westEuropeWs2, existingFlag, false),
            WorkspaceFeatureFlag(UUID.randomUUID(), eastUsWs1, existingFlag, false),
            WorkspaceFeatureFlag(UUID.randomUUID(), eastUsWs2, existingFlag, false),
            WorkspaceFeatureFlag(UUID.randomUUID(), canadaWs1, existingFlag, false),
            WorkspaceFeatureFlag(UUID.randomUUID(), canadaWs2, existingFlag, false),
            WorkspaceFeatureFlag(UUID.randomUUID(), japanWs1, existingFlag, false),
            WorkspaceFeatureFlag(UUID.randomUUID(), japanWs2, existingFlag, false)
        )

        val request = UpdateFeatureFlagRequest(
            "multi-region-flag", "description", "team1",
            listOf("WESTEUROPE", "EASTUS", "CANADACENTRAL"), 50
        )
        val updatedFlag = existingFlag.copy(rolloutPercentage = 50)

        every { featureFlagRepository.findById(flagId) } returns Optional.of(existingFlag)
        every { featureFlagRepository.existsByTeamAndName("team1", "multi-region-flag") } returns true
        every { featureFlagRepository.save(any()) } returns updatedFlag
        every { workspaceFeatureFlagRepository.findByFeatureFlag(any()) } returns allWorkspaceFeatureFlags

        val savedWorkspaces = mutableListOf<List<WorkspaceFeatureFlag>>()
        every { workspaceFeatureFlagRepository.saveAll(capture(savedWorkspaces)) } returns emptyList()

        val result = featureFlagService.updateFeatureFlag(flagId, request)

        assertEquals(50, result.rolloutPercentage)

        val saved = savedWorkspaces.first()
        val enabledCount = saved.count { it.isEnabled }
        assertEquals(3, enabledCount, "Approximately 50% of 6 matching workspaces should be enabled")

        assert(saved.none { it.workspace.id == japanWs1.id || it.workspace.id == japanWs2.id }) {
            "JAPANEAST workspaces should not be updated as they don't match feature flag regions"
        }

        val validRegions = setOf(Region.WESTEUROPE, Region.EASTUS, Region.CANADACENTRAL)
        assert(saved.all { it.workspace.region in validRegions }) {
            "Only workspaces in WESTEUROPE, EASTUS, or CANADACENTRAL should be updated"
        }
    }

    @Test
    fun `should update region list and apply new rollout to new regions only`() {
        val flagId = UUID.randomUUID()
        val existingFlag = createMockFeatureFlag("region-change-flag", "team1", flagId).copy(
            regions = "WESTEUROPE",
            rolloutPercentage = 100
        )

        val westEuropeWs = createMockWorkspace(UUID.randomUUID(), Region.WESTEUROPE)
        val eastUsWs = createMockWorkspace(UUID.randomUUID(), Region.EASTUS)

        val existingWorkspaceFeatureFlags = listOf(
            WorkspaceFeatureFlag(UUID.randomUUID(), westEuropeWs, existingFlag, true),
            WorkspaceFeatureFlag(UUID.randomUUID(), eastUsWs, existingFlag, false)
        )

        val request = UpdateFeatureFlagRequest(
            "region-change-flag", "description", "team1",
            listOf("WESTEUROPE", "EASTUS"), 100
        )
        val updatedFlag = existingFlag.copy(regions = "WESTEUROPE,EASTUS", rolloutPercentage = 100)

        every { featureFlagRepository.findById(flagId) } returns Optional.of(existingFlag)
        every { featureFlagRepository.existsByTeamAndName("team1", "region-change-flag") } returns true
        every { featureFlagRepository.save(any()) } returns updatedFlag
        every { workspaceFeatureFlagRepository.findByFeatureFlag(any()) } returns existingWorkspaceFeatureFlags

        val savedWorkspaces = mutableListOf<List<WorkspaceFeatureFlag>>()
        every { workspaceFeatureFlagRepository.saveAll(capture(savedWorkspaces)) } returns emptyList()

        val result = featureFlagService.updateFeatureFlag(flagId, request)

        assertEquals(100, result.rolloutPercentage)
        assertEquals(listOf("WESTEUROPE", "EASTUS"), result.regions)

        val saved = savedWorkspaces.first()
        assert(saved.any { it.workspace.id == eastUsWs.id && it.isEnabled }) {
            "EASTUS workspace should be enabled after being added to regions"
        }
    }

    private fun createMockFeatureFlag(name: String, team: String, id: UUID = UUID.randomUUID()): FeatureFlag {
        return FeatureFlag(
            id = id,
            name = name,
            description = "Test description",
            team = team,
            rolloutPercentage = 50,
            regions = "ALL",
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
    }

    private fun createMockWorkspace(id: UUID = UUID.randomUUID(), region: Region = Region.WESTEUROPE): Workspace {
        return Workspace(
            id = id,
            name = "Test Workspace",
            type = "test",
            region = region,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
    }
}
