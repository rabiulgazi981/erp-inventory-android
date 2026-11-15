package com.bongshaltech.erpinventory.print

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.text.TextPaint
import android.text.TextUtils
import androidx.core.content.FileProvider
import com.bongshaltech.erpinventory.core.report.Csv
import com.bongshaltech.erpinventory.core.report.ReportTable
import com.bongshaltech.erpinventory.data.BusinessProfile
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

/**
 * A4 PDF of a report or an invoice: shop header, title, a table that runs over as many pages as it needs,
 * totals and notes, and a page number with the developer line at the foot of each page.
 */
object ReportPdf {
    private const val W = 595
    private const val H = 842
    private const val M = 36f

    fun write(ctx: Context, t: ReportTable, profile: BusinessProfile, footer: String, fileName: String): File {
        val doc = PdfDocument()
        val title = paint(15f, bold = true)
        val small = paint(9f)
        val head = paint(9.5f, bold = true)
        val cell = paint(9.5f)
        val bold = paint(9.5f, bold = true)
        val line = Paint().apply { color = Color.rgb(200, 200, 200); strokeWidth = 0.6f }
        val usable = W - 2 * M
        // First column gets the room it needs for names; the rest share what is left.
        val weights = t.columns.mapIndexed { i, c -> if (i == 0 && !c.numeric) 2.4f else if (c.numeric) 1f else 1.3f }
        val widths = weights.map { it / weights.sum() * usable }
        val rowH = 16f
        var pageNo = 0
        var page: PdfDocument.Page? = null
        var c: Canvas? = null
        var y = 0f

        fun header(cv: Canvas) {
            var hy = M + 4
            if (profile.name.isNotBlank()) { cv.drawText(profile.name, M, hy + 12, paint(13f, bold = true)); hy += 18 }
            val contact = listOf(profile.address, profile.phone, profile.email).filter { it.isNotBlank() }.joinToString(" · ")
            if (contact.isNotBlank()) { cv.drawText(fit(contact, small, usable), M, hy + 9, small); hy += 14 }
            hy += 6
            cv.drawText(fit(t.title, title, usable), M, hy + 14, title); hy += 20
            if (t.subtitle.isNotBlank()) { cv.drawText(fit(t.subtitle, small, usable), M, hy + 9, small); hy += 14 }
            hy += 6
            var x = M
            t.columns.forEachIndexed { i, col ->
                drawCell(cv, col.header, x, hy + 11, widths[i], head, col.numeric)
                x += widths[i]
            }
            hy += rowH
            cv.drawLine(M, hy - 2, W - M, hy - 2, line)
            y = hy + 2
        }

        fun finishPage() {
            val cv = c ?: return
            cv.drawText("$footer  ·  $pageNo", M, H - M / 2, small)
            doc.finishPage(page!!)
        }

        fun newPage() {
            if (page != null) finishPage()
            pageNo++
            page = doc.startPage(PdfDocument.PageInfo.Builder(W, H, pageNo).create())
            c = page!!.canvas
            header(c!!)
        }

        fun row(cells: List<String>, p: TextPaint) {
            if (c == null || y + rowH > H - M - 10) newPage()
            var x = M
            cells.forEachIndexed { i, v ->
                if (i < widths.size) drawCell(c!!, v, x, y + 11, widths[i], p, t.columns[i].numeric)
                x += widths.getOrElse(i) { 0f }
            }
            y += rowH
            c!!.drawLine(M, y - 2, W - M, y - 2, line)
        }

        newPage()
        t.rows.forEach { row(it, cell) }
        t.totals?.let { row(it, bold) }
        t.notes.forEach { n ->
            if (y + rowH > H - M - 10) newPage()
            y += 4
            c!!.drawText(fit(n, small, usable), M, y + 9, small)
            y += 12
        }
        finishPage()
        val dir = File(ctx.cacheDir, "receipts").apply { mkdirs() }
        val f = File(dir, safe(fileName) + ".pdf")
        FileOutputStream(f).use { doc.writeTo(it) }
        doc.close()
        return f
    }

    private fun paint(size: Float, bold: Boolean = false) = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = size
        color = Color.BLACK
        typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
    }

    private fun fit(s: String, p: TextPaint, w: Float) = TextUtils.ellipsize(s, p, w, TextUtils.TruncateAt.END).toString()

    private fun drawCell(c: Canvas, v: String, x: Float, baseline: Float, w: Float, p: TextPaint, right: Boolean) {
        val text = fit(v, p, w - 6)
        val dx = if (right) w - 4 - p.measureText(text) else 2f
        c.drawText(text, x + dx, baseline, p)
    }

    private fun safe(name: String) = name.map { if (it.isLetterOrDigit() || it == '-' || it == '_') it else '-' }.joinToString("")

    fun writeCsv(ctx: Context, t: ReportTable, fileName: String): File {
        val dir = File(ctx.cacheDir, "receipts").apply { mkdirs() }
        return File(dir, safe(fileName) + ".csv").apply { writeText(Csv.of(t)) }
    }

    fun share(ctx: Context, f: File, mime: String) {
        val uri = FileProvider.getUriForFile(ctx, ctx.packageName + ".files", f)
        val send = Intent(Intent.ACTION_SEND).setType(mime).putExtra(Intent.EXTRA_STREAM, uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        runCatching { ctx.startActivity(Intent.createChooser(send, null)) }
    }

    /** Opens the phone's print dialog (Save as PDF or any printer the phone knows) for a finished PDF file. */
    fun print(ctx: Context, f: File, jobName: String) {
        val pm = ctx.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return
        pm.print(jobName, object : PrintDocumentAdapter() {
            override fun onLayout(old: PrintAttributes?, new: PrintAttributes?, cancel: CancellationSignal?, cb: LayoutResultCallback, extras: Bundle?) {
                if (cancel?.isCanceled == true) { cb.onLayoutCancelled(); return }
                cb.onLayoutFinished(PrintDocumentInfo.Builder(f.name).setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT).build(), true)
            }

            override fun onWrite(pages: Array<out PageRange>?, dest: ParcelFileDescriptor, cancel: CancellationSignal?, cb: WriteResultCallback) {
                runCatching {
                    FileInputStream(f).use { input -> FileOutputStream(dest.fileDescriptor).use { input.copyTo(it) } }
                }.onSuccess { cb.onWriteFinished(arrayOf(PageRange.ALL_PAGES)) }.onFailure { cb.onWriteFailed(it.message) }
            }
        }, PrintAttributes.Builder().setMediaSize(PrintAttributes.MediaSize.ISO_A4).build())
    }
}
