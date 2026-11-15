package com.bongshaltech.erpinventory.core.sheet

import java.io.ByteArrayInputStream
import java.io.InputStream
import java.util.zip.ZipInputStream
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element

/** Reads the first sheet of an .xlsx file, or a .csv file, as rows of text. */
object Sheet {
    fun read(bytes: ByteArray): List<List<String>> =
        if (bytes.size > 4 && bytes[0] == 'P'.code.toByte() && bytes[1] == 'K'.code.toByte()) xlsx(bytes) else csv(String(bytes, Charsets.UTF_8).removePrefix("﻿"))

    fun csv(text: String): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        var row = mutableListOf<String>()
        val cell = StringBuilder()
        var quoted = false
        var i = 0
        val sep = if (text.lineSequence().firstOrNull()?.let { it.count { c -> c == ';' } > it.count { c -> c == ',' } } == true) ';' else ','
        while (i < text.length) {
            val c = text[i]
            when {
                quoted && c == '"' && text.getOrNull(i + 1) == '"' -> { cell.append('"'); i++ }
                c == '"' -> quoted = !quoted
                !quoted && c == sep -> { row.add(cell.toString()); cell.clear() }
                !quoted && (c == '\n' || c == '\r') -> {
                    if (c == '\r' && text.getOrNull(i + 1) == '\n') i++
                    row.add(cell.toString()); cell.clear(); rows.add(row); row = mutableListOf()
                }
                else -> cell.append(c)
            }
            i++
        }
        if (cell.isNotEmpty() || row.isNotEmpty()) { row.add(cell.toString()); rows.add(row) }
        return rows.map { r -> r.map { it.trim() } }.filter { r -> r.any { it.isNotEmpty() } }
    }

    private fun parse(xml: ByteArray) = DocumentBuilderFactory.newInstance().apply {
        runCatching { setFeature("http://apache.org/xml/features/disallow-doctype-decl", true) }
        isNamespaceAware = false
    }.newDocumentBuilder().parse(ByteArrayInputStream(xml))

    private fun entries(bytes: ByteArray): Map<String, ByteArray> {
        val out = mutableMapOf<String, ByteArray>()
        ZipInputStream(bytes.inputStream()).use { z ->
            while (true) {
                val e = z.nextEntry ?: break
                if (e.name == "xl/sharedStrings.xml" || e.name.startsWith("xl/worksheets/sheet")) out[e.name] = z.readLimited(20_000_000)
            }
        }
        return out
    }

    private fun InputStream.readLimited(max: Int): ByteArray {
        val bytes = readBytes()
        require(bytes.size <= max) { "File too large" }
        return bytes
    }

    private fun Element.text(): String {
        val ts = getElementsByTagName("t")
        return (0 until ts.length).joinToString("") { ts.item(it).textContent }
    }

    private fun col(ref: String): Int = ref.takeWhile { it.isLetter() }.fold(0) { acc, c -> acc * 26 + (c.uppercaseChar() - 'A' + 1) } - 1

    private fun number(v: String): String {
        val d = v.toDoubleOrNull() ?: return v
        return if (d == Math.floor(d) && kotlin.math.abs(d) < 1e15) d.toLong().toString() else java.math.BigDecimal(v).stripTrailingZeros().toPlainString()
    }

    fun xlsx(bytes: ByteArray): List<List<String>> {
        val files = entries(bytes)
        val shared = files["xl/sharedStrings.xml"]?.let { x ->
            val si = parse(x).getElementsByTagName("si")
            (0 until si.length).map { (si.item(it) as Element).text() }
        }.orEmpty()
        val sheetName = files.keys.filter { it.startsWith("xl/worksheets/sheet") }.minByOrNull { it.filter(Char::isDigit).toIntOrNull() ?: 0 } ?: return emptyList()
        val rows = parse(files.getValue(sheetName)).getElementsByTagName("row")
        val out = mutableListOf<List<String>>()
        for (r in 0 until rows.length) {
            val cells = (rows.item(r) as Element).getElementsByTagName("c")
            val row = mutableListOf<String>()
            for (c in 0 until cells.length) {
                val e = cells.item(c) as Element
                val at = e.getAttribute("r").takeIf { it.isNotEmpty() }?.let(::col) ?: row.size
                val v = e.getElementsByTagName("v").item(0)?.textContent.orEmpty()
                val value = when (e.getAttribute("t")) {
                    "s" -> v.toIntOrNull()?.let { shared.getOrNull(it) }.orEmpty()
                    "inlineStr" -> e.text()
                    "str", "b" -> v
                    else -> number(v)
                }
                while (row.size < at) row.add("")
                if (row.size == at) row.add(value.trim()) else row[at] = value.trim()
            }
            if (row.any { it.isNotEmpty() }) out.add(row)
        }
        return out
    }
}
