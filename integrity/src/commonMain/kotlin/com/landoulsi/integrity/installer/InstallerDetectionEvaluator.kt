package com.landoulsi.integrity.installer

import com.landoulsi.integrity.SignalEvaluator
import com.landoulsi.integrity.model.IntegrityCategory
import com.landoulsi.integrity.model.IntegritySignal
import com.landoulsi.logger.Logger

/**
 * [SignalEvaluator] implementation for untrusted installer source detection.
 *
 * Classifies the app's install provenance (Play Store, adb/shell, or an untrusted named package)
 * using the platform's [InstallerCheckContext].
 *
 * @property context Platform-specific abstraction for install source queries.
 * @property trustedInstallers Package names treated as trusted distribution channels.
 */
class InstallerDetectionEvaluator(
    private val context: InstallerCheckContext,
    private val trustedInstallers: Set<String> = DEFAULT_TRUSTED_INSTALLERS,
) : SignalEvaluator {

    override val category: IntegrityCategory = IntegrityCategory.UNTRUSTED_INSTALLER

    override val knownSignalIds: Set<String> = InstallerSignal.all

    override suspend fun evaluate(): List<IntegritySignal> {
        val signals = mutableListOf<IntegritySignal>()

        runCatching { checkInstallerSource(context, trustedInstallers)?.let { signals.add(it) } }
            .onFailure { Logger.e(TAG, "Installer source check failed", it) }

        return signals
    }

    private companion object {
        const val TAG = "InstallerDetectionEvaluator"
    }
}
