package com.landoulsi.update.shared

import android.app.Activity
import android.content.Context
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability

/**
 * Maps what Google Play reports about an available update to the update type the app
 * should launch. Availability, staleness, and priority are all set server-side in the
 * Play Console; only these thresholds are the integrating app's call.
 */
data class UpdatePolicy(
    /** Launch a dismissible flexible update once the installed build is at least this stale. */
    val flexibleAfterStalenessDays: Int = 10,
    /** Force an immediate (blocking) update at or above this Play update priority (0..5). */
    val immediateAtPriority: Int = 4,
) {
    /** @return an [AppUpdateType] constant, or null when no update should be offered yet. */
    internal fun resolve(info: AppUpdateInfo): Int? {
        if (info.updateAvailability() != UpdateAvailability.UPDATE_AVAILABLE) return null
        if (info.updatePriority() >= immediateAtPriority) return AppUpdateType.IMMEDIATE
        val staleness = info.clientVersionStalenessDays() ?: return null
        return if (staleness >= flexibleAfterStalenessDays) AppUpdateType.FLEXIBLE else null
    }
}

/** Progress of an in-flight flexible update download/install. */
sealed class UpdateEvent {
    data class DownloadProgress(val percent: Float) : UpdateEvent()
    object Downloaded : UpdateEvent()
    object Installing : UpdateEvent()
    object Installed : UpdateEvent()
    object Failed : UpdateEvent()
}

/**
 * Thin wrapper over Play In-App Updates that owns the parts every Android app otherwise
 * reimplements: type selection, the flexible-download listener lifecycle, and resuming a
 * developer-triggered immediate update after process death.
 *
 * Construct once (e.g. from DI), then drive it from the host Activity's lifecycle:
 * [checkAndStart] from `onCreate`, [onResume] from `onResume`, [release] from `onDestroy`.
 */
class PlayInAppUpdateManager(
    context: Context,
    private val policy: UpdatePolicy = UpdatePolicy(),
    private val appUpdateManager: AppUpdateManager =
        AppUpdateManagerFactory.create(context.applicationContext),
) {
    private var installListener: InstallStateUpdatedListener? = null

    /**
     * Asks Play whether an update is due and, if [policy] says so, launches the flow.
     * [onEvent] receives flexible-update download/install progress; ignore it for immediate updates.
     */
    fun checkAndStart(
        activity: Activity,
        requestCode: Int,
        onEvent: (UpdateEvent) -> Unit = {},
    ) {
        appUpdateManager.appUpdateInfo.addOnSuccessListener { info ->
            val type = policy.resolve(info) ?: return@addOnSuccessListener
            if (!info.isUpdateTypeAllowed(type)) return@addOnSuccessListener
            if (type == AppUpdateType.FLEXIBLE) registerInstallListener(onEvent)
            startFlow(info, type, activity, requestCode)
        }
    }

    /**
     * Re-checks on foreground: re-emits [UpdateEvent.Downloaded] for a flexible update that
     * finished while backgrounded, and resumes an immediate update that Play started but the
     * process never completed.
     */
    fun onResume(
        activity: Activity,
        requestCode: Int,
        onEvent: (UpdateEvent) -> Unit = {},
    ) {
        appUpdateManager.appUpdateInfo.addOnSuccessListener { info ->
            when {
                info.installStatus() == InstallStatus.DOWNLOADED ->
                    onEvent(UpdateEvent.Downloaded)
                info.updateAvailability() ==
                    UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS ->
                    startFlow(info, AppUpdateType.IMMEDIATE, activity, requestCode)
            }
        }
    }

    /** Restart the app to apply a flexible update whose download has finished. */
    fun completeFlexibleUpdate() {
        appUpdateManager.completeUpdate()
    }

    /** Detach the install-state listener; safe to call more than once. */
    fun release() {
        installListener?.let { appUpdateManager.unregisterListener(it) }
        installListener = null
    }

    private fun registerInstallListener(onEvent: (UpdateEvent) -> Unit) {
        release()
        val listener = InstallStateUpdatedListener { state ->
            when (state.installStatus()) {
                InstallStatus.DOWNLOADING -> {
                    val total = state.totalBytesToDownload()
                    val percent = if (total > 0) state.bytesDownloaded() * 100f / total else 0f
                    onEvent(UpdateEvent.DownloadProgress(percent))
                }
                InstallStatus.DOWNLOADED -> onEvent(UpdateEvent.Downloaded)
                InstallStatus.INSTALLING -> onEvent(UpdateEvent.Installing)
                InstallStatus.INSTALLED -> {
                    onEvent(UpdateEvent.Installed)
                    release()
                }
                InstallStatus.FAILED -> {
                    onEvent(UpdateEvent.Failed)
                    release()
                }
                else -> Unit
            }
        }
        appUpdateManager.registerListener(listener)
        installListener = listener
    }

    @Suppress("DEPRECATION") // startUpdateFlowForResult keeps a single request-code path across activities
    private fun startFlow(info: AppUpdateInfo, type: Int, activity: Activity, requestCode: Int) {
        appUpdateManager.startUpdateFlowForResult(info, type, activity, requestCode)
    }
}
