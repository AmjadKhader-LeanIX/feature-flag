package com.featureflag.controller

import com.featureflag.dto.AuditLogDto
import com.featureflag.entity.AuditOperation
import com.featureflag.service.AuditLogService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/api/audit-logs")
class AuditLogController(
    private val auditLogService: AuditLogService
) {

    @GetMapping
    fun getAllAuditLogs(
        @RequestParam(required = false) featureFlagId: UUID?,
        @RequestParam(required = false) team: String?,
        @RequestParam(required = false) operation: AuditOperation?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "100") size: Int,
        @RequestParam(defaultValue = "false") paginated: Boolean,
        @RequestParam(required = false) search: String?
    ): ResponseEntity<*> {
        return if (paginated) {
            val paginatedAuditLogs = auditLogService.getAllAuditLogsPaginated(page, size, search)
            ResponseEntity.ok(paginatedAuditLogs)
        } else {
            // Backward compatibility
            val auditLogs = when {
                featureFlagId != null && operation != null ->
                    auditLogService.getAuditLogsByFeatureFlagIdAndOperation(featureFlagId, operation)
                featureFlagId != null ->
                    auditLogService.getAuditLogsByFeatureFlagId(featureFlagId)
                team != null ->
                    auditLogService.getAuditLogsByTeam(team)
                operation != null ->
                    auditLogService.getAuditLogsByOperation(operation)
                else ->
                    auditLogService.getAllAuditLogs()
            }
            ResponseEntity.ok(auditLogs)
        }
    }

    @GetMapping("/feature-flag/{featureFlagId}")
    fun getAuditLogsByFeatureFlagId(
        @PathVariable featureFlagId: UUID
    ): ResponseEntity<List<AuditLogDto>> {
        val auditLogs = auditLogService.getAuditLogsByFeatureFlagId(featureFlagId)
        return ResponseEntity.ok(auditLogs)
    }

    @GetMapping("/team/{team}")
    fun getAuditLogsByTeam(
        @PathVariable team: String
    ): ResponseEntity<List<AuditLogDto>> {
        val auditLogs = auditLogService.getAuditLogsByTeam(team)
        return ResponseEntity.ok(auditLogs)
    }

    @GetMapping("/operation/{operation}")
    fun getAuditLogsByOperation(
        @PathVariable operation: AuditOperation
    ): ResponseEntity<List<AuditLogDto>> {
        val auditLogs = auditLogService.getAuditLogsByOperation(operation)
        return ResponseEntity.ok(auditLogs)
    }
}
