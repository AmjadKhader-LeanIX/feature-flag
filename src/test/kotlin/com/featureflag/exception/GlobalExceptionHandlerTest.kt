package com.featureflag.exception

import com.featureflag.controller.FeatureFlagController
import com.featureflag.service.FeatureFlagService
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.util.*

@WebMvcTest(FeatureFlagController::class)
@ActiveProfiles("test")
class GlobalExceptionHandlerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockkBean
    private lateinit var featureFlagService: FeatureFlagService

    @Test
    fun `should handle ResourceNotFoundException`() {
        val flagId = UUID.randomUUID()
        val errorMessage = "Feature flag not found with id: $flagId"
        every { featureFlagService.getFeatureFlagById(flagId) } throws ResourceNotFoundException(errorMessage)

        mockMvc.perform(get("/api/feature-flags/$flagId"))
            .andExpect(status().isNotFound)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.error").value("Not Found"))
            .andExpect(jsonPath("$.message").value(errorMessage))
            .andExpect(jsonPath("$.path").value("/api/feature-flags/$flagId"))
            .andExpect(jsonPath("$.timestamp").exists())
    }

    @Test
    fun `should handle ResourceAlreadyExistsException`() {
        val errorMessage = "Feature flag already exists"
        every { featureFlagService.getAllFeatureFlags() } throws ResourceAlreadyExistsException(errorMessage)

        mockMvc.perform(get("/api/feature-flags"))
            .andExpect(status().isConflict)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.status").value(409))
            .andExpect(jsonPath("$.error").value("Conflict"))
            .andExpect(jsonPath("$.message").value(errorMessage))
            .andExpect(jsonPath("$.path").value("/api/feature-flags"))
            .andExpect(jsonPath("$.timestamp").exists())
    }

    @Test
    fun `should handle ValidationException`() {
        val errorMessage = "Validation failed for field"
        every { featureFlagService.getAllFeatureFlags() } throws ValidationException(errorMessage)

        mockMvc.perform(get("/api/feature-flags"))
            .andExpect(status().isBadRequest)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Bad Request"))
            .andExpect(jsonPath("$.message").value(errorMessage))
            .andExpect(jsonPath("$.path").value("/api/feature-flags"))
            .andExpect(jsonPath("$.timestamp").exists())
    }

    @Test
    fun `should handle IllegalArgumentException`() {
        val errorMessage = "Feature flag with name 'test' already exists in this team"
        every { featureFlagService.getAllFeatureFlags() } throws IllegalArgumentException(errorMessage)

        mockMvc.perform(get("/api/feature-flags"))
            .andExpect(status().isBadRequest)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Bad Request"))
            .andExpect(jsonPath("$.message").value(errorMessage))
            .andExpect(jsonPath("$.path").value("/api/feature-flags"))
            .andExpect(jsonPath("$.timestamp").exists())
    }

    @Test
    fun `should handle MethodArgumentNotValidException`() {
        val invalidJson = """
            {
                "name": "",
                "team": "",
                "regions": ["ALL"],
                "rolloutPercentage": -1
            }
        """.trimIndent()

        mockMvc.perform(
            post("/api/feature-flags")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson)
        )
            .andExpect(status().isBadRequest)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Validation Failed"))
            .andExpect(jsonPath("$.message").value("Input validation failed"))
            .andExpect(jsonPath("$.path").value("/api/feature-flags"))
            .andExpect(jsonPath("$.validationErrors").exists())
            .andExpect(jsonPath("$.validationErrors.name").exists())
            .andExpect(jsonPath("$.validationErrors.team").exists())
            .andExpect(jsonPath("$.validationErrors.rolloutPercentage").exists())
            .andExpect(jsonPath("$.timestamp").exists())
    }

    @Test
    fun `should handle FeatureFlagEvaluationException`() {
        val errorMessage = "Feature flag evaluation failed"
        every { featureFlagService.getAllFeatureFlags() } throws FeatureFlagEvaluationException(errorMessage)

        mockMvc.perform(get("/api/feature-flags"))
            .andExpect(status().isInternalServerError)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.status").value(500))
            .andExpect(jsonPath("$.error").value("Internal Server Error"))
            .andExpect(jsonPath("$.message").value(errorMessage))
            .andExpect(jsonPath("$.path").value("/api/feature-flags"))
            .andExpect(jsonPath("$.timestamp").exists())
    }

    @Test
    fun `should handle generic Exception`() {
        every { featureFlagService.getAllFeatureFlags() } throws RuntimeException("Unexpected error")

        mockMvc.perform(get("/api/feature-flags"))
            .andExpect(status().isInternalServerError)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.status").value(500))
            .andExpect(jsonPath("$.error").value("Internal Server Error"))
            .andExpect(jsonPath("$.message").value("An unexpected error occurred"))
            .andExpect(jsonPath("$.path").value("/api/feature-flags"))
            .andExpect(jsonPath("$.timestamp").exists())
    }

    @ParameterizedTest
    @MethodSource("emptyMessageExceptionTestCases")
    fun `should handle exceptions with empty messages`(testCase: ExceptionTestCase) {
        when (testCase.exceptionType) {
            "ResourceNotFoundException" -> {
                val flagId = UUID.randomUUID()
                every { featureFlagService.getFeatureFlagById(flagId) } throws ResourceNotFoundException("")
                mockMvc.perform(get("/api/feature-flags/$flagId"))
                    .andExpect(status().isNotFound)
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.error").value("Not Found"))
            }
            "ResourceAlreadyExistsException" -> {
                every { featureFlagService.getAllFeatureFlags() } throws ResourceAlreadyExistsException("")
                mockMvc.perform(get("/api/feature-flags"))
                    .andExpect(status().isConflict)
                    .andExpect(jsonPath("$.status").value(409))
            }
            "ValidationException" -> {
                every { featureFlagService.getAllFeatureFlags() } throws ValidationException("")
                mockMvc.perform(get("/api/feature-flags"))
                    .andExpect(status().isBadRequest)
                    .andExpect(jsonPath("$.status").value(400))
            }
            "FeatureFlagEvaluationException" -> {
                every { featureFlagService.getAllFeatureFlags() } throws FeatureFlagEvaluationException("")
                mockMvc.perform(get("/api/feature-flags"))
                    .andExpect(status().isInternalServerError)
                    .andExpect(jsonPath("$.status").value(500))
            }
        }
    }

    @Test
    fun `should handle IllegalArgumentException with null message`() {
        every { featureFlagService.getAllFeatureFlags() } throws IllegalArgumentException()

        mockMvc.perform(get("/api/feature-flags"))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("Invalid argument"))
    }

    companion object {
        @JvmStatic
        fun emptyMessageExceptionTestCases() = listOf(
            Arguments.of(ExceptionTestCase("ResourceNotFoundException", 404)),
            Arguments.of(ExceptionTestCase("ResourceAlreadyExistsException", 409)),
            Arguments.of(ExceptionTestCase("ValidationException", 400)),
            Arguments.of(ExceptionTestCase("FeatureFlagEvaluationException", 500))
        )
    }
}

data class ExceptionTestCase(
    val exceptionType: String,
    val expectedStatus: Int
)
