# Payment SDK Roadmap

A mobile payment SDK targeting Kotlin Multiplatform (Android + iOS), with wallet
payments (Google Pay / Apple Pay) and a drop-in card checkout as the headline features.

**Current reality check:** the repository contains a Kotlin Multiplatform structure
under `mobile/` with an Android application module (`:app`) and a shared multiplatform
module (`:shared`) targeting Android and iOS (`commonMain`, `androidMain`, `iosMain`).
Active priority is integrating Google Pay on Android and establishing domain models.

## Goals

- [x] [tool: antigravity] Fix the broken Android build by aligning compileSdk with the AndroidX core 1.19.0 requirement
- [x] [tool: antigravity] Initialize the shared Kotlin Multiplatform module with common, Android and iOS source sets
- [x] [tool: antigravity] Define core payment domain models and Google Pay configuration contracts in commonMain
- [x] [tool: antigravity] Implement the Android GooglePayClient and GooglePayProvider wrapping Google Play Services Wallet API
- [x] [tool: antigravity] Implement Google Pay ActivityResult launcher contract and Compose GooglePayButton component
- [x] [tool: antigravity] Integrate GooglePayProvider and wallet flow into CheckoutViewModel and demo MainActivity
- [x] [tool: claude] Add Ktor HTTP client and gateway token serialization for Google Pay and card payments
- [x] [tool: opencode] Add a design token layer and payment-oriented Material 3 theme replacing template purple palette
- [x] [tool: claude] Build card number, expiry and CVC input components with live formatting and Luhn validation
- [ ] [tool: antigravity] Assemble the drop-in checkout sheet with express Google Pay button rendered above card form
- [ ] [tool: opencode] Add explicit payment feedback states with inline field errors, processing spinner and confirmation
- [ ] [tool: claude] Implement iOS ApplePayProvider using PassKit behind the common PaymentProvider abstraction
- [ ] [tool: claude] Add 3D Secure challenge handling and authentication flow to common checkout
- [ ] [tool: opencode] Add accessibility and one-handed reachability passes over checkout sheet
- [ ] [tool: antigravity] Integrate PayPal or Braintree as alternative payment method
- [ ] [tool: antigravity] Add regional alternative payment methods including Klarna and iDEAL
- [ ] [tool: claude] Write commonMain unit tests for domain models, validation, formatting and Google Pay config
- [ ] [tool: antigravity] Write Android platform integration tests for Google Pay payment flow
- [ ] [tool: claude] Audit sensitive payment data handling ensuring tokens and PANs are never logged or persisted
- [ ] [tool: opencode] Establish automated lint, format (ktlint/detekt) and static analysis checks
- [ ] [tool: opencode] Set up CI workflow to build and test shared and app modules on every push
- [ ] [tool: opencode] Generate Dokka API reference documentation for public SDK surface
- [ ] [tool: opencode] Write integrator Getting Started guide with minimal end-to-end Google Pay sample
- [ ] [tool: opencode] Configure Maven Central publishing scripts for shared KMP library

## Scope notes

Guidance for implementing the current and upcoming milestones:

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
- **Hardening & Security** — 3D Secure challenges, PCI compliance audit, unit & platform integration tests.
- **Reach** — PayPal, Braintree, and regional payment methods (Klarna, iDEAL).
- **Release** — CI/CD automation, Dokka documentation, sample app polish, Maven Central release.
