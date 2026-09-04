package com.landoulsi.fraud.simulator

import platform.Foundation.NSBundle
import platform.Foundation.NSProcessInfo

/**
 * iOS implementation of [SimulatorCheckContext] wiring platform framework types
 * ([NSProcessInfo], [NSBundle]) into simulator detection heuristics.
 */
class IosSimulatorCheckContext : SimulatorCheckContext {

    override fun getEnvironmentVariable(name: String): String? =
        NSProcessInfo.processInfo.environment[name]?.toString()

    override fun isBundlePathWithinCoreSimulator(): Boolean =
        NSBundle.mainBundle.bundlePath.contains("CoreSimulator", ignoreCase = true)
}
