package com.featureflag.service

import com.featureflag.dto.CreateWorkspaceRequest
import com.featureflag.dto.WorkspaceDto
import com.featureflag.entity.Workspace
import com.featureflag.exception.ResourceNotFoundException
import com.featureflag.repository.WorkspaceRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
@Transactional
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

    fun createWorkspace(request: CreateWorkspaceRequest): WorkspaceDto {
        if (workspaceRepository.existsByName(request.name)) {
            throw IllegalArgumentException("Workspace with name '${request.name}' already exists")
        }

        val workspace = Workspace(
            name = request.name,
            type = request.type
        )
        val savedWorkspace = workspaceRepository.save(workspace)
        return savedWorkspace.toDto()
    }

    private fun Workspace.toDto(): WorkspaceDto {
        return WorkspaceDto(
            id = this.id,
            name = this.name,
            type = this.type,
            createdAt = this.createdAt,
            updatedAt = this.updatedAt
        )
    }
}
