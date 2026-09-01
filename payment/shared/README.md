# `:payment:shared`

Shared payment/checkout core for the `payment` sample app — card validation,
gateway tokenization, 3-D Secure models, and native wallet providers (Google Pay
on Android, Apple Pay on iOS).

## What's here

| Area | Files | Role |
| --- | --- | --- |
| Domain models | `commonMain/.../model/` | `Money`, `Currency`, `Address`, `CardNetwork`, `PaymentMethod`, `PaymentRequest`, `PaymentResult`, `ThreeDSModels`, `GooglePayConfig`, `ApplePayConfig`. |
| Card entry | `commonMain/.../validation/` | `CardValidation` (Luhn, network detection, expiry); `CardFormState` + per-field state in `CardInputState.kt`. |
| Checkout state | `commonMain/.../checkout/` | `CheckoutViewModel`, `CheckoutUiState`. |
| Gateway | `commonMain/.../network/` | `GatewayClient` (tokenize card / Google Pay token, confirm payment intent), `GatewayException`, `createPaymentHttpClient(...)` (`HttpClientFactory.kt`), DTOs. |
| Provider abstraction | `commonMain/.../provider/PaymentProvider.kt` | `isReadyToPay()` / `pay(request): PaymentResult`, keyed by `paymentMethodType`. |
| Google Pay | `androidMain/.../googlepay/` | `GooglePayProvider`, `GooglePayClient`, `GooglePayPaymentTaskContract` (`GooglePayLauncherContract.kt`), `GooglePayJsonFactory` (Play Services Wallet). |
| Apple Pay | `iosMain/.../applepay/` | `ApplePayProvider`, `ApplePayClient` (PassKit). |

## Usage

```kotlin
val provider: PaymentProvider = /* GooglePayProvider(...) or ApplePayProvider(...) */
if (provider.isReadyToPay()) {
    when (val result = provider.pay(request)) {
        is PaymentResult.Success  -> confirm(result)
        is PaymentResult.Failure  -> report(result)
        is PaymentResult.Canceled -> { /* user backed out */ }
    }
}
```

## Platforms

Android + iOS. `kotlinx.serialization` for the gateway wire format; Ktor for
transport.

## Tests

```bash
./gradlew :payment:shared:allTests
```
