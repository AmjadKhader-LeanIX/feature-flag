package com.featureflag.dto

data class RegionWorkspaceCountDto(
    val region: String,
    val enabledCount: Long,
    val totalCount: Long
)
