package com.landoulsi.update.shared.model

data class UpdateConfig(
    val latestVersion: String,
    val minRequiredVersion: String,
    val isUpdateRequired: Boolean = false,
    val updateUrl: String? = null
)
