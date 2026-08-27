# Mobile SDKs Roadmap

A suite of Kotlin Multiplatform (Android + iOS) mobile SDKs, including Payment SDK
(Google Pay, Apple Pay, card checkout, 3DS), In-App Update SDK (flexible/immediate updates,
version checking, and What's New / release notes popups), and a unified Styles design system library
(:styles) providing shared Material 3 tokens, colors, typography, dimensions, and UI components.

**Current reality check:** The repository contains modular KMP SDKs (`:payment`, `:update`, `:logger`,
`:location`, `:security`, `:storage`, and `:demo`). The Payment SDK supports Google Pay,
Apple Pay, PayPal, 3D Secure, and card validation. Current priority is establishing a dedicated `:styles`
module consolidating common theme tokens (blue/teal/amber/red palettes, neutral scales, dimensions, typography)
and shared Compose UI components across `:payment:app`, `:update:app`, `:demo:app`, and future apps.

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
- [ ] [complexity: moderate] Create and configure the :styles library module with Jetpack Compose Material 3 support in settings.gradle.kts
- [ ] [complexity: moderate] Define shared design tokens (colors, typography, spacing, radius, elevation) and M3 light/dark theme in :styles
- [ ] [complexity: moderate] Add reusable common UI components and token helpers (cards, chips, buttons, surface wrappers) in :styles
- [ ] [complexity: moderate] Refactor :payment:app, :update:app, and :demo:app to consume the shared :styles module and remove duplicate themes
- [ ] [complexity: simple] Add comprehensive unit tests for color tokens, typography scales, dimension values, and theme schemes in :styles
- [ ] [complexity: moderate] Add release notes and changelog models for new features and bug fixes in update:shared
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

- **Common Styles & Design System (`:styles`).** Centralize colors (Blue primary, Teal secondary, Amber tertiary,
  Red error, Neutral surface scales, brand/status tokens), typography scales (Display, Headline, Title, Body, Label),
  dimensions (Spacing xxs..xxxl, Radius xs..full, Elevation none..xl, TypeSize display..caption), and `AppTheme`
  with dynamic color and light/dark theme schemes into a reusable `:styles` module.
- **UI Component Library (`:styles`).** Provide reusable surface wrappers, card containers, badge chips, buttons,
  status indicators, and modifier extensions to eliminate UI code duplication across apps.
- **Consumer App Integration.** Replace duplicate `com.landoulsi.payment.ui.theme` and `com.landoulsi.update.ui.theme`
  with dependencies on `:styles`, standardizing visual design tokens across all demo and production apps.
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

Competitive review of modern design systems (Google Material 3, Apple Human Interface Guidelines, Stripe Elements,
and GitHub Primer) informs the styles architecture:

- **Unified design tokens.** Single source of truth for semantic colors, 8-pt spacing scales, rounded corners,
  elevations, and typography hierarchies ensuring visual consistency across all apps.
- **Semantic hierarchy.** Clear contrast between primary actions (trustworthy blue), secondary accents (teal),
  tertiary highlights (warm amber), and error/destructive feedback (red).
- **Categorized changelog items.** Distinguish new features (sparkle icon / primary badge) from bug fixes
  (wrench icon / neutral badge) so users quickly grasp value and improvements.
- **Wallets first, card second.** Express payment buttons belong at the top of checkout sheets above an "Or pay with card" divider.
- **Immediate, explicit feedback.** Provide distinct states: disabled & spinning indicator during action authorization,
  clear inline error messages, and animated confirmation.
- **One-handed reachability.** Primary actions reside in the lower half of the viewport, supporting thumb reachability
  and smooth keyboard avoidance.

## Phases

- **Foundation & Core Payment SDK** — core domain models, Google Pay provider, Apple Pay provider, 3DS, card checkout.
- **Styles & Common Design System** — shared `:styles` module, design tokens, light/dark themes, common UI components, app refactoring.
- **In-App Update SDK & Release Notes** — update version checker, native update integration, What's New release notes popup.
- **Payment Methods Expansion** — PayPal/Braintree alternative methods, regional payment methods (Klarna, iDEAL).
- **Hardening & Quality Assurance** — unit tests, platform integration tests, security audits, static analysis.
- **Release & Distribution** — CI/CD automation, Dokka documentation, sample app polish, Maven Central publication.
