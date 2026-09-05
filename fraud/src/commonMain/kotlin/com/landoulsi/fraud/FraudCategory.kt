package com.landoulsi.fraud

/**
 * Categories of fraud signals. Each category represents a different type of
 * threat or suspicious activity detected on the device.
 */
enum class FraudCategory {
    ROOT_DETECTION,
    JAILBREAK_DETECTION,
    EMULATOR_DETECTION,
    VIRTUAL_OS_DETECTION,
    MOCK_LOCATION_DETECTION,
    FRIDA_HOOKING,
    XPOSED_HOOKING,
    SUBSTRATE_HOOKING,
    APP_CLONER_DETECTION,
    NETWORK_VPN_DETECTION,
    NETWORK_PROXY_DETECTION,
    SIM_SWAP_DETECTION,
    DEVICE_CLONING_DETECTION,
    UNAUTHORIZED_ACCESS_ATTEMPT,
    UNUSUAL_NETWORK_ACTIVITY,
    SUSPICIOUS_PERMISSION_USAGE
}
