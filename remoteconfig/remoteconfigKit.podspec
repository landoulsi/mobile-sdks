Pod::Spec.new do |spec|
    spec.name                     = 'remoteconfigKit'
    spec.version                  = '1.0.0'
    spec.homepage                 = 'https://github.com/landoulsi/mobile-sdks'
    spec.source                   = { :http=> ''}
    spec.authors                  = ''
    spec.license                  = 'Apache-2.0'
    spec.summary                  = 'Cross-platform remote configuration with Firebase Remote Config'
    spec.vendored_frameworks      = 'build/cocoapods/framework/remoteconfigKit.framework'
    spec.libraries                = 'c++'
    spec.ios.deployment_target    = '16.0'
    spec.dependency 'FirebaseRemoteConfig', '~> 11.0'
    if !Dir.exist?('build/cocoapods/framework/remoteconfigKit.framework') || Dir.empty?('build/cocoapods/framework/remoteconfigKit.framework')
        raise "
        Kotlin framework 'remoteconfigKit' doesn't exist yet, so a proper Xcode project can't be generated.
        'pod install' should be executed after running ':generateDummyFramework' Gradle task:
            ./gradlew :remoteconfig:generateDummyFramework
        Alternatively, proper pod installation is performed during Gradle sync in the IDE (if Podfile location is set)"
    end
    spec.xcconfig = {
        'ENABLE_USER_SCRIPT_SANDBOXING' => 'NO',
    }
    spec.pod_target_xcconfig = {
        'KOTLIN_PROJECT_PATH' => ':remoteconfig',
        'PRODUCT_MODULE_NAME' => 'remoteconfigKit',
    }
    spec.script_phases = [
        {
            :name => 'Build remoteconfigKit',
            :execution_position => :before_compile,
            :shell_path => '/bin/sh',
            :script => <<-SCRIPT
                if [ "YES" = "$OVERRIDE_KOTLIN_BUILD_IDE_SUPPORTED" ]; then
                    echo "Skipping Gradle build task invocation due to OVERRIDE_KOTLIN_BUILD_IDE_SUPPORTED environment variable set to \"YES\""
                    exit 0
                fi
                set -ev
                REPO_ROOT="$PODS_TARGET_SRCROOT"
                "$REPO_ROOT/../gradlew" -p "$REPO_ROOT" $KOTLIN_PROJECT_PATH:syncFramework \
                    -Pkotlin.native.cocoapods.platform=$PLATFORM_NAME \
                    -Pkotlin.native.cocoapods.archs="$ARCHS" \
                    -Pkotlin.native.cocoapods.configuration="$CONFIGURATION"
            SCRIPT
        }
    ]
end
