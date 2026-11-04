package com.bongshaltech.erpinventory.data

import androidx.room.withTransaction
import com.bongshaltech.erpinventory.core.DocNumber
import com.bongshaltech.erpinventory.core.DocType
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
import com.bongshaltech.erpinventory.security.PinHasher
import java.time.LocalDate
import kotlinx.coroutines.flow.map

data class CartLine(val product: ProductEntity, val qtyMilli: Long) {
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
)

data class PurchaseLine(val product: ProductEntity, val qtyMilli: Long, val unitCostPaisa: Long) {
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
    data class Saved(val number: String, val totalPaisa: Long) : SaleResult
    data class NotEnoughStock(val productName: String) : SaleResult
    data object DueNeedsCustomer : SaleResult
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

    suspend fun saveProduct(p: ProductEntity) = db.products().upsert(p)

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
                SaleResult.Saved(number, total)
            }
        } catch (e: NotEnough) {
            SaleResult.NotEnoughStock(e.productName)
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
            number
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
}

fun StaffEntity.menuSet(): Set<StaffMenu> =
    menus.split(",").mapNotNull { runCatching { StaffMenu.valueOf(it) }.getOrNull() }.toSet()

/** A sale put on hold with the Hold button, kept until the app is closed. */
object HeldSale {
    var lines: List<CartLine> = emptyList()
}
