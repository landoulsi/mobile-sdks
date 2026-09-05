package com.landoulsi.integrity.installer

import com.landoulsi.integrity.model.IntegrityCategory
import com.landoulsi.integrity.model.IntegritySignal
import com.landoulsi.integrity.model.SignalSeverity
import kotlin.time.Clock

object InstallerSignal {
    const val UNKNOWN = "installer_unknown"
    const val SHELL = "installer_shell"
    const val UNTRUSTED = "installer_untrusted"

    /** Every signal id this vector can emit; used to seed the [com.landoulsi.integrity.IntegrityResult] catalog. */
    val all: Set<String> = setOf(
        UNKNOWN,
        SHELL,
        UNTRUSTED,
    )

    object Check {
        const val INSTALL_SOURCE = "install_source"
    }
}

internal val DEFAULT_TRUSTED_INSTALLERS = setOf("com.android.vending")

private const val SHELL_PACKAGE_NAME = "com.android.shell"

/**
 * Classifies the package's install source against [trustedInstallers], emitting at most one signal.
 *
 * Precedence:
 * 1. An explicit adb/shell install (via either [InstallSourceInfo.installingPackageName] on API < 30,
 *    or [InstallSourceInfo.initiatingPackageName] on API 30+) is the strongest, most specific signal.
 * 2. Otherwise the installing package (the store currently responsible for the app) is checked against
 *    [trustedInstallers]; when it is absent, the initiating package (who actually performed the install,
 *    only populated on API 30+) is used as a fallback so sideloads that clear the installer of record
 *    are still attributed to their real origin.
 * 3. If neither field is present, the install source is unknown rather than untrusted.
 *
 * Framework failures are surfaced by letting [InstallerCheckContext.getInstallSourceInfo] throw; this
 * function does not swallow exceptions into a synthetic signal, so a genuine platform error never
 * masquerades as an active threat detection. Callers (see [InstallerDetectionEvaluator]) are expected
 * to catch and log at the evaluator boundary, consistent with every other detection vector.
 */
internal fun checkInstallerSource(
    context: InstallerCheckContext,
    trustedInstallers: Set<String> = DEFAULT_TRUSTED_INSTALLERS,
): IntegritySignal? {
    val sourceInfo = context.getInstallSourceInfo()
    val installingPackageName = sourceInfo.installingPackageName
    val initiatingPackageName = sourceInfo.initiatingPackageName

    val currentTimestampMs = Clock.System.now().toEpochMilliseconds()

    if (installingPackageName == SHELL_PACKAGE_NAME || initiatingPackageName == SHELL_PACKAGE_NAME) {
        return IntegritySignal(
            id = InstallerSignal.SHELL,
            name = "Installed via ADB/Shell",
            category = IntegrityCategory.UNTRUSTED_INSTALLER,
            severity = SignalSeverity.MEDIUM,
            confidence = 0.9,
            details = "App was installed via adb/shell rather than a trusted app store",
            detectedAt = currentTimestampMs,
            metadata = mapOf("check" to InstallerSignal.Check.INSTALL_SOURCE),
        )
    }

    val untrustedInstaller = when {
        installingPackageName != null -> installingPackageName.takeIf { it !in trustedInstallers }
        initiatingPackageName != null -> initiatingPackageName.takeIf { it !in trustedInstallers }
        else -> null
    }

    if (untrustedInstaller != null) {
        return IntegritySignal(
            id = InstallerSignal.UNTRUSTED,
            name = "Untrusted Install Source",
            category = IntegrityCategory.UNTRUSTED_INSTALLER,
            severity = SignalSeverity.MEDIUM,
            confidence = 0.85,
            details = "App was installed by an untrusted package: $untrustedInstaller",
            detectedAt = currentTimestampMs,
            metadata = mapOf(
                "check" to InstallerSignal.Check.INSTALL_SOURCE,
                "installer" to untrustedInstaller,
            ),
        )
    }

    if (installingPackageName == null && initiatingPackageName == null) {
        return IntegritySignal(
            id = InstallerSignal.UNKNOWN,
            name = "Unknown Install Source",
            category = IntegrityCategory.UNTRUSTED_INSTALLER,
            severity = SignalSeverity.LOW,
            confidence = 0.5,
            details = "No installer package is on record for this app (e.g. OTA, sideload, or debug install)",
            detectedAt = currentTimestampMs,
            metadata = mapOf("check" to InstallerSignal.Check.INSTALL_SOURCE),
        )
    }

    return null
}
