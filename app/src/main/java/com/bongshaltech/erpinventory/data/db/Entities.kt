package com.bongshaltech.erpinventory.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

// Money is stored as paisa (Long) everywhere so totals never drift. Stock is stored in thousandths so 0.25 kg works.

@Entity(tableName = "product", indices = [Index("barcode"), Index("name")])
data class ProductEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val barcode: String? = null,
    val unit: String = "pcs",
    val costPaisa: Long = 0,
    val pricePaisa: Long,
    /** Stock × 1000. */
    val stockMilli: Long = 0,
    val lowStockMilli: Long = 0,
    val active: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "customer", indices = [Index(value = ["phone"], unique = true)])
data class CustomerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String,
    val duePaisa: Long = 0,
    val createdAt: Long = System.currentTimeMillis(),
)

enum class PayMethod { CASH, BKASH, NAGAD, CARD }

@Entity(
    tableName = "sale",
    indices = [Index(value = ["number"], unique = true), Index("createdAt"), Index("customerId")],
)
data class SaleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val number: String,
    val customerId: Long? = null,
    val subtotalPaisa: Long,
    val discountPaisa: Long,
    val totalPaisa: Long,
    val paidPaisa: Long,
    val method: PayMethod,
    val smsRequested: Boolean,
    /** Staff who made the sale; null means the owner. */
    val staffId: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "sale_item",
    foreignKeys = [ForeignKey(entity = SaleEntity::class, parentColumns = ["id"], childColumns = ["saleId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("saleId"), Index("productId")],
)
data class SaleItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val saleId: Long,
    val productId: Long,
    /** Name and price are copied so old memos stay correct after a product is edited. */
    val name: String,
    val qtyMilli: Long,
    val unitPricePaisa: Long,
    val lineTotalPaisa: Long,
)

@Entity(tableName = "staff")
data class StaffEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val pinHash: String,
    val pinSalt: String,
    /** Comma separated [com.bongshaltech.erpinventory.data.StaffMenu] names. */
    val menus: String,
    val active: Boolean = true,
)

/** Last serial used per document type per year, so numbers restart every year and never repeat. */
@Entity(tableName = "doc_counter", primaryKeys = ["type", "year"])
data class DocCounterEntity(val type: String, val year: Int, val last: Int)
