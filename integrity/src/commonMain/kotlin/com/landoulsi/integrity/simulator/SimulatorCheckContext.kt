package com.landoulsi.integrity.simulator

/**
 * Abstraction for platform-specific operations used by iOS simulator detection checks.
 *
 * Decouples detection heuristics from iOS framework types ([NSProcessInfo], [NSBundle])
 * to enable host testing with fakes.
 */
interface SimulatorCheckContext {
    fun getEnvironmentVariable(name: String): String?

    /**
     * Whether the running app's bundle resolves inside a CoreSimulator device sandbox.
     * Exposed as a boolean rather than the raw path since the simulator's path embeds
     * the host Mac's local username, which must never end up in signal metadata.
     */
    fun isBundlePathWithinCoreSimulator(): Boolean
}
