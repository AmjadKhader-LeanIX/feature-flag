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
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "100") size: Int,
        @RequestParam(defaultValue = "false") paginated: Boolean,
        @RequestParam(required = false) search: String?
    ): ResponseEntity<*> {
        return if (paginated) {
            val paginatedAuditLogs = auditLogService.getAllAuditLogsPaginated(page, size, search)
            ResponseEntity.ok(paginatedAuditLogs)
        } else {
            val auditLogs = auditLogService.getAllAuditLogs()
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
}
