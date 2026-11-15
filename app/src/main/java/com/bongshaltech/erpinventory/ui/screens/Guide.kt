package com.bongshaltech.erpinventory.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bongshaltech.erpinventory.R
import com.bongshaltech.erpinventory.ui.components.Card
import com.bongshaltech.erpinventory.ui.components.Header
import com.bongshaltech.erpinventory.ui.components.Muted
import com.bongshaltech.erpinventory.ui.components.T
import com.bongshaltech.erpinventory.ui.theme.Ui

/** Topic title and text, in the order shown. */
private val topics = listOf(
    R.string.guide_start_t to R.string.guide_start_b,
    R.string.guide_sale_t to R.string.guide_sale_b,
    R.string.guide_products_t to R.string.guide_products_b,
    R.string.guide_customers_t to R.string.guide_customers_b,
    R.string.guide_purchase_t to R.string.guide_purchase_b,
    R.string.guide_expense_t to R.string.guide_expense_b,
    R.string.guide_reports_t to R.string.guide_reports_b,
    R.string.guide_print_t to R.string.guide_print_b,
    R.string.guide_staff_t to R.string.guide_staff_b,
    R.string.guide_features_t to R.string.guide_features_b,
    R.string.guide_backup_t to R.string.guide_backup_b,
    R.string.guide_adfree_t to R.string.guide_adfree_b,
)

/** More › User guide: short how-tos that open one at a time. Works offline. */
@Composable
fun GuideScreen(onBack: () -> Unit) {
    var open by remember { mutableStateOf(0) }
    Column(Modifier.fillMaxSize().background(Ui.c.bg)) {
        Header(stringResource(R.string.guide_title), onBack)
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            itemsIndexed(topics) { i, (t, b) ->
                Card(Modifier.fillMaxWidth(), padding = 12.dp, onClick = { open = if (open == i) -1 else i }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        T("${i + 1}. " + stringResource(t), 15.sp, FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        Muted(if (open == i) "−" else "+", 18.sp)
                    }
                    if (open == i) T(stringResource(b), 14.sp, color = Ui.c.label)
                }
            }
            item { Muted(stringResource(R.string.guide_help), 13.sp) }
        }
    }
}
