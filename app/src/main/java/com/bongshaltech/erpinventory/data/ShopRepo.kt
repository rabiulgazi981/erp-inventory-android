package com.bongshaltech.erpinventory.data

import androidx.room.withTransaction
import com.bongshaltech.erpinventory.core.DocNumber
import com.bongshaltech.erpinventory.core.DocType
import com.bongshaltech.erpinventory.core.Money
import com.bongshaltech.erpinventory.data.db.AuditLogEntity
import com.bongshaltech.erpinventory.data.db.AppDatabase
import com.bongshaltech.erpinventory.data.db.CustomerEntity
import com.bongshaltech.erpinventory.data.db.DocCounterEntity
import com.bongshaltech.erpinventory.data.db.DuePaymentEntity
import com.bongshaltech.erpinventory.data.db.ExpenseEntity
import com.bongshaltech.erpinventory.data.db.PurchaseEntity
import com.bongshaltech.erpinventory.data.db.PurchaseItemEntity
import com.bongshaltech.erpinventory.data.db.SupplierEntity
import com.bongshaltech.erpinventory.data.db.SupplierPaymentEntity
import com.bongshaltech.erpinventory.data.db.PayMethod
import com.bongshaltech.erpinventory.data.db.ProductEntity
import com.bongshaltech.erpinventory.data.db.SaleEntity
import com.bongshaltech.erpinventory.data.db.SaleItemEntity
import com.bongshaltech.erpinventory.data.db.StaffEntity
import com.bongshaltech.erpinventory.data.db.MfsTxnEntity
import com.bongshaltech.erpinventory.data.db.MfsWalletEntity
import com.bongshaltech.erpinventory.data.db.PurchaseReturnEntity
import com.bongshaltech.erpinventory.data.db.PurchaseReturnItemEntity
import com.bongshaltech.erpinventory.data.db.ReturnableLine
import com.bongshaltech.erpinventory.data.db.OrderDocEntity
import com.bongshaltech.erpinventory.data.db.CashSessionEntity
import com.bongshaltech.erpinventory.data.db.LoanEntity
import com.bongshaltech.erpinventory.data.db.MoneyMoveEntity
import com.bongshaltech.erpinventory.data.db.MoveKind
import com.bongshaltech.erpinventory.data.db.PromoEntity
import com.bongshaltech.erpinventory.data.db.StockBatchEntity
import com.bongshaltech.erpinventory.data.db.ProductSerialEntity
import com.bongshaltech.erpinventory.data.db.StockJobEntity
import com.bongshaltech.erpinventory.data.db.StockJobInputEntity
import com.bongshaltech.erpinventory.data.db.WarehouseStockEntity
import com.bongshaltech.erpinventory.data.db.TableOrderEntity
import com.bongshaltech.erpinventory.data.db.TableOrderItemEntity
import com.bongshaltech.erpinventory.data.db.SalaryPaymentEntity
import com.bongshaltech.erpinventory.data.db.PromoKind
import com.bongshaltech.erpinventory.data.db.SalePromoEntity
import com.bongshaltech.erpinventory.data.db.OrderDocItemEntity
import com.bongshaltech.erpinventory.data.db.OrderKind
import com.bongshaltech.erpinventory.data.db.OrderStatus
import com.bongshaltech.erpinventory.data.db.SaleReturnEntity
import com.bongshaltech.erpinventory.data.db.SaleReturnItemEntity
import com.bongshaltech.erpinventory.security.PinHasher
import java.time.LocalDate
import kotlinx.coroutines.flow.map

data class CartLine(val product: ProductEntity, val qtyMilli: Long, val serials: List<String> = emptyList()) {
    /** price × qty, rounded to the nearest paisa. */
    val totalPaisa: Long get() = (product.pricePaisa * qtyMilli + 500) / 1000
}

data class NewSale(
    val lines: List<CartLine>,
    val discountPaisa: Long,
    val paidPaisa: Long,
    val method: PayMethod,
    val customerPhone: String?,
    val customerName: String?,
    val smsRequested: Boolean,
    val staffId: Long?,
    /** Offers, coupon and voucher already counted inside [discountPaisa]. */
    val promos: List<AppliedPromo> = emptyList(),
)

data class AppliedPromo(val promoId: Long?, val kind: PromoKind, val code: String, val amountPaisa: Long)

data class PurchaseLine(val product: ProductEntity, val qtyMilli: Long, val unitCostPaisa: Long, val batchNo: String = "", val expiryDay: Long? = null) {
    val totalPaisa: Long get() = (unitCostPaisa * qtyMilli + 500) / 1000
}

data class NewPurchase(
    val lines: List<PurchaseLine>,
    val discountPaisa: Long,
    val paidPaisa: Long,
    val method: PayMethod,
    /** An existing supplier, or a new one made from [supplierName] / [supplierPhone]. */
    val supplierId: Long?,
    val supplierName: String,
    val supplierPhone: String,
    val staffId: Long?,
)

sealed interface PurchaseResult {
    data class Saved(val number: String, val totalPaisa: Long) : PurchaseResult
    data object DueNeedsSupplier : PurchaseResult
}

sealed interface SaleResult {
    data class Saved(val id: Long, val number: String, val totalPaisa: Long) : SaleResult
    data class NotEnoughStock(val productName: String) : SaleResult
    data object DueNeedsCustomer : SaleResult
    /** A coupon ran out of uses or a voucher's balance dropped while the sale was open. */
    data class PromoUsedUp(val code: String) : SaleResult
}

data class OrderLine(val product: ProductEntity, val qtyMilli: Long, val unitPricePaisa: Long) {
    val totalPaisa: Long get() = (unitPricePaisa * qtyMilli + 500) / 1000
}

data class NewOrder(
    val kind: OrderKind,
    /** 0 for a new one, otherwise an open document being edited. */
    val id: Long,
    val lines: List<OrderLine>,
    val discountPaisa: Long,
    val partyName: String,
    val partyPhone: String,
    val supplierId: Long?,
    val note: String,
    val staffId: Long?,
)

/** A quote, sales order or purchase order handed to the Sale or Purchase screen to finish. */
object PendingDoc {
    data class ForSale(val orderId: Long, val lines: List<CartLine>, val phone: String, val name: String, val discountPaisa: Long, val tableOrderId: Long? = null)
    data class ForPurchase(val orderId: Long, val lines: List<PurchaseLine>, val supplierId: Long?, val name: String, val phone: String, val discountPaisa: Long)
    var sale: ForSale? = null
    var purchase: ForPurchase? = null
}

/** How many of one line are coming back. */
data class ReturnPick(val line: ReturnableLine, val qtyMilli: Long)

/** What a return is worth and how it is settled: first off the party's due, the rest paid back. */
data class ReturnSplit(val totalPaisa: Long, val dueCutPaisa: Long, val refundPaisa: Long)

sealed interface ReturnResult {
    data class Saved(val number: String, val split: ReturnSplit) : ReturnResult
    data class NotEnoughStock(val productName: String) : ReturnResult
    /** Nothing picked, or more than what is left to return. */
    data object Invalid : ReturnResult
}

object Returns {
    private fun part(amount: Long, num: Long, den: Long) = if (den <= 0) 0 else (amount * num + den / 2) / den

    /**
     * Value of the picked goods at the price on the memo, with the memo's discount shared out
     * by value, so a return never pays back more than the customer actually paid for those goods.
     */
    fun value(picks: List<ReturnPick>, docSubtotal: Long, docTotal: Long): Long =
        picks.sumOf { p -> part(part(p.line.lineTotalPaisa, p.qtyMilli, p.line.qtyMilli), docTotal, docSubtotal) }

    fun split(total: Long, partyDue: Long): ReturnSplit {
        val cut = minOf(partyDue.coerceAtLeast(0), total)
        return ReturnSplit(total, cut, total - cut)
    }

    fun cost(picks: List<ReturnPick>): Long = picks.sumOf { (it.line.unitCostPaisa * it.qtyMilli + 500) / 1000 }
}

class ShopRepo(private val db: AppDatabase, private val settings: SettingsRepo) {
    val products get() = db.products()
    val customers get() = db.customers()
    val sales get() = db.sales()
    val staff get() = db.staff()
    val suppliers get() = db.suppliers()
    val purchases get() = db.purchases()
    val expenses get() = db.expenses()
    val reports get() = db.reports()
    val mfs get() = db.mfs()
    val returns get() = db.returns()
    val orders get() = db.orders()
    val money get() = db.money()

    /** Makes sure every provider has a row, so the owner only switches them on. */
    suspend fun ensureWallets() = db.mfs().insertWallets(MfsProvider.entries.map { MfsWalletEntity(provider = it.name) })

    suspend fun saveWallet(w: MfsWalletEntity) = db.mfs().updateWallet(w)

    /** Saves one mobile banking / load entry and moves the wallet balance. E-money can never go below zero. */
    suspend fun addMfs(n: NewMfs): MfsResult {
        val code = settings.current().shopCode
        return db.withTransaction {
            val w = db.mfs().wallet(n.walletId) ?: return@withTransaction MfsResult.NoWallet
            val e = mfsEffect(w, n)
            if (w.balancePaisa + e.walletDelta.paisa < 0) return@withTransaction MfsResult.LowBalance(w.balancePaisa)
            val number = nextNumber(code, DocType.MFS)
            db.mfs().insertTxn(
                MfsTxnEntity(
                    number = number, walletId = w.id, type = n.type.name, customerPhone = n.customerPhone.filter(Char::isDigit),
                    amountPaisa = n.amountPaisa,
                    chargePaisa = if (n.type.isBalance) n.settledPaisa ?: n.amountPaisa else n.chargePaisa,
                    feePaisa = n.feePaisa, walletDeltaPaisa = e.walletDelta.paisa, cashDeltaPaisa = e.cashDelta.paisa,
                    earningPaisa = e.earning.paisa, via = if (n.type.isBalance) n.via.name else "",
                    trxId = n.trxId.trim().uppercase(), note = n.note.trim(), staffId = n.staffId,
                )
            )
            db.mfs().addBalance(w.id, e.walletDelta.paisa)
            MfsResult.Saved(number)
        }
    }

    suspend fun saveProduct(p: ProductEntity): Long {
        val old = if (p.id != 0L) db.products().get(p.id) else null
        val id = db.products().upsert(p)
        when {
            old == null -> audit("PRODUCT_NEW", "${p.name} · ${tk(p.pricePaisa)}")
            else -> {
                if (old.pricePaisa != p.pricePaisa) audit("PRICE_CHANGE", "${p.name}: ${tk(old.pricePaisa)} → ${tk(p.pricePaisa)}")
                if (old.costPaisa != p.costPaisa) audit("COST_CHANGE", "${p.name}: ${tk(old.costPaisa)} → ${tk(p.costPaisa)}")
                if (old.stockMilli != p.stockMilli) audit("STOCK_EDIT", "${p.name}: ${Qty.text(old.stockMilli)} → ${Qty.text(p.stockMilli)}")
            }
        }
        return id
    }

    private fun tk(paisa: Long) = Money(paisa).format()

    private object Qty {
        fun text(milli: Long) = java.math.BigDecimal(milli).movePointLeft(3).stripTrailingZeros().toPlainString()
    }

    val audit get() = db.audit()

    /** One line in the activity log, as whoever is signed in now. */
    private suspend fun audit(action: String, detail: String) {
        val who = (settings.current().session as? Session.Staff)?.id
        db.audit().insert(AuditLogEntity(staffId = who, action = action, detail = detail.take(300)))
    }

    /** Gives a product a barcode when it has none, so a label can be printed. Returns the barcode. */
    suspend fun ensureBarcode(p: ProductEntity): String {
        p.barcode?.takeIf { it.isNotBlank() }?.let { return it }
        var code = "2" + p.id.toString().padStart(11, '0')
        if (db.products().byBarcode(code) != null) code += System.currentTimeMillis() % 1000
        db.products().upsert(p.copy(barcode = code))
        return code
    }

    data class ImportResult(val added: Int, val updated: Int, val skipped: List<Int>)

    /**
     * Adds products from a sheet. A row whose barcode (or, without one, exact name) matches a product updates its
     * name, category, prices; stock is only taken for new products, so re-importing never doubles stock.
     */
    suspend fun importProducts(rows: List<com.bongshaltech.erpinventory.core.sheet.ProductRow>): ImportResult {
        var added = 0
        var updated = 0
        val skipped = mutableListOf<Int>()
        db.withTransaction {
            val all = db.products().allNow()
            val byName = all.associateBy { it.name.trim().lowercase() }
            for (r in rows) {
                val price = Money.parse(r.price)?.paisa
                val cost = Money.parse(r.cost)?.paisa
                val barcode = r.barcode.trim().ifEmpty { null }
                val existing = barcode?.let { db.products().byBarcode(it) } ?: byName[r.name.trim().lowercase()]
                if (existing == null && price == null) { skipped += r.line; continue }
                if (existing == null) {
                    db.products().upsert(
                        ProductEntity(
                            name = r.name.trim(), barcode = barcode, category = r.category, unit = r.unit.ifBlank { "pcs" },
                            costPaisa = cost ?: 0, pricePaisa = price ?: 0, generic = r.generic,
                            stockMilli = milli(r.stock) ?: 0, lowStockMilli = milli(r.lowAt) ?: 0,
                        )
                    )
                    added++
                } else {
                    db.products().upsert(
                        existing.copy(
                            name = r.name.trim(), barcode = existing.barcode ?: barcode,
                            category = r.category.ifBlank { existing.category }, unit = r.unit.ifBlank { existing.unit },
                            costPaisa = cost ?: existing.costPaisa, pricePaisa = price ?: existing.pricePaisa,
                            generic = r.generic.ifBlank { existing.generic }, lowStockMilli = milli(r.lowAt) ?: existing.lowStockMilli,
                            active = true,
                        )
                    )
                    updated++
                }
            }
        }
        audit("IMPORT", "+$added · ~$updated")
        return ImportResult(added, updated, skipped)
    }

    private fun milli(text: String): Long? = text.replace(",", "").trim().toBigDecimalOrNull()?.movePointRight(3)?.toLong()

    suspend fun saveSale(s: NewSale): SaleResult {
        val subtotal = s.lines.sumOf { it.totalPaisa }
        val total = (subtotal - s.discountPaisa).coerceAtLeast(0)
        val due = total - s.paidPaisa
        val phone = s.customerPhone?.filter(Char::isDigit)?.takeIf { it.isNotEmpty() }
        if (due > 0 && phone == null) return SaleResult.DueNeedsCustomer
        val code = settings.current().shopCode

        return try {
            db.withTransaction {
                // Stock check and decrement in one step per line, so two quick sales can never oversell.
                for (line in s.lines) {
                    if (db.products().takeStock(line.product.id, line.qtyMilli) == 0) {
                        throw NotEnough(line.product.name)
                    }
                    takeFromBatches(line.product.id, line.qtyMilli)
                }
                val customerId = phone?.let {
                    db.customers().byPhone(it)?.id
                        ?: db.customers().insert(CustomerEntity(name = s.customerName?.ifBlank { null } ?: it, phone = it))
                }
                if (due > 0 && customerId != null) db.customers().addDue(customerId, due)

                val year = LocalDate.now().year
                val serial = DocNumber.next(db.docCounters().last(DocType.SALE.name, year))
                db.docCounters().set(DocCounterEntity(DocType.SALE.name, year, serial))
                val number = DocNumber.format(code, DocType.SALE, year, serial)

                val saleId = db.sales().insert(
                    SaleEntity(
                        number = number, customerId = customerId, subtotalPaisa = subtotal, discountPaisa = s.discountPaisa,
                        totalPaisa = total, paidPaisa = minOf(s.paidPaisa, total), method = s.method,
                        smsRequested = s.smsRequested && phone != null, staffId = s.staffId,
                    )
                )
                db.sales().insertItems(s.lines.map {
                    SaleItemEntity(
                        saleId = saleId, productId = it.product.id, name = it.product.name,
                        qtyMilli = it.qtyMilli, unitPricePaisa = it.product.pricePaisa, lineTotalPaisa = it.totalPaisa,
                        unitCostPaisa = it.product.costPaisa,
                    )
                })
                val now = System.currentTimeMillis()
                for (line in s.lines) for (serial in line.serials) {
                    if (db.stockTools().sell(serial, saleId, now) == 0) throw NotEnough(line.product.name + " · " + serial)
                }
                for (p in s.promos) {
                    val id = p.promoId ?: continue
                    val ok = when (p.kind) {
                        PromoKind.OFFER -> 1
                        PromoKind.COUPON -> db.promos().use(id)
                        PromoKind.VOUCHER -> db.promos().spend(id, p.amountPaisa)
                    }
                    if (ok == 0) throw PromoGone(p.code)
                }
                if (s.promos.isNotEmpty()) {
                    db.promos().insertSalePromos(s.promos.filter { it.amountPaisa > 0 }.map {
                        SalePromoEntity(saleId = saleId, promoId = it.promoId, kind = it.kind, code = it.code, amountPaisa = it.amountPaisa)
                    })
                }
                audit("SALE", "$number · ${tk(total)}" + if (s.discountPaisa > 0) " · −${tk(s.discountPaisa)}" else "")
                SaleResult.Saved(saleId, number, total)
            }
        } catch (e: NotEnough) {
            SaleResult.NotEnoughStock(e.productName)
        } catch (e: PromoGone) {
            SaleResult.PromoUsedUp(e.code)
        }
    }

    /** Adds or edits a customer. Returns false when another customer already has this phone. */
    suspend fun saveCustomer(c: CustomerEntity): Boolean {
        val phone = c.phone.filter(Char::isDigit)
        val other = db.customers().byPhone(phone)
        if (other != null && other.id != c.id) return false
        val clean = c.copy(name = c.name.trim(), phone = phone, address = c.address.trim())
        if (c.id == 0L) db.customers().insert(clean) else db.customers().update(clean)
        return true
    }

    /** Takes a payment against a customer's due. The amount is capped at what they owe. Returns the receipt number. */
    suspend fun collectDue(customerId: Long, amountPaisa: Long, method: PayMethod, note: String, staffId: Long?): String? {
        val code = settings.current().shopCode
        return db.withTransaction {
            val c = db.customers().get(customerId) ?: return@withTransaction null
            val amount = minOf(amountPaisa, c.duePaisa)
            if (amount <= 0) return@withTransaction null
            val year = LocalDate.now().year
            val serial = DocNumber.next(db.docCounters().last(DocType.PAYMENT.name, year))
            db.docCounters().set(DocCounterEntity(DocType.PAYMENT.name, year, serial))
            val number = DocNumber.format(code, DocType.PAYMENT, year, serial)
            db.customers().insertPayment(
                DuePaymentEntity(number = number, customerId = customerId, amountPaisa = amount, method = method, note = note.trim(), staffId = staffId)
            )
            db.customers().addDue(customerId, -amount)
            number
        }
    }

    suspend fun saveSupplier(s: SupplierEntity) {
        val clean = s.copy(name = s.name.trim(), phone = s.phone.filter(Char::isDigit), address = s.address.trim())
        if (s.id == 0L) db.suppliers().insert(clean) else db.suppliers().update(clean)
    }

    /** Goods in: stock up, latest cost saved on each product, and any unpaid part added to the supplier's due. */
    suspend fun savePurchase(p: NewPurchase): PurchaseResult {
        val subtotal = p.lines.sumOf { it.totalPaisa }
        val total = (subtotal - p.discountPaisa).coerceAtLeast(0)
        val due = total - p.paidPaisa
        val hasNew = p.supplierName.isNotBlank()
        if (due > 0 && p.supplierId == null && !hasNew) return PurchaseResult.DueNeedsSupplier
        val code = settings.current().shopCode
        return db.withTransaction {
            for (l in p.lines) db.products().receive(l.product.id, l.qtyMilli, l.unitCostPaisa)
            val supplierId = p.supplierId ?: if (hasNew) {
                db.suppliers().insert(SupplierEntity(name = p.supplierName.trim(), phone = p.supplierPhone.filter(Char::isDigit)))
            } else null
            if (due > 0 && supplierId != null) db.suppliers().addDue(supplierId, due)
            val number = nextNumber(code, DocType.PURCHASE)
            val id = db.purchases().insert(
                PurchaseEntity(
                    number = number, supplierId = supplierId, subtotalPaisa = subtotal, discountPaisa = p.discountPaisa,
                    totalPaisa = total, paidPaisa = minOf(p.paidPaisa, total), method = p.method, staffId = p.staffId,
                )
            )
            db.purchases().insertItems(p.lines.map {
                PurchaseItemEntity(
                    purchaseId = id, productId = it.product.id, name = it.product.name,
                    qtyMilli = it.qtyMilli, unitCostPaisa = it.unitCostPaisa, lineTotalPaisa = it.totalPaisa,
                )
            })
            for (l in p.lines) if (l.batchNo.isNotBlank() || l.expiryDay != null) {
                db.batches().insert(StockBatchEntity(productId = l.product.id, batchNo = l.batchNo.trim(), expiryDay = l.expiryDay, qtyMilli = l.qtyMilli, purchaseId = id))
            }
            audit("PURCHASE", "$number · ${tk(total)}")
            PurchaseResult.Saved(number, total)
        }
    }

    /** Pays a supplier against what the shop owes them, capped at the due. Returns the voucher number. */
    suspend fun paySupplier(supplierId: Long, amountPaisa: Long, method: PayMethod, note: String, staffId: Long?): String? {
        val code = settings.current().shopCode
        return db.withTransaction {
            val s = db.suppliers().get(supplierId) ?: return@withTransaction null
            val amount = minOf(amountPaisa, s.duePaisa)
            if (amount <= 0) return@withTransaction null
            val number = nextNumber(code, DocType.SUPPLIER_PAYMENT)
            db.suppliers().insertPayment(
                SupplierPaymentEntity(number = number, supplierId = supplierId, amountPaisa = amount, method = method, note = note.trim(), staffId = staffId)
            )
            db.suppliers().addDue(supplierId, -amount)
            number
        }
    }

    suspend fun addExpense(category: String, amountPaisa: Long, method: PayMethod, note: String, staffId: Long?): String {
        val code = settings.current().shopCode
        return db.withTransaction {
            val number = nextNumber(code, DocType.EXPENSE)
            db.expenses().insert(
                ExpenseEntity(number = number, category = category.trim(), amountPaisa = amountPaisa, method = method, note = note.trim(), staffId = staffId)
            )
            audit("EXPENSE", "$number · ${category.trim()} · ${tk(amountPaisa)}")
            number
        }
    }

    val promos get() = db.promos()
    val batches get() = db.batches()
    val stockTools get() = db.stockTools()

    /** Repack or make: [inputs] leave the shop's stock and [outQty] of [out] arrives carrying their cost. Null = saved; else the short product. */
    suspend fun repack(inputs: List<Pair<ProductEntity, Long>>, out: ProductEntity, outQty: Long, note: String, staffId: Long?): String? {
        if (outQty <= 0 || inputs.isEmpty() || inputs.any { it.second <= 0 || it.first.id == out.id }) return ""
        val code = settings.current().shopCode
        return try {
            db.withTransaction {
                var cost = 0L
                val rows = inputs.map { (p, q) ->
                    if (db.products().takeStock(p.id, q) == 0) throw NotEnough(p.name)
                    takeFromBatches(p.id, q)
                    val fresh = db.products().get(p.id) ?: p
                    cost += (fresh.costPaisa * q + 500) / 1000
                    StockJobInputEntity(jobId = 0, productId = p.id, name = p.name, qtyMilli = q, unitCostPaisa = fresh.costPaisa)
                }
                db.stockTools().produce(out.id, outQty, cost * 1000 / outQty)
                val number = nextNumber(code, DocType.REPACK)
                val jobId = db.stockTools().insertJob(
                    StockJobEntity(number = number, outProductId = out.id, outName = out.name, outQtyMilli = outQty, costPaisa = cost, note = note.trim(), staffId = staffId)
                )
                db.stockTools().insertJobInputs(rows.map { it.copy(jobId = jobId) })
                audit("REPACK", "$number · ${out.name} +${Qty.text(outQty)}")
                null
            }
        } catch (e: NotEnough) {
            e.productName
        }
    }

    /** Records serial / IMEI numbers for stock of a product. Returns how many were new. */
    suspend fun addSerials(productId: Long, serials: List<String>): Int {
        val clean = serials.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
        if (clean.isEmpty()) return 0
        return db.stockTools().insertSerials(clean.map { ProductSerialEntity(productId = productId, serial = it) }).count { it > 0 }
    }

    /** A scanned code that is an unsold serial: its product. */
    suspend fun productForSerial(code: String): ProductEntity? =
        db.stockTools().inStock(code.trim())?.let { db.products().get(it.productId) }

    /** Moves stock between the shop and a warehouse. False when there isn't that much where it's coming from. */
    suspend fun moveStock(warehouseId: Long, productId: Long, qtyMilli: Long, intoWarehouse: Boolean): Boolean {
        if (qtyMilli <= 0) return false
        return db.withTransaction {
            val p = db.products().get(productId) ?: return@withTransaction false
            val held = db.stockTools().heldIn(warehouseId, productId)
            if (intoWarehouse) {
                if (p.stockMilli - db.stockTools().heldAll(productId) < qtyMilli) return@withTransaction false
                db.stockTools().setHeld(WarehouseStockEntity(warehouseId, productId, held + qtyMilli))
            } else {
                if (held < qtyMilli) return@withTransaction false
                db.stockTools().setHeld(WarehouseStockEntity(warehouseId, productId, held - qtyMilli))
            }
            audit("STOCK_MOVE", "${p.name} · ${Qty.text(qtyMilli)} " + if (intoWarehouse) "→ #$warehouseId" else "← #$warehouseId")
            true
        }
    }
    val tables get() = db.tables()

    /** Sells from the batch that expires first. Stock outside batches covers the rest. */
    private suspend fun takeFromBatches(productId: Long, qtyMilli: Long) {
        var left = qtyMilli
        for (b in db.batches().sellable(productId, LocalDate.now().toEpochDay())) {
            if (left <= 0) break
            val take = minOf(left, b.qtyMilli)
            db.batches().take(b.id, take)
            left -= take
        }
    }

    /** Labels stock already on hand with a batch and expiry. It does not change the stock count. */
    suspend fun addBatch(productId: Long, batchNo: String, expiryDay: Long?, qtyMilli: Long): Boolean {
        if (qtyMilli <= 0 || (batchNo.isBlank() && expiryDay == null)) return false
        db.batches().insert(StockBatchEntity(productId = productId, batchNo = batchNo.trim(), expiryDay = expiryDay, qtyMilli = qtyMilli))
        return true
    }

    /** Throws away what is left of a batch (expired or damaged): it leaves the stock too. */
    suspend fun writeOffBatch(batchId: Long) = db.withTransaction {
        val b = db.batches().get(batchId) ?: return@withTransaction
        if (b.qtyMilli <= 0) return@withTransaction
        db.batches().take(b.id, b.qtyMilli)
        db.products().addStock(b.productId, -b.qtyMilli)
        audit("WRITE_OFF", "${db.products().get(b.productId)?.name.orEmpty()} · ${b.batchNo} · ${Qty.text(b.qtyMilli)}")
    }

    /** Adds one of a product to a table's open order, opening one if needed. */
    suspend fun addToTable(tableId: Long, p: ProductEntity) = db.withTransaction {
        val orderId = db.tables().openOrder(tableId)?.id ?: db.tables().insertOrder(TableOrderEntity(tableId = tableId))
        val same = db.tables().itemsNow(orderId).firstOrNull { it.productId == p.id && it.unitPricePaisa == p.pricePaisa }
        if (same != null) db.tables().setQty(same.id, same.qtyMilli + 1000)
        else db.tables().insertItem(TableOrderItemEntity(orderId = orderId, productId = p.id, name = p.name, unitPricePaisa = p.pricePaisa, qtyMilli = 1000))
    }

    /** Lines not yet sent to the kitchen, with a new KOT number, then marks them sent. Null when nothing is new. */
    suspend fun sendKot(orderId: Long): Pair<String, List<Pair<String, Long>>>? {
        val code = settings.current().shopCode
        return db.withTransaction {
            val fresh = db.tables().itemsNow(orderId).filter { it.qtyMilli > it.sentMilli }.map { it.name to (it.qtyMilli - it.sentMilli) }
            if (fresh.isEmpty()) return@withTransaction null
            val number = nextNumber(code, DocType.KOT)
            db.tables().markSent(orderId)
            number to fresh
        }
    }

    /** The table's order as a cart for the sale screen. Products that were deleted are left out. */
    suspend fun tableToSale(orderId: Long, tableName: String): PendingDoc.ForSale? {
        val lines = db.tables().itemsNow(orderId).mapNotNull { i ->
            db.products().get(i.productId)?.let { CartLine(it.copy(pricePaisa = i.unitPricePaisa), i.qtyMilli) }
        }
        if (lines.isEmpty()) return null
        return PendingDoc.ForSale(0, lines, "", "", 0, tableOrderId = orderId)
    }

    suspend fun billTable(orderId: Long, saleNumber: String) = db.tables().bill(orderId, saleNumber)
    val hr get() = db.hr()

    /** Pays salary (or an advance) for [month] (yyyyMM). It is also saved as an expense, so cash and profit include it. */
    suspend fun paySalary(employeeId: Long, month: Int, amountPaisa: Long, method: PayMethod, category: String, note: String, staffId: Long?): String? {
        if (amountPaisa <= 0) return null
        val code = settings.current().shopCode
        return db.withTransaction {
            val number = nextNumber(code, DocType.EXPENSE)
            val expenseId = db.expenses().insert(
                ExpenseEntity(number = number, category = category.trim(), amountPaisa = amountPaisa, method = method, note = note.trim(), staffId = staffId)
            )
            db.hr().insertPayment(SalaryPaymentEntity(employeeId = employeeId, month = month, amountPaisa = amountPaisa, expenseId = expenseId))
            audit("SALARY", "$number · ${note.trim()} · ${tk(amountPaisa)}")
            number
        }
    }

    /** Saves an offer, coupon or voucher. Returns false when the code is empty or already used by another one. */
    suspend fun savePromo(p: PromoEntity, soldFor: PayMethod? = null, staffId: Long? = null): Boolean {
        val code = p.code.trim().uppercase().filter { !it.isWhitespace() }
        if (p.kind != PromoKind.OFFER && (code.isEmpty() || db.promos().codeTaken(code, p.id) > 0)) return false
        if (p.percentBp == 0 && p.fixedPaisa <= 0 && p.balancePaisa <= 0) return false
        db.withTransaction {
            db.promos().insert(p.copy(code = code, name = p.name.trim().ifEmpty { code }, category = p.category.trim()))
            // A gift voucher sold for money: the money comes in now, the goods go out later at no charge.
            if (p.kind == PromoKind.VOUCHER && soldFor != null) {
                moveMoney(MoveKind.VOUCHER_SOLD, null, soldFor, p.balancePaisa, code, staffId)
            }
        }
        return true
    }

    /** Looks up a coupon or voucher typed at the sale; null when unknown, off, out of date or used up. */
    suspend fun findCode(raw: String, now: Long = System.currentTimeMillis()): PromoEntity? {
        val code = raw.trim().uppercase().filter { !it.isWhitespace() }
        if (code.isEmpty()) return null
        val p = db.promos().byCode(PromoKind.COUPON, code) ?: db.promos().byCode(PromoKind.VOUCHER, code) ?: return null
        if (p.startAt != null && p.startAt > now) return null
        if (p.endAt != null && p.endAt < now) return null
        if (p.kind == PromoKind.COUPON && p.maxUses > 0 && p.uses >= p.maxUses) return null
        if (p.kind == PromoKind.VOUCHER && p.balancePaisa <= 0) return null
        return p
    }

    /** Records a money movement that isn't a sale or purchase. Returns its number. */
    suspend fun moveMoney(kind: MoveKind, from: PayMethod?, to: PayMethod?, amountPaisa: Long, note: String, staffId: Long?, loanId: Long? = null): String? {
        if (amountPaisa <= 0 || (from == null && to == null) || from == to) return null
        val code = settings.current().shopCode
        return db.withTransaction {
            val number = nextNumber(code, DocType.MONEY)
            db.money().insertMove(
                MoneyMoveEntity(number = number, kind = kind.name, fromMethod = from?.name, toMethod = to?.name,
                    amountPaisa = amountPaisa, loanId = loanId, note = note.trim(), staffId = staffId)
            )
            audit("MONEY", "$number · ${kind.name} · ${tk(amountPaisa)}")
            number
        }
    }

    /** A new loan: borrowed money comes in through [method], lent money goes out through it. */
    suspend fun newLoan(given: Boolean, party: String, phone: String, amountPaisa: Long, method: PayMethod, note: String, staffId: Long?): Long? {
        if (amountPaisa <= 0 || party.isBlank()) return null
        return db.withTransaction {
            val id = db.money().insertLoan(LoanEntity(party = party.trim(), phone = phone.filter(Char::isDigit), given = given,
                principalPaisa = amountPaisa, outstandingPaisa = amountPaisa, note = note.trim()))
            if (given) moveMoney(MoveKind.LOAN_GIVEN, method, null, amountPaisa, note, staffId, id)
            else moveMoney(MoveKind.LOAN_TAKEN, null, method, amountPaisa, note, staffId, id)
            id
        }
    }

    /** A payment against a loan, capped at what is still outstanding. */
    suspend fun loanPayment(loanId: Long, amountPaisa: Long, method: PayMethod, note: String, staffId: Long?): String? =
        db.withTransaction {
            val l = db.money().loan(loanId) ?: return@withTransaction null
            val amount = minOf(amountPaisa, l.outstandingPaisa)
            if (amount <= 0) return@withTransaction null
            val n = if (l.given) moveMoney(MoveKind.LOAN_RECEIVED, null, method, amount, note, staffId, loanId)
            else moveMoney(MoveKind.LOAN_REPAID, method, null, amount, note, staffId, loanId)
            db.money().addOutstanding(loanId, -amount)
            n
        }

    suspend fun openDay(countedPaisa: Long, staffId: Long?): Long =
        db.money().insertSession(CashSessionEntity(openingPaisa = countedPaisa, staffId = staffId))

    /** Closes the open day: expected cash = counted at opening + every cash flow since. */
    suspend fun closeDay(countedPaisa: Long, note: String): CashSessionEntity? = db.withTransaction {
        val s = db.money().currentSession() ?: return@withTransaction null
        val expected = s.openingPaisa + db.money().cashFlowSince(s.openedAt)
        val closed = s.copy(closedAt = System.currentTimeMillis(), expectedPaisa = expected, countedPaisa = countedPaisa, note = note.trim())
        db.money().updateSession(closed)
        audit("DAY_CLOSE", "${tk(expected)} / ${tk(countedPaisa)}")
        db.audit().trim(System.currentTimeMillis() - 400L * 24 * 60 * 60 * 1000)
        closed
    }

    /** Saves a new quote / order, or edits an open one. Returns its id and number. */
    suspend fun saveOrder(o: NewOrder): Pair<Long, String>? {
        val code = settings.current().shopCode
        val subtotal = o.lines.sumOf { it.totalPaisa }
        val discount = o.discountPaisa.coerceIn(0, subtotal)
        return db.withTransaction {
            val existing = if (o.id > 0) db.orders().get(o.id)?.takeIf { it.status == OrderStatus.OPEN.name } ?: return@withTransaction null else null
            val doc = OrderDocEntity(
                id = existing?.id ?: 0, number = existing?.number ?: nextNumber(code, docTypeOf(o.kind)), kind = o.kind.name,
                partyName = o.partyName.trim(), partyPhone = o.partyPhone.filter(Char::isDigit), supplierId = o.supplierId,
                subtotalPaisa = subtotal, discountPaisa = discount, totalPaisa = subtotal - discount, note = o.note.trim(),
                staffId = existing?.staffId ?: o.staffId, createdAt = existing?.createdAt ?: System.currentTimeMillis(),
            )
            val id = if (existing != null) { db.orders().update(doc); db.orders().clearItems(doc.id); doc.id } else db.orders().insert(doc)
            db.orders().insertItems(o.lines.map {
                OrderDocItemEntity(docId = id, productId = it.product.id, name = it.product.name, qtyMilli = it.qtyMilli,
                    unitPricePaisa = it.unitPricePaisa, lineTotalPaisa = it.totalPaisa)
            })
            id to doc.number
        }
    }

    private fun docTypeOf(k: OrderKind) = when (k) {
        OrderKind.QUOTE -> DocType.QUOTE
        OrderKind.SALES_ORDER -> DocType.SALES_ORDER
        OrderKind.PURCHASE_ORDER -> DocType.PURCHASE_ORDER
    }

    /** Copies a quote into a new sales order. Returns the new order's id. */
    suspend fun quoteToOrder(quoteId: Long, staffId: Long?): Long? {
        val q = db.orders().get(quoteId)?.takeIf { it.status == OrderStatus.OPEN.name } ?: return null
        val lines = db.orders().items(quoteId).mapNotNull { i -> db.products().get(i.productId)?.let { OrderLine(it, i.qtyMilli, i.unitPricePaisa) } }
        val (id, number) = saveOrder(NewOrder(OrderKind.SALES_ORDER, 0, lines, q.discountPaisa, q.partyName, q.partyPhone, null, q.note, staffId)) ?: return null
        db.orders().setStatus(quoteId, OrderStatus.DONE.name, number)
        return id
    }

    suspend fun finishOrder(id: Long, doneAs: String) = db.orders().setStatus(id, OrderStatus.DONE.name, doneAs)

    suspend fun cancelOrder(id: Long) = db.orders().setStatus(id, OrderStatus.CANCELLED.name, "")

    /** Customer returns goods from a sale: stock back, then off their due first, the rest paid back by [method]. */
    suspend fun returnSale(saleId: Long, picks: List<ReturnPick>, method: PayMethod, note: String, staffId: Long?): ReturnResult {
        val code = settings.current().shopCode
        return db.withTransaction {
            val sale = db.sales().get(saleId) ?: return@withTransaction ReturnResult.Invalid
            val left = db.returns().saleLines(saleId).associateBy { it.id }
            val ok = picks.filter { it.qtyMilli > 0 }
            if (ok.isEmpty() || ok.any { it.qtyMilli > (left[it.line.id]?.leftMilli ?: 0) }) return@withTransaction ReturnResult.Invalid
            val fresh = ok.map { it.copy(line = left.getValue(it.line.id)) }
            val total = Returns.value(fresh, sale.subtotalPaisa, sale.totalPaisa)
            val due = sale.customerId?.let { db.customers().get(it)?.duePaisa } ?: 0
            val split = Returns.split(total, due)
            for (p in fresh) db.products().addStock(p.line.productId, p.qtyMilli)
            if (split.dueCutPaisa > 0 && sale.customerId != null) db.customers().addDue(sale.customerId, -split.dueCutPaisa)
            val number = nextNumber(code, DocType.RETURN)
            val id = db.returns().insertSaleReturn(
                SaleReturnEntity(
                    number = number, saleId = saleId, customerId = sale.customerId, totalPaisa = total,
                    dueCutPaisa = split.dueCutPaisa, refundPaisa = split.refundPaisa, method = method,
                    costPaisa = Returns.cost(fresh), note = note.trim(), staffId = staffId,
                )
            )
            db.returns().insertSaleReturnItems(fresh.map {
                SaleReturnItemEntity(
                    returnId = id, saleItemId = it.line.id, productId = it.line.productId, name = it.line.name, qtyMilli = it.qtyMilli,
                    lineTotalPaisa = Returns.value(listOf(it), sale.subtotalPaisa, sale.totalPaisa), unitCostPaisa = it.line.unitCostPaisa,
                )
            })
            audit("SALE_RETURN", "$number · ${tk(split.totalPaisa)}")
            ReturnResult.Saved(number, split)
        }
    }

    /** Shop sends goods back to the supplier: stock down, then off what the shop owes first, the rest paid back to the shop. */
    suspend fun returnPurchase(purchaseId: Long, picks: List<ReturnPick>, method: PayMethod, note: String, staffId: Long?): ReturnResult {
        val code = settings.current().shopCode
        return try {
            db.withTransaction {
                val pur = db.purchases().get(purchaseId) ?: return@withTransaction ReturnResult.Invalid
                val left = db.returns().purchaseLines(purchaseId).associateBy { it.id }
                val ok = picks.filter { it.qtyMilli > 0 }
                if (ok.isEmpty() || ok.any { it.qtyMilli > (left[it.line.id]?.leftMilli ?: 0) }) return@withTransaction ReturnResult.Invalid
                val fresh = ok.map { it.copy(line = left.getValue(it.line.id)) }
                for (p in fresh) if (db.products().takeStock(p.line.productId, p.qtyMilli) == 0) throw NotEnough(p.line.name)
                val total = Returns.value(fresh, pur.subtotalPaisa, pur.totalPaisa)
                val due = pur.supplierId?.let { db.suppliers().get(it)?.duePaisa } ?: 0
                val split = Returns.split(total, due)
                if (split.dueCutPaisa > 0 && pur.supplierId != null) db.suppliers().addDue(pur.supplierId, -split.dueCutPaisa)
                val number = nextNumber(code, DocType.PURCHASE_RETURN)
                val id = db.returns().insertPurchaseReturn(
                    PurchaseReturnEntity(
                        number = number, purchaseId = purchaseId, supplierId = pur.supplierId, totalPaisa = total,
                        dueCutPaisa = split.dueCutPaisa, refundPaisa = split.refundPaisa, method = method,
                        note = note.trim(), staffId = staffId,
                    )
                )
                db.returns().insertPurchaseReturnItems(fresh.map {
                    PurchaseReturnItemEntity(
                        returnId = id, purchaseItemId = it.line.id, productId = it.line.productId, name = it.line.name,
                        qtyMilli = it.qtyMilli, lineTotalPaisa = Returns.value(listOf(it), pur.subtotalPaisa, pur.totalPaisa),
                    )
                })
                audit("PURCHASE_RETURN", "$number · ${tk(split.totalPaisa)}")
                ReturnResult.Saved(number, split)
            }
        } catch (e: NotEnough) {
            ReturnResult.NotEnoughStock(e.productName)
        }
    }

    /** Next document number of [type] for this year. Call inside a transaction. */
    private suspend fun nextNumber(code: String, type: DocType): String {
        val year = LocalDate.now().year
        val serial = DocNumber.next(db.docCounters().last(type.name, year))
        db.docCounters().set(DocCounterEntity(type.name, year, serial))
        return DocNumber.format(code, type, year, serial)
    }

    suspend fun addStaff(name: String, role: String, pin: String, menus: Set<StaffMenu>): Long {
        val salt = PinHasher.newSalt()
        return db.staff().insert(
            StaffEntity(
                name = name.trim(), role = role.trim(), pinHash = PinHasher.hash(pin, salt), pinSalt = salt,
                menus = menus.joinToString(",") { it.name },
            )
        )
    }

    /** The number the next sale will get, shown in the Sale header. */
    fun nextSaleNumber(shopCode: String) = db.docCounters().lastFlow(DocType.SALE.name, LocalDate.now().year)
        .map { DocNumber.format(shopCode, DocType.SALE, LocalDate.now().year, DocNumber.next(it)) }

    suspend fun checkStaffPin(id: Long, pin: String): Boolean {
        val s = db.staff().get(id) ?: return false
        return s.active && PinHasher.matches(pin, s.pinSalt, s.pinHash)
    }

    private class NotEnough(val productName: String) : RuntimeException()
    private class PromoGone(val code: String) : RuntimeException()
}

fun StaffEntity.menuSet(): Set<StaffMenu> =
    menus.split(",").mapNotNull { runCatching { StaffMenu.valueOf(it) }.getOrNull() }.toSet()

/** A sale put on hold with the Hold button, kept until the app is closed. */
object HeldSale {
    var lines: List<CartLine> = emptyList()
}
