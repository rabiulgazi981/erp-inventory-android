package com.bongshaltech.erpinventory.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bongshaltech.erpinventory.R
import com.bongshaltech.erpinventory.data.SmsCloud
import com.bongshaltech.erpinventory.ui.components.Card
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

/** More › SMS pack: connect the shop's erpinventory.bd account, see the SMS balance, ask for a top-up. */
@Composable
fun SmsAccountScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val who = rememberWho()
    if (who.ready && !who.owner) return NotAllowed(onBack)
    val scope = rememberCoroutineScope()
    var email by remember { mutableStateOf(SmsCloud.signedInEmail(ctx)) }
    var balance by remember { mutableStateOf<SmsCloud.Balance?>(null) }
    var loading by remember { mutableStateOf(false) }
    var formEmail by remember { mutableStateOf("") }
    var formPass by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    fun open(url: String) = runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }

    LaunchedEffect(email) { if (email != null) { loading = true; balance = SmsCloud.balance(ctx); loading = false } }

    Column(Modifier.fillMaxSize().background(Ui.c.bg)) {
        Header(stringResource(R.string.sms_pack), onBack)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Muted(stringResource(R.string.sms_pack_help), 13.sp)
            val signed = email
            if (signed == null) {
                Panel(stringResource(R.string.sms_connect), Brand.BlueGrad, PanelColors.Blue) {
                    Input(formEmail, { formEmail = it }, label = stringResource(R.string.sms_email), keyboard = KeyboardType.Email)
                    Input(formPass, { formPass = it }, label = stringResource(R.string.sms_password), password = true, error = error)
                    GradButton(stringResource(if (loading) R.string.sms_working else R.string.sms_connect), Brand.BlueGrad, Modifier.fillMaxWidth(), enabled = !loading) {
                        if (formEmail.isBlank() || formPass.isBlank()) return@GradButton
                        loading = true; error = null
                        scope.launch {
                            when (val r = SmsCloud.login(ctx, formEmail, formPass)) {
                                SmsCloud.Login.Ok -> { formPass = ""; email = SmsCloud.signedInEmail(ctx) }
                                SmsCloud.Login.WrongPassword -> error = ctx.getString(R.string.sms_wrong_login)
                                SmsCloud.Login.NoShop -> error = ctx.getString(R.string.sms_no_shop)
                                SmsCloud.Login.Offline -> error = ctx.getString(R.string.sms_offline)
                                is SmsCloud.Login.Error -> error = r.message
                            }
                            loading = false
                        }
                    }
                    T(stringResource(R.string.sms_signup), 13.sp, FontWeight.SemiBold, Brand.Link,
                        Modifier.clickable { open("https://erpinventory.bd/free-trial") }.padding(vertical = 4.dp))
                }
            } else {
                Card(Modifier.fillMaxWidth(), padding = 14.dp) {
                    Muted(signed)
                    T(
                        balance?.let { stringResource(R.string.sms_balance_is, it.balance) }
                            ?: stringResource(if (loading) R.string.sms_working else R.string.sms_balance_unknown),
                        22.sp, FontWeight.Bold, if ((balance?.balance ?: 1) <= 0) Brand.Red else Ui.c.text,
                    )
                    balance?.lastTopups?.firstOrNull()?.let { (credits, at) ->
                        Muted(stringResource(R.string.sms_last_topup, credits, at.take(10)))
                    }
                    Muted(stringResource(R.string.sms_segments), 12.sp)
                }
                GradButton(stringResource(R.string.sms_buy), Brand.GreenGrad, Modifier.fillMaxWidth()) {
                    val text = Uri.encode("SMS pack for $signed")
                    open("https://wa.me/8801938273878?text=$text")
                }
                WhiteButton(stringResource(R.string.sms_disconnect), Modifier.fillMaxWidth()) {
                    scope.launch { SmsCloud.signOut(ctx); email = null; balance = null }
                }
            }
        }
    }
}
