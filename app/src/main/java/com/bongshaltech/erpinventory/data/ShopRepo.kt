package com.bongshaltech.erpinventory.data

import androidx.room.withTransaction
import com.bongshaltech.erpinventory.core.DocNumber
import com.bongshaltech.erpinventory.core.DocType
import com.bongshaltech.erpinventory.data.db.AppDatabase
import com.bongshaltech.erpinventory.data.db.CustomerEntity
import com.bongshaltech.erpinventory.data.db.DocCounterEntity
import com.bongshaltech.erpinventory.data.db.PayMethod
import com.bongshaltech.erpinventory.data.db.ProductEntity
import com.bongshaltech.erpinventory.data.db.SaleEntity
import com.bongshaltech.erpinventory.data.db.SaleItemEntity
import com.bongshaltech.erpinventory.data.db.StaffEntity
import com.bongshaltech.erpinventory.security.PinHasher
import java.time.LocalDate

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
                    )
                })
                SaleResult.Saved(number, total)
            }
        } catch (e: NotEnough) {
            SaleResult.NotEnoughStock(e.productName)
        }
    }

    suspend fun addStaff(name: String, pin: String, menus: Set<StaffMenu>): Long {
        val salt = PinHasher.newSalt()
        return db.staff().insert(
            StaffEntity(name = name.trim(), pinHash = PinHasher.hash(pin, salt), pinSalt = salt, menus = menus.joinToString(",") { it.name })
        )
    }

    suspend fun checkStaffPin(id: Long, pin: String): Boolean {
        val s = db.staff().get(id) ?: return false
        return s.active && PinHasher.matches(pin, s.pinSalt, s.pinHash)
    }

    private class NotEnough(val productName: String) : RuntimeException()
}

fun StaffEntity.menuSet(): Set<StaffMenu> =
    menus.split(",").mapNotNull { runCatching { StaffMenu.valueOf(it) }.getOrNull() }.toSet()
