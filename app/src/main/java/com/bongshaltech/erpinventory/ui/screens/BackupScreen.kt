package com.bongshaltech.erpinventory.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bongshaltech.erpinventory.R
import com.bongshaltech.erpinventory.data.Backup
import com.bongshaltech.erpinventory.ui.components.GradButton
import com.bongshaltech.erpinventory.ui.components.Header
import com.bongshaltech.erpinventory.ui.components.Input
import com.bongshaltech.erpinventory.ui.components.Muted
import com.bongshaltech.erpinventory.ui.components.Panel
import com.bongshaltech.erpinventory.ui.components.PanelColors
import com.bongshaltech.erpinventory.ui.components.T
import com.bongshaltech.erpinventory.ui.components.WhiteButton
import com.bongshaltech.erpinventory.ui.theme.Brand
import com.bongshaltech.erpinventory.ui.theme.Ui
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.Date
import java.util.Locale

/** More › Backup: save a password-locked backup file (Google Drive, phone, SD card) and restore one. */
@Composable
fun BackupScreen(onBack: () -> Unit) {
    val app = app()
    val ctx = LocalContext.current
    val who = rememberWho()
    if (who.ready && !who.owner) return NotAllowed(onBack)
    val scope = rememberCoroutineScope()
    val settings by app.settings.settings.collectAsStateWithLifecycle(null)
    var pass by remember { mutableStateOf("") }
    var pass2 by remember { mutableStateOf("") }
    var tried by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var restoreUri by remember { mutableStateOf<Uri?>(null) }
    var restorePass by remember { mutableStateOf("") }
    var restoreError by remember { mutableStateOf<String?>(null) }
    val tooShort = stringResource(R.string.bk_pass_short)
    val noMatch = stringResource(R.string.bk_pass_mismatch)
    val passError = when {
        !tried -> null
        pass.length < 6 -> tooShort
        pass != pass2 -> noMatch
        else -> null
    }

    val create = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        busy = true
        scope.launch {
            val ok = runCatching { Backup.write(ctx, uri, pass) }.isSuccess
            if (ok) app.settings.setLastBackup(System.currentTimeMillis())
            Toast.makeText(ctx, ctx.getString(if (ok) R.string.bk_saved else R.string.bk_failed), Toast.LENGTH_LONG).show()
            busy = false
        }
    }
    val open = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) { restoreUri = uri; restorePass = ""; restoreError = null }
    }

    Column(Modifier.fillMaxSize().background(Ui.c.bg)) {
        Header(stringResource(R.string.backup), onBack)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val last = settings?.lastBackupAt ?: 0L
            T(
                if (last == 0L) stringResource(R.string.bk_never)
                else stringResource(R.string.bk_last, SimpleDateFormat("d MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(last))),
                14.sp, FontWeight.SemiBold, if (last == 0L) Brand.Red else Ui.c.text,
            )
            Panel(stringResource(R.string.bk_save_panel), Brand.BlueGrad, PanelColors.Blue) {
                Muted(stringResource(R.string.bk_save_help), 13.sp)
                Input(pass, { pass = it }, label = stringResource(R.string.bk_pass), password = true, error = passError)
                Input(pass2, { pass2 = it }, label = stringResource(R.string.bk_pass_again), password = true)
                Muted(stringResource(R.string.bk_pass_note), 12.sp)
                GradButton(stringResource(if (busy) R.string.bk_working else R.string.bk_save), Brand.BlueGrad, Modifier.fillMaxWidth(), enabled = !busy) {
                    tried = true
                    if (pass.length >= 6 && pass == pass2) create.launch("ERP-Inventory-backup-${LocalDate.now()}.erpbak")
                }
            }
            Panel(stringResource(R.string.bk_restore_panel), Brand.OrangeGrad, PanelColors.Orange) {
                Muted(stringResource(R.string.bk_restore_help), 13.sp)
                WhiteButton(stringResource(R.string.bk_restore), Modifier.fillMaxWidth()) { open.launch(arrayOf("*/*")) }
            }
        }
    }

    restoreUri?.let { uri ->
        val wrong = stringResource(R.string.bk_wrong_pass)
        val notBackup = stringResource(R.string.bk_not_backup)
        AlertDialog(
            onDismissRequest = { if (!busy) restoreUri = null },
            title = { T(stringResource(R.string.bk_restore), 18.sp, FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    T(stringResource(R.string.bk_restore_warn), 14.sp, FontWeight.SemiBold, Brand.Red)
                    Input(restorePass, { restorePass = it }, label = stringResource(R.string.bk_pass), password = true, error = restoreError)
                }
            },
            confirmButton = {
                GradButton(stringResource(if (busy) R.string.bk_working else R.string.bk_restore_now), Brand.OrangeGrad, height = 44.dp, enabled = !busy) {
                    busy = true
                    scope.launch {
                        val r = runCatching { Backup.restore(ctx, uri, restorePass) }.getOrDefault(Backup.RestoreResult.NotABackup)
                        busy = false
                        when (r) {
                            Backup.RestoreResult.Done -> {
                                // Start fresh so the restored database and settings are opened cleanly.
                                ctx.packageManager.getLaunchIntentForPackage(ctx.packageName)?.let {
                                    ctx.startActivity(it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
                                }
                                Runtime.getRuntime().exit(0)
                            }
                            Backup.RestoreResult.WrongPassword -> restoreError = wrong
                            Backup.RestoreResult.NotABackup -> restoreError = notBackup
                        }
                    }
                }
            },
            dismissButton = { WhiteButton(stringResource(R.string.bk_cancel), height = 44.dp) { if (!busy) restoreUri = null } },
            containerColor = Ui.c.card,
        )
    }
}
