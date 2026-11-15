package com.bongshaltech.erpinventory.print

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.bongshaltech.erpinventory.data.BusinessProfile

data class ReceiptLine(val name: String, val qtyText: String, val unitText: String, val totalText: String)

/** Words printed on the memo, in the app's language. */
data class ReceiptLabels(
    val memo: String,
    val date: String,
    val customer: String,
    val subtotal: String,
    val discount: String,
    val total: String,
    val paid: String,
    val due: String,
    val method: String,
)

data class ReceiptData(
    val profile: BusinessProfile,
    val number: String,
    val dateText: String,
    val customer: String?,
    val lines: List<ReceiptLine>,
    val subtotal: String,
    val discount: String?,
    val total: String,
    val paid: String,
    val due: String?,
    val method: String,
)

/**
 * Draws the POS memo as a black-and-white picture. Thermal printers rarely have Bangla
 * fonts, so printing a picture works for every language and every printer.
 */
object ReceiptRenderer {
    /** Printable dots across: 58 mm rolls print 384 dots, 80 mm rolls 576. */
    fun widthFor(paperMm: Int) = if (paperMm >= 80) 576 else 384

    private sealed interface Op { val h: Int }
    private class TextOp(val layout: StaticLayout) : Op { override val h get() = layout.height }
    private class RowOp(val left: StaticLayout, val right: StaticLayout) : Op { override val h get() = maxOf(left.height, right.height) }
    private class RuleOp(override val h: Int) : Op
    private class GapOp(override val h: Int) : Op

    fun render(d: ReceiptData, l: ReceiptLabels, widthPx: Int): Bitmap {
        val k = widthPx / 384f
        val pad = (6 * k).toInt()
        val w = widthPx - pad * 2
        fun paint(px: Float, bold: Boolean = false) = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = px * k
            typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        }
        val title = paint(30f, true)
        val normal = paint(21f)
        val bold = paint(21f, true)
        val big = paint(26f, true)
        val small = paint(18f)

        fun layout(text: String, p: TextPaint, width: Int, align: Layout.Alignment = Layout.Alignment.ALIGN_NORMAL) =
            StaticLayout.Builder.obtain(text, 0, text.length, p, width.coerceAtLeast(1)).setAlignment(align).build()

        val ops = mutableListOf<Op>()
        fun center(text: String, p: TextPaint) { if (text.isNotBlank()) ops += TextOp(layout(text, p, w, Layout.Alignment.ALIGN_CENTER)) }
        fun row(left: String, right: String, p: TextPaint = normal, rp: TextPaint = p) {
            val rw = rp.measureText(right).toInt() + 1
            ops += RowOp(layout(left, p, w - rw - (8 * k).toInt()), layout(right, rp, rw))
        }
        fun rule() { ops += RuleOp((14 * k).toInt()) }

        val b = d.profile
        center(b.name, title)
        center(b.slogan, small)
        center(b.address, small)
        center(listOf(b.phone, b.email).filter { it.isNotBlank() }.joinToString("  "), small)
        center(b.web, small)
        if (b.bin.isNotBlank()) center("BIN: ${b.bin}", small)
        rule()
        row("${l.memo}: ${d.number}", "", bold)
        row("${l.date}: ${d.dateText}", "")
        d.customer?.let { row("${l.customer}: $it", "") }
        rule()
        d.lines.forEach { line ->
            ops += TextOp(layout(line.name, normal, w))
            row("  ${line.qtyText} × ${line.unitText}", line.totalText)
        }
        rule()
        row(l.subtotal, d.subtotal)
        d.discount?.let { row(l.discount, "-$it") }
        row(l.total, d.total, big)
        row("${l.paid} (${d.method})", d.paid)
        d.due?.let { row(l.due, it, bold) }
        rule()
        center(b.thanks.ifBlank { "Thank you!" }, bold)
        center(b.returnPolicy, small)
        ops += GapOp((6 * k).toInt())
        center("Software: ERP Inventory by Bongshal Tech", small)
        ops += GapOp((24 * k).toInt())

        val height = ops.sumOf { it.h } + pad * 2
        val bmp = Bitmap.createBitmap(widthPx, height, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawColor(Color.WHITE)
        val line = Paint().apply { color = Color.BLACK; strokeWidth = 1.5f * k; pathEffect = android.graphics.DashPathEffect(floatArrayOf(6 * k, 4 * k), 0f) }
        var y = pad.toFloat()
        for (op in ops) {
            when (op) {
                is TextOp -> { c.save(); c.translate(pad.toFloat(), y); op.layout.draw(c); c.restore() }
                is RowOp -> {
                    c.save(); c.translate(pad.toFloat(), y); op.left.draw(c); c.restore()
                    c.save(); c.translate((pad + w - op.right.width).toFloat(), y); op.right.draw(c); c.restore()
                }
                is RuleOp -> c.drawLine(pad.toFloat(), y + op.h / 2f, (pad + w).toFloat(), y + op.h / 2f, line)
                is GapOp -> Unit
            }
            y += op.h
        }
        return bmp
    }
}
