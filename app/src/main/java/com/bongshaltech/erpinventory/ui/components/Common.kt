package com.bongshaltech.erpinventory.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bongshaltech.erpinventory.R

const val DEV_URL = "https://bongshaltech.com"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ErpTopBar(title: String, onBack: (() -> Unit)?, actions: @Composable () -> Unit = {}) {
    TopAppBar(
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        navigationIcon = {
            if (onBack != null) IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
            }
        },
        actions = { actions() },
    )
}

/** "Developed by Bongshal Tech", opens bongshaltech.com. Shown on Splash, Login, Home and More. */
@Composable
fun DevFooter(modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    Text(
        text = stringResource(R.string.dev_footer),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
        textAlign = TextAlign.Center,
        modifier = modifier
            .fillMaxWidth()
            .clickable { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(DEV_URL))) }
            .padding(12.dp),
    )
}

@Composable
fun StatCard(title: String, value: String, sub: String?, brush: Brush, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    Box(
        modifier
            .clip(RoundedCornerShape(18.dp))
            .background(brush)
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, color = Color.White.copy(alpha = 0.9f), style = MaterialTheme.typography.labelMedium)
            Text(value, color = Color.White, style = MaterialTheme.typography.titleLarge)
            if (sub != null) Text(sub, color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun Field(
    value: String,
    onChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    number: Boolean = false,
    phone: Boolean = false,
    error: String? = null,
    singleLine: Boolean = true,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        isError = error != null,
        supportingText = error?.let { { Text(it) } },
        singleLine = singleLine,
        keyboardOptions = KeyboardOptions(
            keyboardType = when {
                phone -> KeyboardType.Phone
                number -> KeyboardType.Decimal
                else -> KeyboardType.Text
            }
        ),
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
fun SectionRow(label: String, value: String, strong: Boolean = false, color: Color = Color.Unspecified) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), style = if (strong) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium)
        Text(value, style = if (strong) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium, color = color)
    }
}
