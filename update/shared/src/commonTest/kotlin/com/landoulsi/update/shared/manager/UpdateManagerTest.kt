package com.landoulsi.update.shared.manager

import com.landoulsi.update.shared.model.UpdateConfig
import com.landoulsi.update.shared.model.UpdateState
import kotlin.test.Test
import kotlin.test.assertTrue

class UpdateManagerTest {

    private val updateManager = UpdateManager()

    @Test
    fun testUpdateRequiredWhenCurrentIsBelowMin() {
        val config = UpdateConfig(latestVersion = "2.0.0", minRequiredVersion = "1.5.0")
        val state = updateManager.checkUpdate("1.0.0", config)
        assertTrue(state is UpdateState.UpdateRequired)
    }

    @Test
    fun testUpdateRecommendedWhenCurrentIsBelowLatestButAboveMin() {
        val config = UpdateConfig(latestVersion = "2.0.0", minRequiredVersion = "1.0.0")
        val state = updateManager.checkUpdate("1.5.0", config)
        assertTrue(state is UpdateState.UpdateRecommended)
    }

    @Test
    fun testNoUpdateWhenCurrentIsLatest() {
        val config = UpdateConfig(latestVersion = "2.0.0", minRequiredVersion = "1.0.0")
        val state = updateManager.checkUpdate("2.0.0", config)
        assertTrue(state is UpdateState.NoUpdate)
    }
    
    @Test
    fun testUpdateRequiredWhenFlagIsTrue() {
        val config = UpdateConfig(latestVersion = "2.0.0", minRequiredVersion = "1.0.0", isUpdateRequired = true)
        val state = updateManager.checkUpdate("1.5.0", config)
        assertTrue(state is UpdateState.UpdateRequired)
    }
}
