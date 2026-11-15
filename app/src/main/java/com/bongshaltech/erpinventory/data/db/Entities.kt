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
    /** Generic (chemical) name, for pharmacies. */
    @ColumnInfo(defaultValue = "") val generic: String = "",
    val costPaisa: Long = 0,
    val pricePaisa: Long,
    /** Stock × 1000. */
    val stockMilli: Long = 0,
    val lowStockMilli: Long = 0,
    val active: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    /** Bought in packs: how many units one pack holds (× 1000), 0 when not. */
    @ColumnInfo(defaultValue = "0") val packMilli: Long = 0,
    /** What the pack is called: box, carton, strip. */
    @ColumnInfo(defaultValue = "") val packName: String = "",
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
    /** Most this customer may owe; 0 means no limit. */
    @ColumnInfo(defaultValue = "0") val creditLimitPaisa: Long = 0,
    /** Area or route, for grouping dues. */
    @ColumnInfo(defaultValue = "") val area: String = "",
    /** Loyalty points to spend. */
    @ColumnInfo(defaultValue = "0") val points: Long = 0,
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
    /** Loyalty points this sale gave and spent; kept so a delete can undo them. */
    @ColumnInfo(defaultValue = "0") val pointsEarned: Long = 0,
    @ColumnInfo(defaultValue = "0") val pointsUsed: Long = 0,
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
enum class MoveKind { OPENING, TRANSFER, CAPITAL_IN, DRAWING, LOAN_TAKEN, LOAN_REPAID, LOAN_GIVEN, LOAN_RECEIVED, VOUCHER_SOLD }

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

/** Offers apply by themselves; coupons and gift vouchers are typed in as a code at the sale. */
enum class PromoKind { OFFER, COUPON, VOUCHER }

@Entity(tableName = "promo", indices = [Index("kind"), Index("code")])
data class PromoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val kind: PromoKind,
    val name: String,
    /** Upper-case code for a coupon or voucher; empty for an offer. */
    val code: String = "",
    /** Basis points (1000 = 10%); 0 means [fixedPaisa] is used. */
    val percentBp: Int = 0,
    val fixedPaisa: Long = 0,
    /** Offer on one product, or a category, or (both empty) everything. */
    val productId: Long? = null,
    val category: String = "",
    /** A coupon only works on a bill of at least this much. */
    val minPaisa: Long = 0,
    /** What is left on a gift voucher. */
    val balancePaisa: Long = 0,
    val startAt: Long? = null,
    val endAt: Long? = null,
    /** 0 = no limit. */
    val maxUses: Int = 0,
    val uses: Int = 0,
    val active: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
)

/** Which offer, coupon or voucher took how much off a sale. */
@Entity(
    tableName = "sale_promo",
    foreignKeys = [ForeignKey(entity = SaleEntity::class, parentColumns = ["id"], childColumns = ["saleId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("saleId"), Index("promoId")],
)
data class SalePromoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val saleId: Long,
    val promoId: Long?,
    val kind: PromoKind,
    val code: String,
    val amountPaisa: Long,
)

/** Someone the shop pays a salary to. They don't need an app login (that's Staff). */
@Entity(tableName = "employee")
data class EmployeeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String = "",
    val role: String = "",
    val salaryPaisa: Long = 0,
    val active: Boolean = true,
    val joinedAt: Long = System.currentTimeMillis(),
)

enum class AttendanceStatus { PRESENT, ABSENT, LEAVE, HALF }

/** One mark per employee per day; [day] is yyyyMMdd. */
@Entity(tableName = "attendance", indices = [Index(value = ["employeeId", "day"], unique = true), Index("day")])
data class AttendanceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val employeeId: Long,
    val day: Int,
    val status: AttendanceStatus,
)

/** A salary (or advance) paid for [month] (yyyyMM). The money itself is the linked expense. */
@Entity(tableName = "salary_payment", indices = [Index("employeeId"), Index("month")])
data class SalaryPaymentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val employeeId: Long,
    val month: Int,
    val amountPaisa: Long,
    val expenseId: Long,
    val createdAt: Long = System.currentTimeMillis(),
)

/** Part of a product's stock that came in together: a batch number and its expiry. Stock outside batches has none. */
@Entity(tableName = "stock_batch", indices = [Index("productId"), Index("expiryDay")])
data class StockBatchEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val productId: Long,
    val batchNo: String,
    /** Expiry as epoch day, or null. */
    val expiryDay: Long?,
    val qtyMilli: Long,
    val purchaseId: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "dining_table")
data class DiningTableEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val active: Boolean = true,
)

/** What a table has ordered until it is billed. */
@Entity(tableName = "table_order", indices = [Index("tableId"), Index("status")])
data class TableOrderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tableId: Long,
    /** OPEN or BILLED. */
    val status: String = "OPEN",
    val saleNumber: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "table_order_item",
    foreignKeys = [ForeignKey(entity = TableOrderEntity::class, parentColumns = ["id"], childColumns = ["orderId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("orderId")],
)
data class TableOrderItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val orderId: Long,
    val productId: Long,
    val name: String,
    val unitPricePaisa: Long,
    val qtyMilli: Long,
    /** How much of [qtyMilli] has already gone to the kitchen on a KOT. */
    val sentMilli: Long = 0,
)

/** A repack or small production run: some products go in, one product comes out. */
@Entity(tableName = "stock_job", indices = [Index("createdAt")])
data class StockJobEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val number: String,
    val outProductId: Long,
    val outName: String,
    val outQtyMilli: Long,
    /** Cost of everything that went in, now carried by the output. */
    val costPaisa: Long,
    val note: String = "",
    val staffId: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "stock_job_input",
    foreignKeys = [ForeignKey(entity = StockJobEntity::class, parentColumns = ["id"], childColumns = ["jobId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("jobId")],
)
data class StockJobInputEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val jobId: Long,
    val productId: Long,
    val name: String,
    val qtyMilli: Long,
    val unitCostPaisa: Long,
)

/** One serial / IMEI number of a product. [saleId] is set once it is sold. */
@Entity(tableName = "product_serial", indices = [Index(value = ["serial"], unique = true), Index("productId"), Index("saleId")])
data class ProductSerialEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val productId: Long,
    val serial: String,
    val saleId: Long? = null,
    val soldAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
)

/** A store room or second branch. Stock not in any warehouse is in the shop, where sales take from. */
@Entity(tableName = "warehouse")
data class WarehouseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val active: Boolean = true,
)

@Entity(tableName = "warehouse_stock", primaryKeys = ["warehouseId", "productId"], indices = [Index("productId")])
data class WarehouseStockEntity(
    val warehouseId: Long,
    val productId: Long,
    val qtyMilli: Long,
)

/** Who did what and when, for the owner to check. [staffId] null means the owner. */
@Entity(tableName = "audit_log", indices = [Index("at"), Index("action")])
data class AuditLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val at: Long = System.currentTimeMillis(),
    val staffId: Long?,
    val action: String,
    val detail: String,
)

/** A sale the owner deleted. The sale itself is gone from every total; this keeps what it was and why. */
@Entity(tableName = "deleted_sale", indices = [Index("deletedAt")])
data class DeletedSaleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val saleId: Long,
    val number: String,
    val customerId: Long?,
    val totalPaisa: Long,
    val paidPaisa: Long,
    val method: PayMethod,
    val saleAt: Long,
    /** One line per item: name, quantity and line total, as it was on the memo. */
    val itemsText: String,
    val reason: String,
    /** Staff who asked for the delete; null when the owner deleted it directly. */
    val requestedBy: Long?,
    val deletedAt: Long = System.currentTimeMillis(),
)

/** A staff member asks the owner to delete a sale. Nothing changes until the owner approves. */
@Entity(tableName = "sale_delete_request", indices = [Index(value = ["saleId"], unique = true)])
data class SaleDeleteRequestEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val saleId: Long,
    val number: String,
    val reason: String,
    val staffId: Long?,
    val createdAt: Long = System.currentTimeMillis(),
)

/** One planned payment of a sale sold on instalments. Due payments from the customer fill the oldest first. */
@Entity(tableName = "installment", indices = [Index("customerId"), Index("saleNumber"), Index("dueDay")])
data class InstallmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val customerId: Long,
    val saleNumber: String,
    /** 1 for the first payment, 2 for the second, and so on. */
    val seq: Int,
    /** Day the payment is due, as days since 1970-01-01. */
    val dueDay: Long,
    val amountPaisa: Long,
    val paidPaisa: Long = 0,
)

/** A stock correction with a reason: damaged, lost, found, counted. [qtyMilli] is + or −. */
@Entity(tableName = "stock_adjust", indices = [Index("productId"), Index("createdAt")])
data class StockAdjustEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val productId: Long,
    val qtyMilli: Long,
    val reason: String,
    val note: String,
    /** Cost of the change at the product's buying price, + or −. */
    val costPaisa: Long,
    val staffId: Long?,
    val createdAt: Long = System.currentTimeMillis(),
)
