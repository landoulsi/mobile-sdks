# Mobile SDKs Roadmap

A suite of Kotlin Multiplatform (Android + iOS) mobile SDKs, including Lifecycle-Aware
ViewModel SDK (:viewmodel), Integrity & Threat Detection SDK (:integrity), Push Notification SDK
(:pushnotification), Tutorial & Onboarding SDK (:tutorial), Payment SDK (Google Pay, Apple Pay,
card checkout, 3DS), In-App Update SDK (flexible/immediate updates, version checking, and What's New /
release notes popups), unified Design system library (:design), Document Processing SDK (:document),
and cross-platform infrastructure libraries (Analytics, Location, Logger, RemoteConfig, Storage).

**Current reality check:** The repository contains modular KMP SDKs (`:diagnostic`, `:viewmodel`,
`:integrity`, `:pushnotification`, `:tutorial`, `:payment`, `:update`, `:logger`, `:location`,
`:biometric`, `:storage`, `:design`, `:analytics`, `:remoteconfig`, `:document`, `:schemaui`, and
`:demo`). Current priority is building the configurable Diagnostic SDK (`:diagnostic`) with domain
models, diagnostic helpers (network, location), and Compose UI for app-specific health checks.

## Goals

- [x] [complexity: moderate] Fix the broken Android build by aligning compileSdk with the AndroidX core 1.19.0 requirement
- [x] [complexity: complex] Initialize the shared Kotlin Multiplatform module with common, Android and iOS source sets
- [x] [complexity: moderate] Define core payment domain models and Google Pay configuration contracts in commonMain
- [x] [complexity: moderate] Implement the Android GooglePayClient and GooglePayProvider wrapping Google Play Services Wallet API
- [x] [complexity: moderate] Implement Google Pay ActivityResult launcher contract and Compose GooglePayButton component
- [x] [complexity: moderate] Integrate GooglePayProvider and wallet flow into CheckoutViewModel and demo MainActivity
- [x] [complexity: moderate] Add Ktor HTTP client and gateway token serialization for Google Pay and card payments
- [x] [complexity: simple] Add a design token layer and payment-oriented Material 3 theme replacing template purple palette
- [x] [complexity: moderate] Build card number, expiry and CVC input components with live formatting and Luhn validation
- [x] [complexity: moderate] Assemble the drop-in checkout sheet with express Google Pay button rendered above card form
- [x] [complexity: simple] Add explicit payment feedback states with inline field errors, processing spinner and confirmation
- [x] [complexity: moderate] Implement iOS ApplePayProvider using PassKit behind the common PaymentProvider abstraction
- [x] [complexity: complex] Add 3D Secure challenge handling and authentication flow to common checkout
- [x] [complexity: complex] Conduct security audit to identify and fix critical vulnerabilities across HTTPS enforcement, 3DS WebView navigation, and data masking
- [x] [complexity: moderate] Mask PAN and redact CVC in toString methods and debug logs to prevent sensitive credential exposure
- [x] [complexity: moderate] Add accessibility and one-handed reachability passes over checkout sheet
- [x] [complexity: moderate] Integrate PayPal or Braintree as alternative payment method
- [x] [complexity: moderate] Create and configure the :styles library module with Jetpack Compose Material 3 support in settings.gradle.kts
- [x] [complexity: moderate] Add IP-based approximate location provider and lastKnownLocation API across providers in :location
- [x] [complexity: moderate] Build and maintain a comprehensive changelog for the update:shared module detailing new features and bug fixes
- [x] [complexity: moderate] Define Event data class and EventTracker interface with standard tracking methods in :analytics commonMain
- [x] [complexity: moderate] Implement Firebase Analytics EventTracker for Android and iOS in :analytics module
- [x] [complexity: simple] Add composite multi-tracker support and unit tests for Event and EventTracker in :analytics
- [x] [complexity: moderate] Define shared design tokens (colors, typography, spacing, radius, elevation) and M3 light/dark theme in :design
- [x] [complexity: moderate] Add reusable common UI components and token helpers (cards, chips, buttons, surface wrappers) in :design
- [x] [complexity: moderate] Define tutorial domain models, step configurations, and persistent completion tracker in :tutorial commonMain
- [x] [complexity: moderate] Implement customizable onboarding pager and feature carousel with swipe gestures and page indicator in :tutorial
- [x] [complexity: moderate] Define PushNotification, NotificationChannel models and PushNotificationManager interface in :pushnotification commonMain
- [x] [complexity: moderate] Define IntegritySignal, IntegrityCategory, IntegrityRiskScore models and IntegrityDetector interface in :integrity commonMain
- [x] [complexity: complex] Implement cross-platform root and jailbreak detection checks covering su binaries, Magisk, Cydia, and sandbox integrity in :integrity
- [x] [complexity: moderate] Create and configure the :viewmodel KMP module in settings.gradle.kts with Android and iOS targets, defining core ViewModel abstraction with viewModelScope and onCleared lifecycle
- [x] [complexity: moderate] Implement cross-platform LifecycleState enum, LifecycleOwner, and LifecycleObserver in :viewmodel commonMain to track active and inactive component lifecycles
- [x] [complexity: moderate] Implement Android bindings in :viewmodel androidMain integrating with androidx.lifecycle.ViewModel and LifecycleOwner for automatic coroutine scope cancellation on cleared
- [x] [complexity: moderate] Define DiagnosticResult, DiagnosticState (PASS/WARNING/ERROR), DiagnosticCheck, and DiagnosticEngine orchestrator in :diagnostic commonMain
- [x] [complexity: moderate] Implement network and location diagnostic helpers detecting VPN, low signal, GPS disabled, and low accuracy in :diagnostic
- [x] [complexity: moderate] Build Compose DiagnosticView with run diagnostics button, status indicator, and result items rendering Pass, Warning, Error and cause text in :diagnostic
- [x] [complexity: simple] Add comprehensive unit and host tests for DiagnosticEngine, result evaluators, and state transitions in :diagnostic
- [x] [complexity: moderate] Add diagnostic showcase screen in :demo:app demonstrating configurable diagnostic checks, Run Diagnostics button, and item result cards
- [x] [complexity: complex] Implement iOS lifecycle bindings in :viewmodel iosMain wrapping UIViewController and SwiftUI lifecycle notifications with Swift-friendly dealloc and scope cancellation hooks
- [x] [complexity: moderate] Add lifecycle-aware Flow extensions like flowWithLifecycle and state preservation utilities in :viewmodel commonMain for UI subscription management
- [x] [complexity: simple] Add unit and host tests in :viewmodel commonTest and androidHostTest verifying ViewModel coroutine cancellation, lifecycle state transitions, and clear callbacks
- [x] [complexity: moderate] Add lifecycle-aware ViewModel showcase screen in :demo:app demonstrating StateFlow observation, coroutine auto-cancellation, and lifecycle event logging
- [x] [complexity: complex] Implement virtual OS, emulator, and parallel space cloning detection for Android and iOS simulator in :integrity
- [x] [complexity: moderate] Implement mock location and GPS spoofing detection covering mock provider APIs, developer settings, and location anomaly checks in :integrity
- [x] [complexity: complex] Implement hooking and tampering detection covering Frida, Xposed, and Substrate dynamic library injection in :integrity
- [ ] [complexity: moderate] Implement network integrity signal detection covering active VPN interfaces, system proxy configurations, and developer ADB status in :integrity
- [x] [complexity: moderate] Implement composite risk scoring engine computing normalized IntegrityRiskScore with configurable thresholds and signal flows in :integrity
- [ ] [complexity: simple] Add comprehensive unit and host tests for integrity signal evaluators, risk score calculations, and detection configurations in :integrity
- [ ] [complexity: moderate] Add integrity detection showcase screen in :demo:app with real-time risk gauge, signal breakdown list, and threat inspection UI
- [ ] [complexity: simple] Implement untrusted installer source detection using PackageManager installer package name and install source info in :integrity
- [ ] [complexity: simple] Implement debugger-attached detection covering Debug.isDebuggerConnected(), /proc/self/status TracerPid, and iOS sysctl P_TRACED flag checks in :integrity
- [ ] [complexity: moderate] Implement app cloning detection covering Android multi-user/work-profile UserManager checks for duplicate native app instances in :integrity
- [ ] [complexity: moderate] Add a default-evaluators factory auto-registering platform-appropriate evaluators without manual wiring in :integrity
- [ ] [complexity: moderate] Wire IntegrityConfig thresholds and enabled categories to :remoteconfig for server-side tuning without a release
- [ ] [complexity: complex] Integrate Play Integrity API (Android) and DeviceCheck/App Attest (iOS) for hardware-backed firmware/OS attestation in :integrity
- [ ] [complexity: moderate] Implement Android push notification manager with FCM, NotificationChannel setup and POST_NOTIFICATIONS in :pushnotification
- [ ] [complexity: moderate] Implement iOS push notification manager wrapping APNs and UNUserNotificationCenter in :pushnotification
- [ ] [complexity: moderate] Add in-app notification banner UI and topic subscription manager in :pushnotification
- [ ] [complexity: simple] Add unit tests for push message parsing, topic validation, and payload serialization in :pushnotification
- [ ] [complexity: moderate] Add push notification showcase screen in :demo:app demonstrating token, local push, and topic subscriptions
- [ ] [complexity: complex] Implement interactive spotlight overlay engine with target cutouts, pointer/finger animations, and tooltip bubble in :tutorial
- [ ] [complexity: moderate] Add multi-step tour orchestrator and automated first-install/new-feature trigger manager in :tutorial
- [ ] [complexity: moderate] Add interactive tutorial and spotlight showcase demo screen in :demo:app demonstrating onboarding and button tour
- [ ] [complexity: simple] Add unit tests for tutorial state machine, persistence tracker, and spotlight layout coordinates in :tutorial
- [ ] [complexity: moderate] Define core Document, DocumentFormat, and DocumentReader/DocumentWriter interfaces with conversion models in :document commonMain
- [ ] [complexity: moderate] Implement cross-platform PDF reading and text/metadata extraction for Android and iOS in :document module
- [ ] [complexity: moderate] Implement cross-platform PDF generation and writing from plain text with page formatting in :document module
- [ ] [complexity: complex] Implement bi-directional Markdown and PDF converter supporting headings, lists, and formatting in :document module
- [ ] [complexity: simple] Add comprehensive unit and host tests for PDF reading, writing, and format conversions in :document module
- [ ] [complexity: moderate] Add a document viewer and converter showcase screen in :demo:app demonstrating PDF, Text, and Markdown workflows
- [ ] [complexity: moderate] Refactor :payment:app, :update:app, and :demo:app to consume the shared :design module and remove duplicate themes
- [ ] [complexity: simple] Add comprehensive unit tests for color tokens, typography scales, dimension values, and theme schemes in :design
- [ ] [complexity: moderate] Implement What's New popup dialog in update:app to display new features and bug fixes
- [ ] [complexity: moderate] Add version changelog tracking and display triggers for What's New popup in UpdateManager
- [ ] [complexity: moderate] Add regional alternative payment methods including Klarna and iDEAL
- [ ] [complexity: simple] Write commonMain unit tests for domain models, validation, formatting and Google Pay config
- [ ] [complexity: moderate] Write Android platform integration tests for Google Pay payment flow
- [ ] [complexity: simple] Establish automated lint, format (ktlint/detekt) and static analysis checks
- [ ] [complexity: simple] Set up CI workflow to build and test shared and app modules on every push
- [ ] [complexity: simple] Generate Dokka API reference documentation for public SDK surface
- [ ] [complexity: simple] Write integrator Getting Started guide with minimal end-to-end Google Pay sample
- [ ] [complexity: simple] Configure Maven Central publishing scripts for shared KMP library

## Scope notes

Guidance for implementing the current and upcoming milestones:

- **Device Diagnostic SDK (`:diagnostic`).** Provide a lightweight, cross-platform Kotlin Multiplatform library for app-configurable device health checks and reactive diagnostic UI:
  - Core domain models: `DiagnosticState` (PASS, WARNING, ERROR), `DiagnosticResult` (id, title, state, cause: String?, timestamp: Long, metadata: Map<String, String>), `DiagnosticCheck` interface (`id: String`, `name: String`, `suspend fun run(): DiagnosticResult`), `DiagnosticSuite` (declarative collection of checks tailored per application).
  - DiagnosticEngine orchestrator: executes diagnostic checks concurrently or sequentially, tracks execution state (IDLE, RUNNING, COMPLETED), and exposes reactive results via Kotlin Coroutines `StateFlow<DiagnosticUiState>`.
  - Built-in diagnostic helpers:
    - Network diagnostic helper: evaluates active connectivity, detects low signal strength / packet degradation (WARNING), active VPN or system proxy configurations (WARNING), and complete offline state (ERROR).
    - Location diagnostic helper: inspects GPS / location service availability, identifies disabled location services (ERROR), missing runtime permissions (ERROR), or low accuracy / coarse provider active (WARNING).
  - Diagnostic UI (`DiagnosticView` / `DiagnosticScreen`):
    - Top header with prominent "Run Diagnostics" button and animated loading indicator.
    - System health summary pill reflecting overall status (e.g., "All systems operational", "2 warnings detected", "1 error").
    - Results list where each item displays its title, 3-state badge / status icon (Pass in green, Warning in amber, Error in red), and human-readable cause below explaining why a warning or error occurred.
    - Modular check composition: allows individual apps (rider, driver, courier, retail) to pass custom sets of diagnostic checks.
- **Lifecycle-Aware ViewModel SDK (`:viewmodel`).** Provide a lightweight, cross-platform Kotlin Multiplatform library for lifecycle-aware state holders and coroutine orchestration across Android and iOS:
  - Base abstraction: `ViewModel` class providing a bound `viewModelScope` (SupervisorJob + Main/Default dispatcher) and `onCleared()` lifecycle callback.
  - Lifecycle state: `LifecycleState` (INITIALIZED, CREATED, STARTED, RESUMED, DESTROYED), `LifecycleOwner`, and `LifecycleObserver` event listeners for foreground/background and visibility transitions.
  - Android bindings: Seamless interop with AndroidX `androidx.lifecycle.ViewModel`, `ViewModelProvider`, and Jetpack Compose lifecycle without boilerplate.
  - iOS bindings: Native Swift / SwiftUI lifecycle bridge hooking view `onAppear`/`onDisappear`, `UIViewController` lifecycle methods, and Swift deinit cancellation hooks.
  - Reactive Flow extensions: `Flow.flowWithLifecycle(...)`, `collectAsStateWithLifecycle` equivalents, and state preservation utilities.
  - Demo App Showcase: Compose screen displaying live lifecycle transitions, counter/state streams, background job auto-cancellation, and logging.
- **Integrity & Threat Detection SDK (`:integrity`).** Provide a Kotlin Multiplatform library for comprehensive device integrity, tampering, and threat signal detection across Android and iOS:
  - Domain models: `IntegritySignal` (id, name, category, severity, details, detectedAt, confidence), `IntegrityCategory` (ROOT_OR_JAILBREAK, VIRTUAL_OS_OR_EMULATOR, MOCK_LOCATION, HOOKING_OR_TAMPERING, DEBUGGER_ATTACHED, APP_CLONING, NETWORK_ANOMALY, UNTRUSTED_INSTALLER), `SignalSeverity` (INFO, LOW, MEDIUM, HIGH, CRITICAL), `IntegrityRiskScore` (score 0-100, riskLevel: LOW/MEDIUM/HIGH/CRITICAL, action: ALLOW/WARN/CHALLENGE/BLOCK, signals: List<IntegritySignal>), `IntegrityConfig` (thresholds, enabled categories, custom weights).
  - IntegrityDetector interface: `detectSignals(): List<IntegritySignal>`, `evaluateRisk(): IntegrityRiskScore`, `observeSignals(): Flow<List<IntegritySignal>>`, `evaluateCategory(category: IntegrityCategory): List<IntegritySignal>`.
  - Platform detection checks:
    - Root / Jailbreak: Android `su` binary inspection (`/system/bin/su`, `/system/xbin/su`, `/sbin/su`), Magisk / KernelSU mounts and packages, `test-keys` build tags, writable system mounts; iOS Cydia / Sileo / Zebra app paths (`/Applications/Cydia.app`), `/bin/sh`, `/usr/sbin/sshd`, fork() capability, and sandbox escape checks.
    - Virtual OS & Emulator: Android build properties (goldfish, ranchu, generic, sdk_gphone, vbox86, qemu), absence of standard hardware sensors, telephony device ID anomalies, parallel space sandboxes (VirtualApp, DualSpace, Parallel Space UID/path anomalies); iOS `TARGET_OS_SIMULATOR` and sysctl model inspection.
    - Mock Location & GPS Spoofing: Android `Location.isMock` (API 31+) / `Location.isFromMockProvider()`, `Settings.Secure.ALLOW_MOCK_LOCATION`, mock location provider active, developer mock app selection, and impossible velocity/jump anomalies.
    - Hooking & Tampering: Frida server port 27042 inspection, `/proc/self/maps` scanning for `frida` and `gadget.so`, Xposed framework classes (`XposedBridge`), Substrate / Substitute dynamic library injection, debugger attach (`Debug.isDebuggerConnected()`, `P_TRACED`, `TracerPid`).
    - Network & Proxy Anomaly: Active VPN interface detection (`NetworkCapabilities.TRANSPORT_VPN`), system HTTP proxy configuration (`http.proxyHost`), and developer ADB status.
  - Risk Scoring Engine: Composite 0-100 score weighted by severity (CRITICAL: 40 pts, HIGH: 25 pts, MEDIUM: 15 pts, LOW: 5 pts) mapped to actionable verdicts (`ALLOW`, `WARN`, `CHALLENGE`, `BLOCK`).
  - Demo App Showcase: Compose UI with risk radar gauge, threat indicator chips, categorized signal drill-downs, and manual test triggers.
- **Push Notification SDK (`:pushnotification`).** Provide a Kotlin Multiplatform library for remote and local push notification delivery, permission handling, token registration, topic subscriptions, and in-app alert presentation across Android and iOS:
  - Domain models: `PushNotification` (id, title, body, imageUrl, data/payload map, sound, badge, channelId, clickAction, timestamp, priority), `NotificationChannel` (id, name, description, importance, sound, vibration, badgeEnabled), `NotificationCategory`, `PushPermissionStatus` (Granted, Denied, NotDetermined, Ephemeral).
  - Push Notification Manager interface (`PushNotificationManager`): `getToken()`, `tokenFlow`, `requestPermission()`, `hasPermission()`, `subscribeToTopic(topic)`, `unsubscribeFromTopic(topic)`, `showLocalNotification(notification)`, `clearNotification(id)`, `clearAllNotifications()`, `messageFlow`, `notificationClickFlow`.
  - Android FCM & NotificationManager implementation: `FirebasePushNotificationManager` wrapping Firebase Cloud Messaging, `FirebaseMessagingService` background receiver, Android 13+ `POST_NOTIFICATIONS` runtime permission request integration, custom `NotificationChannel` creation with importance levels, deep-link pending intent handling, and foreground presentation.
  - iOS APNs & UNUserNotificationCenter implementation: `IosPushNotificationManager` utilizing `UNUserNotificationCenter` for remote registration, APNs device token retrieval, alert/badge/sound permission requests, foreground notification presentation options, and background payload handlers.
  - In-App UI & Topic Management: Jetpack Compose / KMP `InAppNotificationBanner` for heads-up alert display with slide animations and touch gestures, coupled with subscription management for topical push messaging.
- **Tutorial & Onboarding SDK (`:tutorial`).** Provide a Kotlin Multiplatform library for app onboarding and feature discovery:
  - Domain models: `TutorialStep`, `TutorialPage`, `SpotlightTarget`, `SpotlightShape` (Circle, RoundedRectangle, Oval), `PointerStyle` (Hand/Finger, Arrow, PulseRing), `TooltipPosition` (Top, Bottom, Start, End, Auto), `TutorialConfig`, `TutorialTracker`.
  - Onboarding Pager: Jetpack Compose / KMP `TutorialPager` and `OnboardingScreen` supporting swipeable card carousels, animated progress dots/bars, custom illustration/Lottie slots, title, description, and skip/next/get-started actions.
  - Interactive Spotlight & Coach Marks: `SpotlightOverlay` with dynamic canvas cutout masking (`BlendMode.Clear`), pulsating target highlight rings, animated finger/hand pointer gestures pointing directly to UI elements (buttons, icons, cards), rich tooltip callout balloons with title, description, and action controls.
  - Tour Controller & Persistence: `TutorialController` for multi-step guided sequence execution, target measurement coordination via Compose modifiers (`Modifier.spotlightTarget(...)`), and `TutorialTracker` (backed by key-value storage) tracking seen steps to automatically show tutorials on fresh installs or when new features are introduced.
- **Document Processing SDK (`:document`).** Provide a Kotlin Multiplatform library for reading, creating, and converting
  documents across PDF, Markdown (`.md`), and plain text (`.txt`) formats:
  - `Document`, `DocumentFormat` (PDF, PLAIN_TEXT, MARKDOWN), `DocumentMetadata` (title, author, page count, creation date).
  - Reader/Writer contracts: `DocumentReader`, `DocumentWriter`, `PdfReader`, `PdfWriter`.
  - Platform implementations for PDF handling:
    - Android: `android.graphics.pdf.PdfRenderer` (for reading and rendering pages/extracting text) and `android.graphics.pdf.PdfDocument` (for drawing pages, text layouts, headers/footers).
    - iOS: `PDFKit` (`PDFDocument`, `PDFPage`) and `UIGraphicsPDFRenderer` / CoreGraphics for native PDF rendering and creation.
  - Bi-directional conversion engine: `DocumentConverter` supporting `pdfToText`, `textToPdf`, `pdfToMarkdown`, `markdownToPdf`, and `markdownToText`.
- **Analytics SDK (`:analytics`).** Define generic cross-platform analytics interfaces and data models in `commonMain`:
  `Event` (name, parameter map of primitives/strings/numbers), `EventTracker` (or `EventManager`) with methods
  like `track(event: Event)`, `track(name: String, params: Map<String, Any?>)`, `setUserId(userId: String?)`,
  and `setUserProperty(name: String, value: String?)`. Provide concrete platform implementations (such as Firebase
  Analytics wrapping Android's `FirebaseAnalytics` and iOS Firebase SDK or no-op/delegation fallbacks) and a
  composite `CompositeEventTracker` for broadcasting events to multiple backends simultaneously.
- **Common Design System (`:design`).** Centralize colors (Blue primary, Teal secondary, Amber tertiary,
  Red error, Neutral surface scales, brand/status tokens), typography scales (Display, Headline, Title, Body, Label),
  dimensions (Spacing xxs..xxxl, Radius xs..full, Elevation none..xl, TypeSize display..caption), and `AppTheme`
  with dynamic color and light/dark theme schemes into the reusable `:design` module.
- **UI Component Library (`:design`).** Provide reusable surface wrappers, card containers, badge chips, buttons,
  status indicators, and modifier extensions to eliminate UI code duplication across apps.
- **Consumer App Integration.** Replace duplicate `com.landoulsi.payment.ui.theme` and `com.landoulsi.update.ui.theme`
  with dependencies on `:design`, standardizing visual design tokens across all demo and production apps.
- **What's New & Release Notes (`:update`).** Add domain models (`ReleaseNotes`, `ReleaseItem`, `ReleaseCategory`
  for `FEATURE` and `BUG_FIX`) in `update:shared`. Support parsing release notes from `UpdateConfig` or local bundles.
- **What's New UI (`:update:app`).** Build a Jetpack Compose dialog/bottom sheet displaying categorized items
  with badge chips (e.g. "New Feature", "Bug Fix"), release date/version, and action buttons ("Update Now", "Got It", "Later").
- **Update Triggers & Tracking (`:update`).** Coordinate `UpdateManager` with version history to trigger the What's New popup
  on first launch of an updated version or when an optional/recommended update is available.
- **Security & Vulnerability Hardening.** Audit gateway networking (enforce HTTPS for production endpoints),
  restrict WebView scheme handling and file access in 3DS challenges, and eliminate plaintext PAN/CVC logging
  and default data class `toString()` credential leaks.

## Design direction

Competitive review of modern design systems, device integrity engines (SHIELD, Incode, ThreatMetrix, Sift, Approov), device diagnostics tools (Uber Driver, Lyft, Apple Diagnostics), push notification frameworks, onboarding engines, and mobile SDKs:

- **Modular Diagnostic Health Checks.** Distinct 3-state evaluation taxonomy (`PASS`, `WARNING`, `ERROR`) pairing emerald green checkmarks, amber warning shields, and crimson error octagons with clear, human-readable cause descriptions.
- **Top-Anchored Actionable Diagnostics.** A high-visibility "Run Diagnostics" action button at the top of the screen with active test progress feedback, accompanied by individual card re-run triggers and deep links to system settings where applicable.
- **Structured ViewModel Lifecycle & Auto-Cancellation.** Deterministic coroutine cancellation on view disappearance or component destruction preventing memory leaks and orphaned network/compute tasks across both Android and iOS.
- **Unified Reactive State Observation.** Clean StateFlow and SharedFlow observation pipelines adapted for Compose (Android) and SwiftUI/Combine (iOS) with zero platform boilerplate.
- **Threat Radar & Real-Time Risk Gauge.** A high-visibility Circular Gauge / Speedometer (0-100) reflecting overall device health with clear semantic color banding (Green = Secure, Amber = Elevated, Orange = High, Red = Critical) and explicit mitigation badges ("ALLOW", "WARN", "CHALLENGE", "BLOCK").
- **Modular Signal Breakdown Cards.** Clean categorized accordion cards separating Root/Jailbreak, Virtual OS/Emulator, Mock Location, Tampering, and Network signals with severity badges, detection timestamps, and remediation suggestions.
- **Non-blocking Background Telemetry.** Security sweeps evaluate asynchronously off the main thread, caching results and notifying observers via reactive Kotlin Flows without UI stutters.
- **Simulation & Sandbox Playground.** In demo apps, developers can toggle simulated mock GPS, root traces, or proxy settings to verify reactive UI adaptations without altering real device state.
- **Unified Push & Local Messaging Model.** Single normalized `PushNotification` structure handling both remote FCM/APNs messages and local notifications with payload dictionaries, action deep links, and category actions.
- **Explicit Permission Lifecycle.** Structured `PushPermissionStatus` handling Android 13+ runtime POST_NOTIFICATIONS permission dialogs and iOS notification authorization requests with clear status transitions.
- **Fine-Grained Notification Channels.** Comprehensive channel configuration supporting importance/priority levels, sound URIs, vibration patterns, and badge flags matching modern Android O+ requirements.
- **Zero-Friction Onboarding Carousels.** Full-bleed swipeable onboarding cards with fluid animations, intuitive page indicators, accessible skip options, and clear calls-to-action ("Get Started", "Continue").
- **Interactive Feature Spotlight & Coach Marks.** Non-intrusive backdrop dimming with clean cutouts around target UI elements, animated finger tap / pulse indicators directing attention, and context-aware tooltips with auto-positioning.
- **Declarative Compose Modifiers.** Seamless integration via `Modifier.spotlightTarget(tag = "button_id")` allowing developers to annotate existing UI components with zero invasive layout changes.
- **Smart Triggering & State Persistence.** Local persistence of completed tutorials preventing annoying repeated prompts while supporting feature-specific version triggers for new releases.
- **Modular Document Pipeline.** Clear separation between document parsing/reading, structured content representation, layout formatting, and binary serialization across platforms.
- **Bi-directional Conversion Fidelity.** Preserve headings (H1-H6), bulleted/numbered lists, inline emphasis (bold, italic), code snippets, and paragraphs when converting between Markdown, plain text, and PDF formats.
- **Clean Analytics Abstractions.** Decouple app analytics instrumentation from vendor SDKs via provider-agnostic interfaces (`EventTracker`), typed event builders, and flexible parameter structures.
- **Unified design tokens.** Single source of truth for semantic colors, 8-pt spacing scales, rounded corners, elevations, and typography hierarchies ensuring visual consistency across all apps.
- **Semantic hierarchy.** Clear contrast between primary actions (trustworthy blue), secondary accents (teal), tertiary highlights (warm amber), and error/destructive feedback (red).
- **Categorized changelog items.** Distinguish new features (sparkle icon / primary badge) from bug fixes (wrench icon / neutral badge) so users quickly grasp value and improvements.
- **Wallets first, card second.** Express payment buttons belong at the top of checkout sheets above an "Or pay with card" divider.
- **Immediate, explicit feedback.** Provide distinct states: disabled & spinning indicator during action authorization, clear inline error messages, and animated confirmation.
- **One-handed reachability.** Primary actions reside in the lower half of the viewport, supporting thumb reachability and smooth keyboard avoidance.

## Phases

- **Device Diagnostic SDK** — DiagnosticState & DiagnosticResult models, DiagnosticCheck & DiagnosticEngine, network & location diagnostic helpers, Compose DiagnosticView, demo showcase screen.
- **Lifecycle-Aware ViewModel SDK** — ViewModel base abstraction, viewModelScope coroutine management, LifecycleState machine, AndroidX ViewModel interop, iOS lifecycle bridge, Flow extensions, demo screen.
- **Integrity & Threat Detection SDK** — IntegritySignal & IntegrityRiskScore models, root/jailbreak detection, emulator/virtual OS checks, mock location detector, hooking/Frida/Xposed defense, network VPN/proxy anomalies, composite scoring engine, showcase UI.
- **Push Notification SDK** — PushNotification domain models, Android FCM & NotificationChannel integration, iOS APNs & UNUserNotificationCenter wrapper, in-app notification banner UI, topic management, demo showcase screen.
- **Tutorial & Onboarding SDK** — onboarding carousel pager, interactive spotlight & coach mark overlay, finger/hand pointing animations, multi-step sequence orchestrator, showcase demo screen.
- **Foundation & Core Payment SDK** — core domain models, Google Pay provider, Apple Pay provider, 3DS, card checkout.
- **Cross-Platform Analytics SDK** — Event model, EventTracker interface, Firebase Analytics provider, composite tracker.
- **Document Processing & Conversion SDK** — Document domain models, Android & iOS PDF reading/writing, Markdown-PDF bi-directional converter, demo viewer screen.
- **Design System & Common UI** — shared `:design` module, design tokens, light/dark themes, common UI components, app refactoring.
- **In-App Update SDK & Release Notes** — update version checker, native update integration, What's New release notes popup.
- **Payment Methods Expansion** — PayPal/Braintree alternative methods, regional payment methods (Klarna, iDEAL).
- **Hardening & Quality Assurance** — unit tests, platform integration tests, security audits, static analysis.
- **Release & Distribution** — CI/CD automation, Dokka documentation, sample app polish, Maven Central publication.
