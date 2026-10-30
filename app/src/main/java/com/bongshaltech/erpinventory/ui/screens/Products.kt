package com.bongshaltech.erpinventory.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bongshaltech.erpinventory.R
import com.bongshaltech.erpinventory.data.db.ProductEntity
import com.bongshaltech.erpinventory.ui.components.ErpTopBar
import com.bongshaltech.erpinventory.ui.components.Field
import com.bongshaltech.erpinventory.ui.qty
import com.bongshaltech.erpinventory.ui.taka
import com.bongshaltech.erpinventory.ui.theme.Brand
import com.bongshaltech.erpinventory.ui.toMilli
import com.bongshaltech.erpinventory.ui.toPaisa
import kotlinx.coroutines.launch

@Composable
fun ProductsScreen(onBack: () -> Unit, onEdit: (Long) -> Unit) {
    val app = app()
    var q by rememberSaveable { mutableStateOf("") }
    val list by remember(q) { app.repo.products.search(q.trim()) }.collectAsStateWithLifecycle(emptyList())
    Scaffold(
        topBar = { ErpTopBar(stringResource(R.string.products_title), onBack) },
        floatingActionButton = {
            FloatingActionButton(onClick = { onEdit(0) }, containerColor = Brand.Blue) {
                Icon(Icons.Default.Add, stringResource(R.string.product_add), tint = androidx.compose.ui.graphics.Color.White)
            }
        },
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize().padding(horizontal = 16.dp)) {
            Field(q, { q = it }, stringResource(R.string.products_search))
            if (list.isEmpty() && q.isBlank()) Text(stringResource(R.string.products_empty), Modifier.padding(top = 16.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 12.dp)) {
                items(list, key = { it.id }) { p -> ProductRow(p) { onEdit(p.id) } }
            }
        }
    }
}

@Composable
fun ProductRow(p: ProductEntity, onClick: () -> Unit) {
    val low = p.stockMilli <= p.lowStockMilli
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surface).clickable(onClick = onClick).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(p.name, style = MaterialTheme.typography.titleSmall)
            Text(
                stringResource(R.string.product_stock_short, p.stockMilli.qty(), p.unit),
                style = MaterialTheme.typography.bodySmall,
                color = if (low) Brand.Danger else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            )
        }
        Text(p.pricePaisa.taka(), style = MaterialTheme.typography.titleSmall)
    }
}

@Composable
fun ProductEditScreen(id: Long, onDone: () -> Unit) {
    val app = app()
    val scope = rememberCoroutineScope()
    var loaded by remember { mutableStateOf<ProductEntity?>(null) }
    var name by rememberSaveable { mutableStateOf("") }
    var barcode by rememberSaveable { mutableStateOf("") }
    var unit by rememberSaveable { mutableStateOf("pcs") }
    var cost by rememberSaveable { mutableStateOf("") }
    var price by rememberSaveable { mutableStateOf("") }
    var stock by rememberSaveable { mutableStateOf("") }
    var lowAt by rememberSaveable { mutableStateOf("") }
    var tried by remember { mutableStateOf(false) }
    val required = stringResource(R.string.required)

    LaunchedEffect(id) {
        if (id != 0L) app.repo.products.get(id)?.let { p ->
            loaded = p; name = p.name; barcode = p.barcode.orEmpty(); unit = p.unit
            cost = (p.costPaisa / 100.0).toString(); price = (p.pricePaisa / 100.0).toString()
            stock = p.stockMilli.qty(); lowAt = p.lowStockMilli.qty()
        }
    }

    Scaffold(topBar = { ErpTopBar(stringResource(if (id == 0L) R.string.product_add else R.string.product_edit), onDone) }) { pad ->
        Column(
            Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Field(name, { name = it }, stringResource(R.string.product_name), error = required.takeIf { tried && name.isBlank() })
            Field(barcode, { barcode = it }, stringResource(R.string.product_barcode))
            Field(unit, { unit = it }, stringResource(R.string.product_unit))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Field(cost, { cost = it }, stringResource(R.string.product_cost), Modifier.weight(1f), number = true)
                Field(price, { price = it }, stringResource(R.string.product_price), Modifier.weight(1f), number = true,
                    error = required.takeIf { tried && price.toPaisa() == null })
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Field(stock, { stock = it }, stringResource(R.string.product_stock), Modifier.weight(1f), number = true)
                Field(lowAt, { lowAt = it }, stringResource(R.string.product_low_at), Modifier.weight(1f), number = true)
            }
            Button(
                onClick = {
                    tried = true
                    val pricePaisa = price.toPaisa()
                    if (name.isBlank() || pricePaisa == null) return@Button
                    val base = loaded ?: ProductEntity(name = "", pricePaisa = 0)
                    scope.launch {
                        app.repo.saveProduct(
                            base.copy(
                                name = name.trim(), barcode = barcode.trim().ifEmpty { null }, unit = unit.trim().ifEmpty { "pcs" },
                                costPaisa = cost.toPaisa() ?: 0, pricePaisa = pricePaisa,
                                stockMilli = stock.toMilli() ?: 0, lowStockMilli = lowAt.toMilli() ?: 0,
                            )
                        )
                        onDone()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) { Text(stringResource(R.string.save)) }
            if (loaded != null) TextButton(
                onClick = { scope.launch { app.repo.products.archive(id); onDone() } },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.delete), color = Brand.Danger) }
        }
    }
}
