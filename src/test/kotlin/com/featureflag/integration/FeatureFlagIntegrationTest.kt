package com.featureflag.integration

import com.fasterxml.jackson.databind.ObjectMapper
import com.featureflag.dto.CreateFeatureFlagRequest
import com.featureflag.dto.FeatureFlagDto
import com.featureflag.dto.UpdateFeatureFlagRequest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestDatabase
@ActiveProfiles("test")
@Transactional
class FeatureFlagIntegrationTest {

    @Autowired
    private lateinit var restTemplate: TestRestTemplate

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @LocalServerPort
    private var port: Int = 0

    private fun baseUrl(): String = "http://localhost:$port"

    @Test
    fun `should perform complete feature flag lifecycle`() {
        val createRequest = CreateFeatureFlagRequest(
            name = "integration-test-flag",
            description = "Test flag for integration testing",
            team = "integration-team",
            rolloutPercentage = 75
        )

        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        val createEntity = HttpEntity(createRequest, headers)

        // Create feature flag
        val createResponse = restTemplate.postForEntity(
            "${baseUrl()}/api/feature-flags",
            createEntity,
            FeatureFlagDto::class.java
        )

        assertEquals(HttpStatus.CREATED, createResponse.statusCode)
        val createdFlag = createResponse.body!!
        assertEquals("integration-test-flag", createdFlag.name)
        assertEquals("integration-team", createdFlag.team)
        assertEquals(75, createdFlag.rolloutPercentage)

        val flagId = createdFlag.id

        // Get feature flag
        val getResponse = restTemplate.getForEntity(
            "${baseUrl()}/api/feature-flags/$flagId",
            FeatureFlagDto::class.java
        )
        assertEquals(HttpStatus.OK, getResponse.statusCode)
        assertEquals("integration-test-flag", getResponse.body!!.name)

        // Update feature flag
        val updateRequest = UpdateFeatureFlagRequest(
            name = "updated-integration-flag",
            description = "Updated description",
            team = "integration-team",
            rolloutPercentage = 50
        )
        val updateEntity = HttpEntity(updateRequest, headers)
        val updateResponse = restTemplate.exchange(
            "${baseUrl()}/api/feature-flags/$flagId",
            HttpMethod.PUT,
            updateEntity,
            FeatureFlagDto::class.java
        )

        assertEquals(HttpStatus.OK, updateResponse.statusCode)
        assertEquals("updated-integration-flag", updateResponse.body!!.name)
        assertEquals(50, updateResponse.body!!.rolloutPercentage)

        // Delete feature flag
        val deleteResponse = restTemplate.exchange(
            "${baseUrl()}/api/feature-flags/$flagId",
            HttpMethod.DELETE,
            null,
            Void::class.java
        )
        assertEquals(HttpStatus.NO_CONTENT, deleteResponse.statusCode)

        // Verify deletion
        val getDeletedResponse = restTemplate.getForEntity(
            "${baseUrl()}/api/feature-flags/$flagId",
            String::class.java
        )
        assertEquals(HttpStatus.NOT_FOUND, getDeletedResponse.statusCode)
    }

    @Test
    fun `should handle validation errors during creation`() {
        val invalidRequest = CreateFeatureFlagRequest(
            name = "",
            description = null,
            team = "",
            rolloutPercentage = 150
        )

        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        val entity = HttpEntity(invalidRequest, headers)

        val response = restTemplate.postForEntity(
            "${baseUrl()}/api/feature-flags",
            entity,
            String::class.java
        )

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
    }

    @Test
    fun `should handle duplicate feature flag names in same team`() {
        val firstRequest = CreateFeatureFlagRequest(
            name = "duplicate-flag",
            description = "First flag",
            team = "test-team",
            rolloutPercentage = 50
        )

        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        val firstEntity = HttpEntity(firstRequest, headers)

        val firstResponse = restTemplate.postForEntity(
            "${baseUrl()}/api/feature-flags",
            firstEntity,
            FeatureFlagDto::class.java
        )
        assertEquals(HttpStatus.CREATED, firstResponse.statusCode)

        val duplicateRequest = CreateFeatureFlagRequest(
            name = "duplicate-flag",
            description = "Duplicate flag",
            team = "test-team",
            rolloutPercentage = 75
        )
        val duplicateEntity = HttpEntity(duplicateRequest, headers)

        val duplicateResponse = restTemplate.postForEntity(
            "${baseUrl()}/api/feature-flags",
            duplicateEntity,
            String::class.java
        )
        assertEquals(HttpStatus.BAD_REQUEST, duplicateResponse.statusCode)
    }

    @Test
    fun `should allow same flag names in different teams`() {
        val team1Request = CreateFeatureFlagRequest(
            name = "same-name-flag",
            description = "Flag for team 1",
            team = "team-1",
            rolloutPercentage = 25
        )

        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        val team1Entity = HttpEntity(team1Request, headers)

        val team1Response = restTemplate.postForEntity(
            "${baseUrl()}/api/feature-flags",
            team1Entity,
            FeatureFlagDto::class.java
        )
        assertEquals(HttpStatus.CREATED, team1Response.statusCode)

        val team2Request = CreateFeatureFlagRequest(
            name = "same-name-flag",
            description = "Flag for team 2",
            team = "team-2",
            rolloutPercentage = 75
        )
        val team2Entity = HttpEntity(team2Request, headers)

        val team2Response = restTemplate.postForEntity(
            "${baseUrl()}/api/feature-flags",
            team2Entity,
            FeatureFlagDto::class.java
        )
        assertEquals(HttpStatus.CREATED, team2Response.statusCode)
    }

    @Test
    fun `should handle rollout percentage boundary values`() {
        val zeroPercentRequest = CreateFeatureFlagRequest(
            name = "zero-percent-flag",
            description = "0% rollout",
            team = "boundary-test",
            rolloutPercentage = 0
        )

        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        val zeroEntity = HttpEntity(zeroPercentRequest, headers)

        val zeroResponse = restTemplate.postForEntity(
            "${baseUrl()}/api/feature-flags",
            zeroEntity,
            FeatureFlagDto::class.java
        )
        assertEquals(HttpStatus.CREATED, zeroResponse.statusCode)
        assertEquals(0, zeroResponse.body!!.rolloutPercentage)

        val hundredPercentRequest = CreateFeatureFlagRequest(
            name = "hundred-percent-flag",
            description = "100% rollout",
            team = "boundary-test",
            rolloutPercentage = 100
        )
        val hundredEntity = HttpEntity(hundredPercentRequest, headers)

        val hundredResponse = restTemplate.postForEntity(
            "${baseUrl()}/api/feature-flags",
            hundredEntity,
            FeatureFlagDto::class.java
        )
        assertEquals(HttpStatus.CREATED, hundredResponse.statusCode)
        assertEquals(100, hundredResponse.body!!.rolloutPercentage)
    }

    @Test
    fun `should get feature flags by team`() {
        val team = "team-filter-test"
        val flag1Request = CreateFeatureFlagRequest(
            name = "team-flag-1",
            description = "First team flag",
            team = team,
            rolloutPercentage = 30
        )

        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        val flag1Entity = HttpEntity(flag1Request, headers)

        val flag1Response = restTemplate.postForEntity(
            "${baseUrl()}/api/feature-flags",
            flag1Entity,
            FeatureFlagDto::class.java
        )
        assertEquals(HttpStatus.CREATED, flag1Response.statusCode)

        val flag2Request = CreateFeatureFlagRequest(
            name = "team-flag-2",
            description = "Second team flag",
            team = team,
            rolloutPercentage = 70
        )
        val flag2Entity = HttpEntity(flag2Request, headers)

        val flag2Response = restTemplate.postForEntity(
            "${baseUrl()}/api/feature-flags",
            flag2Entity,
            FeatureFlagDto::class.java
        )
        assertEquals(HttpStatus.CREATED, flag2Response.statusCode)

        val teamResponse = restTemplate.getForEntity(
            "${baseUrl()}/api/feature-flags/team/$team",
            Array<FeatureFlagDto>::class.java
        )
        assertEquals(HttpStatus.OK, teamResponse.statusCode)
        val flags = teamResponse.body!!
        assertEquals(2, flags.size)
        assertTrue(flags.all { it.team == team })
    }

    @Test
    fun `should search feature flags by name`() {
        val searchableRequest = CreateFeatureFlagRequest(
            name = "searchable-integration-flag",
            description = "A flag that can be searched",
            team = "search-team",
            rolloutPercentage = 60
        )

        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        val searchableEntity = HttpEntity(searchableRequest, headers)

        val createResponse = restTemplate.postForEntity(
            "${baseUrl()}/api/feature-flags",
            searchableEntity,
            FeatureFlagDto::class.java
        )
        assertEquals(HttpStatus.CREATED, createResponse.statusCode)

        val searchResponse = restTemplate.getForEntity(
            "${baseUrl()}/api/feature-flags/search?name=searchable",
            Array<FeatureFlagDto>::class.java
        )
        assertEquals(HttpStatus.OK, searchResponse.statusCode)
        val searchResults = searchResponse.body!!
        assertEquals(1, searchResults.size)
        assertEquals("searchable-integration-flag", searchResults[0].name)

        val noResultsResponse = restTemplate.getForEntity(
            "${baseUrl()}/api/feature-flags/search?name=nonexistent",
            Array<FeatureFlagDto>::class.java
        )
        assertEquals(HttpStatus.OK, noResultsResponse.statusCode)
        val noResults = noResultsResponse.body!!
        assertEquals(0, noResults.size)
    }
}
