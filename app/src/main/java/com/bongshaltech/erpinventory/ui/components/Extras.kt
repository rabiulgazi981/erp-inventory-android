package com.bongshaltech.erpinventory.ui.components

import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Contacts
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bongshaltech.erpinventory.R
import com.bongshaltech.erpinventory.core.BdPhone
import com.bongshaltech.erpinventory.data.db.PayMethod
import com.bongshaltech.erpinventory.ui.taka
import com.bongshaltech.erpinventory.ui.theme.Brand
import com.bongshaltech.erpinventory.ui.theme.Ui
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * The contacts button next to a phone box: opens the phone's contact list and fills the number (as 01XXXXXXXXX)
 * and, when the name box is still empty, the name. Android hands over only the one contact picked, so the app
 * needs no contacts permission.
 */
@Composable
fun ContactPickButton(onPicked: (phone: String, name: String) -> Unit, size: Dp = 46.dp) {
    val ctx = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        val uri = r.data?.data ?: return@rememberLauncherForActivityResult
        runCatching {
            ctx.contentResolver.query(
                uri, arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER, ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME), null, null, null,
            )?.use { c ->
                if (c.moveToFirst()) {
                    val phone = BdPhone.local(c.getString(0).orEmpty()).orEmpty()
                    if (phone.isNotEmpty()) onPicked(phone, c.getString(1).orEmpty().trim())
                }
            }
        }
    }
    Box(
        Modifier.size(size).clip(RoundedCornerShape(12.dp)).background(Brand.BlueGrad).clickable {
            runCatching { launcher.launch(Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI)) }
        },
        contentAlignment = Alignment.Center,
    ) { Icon(Icons.Outlined.Contacts, stringResource(R.string.contact_pick), tint = Color.White, modifier = Modifier.size(20.dp)) }
}

/** A phone box with the contacts button beside it. */
@Composable
fun PhoneInput(
    phone: String, onPhone: (String) -> Unit, modifier: Modifier = Modifier, label: String? = null, placeholder: String = "",
    height: Dp = 46.dp, onName: ((String) -> Unit)? = null,
) {
    Row(modifier, verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Input(phone, onPhone, Modifier.weight(1f), label = label, placeholder = placeholder, keyboard = KeyboardType.Phone, height = height)
        ContactPickButton({ p, n -> onPhone(p); if (n.isNotBlank()) onName?.invoke(n) }, height)
    }
}

private val dayFmt = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault())

fun Long.dayText(): String = LocalDate.ofEpochDay(this).format(dayFmt)

/** A date box that opens the calendar. [day] is days since 1970-01-01, or null. */
@Composable
fun DateBox(day: Long?, onDay: (Long?) -> Unit, label: String, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        T(label, 13.sp, FontWeight.SemiBold, Ui.c.label)
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Ui.c.input).border(1.dp, Ui.c.inputBorder, RoundedCornerShape(12.dp))
                .clickable { pickDay(ctx, day) { onDay(it) } }.padding(horizontal = 12.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(Icons.Outlined.CalendarMonth, null, tint = Brand.Blue, modifier = Modifier.size(18.dp))
            T(day?.dayText() ?: "—", 15.sp, modifier = Modifier.weight(1f))
            if (day != null) T("✕", 14.sp, color = Ui.c.muted, modifier = Modifier.clickable { onDay(null) })
        }
    }
}

fun pickDay(ctx: Context, day: Long?, onPick: (Long) -> Unit) {
    val d = day?.let { LocalDate.ofEpochDay(it) } ?: LocalDate.now()
    DatePickerDialog(ctx, { _, y, m, dd -> onPick(LocalDate.of(y, m + 1, dd).toEpochDay()) }, d.year, d.monthValue - 1, d.dayOfMonth).show()
}

/** One amount box per cash, bank and wallet account, for paying from several at once. Blank boxes are 0. */
@Composable
fun SplitPayBoxes(methods: List<Pair<PayMethod, Int>>, amounts: MutableMap<PayMethod, String>, totalPaisa: Long?) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        methods.forEach { (m, label) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                T(stringResource(label), 14.sp, color = Ui.c.label, modifier = Modifier.weight(1f))
                Input(amounts[m].orEmpty(), { amounts[m] = it }, Modifier.width(130.dp), keyboard = KeyboardType.Decimal, height = 38.dp, alignEnd = true, placeholder = "0")
            }
        }
        val sum = amounts.values.sumOf { it.toPaisaOrZero() }
        SumRow(stringResource(R.string.split_total), sum.taka(), strong = true)
        if (totalPaisa != null && sum < totalPaisa) SumRow(stringResource(R.string.split_left), (totalPaisa - sum).taka(), color = Brand.Red)
    }
}

fun String.toPaisaOrZero(): Long = com.bongshaltech.erpinventory.core.Money.parse(this)?.paisa?.coerceAtLeast(0) ?: 0

fun Map<PayMethod, String>.parts(): List<Pair<PayMethod, Long>> = entries.map { it.key to it.value.toPaisaOrZero() }.filter { it.second > 0 }

/** A plain white picture of titled lines, for sharing a slip on WhatsApp when there is no printer. */
object SlipImage {
    fun render(title: String, sub: String, sections: List<Pair<String, List<Pair<String, String>>>>, footer: String, widthPx: Int = 720): Bitmap {
        val pad = 36f
        val titleP = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 40f; typeface = Typeface.DEFAULT_BOLD; color = 0xFF111827.toInt() }
        val subP = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 26f; color = 0xFF4B5563.toInt() }
        val headP = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 31f; typeface = Typeface.DEFAULT_BOLD; color = 0xFF1E3A8A.toInt() }
        val keyP = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 28f; color = 0xFF374151.toInt() }
        val valP = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 30f; typeface = Typeface.DEFAULT_BOLD; color = 0xFF111827.toInt(); textAlign = Paint.Align.RIGHT }
        val line = Paint().apply { color = 0xFFE5E7EB.toInt(); strokeWidth = 2f }
        val rows = sections.sumOf { 1 + it.second.size }
        val height = (pad * 2 + 60 + 40 + rows * 46 + sections.size * 24 + 70).toInt()
        val bmp = Bitmap.createBitmap(widthPx, height, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawColor(0xFFFFFFFF.toInt())
        var y = pad + 40
        c.drawText(title, pad, y, titleP)
        y += 40
        c.drawText(sub, pad, y, subP)
        y += 30
        for ((head, items) in sections) {
            y += 24
            c.drawLine(pad, y, widthPx - pad, y, line)
            y += 40
            c.drawText(head, pad, y, headP)
            for ((k, v) in items) {
                y += 46
                c.drawText(k, pad, y, keyP)
                c.drawText(v, widthPx - pad, y, valP)
            }
        }
        y += 60
        c.drawText(footer, pad, y, subP)
        return bmp
    }
}
