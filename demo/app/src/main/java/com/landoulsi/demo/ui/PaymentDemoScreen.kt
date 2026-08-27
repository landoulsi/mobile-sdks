package com.landoulsi.demo.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentDemoScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Payment SDK Demo") }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Checkout",
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = { println("Google Pay initiated") },
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text("Pay with Google Pay")
            }

            Spacer(modifier = Modifier.height(16.dp))
            
            Text(text = "--- OR ---", style = MaterialTheme.typography.bodyMedium)

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { println("Card Checkout initiated") },
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text("Pay with Card")
            }
        }
    }
}
