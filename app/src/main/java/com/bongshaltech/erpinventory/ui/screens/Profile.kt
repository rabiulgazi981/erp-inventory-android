package com.bongshaltech.erpinventory.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bongshaltech.erpinventory.R
import com.bongshaltech.erpinventory.ui.components.Chip
import com.bongshaltech.erpinventory.ui.components.GradButton
import com.bongshaltech.erpinventory.ui.components.Header
import com.bongshaltech.erpinventory.ui.components.Initials
import com.bongshaltech.erpinventory.ui.components.Muted
import com.bongshaltech.erpinventory.ui.components.Panel
import com.bongshaltech.erpinventory.ui.components.PanelColors
import com.bongshaltech.erpinventory.ui.components.T
import com.bongshaltech.erpinventory.ui.theme.Accent
import com.bongshaltech.erpinventory.ui.theme.Brand
import com.bongshaltech.erpinventory.ui.theme.Ui
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private fun photoFile(ctx: Context, userKey: String) = File(File(ctx.filesDir, "avatars").apply { mkdirs() }, "$userKey.jpg")

/** Copies a picked picture in as a small square JPEG (256 px), so it stays light in backups and memory. */
private fun savePhoto(ctx: Context, uri: Uri, userKey: String): Boolean = runCatching {
    val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
    var sample = 1
    while (opts.outWidth / (sample * 2) >= 256 && opts.outHeight / (sample * 2) >= 256) sample *= 2
    val src = ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample }) }
        ?: return false
    val side = minOf(src.width, src.height)
    val square = Bitmap.createBitmap(src, (src.width - side) / 2, (src.height - side) / 2, side, side)
    val out = Bitmap.createScaledBitmap(square, 256, 256, true)
    photoFile(ctx, userKey).outputStream().use { out.compress(Bitmap.CompressFormat.JPEG, 88, it) }
    true
}.getOrDefault(false)

/** The person's photo in a circle, or their initials when there is none. */
@Composable
fun Avatar(name: String, size: Dp = 44.dp) {
    val app = app()
    val ctx = LocalContext.current
    val s by app.settings.settings.collectAsStateWithLifecycle(null)
    val key = s?.userKey ?: "owner"
    val ver = s?.photoVersion ?: 0
    val bmp by produceState<Bitmap?>(null, key, ver) {
        value = withContext(Dispatchers.IO) { photoFile(ctx, key).takeIf { it.exists() }?.let { BitmapFactory.decodeFile(it.path) } }
    }
    val b = bmp
    if (b != null) Image(b.asImageBitmap(), name, Modifier.size(size).clip(CircleShape), contentScale = ContentScale.Crop)
    else Initials(name, size)
}

/** More › My colour & photo: each person (owner or staff) picks their own accent colour and photo on this phone. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileLookScreen(onBack: () -> Unit) {
    val app = app()
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val who = rememberWho()
    val s by app.settings.settings.collectAsStateWithLifecycle(null)
    val key = s?.userKey ?: "owner"
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) scope.launch {
            if (withContext(Dispatchers.IO) { savePhoto(ctx, uri, key) }) app.settings.photoChanged()
        }
    }
    Column(Modifier.fillMaxSize().background(Ui.c.bg)) {
        Header(stringResource(R.string.look_title), onBack)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Panel(stringResource(R.string.look_photo), Brand.BlueGrad, PanelColors.Blue) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Avatar(who.name.ifBlank { s?.profile?.name.orEmpty() }, 72.dp)
                    Column(Modifier.weight(1f)) {
                        T(who.name.ifBlank { s?.profile?.name.orEmpty() }, 15.sp, FontWeight.Bold)
                        Muted(stringResource(R.string.look_photo_note))
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GradButton(stringResource(R.string.look_pick), Brand.BlueGrad, Modifier.weight(1f), height = 44.dp) {
                        pick.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }
                    Chip(stringResource(R.string.look_remove), false, {
                        scope.launch { withContext(Dispatchers.IO) { photoFile(ctx, key).delete() }; app.settings.photoChanged() }
                    }, soft = true)
                }
            }
            Panel(stringResource(R.string.look_colour), Brand.BlueGrad, PanelColors.Blue) {
                Muted(stringResource(R.string.look_colour_note))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Accent.entries.forEach { a ->
                        val on = (s?.accent ?: "BLUE") == a.name
                        Box(
                            Modifier.size(48.dp).clip(CircleShape).background(Brush.linearGradient(listOf(a.main, a.light)))
                                .border(if (on) 3.dp else 0.dp, Ui.c.text, CircleShape)
                                .clickable { scope.launch { app.settings.setAccent(key, a.name) } },
                            contentAlignment = Alignment.Center,
                        ) { if (on) T("✓", 18.sp, FontWeight.Bold, androidx.compose.ui.graphics.Color.White) }
                    }
                }
            }
        }
    }
}
