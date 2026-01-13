package com.featureflag.service

import com.featureflag.dto.WorkspaceDto
import com.featureflag.entity.Workspace
import com.featureflag.exception.ResourceNotFoundException
import com.featureflag.repository.WorkspaceRepository
import org.springframework.stereotype.Service
import java.util.*

@Service
class WorkspaceService(
    private val workspaceRepository: WorkspaceRepository
) {

    fun getAllWorkspaces(): List<WorkspaceDto> {
        return workspaceRepository.findAll().map { it.toDto() }
    }

    fun getWorkspaceById(id: UUID): WorkspaceDto {
        val workspace = workspaceRepository.findById(id)
            .orElseThrow { ResourceNotFoundException("Workspace not found with id: $id") }
        return workspace.toDto()
    }

    private fun Workspace.toDto(): WorkspaceDto {
        return WorkspaceDto(
            id = this.id,
            name = this.name,
            type = this.type,
            region = this.region?.name,
            createdAt = this.createdAt,
            updatedAt = this.updatedAt
        )
    }
}
