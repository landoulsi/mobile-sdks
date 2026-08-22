# Payment SDK Roadmap

A mobile payment SDK targeting Kotlin Multiplatform (Android + iOS), with wallet
payments (Google Pay / Apple Pay) and a drop-in card checkout as the headline features.

**Current reality check:** the repository contains a Kotlin Multiplatform structure
under `mobile/` with an Android application module (`:app`) and a shared multiplatform
module (`:shared`) targeting Android and iOS (`commonMain`, `androidMain`, `iosMain`).
Active priority is security hardening, HTTPS validation, and credential protection.

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
- [ ] [complexity: moderate] Mask PAN and redact CVC in toString methods and debug logs to prevent sensitive credential exposure
- [ ] [complexity: moderate] Add accessibility and one-handed reachability passes over checkout sheet
- [ ] [complexity: moderate] Integrate PayPal or Braintree as alternative payment method
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

- **Security & Vulnerability Hardening.** Audit gateway networking (enforce HTTPS for production endpoints),
  restrict WebView scheme handling and file access in 3DS challenges, and eliminate plaintext PAN/CVC logging
  and default data class `toString()` credential leaks.
- **Google Pay Domain Models & Contracts (`commonMain`).** Define platform-agnostic models
  including `PaymentRequest`, `PaymentResult`, `Money`, `Currency`, `PaymentMethod`, `GooglePayConfig`
  (environment, merchant ID/name, allowed card networks, auth methods `PAN_ONLY`/`CRYPTOGRAM_3DS`,
  tokenization spec for gateways like Stripe/Adyen/direct), and the `PaymentProvider` interface.
- **Android GooglePayProvider (`androidMain`).** Integrate `com.google.android.gms:play-services-wallet`
  behind `PaymentProvider`. Implement `isReadyToPay` readiness check and `PaymentDataRequest` JSON builder
  compliant with Google Pay API v2 (`apiVersion: 2, apiVersionMinor: 0`).
- **Google Pay UI & Activity Result (`:app` / Compose).** Build a Jetpack Compose `GooglePayButton`
  following Google Pay Brand Guidelines (official assets, dark/light theme options, minimum 48dp touch target)
  and manage resolution via `rememberLauncherForActivityResult` with `AutoResolveHelper` or `GetPaymentDataContract`.
- **Shared Module Architecture.** Maintain clean separation where platform-specific wallet APIs
  live in target source sets (`androidMain` for Google Pay, `iosMain` for Apple Pay) and are
  orchestrated by common view models and state reducers in `commonMain`.

## Design direction

Competitive review of Stripe PaymentSheet, Adyen Drop-in, Braintree Drop-in, Square
In-App Payments, and Google Pay Brand Guidelines informs the SDK's UX architecture:

- **Wallets first, card second.** Express Google Pay buttons belong at the top of the sheet,
  above an "Or pay with card" divider. Burying wallet buttons is the leading cause of mobile drop-off.
- **Dynamic wallet availability.** The Google Pay button must only render if `isReadyToPay`
  returns true on the user's device. If unavailable, the UI cleanly collapses to card checkout.
- **Google Pay Brand Compliance.** Google Pay buttons must strictly adhere to Google's brand
  rules (appropriate contrast, no distorted logos, localized button labels like "Pay with GPay",
  minimum 48dp touch height).
- **A drop-in sheet, not a screen.** A pre-built bottom sheet keeps the shopper in the host
  app and keeps card/wallet data off the integrator's servers, holding them in the lightest PCI
  tier (SAQ A).
- **Immediate, explicit feedback.** Provide distinct states: disabled & spinning indicator during
  payment authorization, clear inline error messages if user cancels or card is declined, and an
  animated success confirmation.
- **One-handed reachability.** Primary payment triggers and express buttons reside in the lower
  half of the viewport, supporting seamless thumb reachability and keyboard avoidance.

## Phases

- **Foundation & Google Pay** — core domain models, Google Pay provider, Compose button, sample checkout.
- **Core checkout** — Ktor gateway client, payment theme tokens, card inputs with Luhn validation, drop-in sheet.
- **iOS & Wallets** — Apple Pay provider with PassKit, unified cross-platform wallet orchestration.
- **Hardening & Security** — 3D Secure challenges, PCI compliance audit, vulnerability scan, unit & platform integration tests.
- **Reach** — PayPal, Braintree, and regional payment methods (Klarna, iDEAL).
- **Release** — CI/CD automation, Dokka documentation, sample app polish, Maven Central release.
