package com.landoulsi.payment.ui.threeds

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.landoulsi.payment.R
import com.landoulsi.payment.shared.model.PaymentErrorCode
import com.landoulsi.payment.shared.model.PaymentRequest
import com.landoulsi.payment.shared.model.ThreeDSChallenge
import com.landoulsi.payment.shared.model.ThreeDSResult
import com.landoulsi.payment.shared.model.parseThreeDSReturnUrl
import com.landoulsi.payment.ui.payment.PaymentDetailRow
import com.landoulsi.payment.ui.theme.PaymentColorTokens

/**
 * 3D Secure Authentication Challenge composable.
 *
 * Renders the SCA / 3DS challenge container, presenting bank authentication details,
 * redirect/WebView challenge interceptor, and fallback simulation controls.
 *
 * @param challenge The [ThreeDSChallenge] containing redirect and return URLs.
 * @param request The active [PaymentRequest].
 * @param onResult Callback invoked with [ThreeDSResult] upon challenge outcome.
 */
@Composable
fun ThreeDSChallengeCard(
    challenge: ThreeDSChallenge,
    request: PaymentRequest,
    onResult: (ThreeDSResult) -> Unit,
    modifier: Modifier = Modifier
) {
    var isAuthenticating by remember { mutableStateOf(false) }
    var hasDeliveredResult by remember { mutableStateOf(false) }

    val deliverResult: (ThreeDSResult) -> Unit = { result ->
        if (!hasDeliveredResult) {
            hasDeliveredResult = true
            isAuthenticating = (result is ThreeDSResult.Completed)
            onResult(result)
        }
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Row with 3DS Lock Icon and Bank Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "\uD83D\uDD12",
                            fontSize = 16.sp
                        )
                    }
                    Column {
                        Text(
                            text = stringResource(id = R.string.threeds_challenge_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(id = R.string.threeds_challenge_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
                ) {
                    Text(
                        text = stringResource(id = R.string.threeds_bank_verified),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Payment Context Summary
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PaymentDetailRow(
                    label = "Merchant",
                    value = request.merchantName ?: "Demo Store"
                )
                PaymentDetailRow(
                    label = "Total Amount",
                    value = request.amount.formattedWithSymbol()
                )
                PaymentDetailRow(
                    label = "Intent ID",
                    value = challenge.paymentIntentId
                )
            }

            Text(
                text = stringResource(id = R.string.threeds_security_prompt),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Start
            )

            // In-App WebView / Challenge Interceptor
            if (challenge.redirectUrl.isNotBlank() &&
                (challenge.redirectUrl.startsWith("https://") || challenge.redirectUrl.startsWith("data:"))
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    ThreeDSWebView(
                        redirectUrl = challenge.redirectUrl,
                        returnUrl = challenge.returnUrl,
                        onResult = deliverResult,
                        onPageLoadingChanged = { loading ->
                            if (!hasDeliveredResult) {
                                isAuthenticating = loading
                            }
                        }
                    )
                }
            }

            // Processing indicator when authenticating
            if (isAuthenticating) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = stringResource(id = R.string.threeds_challenge_authenticating),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Challenge Interactive Action Controls
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        deliverResult(
                            ThreeDSResult.Completed(
                                returnPayload = "${challenge.returnUrl}?payment_intent=${challenge.paymentIntentId}&status=succeeded&transStatus=Y"
                            )
                        )
                    },
                    enabled = !isAuthenticating && !hasDeliveredResult,
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PaymentColorTokens.success,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.threeds_approve_button),
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            deliverResult(
                                ThreeDSResult.Failed(
                                    errorCode = PaymentErrorCode.AUTHENTICATION_FAILED,
                                    message = "3D Secure authentication declined by issuer"
                                )
                            )
                        },
                        enabled = !isAuthenticating && !hasDeliveredResult,
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                    ) {
                        Text(text = stringResource(id = R.string.threeds_decline_button))
                    }

                    OutlinedButton(
                        onClick = {
                            deliverResult(ThreeDSResult.Canceled)
                        },
                        enabled = !isAuthenticating && !hasDeliveredResult,
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                    ) {
                        Text(text = stringResource(id = R.string.threeds_cancel_button))
                    }
                }
            }
        }
    }
}

/**
 * Embedded WebView for handling 3D Secure redirects and intercepting returnUrl completion callbacks.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun ThreeDSWebView(
    redirectUrl: String,
    returnUrl: String,
    onResult: (ThreeDSResult) -> Unit,
    onPageLoadingChanged: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var isLoading by remember { mutableStateOf(true) }
    var hasError by remember { mutableStateOf(false) }

    val currentOnResult by rememberUpdatedState(onResult)
    val currentReturnUrl by rememberUpdatedState(returnUrl)
    val currentOnLoadingChanged by rememberUpdatedState(onPageLoadingChanged)

    val customScheme = remember(returnUrl) {
        val scheme = returnUrl.substringBefore("://", "")
        if (scheme.isNotBlank() && scheme != "http" && scheme != "https") "$scheme://" else null
    }

    var webViewInstance by remember { mutableStateOf<WebView?>(null) }

    Box(modifier = modifier.fillMaxWidth()) {
        if (!hasError) {
            AndroidView(
                factory = { context ->
                    WebView(context).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.loadWithOverviewMode = true
                        settings.useWideViewPort = true
                        settings.allowFileAccess = false
                        settings.allowFileAccessFromFileURLs = false
                        settings.allowUniversalAccessFromFileURLs = false

                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                super.onPageStarted(view, url, favicon)
                                isLoading = true
                                currentOnLoadingChanged(true)
                                url?.let { targetUrl ->
                                    val result = parseThreeDSReturnUrl(targetUrl, currentReturnUrl)
                                    if (result != null) {
                                        view?.stopLoading()
                                        currentOnResult(result)
                                    }
                                }
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                isLoading = false
                                currentOnLoadingChanged(false)
                            }

                            override fun shouldOverrideUrlLoading(
                                view: WebView?,
                                request: WebResourceRequest?
                            ): Boolean {
                                val targetUrl = request?.url?.toString() ?: return false
                                val result = parseThreeDSReturnUrl(targetUrl, currentReturnUrl)
                                if (result != null) {
                                    currentOnResult(result)
                                    return true
                                }
                                if ((customScheme != null && targetUrl.startsWith(customScheme)) ||
                                    targetUrl.startsWith("paymentsdk://") ||
                                    targetUrl.startsWith("intent://")
                                ) {
                                    return true
                                }
                                return false
                            }

                            override fun onReceivedError(
                                view: WebView?,
                                request: WebResourceRequest?,
                                error: WebResourceError?
                            ) {
                                super.onReceivedError(view, request, error)
                                if (request?.isForMainFrame == true) {
                                    hasError = true
                                    isLoading = false
                                    currentOnLoadingChanged(false)
                                }
                            }

                            override fun onReceivedHttpError(
                                view: WebView?,
                                request: WebResourceRequest?,
                                errorResponse: WebResourceResponse?
                            ) {
                                super.onReceivedHttpError(view, request, errorResponse)
                                if (request?.isForMainFrame == true && (errorResponse?.statusCode ?: 200) >= 400) {
                                    hasError = true
                                    isLoading = false
                                    currentOnLoadingChanged(false)
                                }
                            }
                        }

                        webViewInstance = this
                        loadUrl(redirectUrl)
                    }
                },
                onRelease = { webView ->
                    webView.stopLoading()
                    (webView.parent as? android.view.ViewGroup)?.removeView(webView)
                    webView.destroy()
                },
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Bank authentication page could not be loaded",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {
                        hasError = false
                        webViewInstance?.loadUrl(redirectUrl)
                    }
                ) {
                    Text(text = "Retry Loading")
                }
            }
        }

        if (isLoading && !hasError) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(28.dp),
                    strokeWidth = 2.5.dp
                )
            }
        }
    }
}
