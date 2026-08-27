package com.landoulsi.update.shared.manager

import com.landoulsi.update.shared.model.UpdateConfig
import com.landoulsi.update.shared.model.UpdateState

class UpdateManager {
    /**
     * Checks the current version against the remote configuration to determine the update state.
     */
    fun checkUpdate(currentVersion: String, config: UpdateConfig): UpdateState {
        val current = parseVersion(currentVersion)
        val minRequired = parseVersion(config.minRequiredVersion)
        val latest = parseVersion(config.latestVersion)

        // Compare current against minimum required
        if (current < minRequired || config.isUpdateRequired) {
            return UpdateState.UpdateRequired(config)
        }

        // Compare current against latest
        if (current < latest) {
            return UpdateState.UpdateRecommended(config)
        }

        return UpdateState.NoUpdate
    }

    /**
     * Simple version parsing and comparison logic.
     * Assuming semantic versioning (x.y.z).
     */
    private fun parseVersion(version: String): ComparableVersion {
        val parts = version.split(".").map { it.toIntOrNull() ?: 0 }
        return ComparableVersion(
            parts.getOrElse(0) { 0 },
            parts.getOrElse(1) { 0 },
            parts.getOrElse(2) { 0 }
        )
    }
    
    private data class ComparableVersion(val major: Int, val minor: Int, val patch: Int) : Comparable<ComparableVersion> {
        override fun compareTo(other: ComparableVersion): Int {
            if (this.major != other.major) return this.major.compareTo(other.major)
            if (this.minor != other.minor) return this.minor.compareTo(other.minor)
            return this.patch.compareTo(other.patch)
        }
    }
}
