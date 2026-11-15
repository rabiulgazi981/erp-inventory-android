package com.bongshaltech.erpinventory.data.db

import androidx.room.ColumnInfo
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
    @ColumnInfo(defaultValue = "") val category: String = "",
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
    /** [CustomerType] name. */
    @ColumnInfo(defaultValue = "RETAIL") val type: String = CustomerType.RETAIL.name,
    @ColumnInfo(defaultValue = "") val address: String = "",
)

enum class CustomerType { RETAIL, WHOLESALE }

/** Money a customer paid against their due, outside a sale. */
@Entity(tableName = "due_payment", indices = [Index("customerId"), Index("createdAt")])
data class DuePaymentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val number: String,
    val customerId: Long,
    val amountPaisa: Long,
    val method: PayMethod,
    val note: String = "",
    val staffId: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
)

enum class PayMethod { CASH, BKASH, NAGAD, CARD, BANK }

/** Someone the shop buys from. [duePaisa] is what the shop still owes them. */
@Entity(tableName = "supplier", indices = [Index("phone"), Index("name")])
data class SupplierEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String = "",
    val address: String = "",
    val duePaisa: Long = 0,
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "purchase",
    indices = [Index(value = ["number"], unique = true), Index("createdAt"), Index("supplierId")],
)
data class PurchaseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val number: String,
    val supplierId: Long? = null,
    val subtotalPaisa: Long,
    val discountPaisa: Long,
    val totalPaisa: Long,
    val paidPaisa: Long,
    val method: PayMethod,
    val staffId: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "purchase_item",
    foreignKeys = [ForeignKey(entity = PurchaseEntity::class, parentColumns = ["id"], childColumns = ["purchaseId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("purchaseId"), Index("productId")],
)
data class PurchaseItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val purchaseId: Long,
    val productId: Long,
    val name: String,
    val qtyMilli: Long,
    val unitCostPaisa: Long,
    val lineTotalPaisa: Long,
)

/** Shop running costs: rent, bills, salary and so on. [category] is free text so shops can add their own. */
@Entity(tableName = "expense", indices = [Index("createdAt"), Index("category")])
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val number: String,
    val category: String,
    val amountPaisa: Long,
    val method: PayMethod,
    val note: String = "",
    val staffId: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
)

/** Money the shop paid a supplier against what it owes, outside a purchase. */
@Entity(tableName = "supplier_payment", indices = [Index("supplierId"), Index("createdAt")])
data class SupplierPaymentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val number: String,
    val supplierId: Long,
    val amountPaisa: Long,
    val method: PayMethod,
    val note: String = "",
    val staffId: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
)

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
    /** Buying price at the time of sale, for profit. 0 on sales made before version 6. */
    @ColumnInfo(defaultValue = "0") val unitCostPaisa: Long = 0,
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
    @ColumnInfo(defaultValue = "") val role: String = "",
)

/** Last serial used per document type per year, so numbers restart every year and never repeat. */
@Entity(tableName = "doc_counter", primaryKeys = ["type", "year"])
data class DocCounterEntity(val type: String, val year: Int, val last: Int)

/** One mobile banking wallet (bKash, Nagad...) or operator load balance (GP, Robi...) the shop runs. */
@Entity(tableName = "mfs_wallet", indices = [Index(value = ["provider"], unique = true)])
data class MfsWalletEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** [com.bongshaltech.erpinventory.data.MfsProvider] name. */
    val provider: String,
    /** Personal account instead of an agent account. */
    val personal: Boolean = false,
    val number: String = "",
    val balancePaisa: Long = 0,
    /** Commission the shop earns, in percent of the amount (0.4 = 0.4%). Set by the owner. */
    val cashInPct: Double = 0.0,
    val cashOutPct: Double = 0.0,
    val paymentPct: Double = 0.0,
    val loadPct: Double = 0.0,
    val active: Boolean = false,
)

/** One mobile banking or load entry and what it did to the wallet, the cash drawer and the shop's earning. */
@Entity(tableName = "mfs_txn", indices = [Index("walletId"), Index("createdAt")])
data class MfsTxnEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val number: String,
    val walletId: Long,
    /** [com.bongshaltech.erpinventory.core.mfs.MfsType] name. */
    val type: String,
    val customerPhone: String,
    val amountPaisa: Long,
    /** Service charge taken from the customer (send money, pay bill) or the settled amount on balance in/out. */
    val chargePaisa: Long,
    /** Fee the wallet takes (send money, pay bill). */
    val feePaisa: Long,
    val walletDeltaPaisa: Long,
    val cashDeltaPaisa: Long,
    val earningPaisa: Long,
    /** Balance in/out counterparty name, empty otherwise. */
    val via: String,
    val trxId: String,
    val note: String,
    val staffId: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
)

/**
 * Goods a customer brought back from [saleId]. Stock goes back up. [dueCutPaisa] came off what the
 * customer owed (a credit note); [refundPaisa] was paid back to them by [method].
 */
@Entity(tableName = "sale_return", indices = [Index(value = ["number"], unique = true), Index("saleId"), Index("customerId"), Index("createdAt")])
data class SaleReturnEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val number: String,
    val saleId: Long,
    val customerId: Long? = null,
    val totalPaisa: Long,
    val dueCutPaisa: Long,
    val refundPaisa: Long,
    val method: PayMethod,
    /** Buying cost of the goods that came back, taken off the cost of sales for profit. */
    val costPaisa: Long,
    val note: String = "",
    val staffId: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "sale_return_item",
    foreignKeys = [ForeignKey(entity = SaleReturnEntity::class, parentColumns = ["id"], childColumns = ["returnId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("returnId"), Index("saleItemId")],
)
data class SaleReturnItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val returnId: Long,
    val saleItemId: Long,
    val productId: Long,
    val name: String,
    val qtyMilli: Long,
    val lineTotalPaisa: Long,
    val unitCostPaisa: Long,
)

/**
 * Goods the shop sent back to the supplier from [purchaseId]. Stock goes down. [dueCutPaisa] came off
 * what the shop owed the supplier (a debit note); [refundPaisa] the supplier paid back by [method].
 */
@Entity(tableName = "purchase_return", indices = [Index(value = ["number"], unique = true), Index("purchaseId"), Index("supplierId"), Index("createdAt")])
data class PurchaseReturnEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val number: String,
    val purchaseId: Long,
    val supplierId: Long? = null,
    val totalPaisa: Long,
    val dueCutPaisa: Long,
    val refundPaisa: Long,
    val method: PayMethod,
    val note: String = "",
    val staffId: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "purchase_return_item",
    foreignKeys = [ForeignKey(entity = PurchaseReturnEntity::class, parentColumns = ["id"], childColumns = ["returnId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("returnId"), Index("purchaseItemId")],
)
data class PurchaseReturnItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val returnId: Long,
    val purchaseItemId: Long,
    val productId: Long,
    val name: String,
    val qtyMilli: Long,
    val lineTotalPaisa: Long,
)

/** Quotation, sales order or purchase order: a list of goods and prices with no effect on stock or money until it is turned into a sale or purchase. */
enum class OrderKind { QUOTE, SALES_ORDER, PURCHASE_ORDER }

enum class OrderStatus { OPEN, DONE, CANCELLED }

@Entity(tableName = "order_doc", indices = [Index(value = ["number"], unique = true), Index("kind"), Index("createdAt")])
data class OrderDocEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val number: String,
    /** [OrderKind] name. */
    val kind: String,
    /** [OrderStatus] name. */
    val status: String = OrderStatus.OPEN.name,
    val partyName: String = "",
    val partyPhone: String = "",
    val supplierId: Long? = null,
    val subtotalPaisa: Long,
    val discountPaisa: Long,
    val totalPaisa: Long,
    val note: String = "",
    /** Sale or purchase number it was turned into, empty while open. */
    val doneAs: String = "",
    val staffId: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "order_doc_item",
    foreignKeys = [ForeignKey(entity = OrderDocEntity::class, parentColumns = ["id"], childColumns = ["docId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("docId")],
)
data class OrderDocItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val docId: Long,
    val productId: Long,
    val name: String,
    val qtyMilli: Long,
    /** Selling price for quotes and sales orders, buying price for purchase orders. */
    val unitPricePaisa: Long,
    val lineTotalPaisa: Long,
)

/** Money moved without a sale or purchase: transfers between cash / bank / wallets, owner's money in or out, loans, opening balances. */
enum class MoveKind { OPENING, TRANSFER, CAPITAL_IN, DRAWING, LOAN_TAKEN, LOAN_REPAID, LOAN_GIVEN, LOAN_RECEIVED }

@Entity(tableName = "money_move", indices = [Index("createdAt"), Index("loanId")])
data class MoneyMoveEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val number: String,
    /** [MoveKind] name. */
    val kind: String,
    /** [PayMethod] name money left, or null. */
    val fromMethod: String? = null,
    /** [PayMethod] name money went into, or null. */
    val toMethod: String? = null,
    val amountPaisa: Long,
    val loanId: Long? = null,
    val note: String = "",
    val staffId: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
)

/** Money the shop borrowed ([given] false) or lent ([given] true). [outstandingPaisa] is what is still to be paid back. */
@Entity(tableName = "loan", indices = [Index("createdAt")])
data class LoanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val party: String,
    val phone: String = "",
    val given: Boolean,
    val principalPaisa: Long,
    val outstandingPaisa: Long,
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis(),
)

/** One shop day: cash counted at opening and at closing, and what the app expected. */
@Entity(tableName = "cash_session", indices = [Index("openedAt")])
data class CashSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val openedAt: Long = System.currentTimeMillis(),
    val openingPaisa: Long,
    val closedAt: Long? = null,
    val expectedPaisa: Long? = null,
    val countedPaisa: Long? = null,
    val note: String = "",
    val staffId: Long? = null,
)
