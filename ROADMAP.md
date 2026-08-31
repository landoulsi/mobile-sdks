# Mobile SDKs Roadmap

A suite of Kotlin Multiplatform (Android + iOS) mobile SDKs, including Tutorial & Onboarding SDK
(:tutorial), Payment SDK (Google Pay, Apple Pay, card checkout, 3DS), In-App Update SDK (flexible/immediate
updates, version checking, and What's New / release notes popups), unified Design system library (:design),
Document Processing SDK (:document), and cross-platform infrastructure libraries (Analytics, Location,
Logger, RemoteConfig, Storage).

**Current reality check:** The repository contains modular KMP SDKs (`:tutorial`, `:payment`, `:update`,
`:logger`, `:location`, `:security`, `:storage`, `:design`, `:analytics`, `:remoteconfig`, `:document`,
and `:demo`). Current priority is building the `:tutorial` module into a reusable onboarding and feature
walkthrough SDK featuring swipeable onboarding carousels and interactive button/target spotlight coach marks.

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
- [ ] [complexity: moderate] Implement customizable onboarding pager and feature carousel with swipe gestures and page indicator in :tutorial
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

Competitive review of modern design systems, onboarding engines, and mobile SDKs (Google Apps, Duolingo, Slack, TapTargetView, ShowcaseView, Balloon, Adobe PDF SDK, Material 3):

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

- **Tutorial & Onboarding SDK** — onboarding carousel pager, interactive spotlight & coach mark overlay, finger/hand pointing animations, multi-step sequence orchestrator, showcase demo screen.
- **Foundation & Core Payment SDK** — core domain models, Google Pay provider, Apple Pay provider, 3DS, card checkout.
- **Cross-Platform Analytics SDK** — Event model, EventTracker interface, Firebase Analytics provider, composite tracker.
- **Document Processing & Conversion SDK** — Document domain models, Android & iOS PDF reading/writing, Markdown-PDF bi-directional converter, demo viewer screen.
- **Design System & Common UI** — shared `:design` module, design tokens, light/dark themes, common UI components, app refactoring.
- **In-App Update SDK & Release Notes** — update version checker, native update integration, What's New release notes popup.
- **Payment Methods Expansion** — PayPal/Braintree alternative methods, regional payment methods (Klarna, iDEAL).
- **Hardening & Quality Assurance** — unit tests, platform integration tests, security audits, static analysis.
- **Release & Distribution** — CI/CD automation, Dokka documentation, sample app polish, Maven Central publication.
