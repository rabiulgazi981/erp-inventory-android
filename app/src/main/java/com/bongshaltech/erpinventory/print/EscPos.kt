package com.bongshaltech.erpinventory.print

import android.graphics.Bitmap
import java.io.ByteArrayOutputStream

/** ESC/POS bytes for common 58 / 80 mm Bluetooth thermal printers. */
object EscPos {
    /** Rows per raster command. Small bands keep cheap printers from running out of buffer. */
    private const val BAND = 128

    fun image(bmp: Bitmap, cut: Boolean): ByteArray {
        val out = ByteArrayOutputStream()
        out.write(byteArrayOf(0x1B, 0x40)) // ESC @ : reset
        out.write(byteArrayOf(0x1B, 0x61, 0x01)) // centre
        val w = bmp.width
        val bytesPerRow = (w + 7) / 8
        val px = IntArray(w)
        var top = 0
        while (top < bmp.height) {
            val rows = minOf(BAND, bmp.height - top)
            // GS v 0: raster bit image, normal size
            out.write(byteArrayOf(0x1D, 0x76, 0x30, 0x00))
            out.write(bytesPerRow and 0xFF); out.write((bytesPerRow shr 8) and 0xFF)
            out.write(rows and 0xFF); out.write((rows shr 8) and 0xFF)
            for (y in top until top + rows) {
                bmp.getPixels(px, 0, w, 0, y, w, 1)
                val row = ByteArray(bytesPerRow)
                for (x in 0 until w) {
                    val c = px[x]
                    val lum = ((c shr 16 and 0xFF) * 299 + (c shr 8 and 0xFF) * 587 + (c and 0xFF) * 114) / 1000
                    if (lum < 150) row[x shr 3] = (row[x shr 3].toInt() or (0x80 ushr (x and 7))).toByte()
                }
                out.write(row)
            }
            top += rows
        }
        out.write(byteArrayOf(0x1B, 0x64, 0x03)) // feed 3 lines
        if (cut) out.write(byteArrayOf(0x1D, 0x56, 0x42, 0x00)) // partial cut, ignored by printers without a cutter
        return out.toByteArray()
    }
}
