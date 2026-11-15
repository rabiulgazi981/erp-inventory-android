package com.bongshaltech.erpinventory.print

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.text.TextPaint
import android.text.TextUtils
import com.bongshaltech.erpinventory.core.label.Code128

data class LabelItem(val name: String, val priceText: String, val barcode: String)

/** Barcode sticker, QR sticker, or a shelf label (big price, no code). */
enum class LabelStyle { BARCODE, QR, SHELF }

/** Barcode stickers for a receipt-roll printer: one label under the next. */
object LabelRenderer {
    fun render(items: List<LabelItem>, widthPx: Int, shopName: String, style: LabelStyle = LabelStyle.BARCODE): Bitmap {
        if (style == LabelStyle.SHELF) return shelf(items, widthPx, shopName)
        val labelH = if (style == LabelStyle.QR) 330 else 230
        val gap = 24
        val h = items.size * (labelH + gap)
        val bmp = Bitmap.createBitmap(widthPx, maxOf(h, 1), Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawColor(Color.WHITE)
        val small = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK; textSize = 20f }
        val bold = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK; textSize = 26f; typeface = Typeface.DEFAULT_BOLD }
        val digits = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK; textSize = 22f; typeface = Typeface.MONOSPACE; textAlign = Paint.Align.CENTER }
        val black = Paint().apply { color = Color.BLACK }
        val dash = Paint().apply { color = Color.BLACK; strokeWidth = 2f; pathEffect = android.graphics.DashPathEffect(floatArrayOf(8f, 8f), 0f) }
        items.forEachIndexed { i, it ->
            var y = i * (labelH + gap) + 8f
            fun line(text: String, p: TextPaint) {
                val t = TextUtils.ellipsize(text, p, widthPx - 24f, TextUtils.TruncateAt.END).toString()
                y += -p.ascent()
                c.drawText(t, 12f, y, p)
                y += p.descent() + 4
            }
            if (shopName.isNotBlank()) line(shopName, small)
            line(it.name, bold)
            line(it.priceText, bold)
            if (style == LabelStyle.QR && it.barcode.isNotBlank()) {
                val size = 200
                val m = com.google.zxing.qrcode.QRCodeWriter().encode(it.barcode, com.google.zxing.BarcodeFormat.QR_CODE, size, size,
                    mapOf(com.google.zxing.EncodeHintType.MARGIN to 0))
                val left = (widthPx - size) / 2f
                val top = y + 6
                for (yy in 0 until m.height) for (xx in 0 until m.width) if (m.get(xx, yy)) c.drawRect(left + xx, top + yy, left + xx + 1, top + yy + 1, black)
                c.drawText(it.barcode, widthPx / 2f, top + size + 26, digits)
            } else if (Code128.canEncode(it.barcode)) {
                val mods = Code128.modules(it.barcode)
                val unit = maxOf(1, (widthPx - 40) / mods.sum())
                var x = (widthPx - unit * mods.sum()) / 2f
                val top = y + 4
                mods.forEachIndexed { k, m ->
                    if (k % 2 == 0) c.drawRect(x, top, x + m * unit, top + 80, black)
                    x += m * unit
                }
                c.drawText(it.barcode, widthPx / 2f, top + 80 + 26, digits)
            }
            val cut = (i + 1) * (labelH + gap) - gap / 2f
            if (i < items.size - 1) c.drawLine(0f, cut, widthPx.toFloat(), cut, dash)
        }
        return bmp
    }

    /** Shelf label: product name and a large price, for the shelf edge. */
    private fun shelf(items: List<LabelItem>, widthPx: Int, shopName: String): Bitmap {
        val labelH = 200
        val gap = 24
        val bmp = Bitmap.createBitmap(widthPx, maxOf(items.size * (labelH + gap), 1), Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawColor(Color.WHITE)
        val small = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK; textSize = 20f }
        val name = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK; textSize = 30f; typeface = Typeface.DEFAULT_BOLD }
        val price = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK; textSize = 64f; typeface = Typeface.DEFAULT_BOLD; textAlign = Paint.Align.CENTER }
        val border = Paint().apply { color = Color.BLACK; style = Paint.Style.STROKE; strokeWidth = 3f }
        items.forEachIndexed { i, it ->
            val top = i * (labelH + gap).toFloat()
            c.drawRect(4f, top + 4, widthPx - 4f, top + labelH - 4, border)
            var y = top + 14
            fun line(text: String, p: TextPaint) {
                val t = TextUtils.ellipsize(text, p, widthPx - 32f, TextUtils.TruncateAt.END).toString()
                y += -p.ascent(); c.drawText(t, 16f, y, p); y += p.descent() + 4
            }
            if (shopName.isNotBlank()) line(shopName, small)
            line(it.name, name)
            c.drawText(it.priceText, widthPx / 2f, top + labelH - 30, price)
        }
        return bmp
    }
}
