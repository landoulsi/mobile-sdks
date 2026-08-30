package com.landoulsi.demo.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.landoulsi.design.components.BadgeTone
import com.landoulsi.design.components.BannerTone
import com.landoulsi.design.components.ButtonTone
import com.landoulsi.design.components.CardTone
import com.landoulsi.design.components.DesignBanner
import com.landoulsi.design.components.DesignButton
import com.landoulsi.design.components.DesignCard
import com.landoulsi.design.components.DesignChip
import com.landoulsi.design.components.DesignElevatedCard
import com.landoulsi.design.components.DesignInfoRow
import com.landoulsi.design.components.DesignOutlinedButton
import com.landoulsi.design.components.DesignOutlinedCard
import com.landoulsi.design.components.DesignStatusIcon
import com.landoulsi.design.components.DesignSurface
import com.landoulsi.design.components.StatusBadge
import com.landoulsi.design.components.StatusIconVariant
import com.landoulsi.design.components.SurfaceTone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DesignComponentsDemoScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Design Components") },
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "Cards",
                style = MaterialTheme.typography.titleMedium,
            )
            DesignCard(tone = CardTone.Primary) {
                Text(
                    text = "DesignCard (Primary)",
                    modifier = Modifier.padding(16.dp),
                )
            }
            DesignElevatedCard(tone = CardTone.Neutral) {
                Text(
                    text = "DesignElevatedCard (Neutral)",
                    modifier = Modifier.padding(16.dp),
                )
            }
            DesignOutlinedCard(tone = CardTone.Error) {
                Text(
                    text = "DesignOutlinedCard (Error)",
                    modifier = Modifier.padding(16.dp),
                )
            }

            Text(
                text = "Chips & Badges",
                style = MaterialTheme.typography.titleMedium,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DesignChip(text = "New Feature", tone = BadgeTone.Primary)
                DesignChip(text = "Bug Fix", tone = BadgeTone.Error)
                DesignChip(text = "Success", tone = BadgeTone.Success)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusBadge(text = "Active", tone = BadgeTone.Success)
                StatusBadge(text = "Pending", tone = BadgeTone.Tertiary)
                StatusBadge(text = "Failed", tone = BadgeTone.Error)
            }

            Text(
                text = "Buttons",
                style = MaterialTheme.typography.titleMedium,
            )
            DesignButton(
                text = "Primary Button",
                onClick = {},
                tone = ButtonTone.Primary,
            )
            DesignButton(
                text = "Success Button",
                onClick = {},
                tone = ButtonTone.Success,
            )
            DesignButton(
                text = "Error Button",
                onClick = {},
                tone = ButtonTone.Error,
            )
            DesignOutlinedButton(
                text = "Outlined Button",
                onClick = {},
            )

            Text(
                text = "Banners",
                style = MaterialTheme.typography.titleMedium,
            )
            DesignBanner(
                text = "Info banner message",
                tone = BannerTone.Info,
            )
            DesignBanner(
                text = "Error banner message",
                tone = BannerTone.Error,
            )
            DesignBanner(
                text = "Success banner message",
                tone = BannerTone.Success,
            )

            Text(
                text = "Info Rows",
                style = MaterialTheme.typography.titleMedium,
            )
            DesignInfoRow(label = "Transaction ID", value = "txn_123456")
            DesignInfoRow(label = "Payment Method", value = "Visa •••• 4242")

            Text(
                text = "Status Icons",
                style = MaterialTheme.typography.titleMedium,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                DesignStatusIcon(variant = StatusIconVariant.Success)
                DesignStatusIcon(variant = StatusIconVariant.Error)
                DesignStatusIcon(variant = StatusIconVariant.Warning)
            }

            Text(
                text = "Inverted Surface",
                style = MaterialTheme.typography.titleMedium,
            )
            DesignSurface(tone = SurfaceTone.Inverted) {
                Text(
                    text = "Inverted surface content",
                    modifier = Modifier.padding(16.dp),
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
