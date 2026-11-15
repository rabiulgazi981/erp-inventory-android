package com.bongshaltech.erpinventory.core.report

/** One column: its header and whether its values are numbers (right-aligned). */
data class Col(val header: String, val numeric: Boolean = false)

/** A finished report, already in words and formatted money, ready to show, export as CSV or draw as PDF. */
data class ReportTable(
    val title: String,
    val subtitle: String,
    val columns: List<Col>,
    val rows: List<List<String>>,
    /** Bottom line, same length as [columns]; null for none. */
    val totals: List<String>? = null,
    /** Short lines printed under the table, e.g. how a figure was worked out. */
    val notes: List<String> = emptyList(),
)

object Csv {
    /** RFC 4180 CSV with a BOM so Excel opens Bangla text correctly. */
    fun of(t: ReportTable): String = buildString {
        append('﻿')
        line(t.columns.map { it.header })
        t.rows.forEach { line(it) }
        t.totals?.let { line(it) }
    }

    private fun StringBuilder.line(cells: List<String>) {
        append(cells.joinToString(",") { cell(it) }).append("\r\n")
    }

    fun cell(v: String): String =
        if (v.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) "\"" + v.replace("\"", "\"\"") + "\"" else v
}
