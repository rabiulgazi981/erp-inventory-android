package com.bongshaltech.erpinventory.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/**
 * Every movement of money, one row per payment method touched: number, kind, time, signed amount
 * (+ in, − out) and method. Cash & bank balances, the ledger and "cash today" all read this one list,
 * so they always agree.
 */
const val MONEY_FLOWS = """
    SELECT number, 'SALE' AS kind, createdAt, paidPaisa AS delta, method FROM sale WHERE paidPaisa > 0
    UNION ALL SELECT number, 'DUE_IN' AS kind, createdAt, amountPaisa AS delta, method FROM due_payment
    UNION ALL SELECT number, 'PURCHASE' AS kind, createdAt, -paidPaisa AS delta, method FROM purchase WHERE paidPaisa > 0
    UNION ALL SELECT number, 'SUPPLIER_PAY' AS kind, createdAt, -amountPaisa AS delta, method FROM supplier_payment
    UNION ALL SELECT number, 'EXPENSE' AS kind, createdAt, -amountPaisa AS delta, method FROM expense
    UNION ALL SELECT number, 'SALE_RETURN' AS kind, createdAt, -refundPaisa AS delta, method FROM sale_return WHERE refundPaisa > 0
    UNION ALL SELECT number, 'PURCHASE_RETURN' AS kind, createdAt, refundPaisa AS delta, method FROM purchase_return WHERE refundPaisa > 0
    UNION ALL SELECT number, 'MFS' AS kind, createdAt, cashDeltaPaisa AS delta, 'CASH' AS method FROM mfs_txn WHERE cashDeltaPaisa != 0
    UNION ALL SELECT number, kind, createdAt, -amountPaisa AS delta, fromMethod AS method FROM money_move WHERE fromMethod IS NOT NULL
    UNION ALL SELECT number, kind, createdAt, amountPaisa AS delta, toMethod AS method FROM money_move WHERE toMethod IS NOT NULL
"""

@Dao
interface ProductDao {
    @Query(
        "SELECT * FROM product WHERE active = 1 AND (:q = '' OR name LIKE '%' || :q || '%' OR barcode = :q " +
            "OR (generic != '' AND generic LIKE '%' || :q || '%')) ORDER BY name COLLATE NOCASE"
    )
    fun search(q: String): Flow<List<ProductEntity>>

    @Query("SELECT * FROM product WHERE id = :id")
    suspend fun get(id: Long): ProductEntity?

    @Query("SELECT * FROM product")
    suspend fun allNow(): List<ProductEntity>

    @Query("SELECT * FROM product WHERE active = 1 AND stockMilli <= lowStockMilli AND (:q = '' OR name LIKE '%' || :q || '%' OR barcode = :q) ORDER BY name COLLATE NOCASE")
    fun searchLow(q: String): Flow<List<ProductEntity>>

    @Query("SELECT COUNT(*) FROM product WHERE active = 1 AND stockMilli <= lowStockMilli")
    fun lowStockCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM product WHERE active = 1")
    fun count(): Flow<Int>

    @Query("SELECT * FROM product WHERE active = 1 AND barcode = :code LIMIT 1")
    suspend fun byBarcode(code: String): ProductEntity?

    @Upsert
    suspend fun upsert(p: ProductEntity): Long

    @Query("UPDATE product SET active = 0 WHERE id = :id")
    suspend fun archive(id: Long)

    /** Goods received: stock goes up and the cost becomes the latest buying price. */
    @Query("UPDATE product SET stockMilli = stockMilli + :qtyMilli, costPaisa = :costPaisa WHERE id = :id")
    suspend fun receive(id: Long, qtyMilli: Long, costPaisa: Long)

    /** Returns rows changed: 0 means there was not enough stock. */
    // Stock kept in a warehouse can't be sold from the shop until it is moved back.
    @Query(
        "UPDATE product SET stockMilli = stockMilli - :qtyMilli WHERE id = :id AND " +
            "stockMilli - (SELECT COALESCE(SUM(qtyMilli), 0) FROM warehouse_stock WHERE productId = :id) >= :qtyMilli"
    )
    suspend fun takeStock(id: Long, qtyMilli: Long): Int

    /** Goods coming back from a customer: stock up, cost left as it is. */
    @Query("UPDATE product SET stockMilli = stockMilli + :qtyMilli WHERE id = :id")
    suspend fun addStock(id: Long, qtyMilli: Long)
}

@Dao
interface CustomerDao {
    @Query("SELECT * FROM customer WHERE phone = :phone")
    suspend fun byPhone(phone: String): CustomerEntity?

    @Insert
    suspend fun insert(c: CustomerEntity): Long

    @Query("UPDATE customer SET duePaisa = duePaisa + :delta WHERE id = :id")
    suspend fun addDue(id: Long, delta: Long)

    @Query("SELECT COALESCE(SUM(duePaisa), 0) FROM customer")
    fun totalDue(): Flow<Long>

    @Query("SELECT COUNT(*) FROM customer WHERE duePaisa > 0")
    fun dueCount(): Flow<Int>

    /** [type] '' means every type. */
    @Query(
        """SELECT * FROM customer
           WHERE (:q = '' OR name LIKE '%' || :q || '%' OR phone LIKE '%' || :q || '%')
             AND (:type = '' OR type = :type) AND (:dueOnly = 0 OR duePaisa > 0)
           ORDER BY duePaisa > 0 DESC, name COLLATE NOCASE"""
    )
    fun search(q: String, type: String, dueOnly: Boolean): Flow<List<CustomerEntity>>

    @Query("SELECT COUNT(*) FROM customer")
    fun count(): Flow<Int>

    @Query("SELECT * FROM customer WHERE id = :id")
    fun watch(id: Long): Flow<CustomerEntity?>

    @Query("SELECT * FROM customer WHERE id = :id")
    suspend fun get(id: Long): CustomerEntity?

    @Update
    suspend fun update(c: CustomerEntity)

    @Insert
    suspend fun insertPayment(p: DuePaymentEntity): Long

    /** Sales and due payments for one customer, newest first. */
    @Query(
        """SELECT id, number, createdAt, totalPaisa AS amountPaisa, totalPaisa - paidPaisa AS duePaisa, 0 AS isPayment
           FROM sale WHERE customerId = :id
           UNION ALL
           SELECT id, number, createdAt, amountPaisa, 0 AS duePaisa, 1 AS isPayment
           FROM due_payment WHERE customerId = :id
           UNION ALL
           SELECT id, number, createdAt, dueCutPaisa AS amountPaisa, 0 AS duePaisa, 1 AS isPayment
           FROM sale_return WHERE customerId = :id AND dueCutPaisa > 0
           ORDER BY createdAt DESC LIMIT 100"""
    )
    fun history(id: Long): Flow<List<CustomerTxn>>
}

data class CustomerTxn(
    val id: Long,
    val number: String,
    val createdAt: Long,
    val amountPaisa: Long,
    val duePaisa: Long,
    val isPayment: Boolean,
)

data class SaleRow(
    val id: Long,
    val number: String,
    val totalPaisa: Long,
    val createdAt: Long,
    val customerName: String?,
    val duePaisa: Long,
    val items: Int,
)

@Dao
interface SaleDao {
    @Insert
    suspend fun insert(s: SaleEntity): Long

    @Insert
    suspend fun insertItems(items: List<SaleItemEntity>)

    @Query("SELECT * FROM sale WHERE id = :id")
    suspend fun get(id: Long): SaleEntity?

    @Query("SELECT * FROM sale_item WHERE saleId = :saleId ORDER BY id")
    suspend fun items(saleId: Long): List<SaleItemEntity>

    @Query("SELECT COALESCE(SUM(totalPaisa), 0) FROM sale WHERE createdAt >= :from")
    fun totalSince(from: Long): Flow<Long>

    @Query("SELECT COUNT(*) FROM sale WHERE createdAt >= :from")
    fun countSince(from: Long): Flow<Int>

    @Query("SELECT COALESCE(SUM(totalPaisa), 0) FROM sale WHERE createdAt >= :from AND createdAt < :to")
    fun totalBetween(from: Long, to: Long): Flow<Long>

    /** Net cash since [from]: every money flow that went into or out of the cash drawer. */
    @Query("SELECT COALESCE(SUM(delta), 0) FROM ($MONEY_FLOWS) WHERE method = 'CASH' AND createdAt >= :from")
    fun cashSince(from: Long): Flow<Long>

    @Query(
        """SELECT s.id, s.number, s.totalPaisa, s.createdAt, c.name AS customerName, s.totalPaisa - s.paidPaisa AS duePaisa,
                  (SELECT COUNT(*) FROM sale_item i WHERE i.saleId = s.id) AS items
           FROM sale s LEFT JOIN customer c ON c.id = s.customerId
           ORDER BY s.createdAt DESC LIMIT :limit"""
    )
    fun recent(limit: Int): Flow<List<SaleRow>>
}

@Dao
interface SupplierDao {
    @Query(
        """SELECT * FROM supplier
           WHERE (:q = '' OR name LIKE '%' || :q || '%' OR phone LIKE '%' || :q || '%') AND (:dueOnly = 0 OR duePaisa > 0)
           ORDER BY duePaisa > 0 DESC, name COLLATE NOCASE"""
    )
    fun search(q: String, dueOnly: Boolean): Flow<List<SupplierEntity>>

    @Query("SELECT COUNT(*) FROM supplier")
    fun count(): Flow<Int>

    @Query("SELECT COALESCE(SUM(duePaisa), 0) FROM supplier")
    fun totalDue(): Flow<Long>

    @Query("SELECT * FROM supplier WHERE id = :id")
    fun watch(id: Long): Flow<SupplierEntity?>

    @Query("SELECT * FROM supplier WHERE id = :id")
    suspend fun get(id: Long): SupplierEntity?

    @Insert
    suspend fun insert(s: SupplierEntity): Long

    @Update
    suspend fun update(s: SupplierEntity)

    @Query("UPDATE supplier SET duePaisa = duePaisa + :delta WHERE id = :id")
    suspend fun addDue(id: Long, delta: Long)

    @Insert
    suspend fun insertPayment(p: SupplierPaymentEntity): Long

    /** Purchases and payments for one supplier, newest first. */
    @Query(
        """SELECT id, number, createdAt, totalPaisa AS amountPaisa, totalPaisa - paidPaisa AS duePaisa, 0 AS isPayment
           FROM purchase WHERE supplierId = :id
           UNION ALL
           SELECT id, number, createdAt, amountPaisa, 0 AS duePaisa, 1 AS isPayment
           FROM supplier_payment WHERE supplierId = :id
           UNION ALL
           SELECT id, number, createdAt, dueCutPaisa AS amountPaisa, 0 AS duePaisa, 1 AS isPayment
           FROM purchase_return WHERE supplierId = :id AND dueCutPaisa > 0
           ORDER BY createdAt DESC LIMIT 100"""
    )
    fun history(id: Long): Flow<List<CustomerTxn>>
}

data class PurchaseRow(
    val id: Long,
    val number: String,
    val totalPaisa: Long,
    val createdAt: Long,
    val supplierName: String?,
    val duePaisa: Long,
    val items: Int,
)

@Dao
interface PurchaseDao {
    @Insert
    suspend fun insert(p: PurchaseEntity): Long

    @Insert
    suspend fun insertItems(items: List<PurchaseItemEntity>)

    @Query(
        """SELECT p.id, p.number, p.totalPaisa, p.createdAt, s.name AS supplierName, p.totalPaisa - p.paidPaisa AS duePaisa,
                  (SELECT COUNT(*) FROM purchase_item i WHERE i.purchaseId = p.id) AS items
           FROM purchase p LEFT JOIN supplier s ON s.id = p.supplierId
           ORDER BY p.createdAt DESC LIMIT :limit"""
    )
    fun recent(limit: Int): Flow<List<PurchaseRow>>

    @Query("SELECT COALESCE(SUM(totalPaisa), 0) FROM purchase WHERE createdAt >= :from")
    fun totalSince(from: Long): Flow<Long>

    @Query("SELECT * FROM purchase WHERE id = :id")
    suspend fun get(id: Long): PurchaseEntity?
}

data class CategoryTotal(val category: String, val totalPaisa: Long)

@Dao
interface ExpenseDao {
    @Insert
    suspend fun insert(e: ExpenseEntity): Long

    @Query("DELETE FROM expense WHERE id = :id")
    suspend fun delete(id: Long)

    /** [category] '' means every category. */
    @Query("SELECT * FROM expense WHERE createdAt >= :from AND (:category = '' OR category = :category) ORDER BY createdAt DESC")
    fun since(from: Long, category: String): Flow<List<ExpenseEntity>>

    @Query("SELECT COALESCE(SUM(amountPaisa), 0) FROM expense WHERE createdAt >= :from")
    fun totalSince(from: Long): Flow<Long>

    @Query("SELECT COALESCE(SUM(amountPaisa), 0) FROM expense WHERE createdAt >= :from AND createdAt < :to")
    fun totalBetween(from: Long, to: Long): Flow<Long>

    @Query("SELECT category, SUM(amountPaisa) AS totalPaisa FROM expense WHERE createdAt >= :from GROUP BY category ORDER BY totalPaisa DESC")
    fun byCategory(from: Long): Flow<List<CategoryTotal>>

    /** Categories used before, most used first, for the quick-pick chips. */
    @Query("SELECT category FROM expense GROUP BY category ORDER BY COUNT(*) DESC LIMIT 12")
    fun usedCategories(): Flow<List<String>>
}

data class MethodTotal(val method: String, val totalPaisa: Long)
data class TopProduct(val name: String, val qtyMilli: Long, val totalPaisa: Long)
data class DayTotal(val day: String, val count: Int, val totalPaisa: Long)

/** Totals for the Reports screen, all for [from, to). */
@Dao
interface ReportDao {
    @Query("SELECT COALESCE(SUM(totalPaisa), 0) FROM sale WHERE createdAt >= :from AND createdAt < :to")
    fun sales(from: Long, to: Long): Flow<Long>

    @Query("SELECT COUNT(*) FROM sale WHERE createdAt >= :from AND createdAt < :to")
    fun saleCount(from: Long, to: Long): Flow<Int>

    @Query("SELECT COALESCE(SUM(discountPaisa), 0) FROM sale WHERE createdAt >= :from AND createdAt < :to")
    fun discounts(from: Long, to: Long): Flow<Long>

    /**
     * Cost of the goods sold, from the cost saved on each sale line when it was sold.
     * A later change to the product's cost never rewrites the profit of past sales.
     */
    @Query(
        """SELECT COALESCE(SUM((i.unitCostPaisa * i.qtyMilli + 500) / 1000), 0)
           FROM sale_item i JOIN sale s ON s.id = i.saleId
           WHERE s.createdAt >= :from AND s.createdAt < :to"""
    )
    fun costOfSales(from: Long, to: Long): Flow<Long>

    @Query("SELECT COALESCE(SUM(totalPaisa), 0) FROM purchase WHERE createdAt >= :from AND createdAt < :to")
    fun purchases(from: Long, to: Long): Flow<Long>

    @Query("SELECT COALESCE(SUM(amountPaisa), 0) FROM expense WHERE createdAt >= :from AND createdAt < :to")
    fun expenses(from: Long, to: Long): Flow<Long>

    @Query("SELECT COALESCE(SUM(amountPaisa), 0) FROM due_payment WHERE createdAt >= :from AND createdAt < :to")
    fun dueCollected(from: Long, to: Long): Flow<Long>

    @Query("SELECT COALESCE(SUM(totalPaisa - paidPaisa), 0) FROM sale WHERE createdAt >= :from AND createdAt < :to")
    fun dueGiven(from: Long, to: Long): Flow<Long>

    @Query("SELECT method, SUM(paidPaisa) AS totalPaisa FROM sale WHERE createdAt >= :from AND createdAt < :to GROUP BY method ORDER BY totalPaisa DESC")
    fun byMethod(from: Long, to: Long): Flow<List<MethodTotal>>

    @Query(
        """SELECT i.name AS name, SUM(i.qtyMilli) AS qtyMilli, SUM(i.lineTotalPaisa) AS totalPaisa
           FROM sale_item i JOIN sale s ON s.id = i.saleId
           WHERE s.createdAt >= :from AND s.createdAt < :to
           GROUP BY i.productId ORDER BY totalPaisa DESC LIMIT 10"""
    )
    fun topProducts(from: Long, to: Long): Flow<List<TopProduct>>

    @Query(
        """SELECT strftime('%Y-%m-%d', createdAt / 1000, 'unixepoch', 'localtime') AS day, COUNT(*) AS count, SUM(totalPaisa) AS totalPaisa
           FROM sale WHERE createdAt >= :from AND createdAt < :to
           GROUP BY day ORDER BY day DESC LIMIT 62"""
    )
    fun byDay(from: Long, to: Long): Flow<List<DayTotal>>

    @Query("SELECT COALESCE(SUM(totalPaisa), 0) FROM sale_return WHERE createdAt >= :from AND createdAt < :to")
    fun saleReturns(from: Long, to: Long): Flow<Long>

    @Query("SELECT COALESCE(SUM(costPaisa), 0) FROM sale_return WHERE createdAt >= :from AND createdAt < :to")
    fun saleReturnCost(from: Long, to: Long): Flow<Long>

    @Query("SELECT COALESCE(SUM(totalPaisa), 0) FROM purchase_return WHERE createdAt >= :from AND createdAt < :to")
    fun purchaseReturns(from: Long, to: Long): Flow<Long>
}

/** A sold or bought line with how much of it already came back. */
data class ReturnableLine(
    val id: Long,
    val productId: Long,
    val name: String,
    val qtyMilli: Long,
    val lineTotalPaisa: Long,
    /** Buying price per unit (sale lines: saved at sale time; purchase lines: the price paid). */
    val unitCostPaisa: Long,
    val returnedMilli: Long,
) {
    val leftMilli get() = (qtyMilli - returnedMilli).coerceAtLeast(0)
}

data class ReturnRow(
    val id: Long,
    val number: String,
    val createdAt: Long,
    val totalPaisa: Long,
    val dueCutPaisa: Long,
    val refundPaisa: Long,
    val isPurchase: Boolean,
    val party: String?,
    /** The sale or purchase it came from. */
    val fromNumber: String?,
)

@Dao
interface ReturnDao {
    @Query(
        """SELECT i.id, i.productId, i.name, i.qtyMilli, i.lineTotalPaisa, i.unitCostPaisa,
                  COALESCE((SELECT SUM(r.qtyMilli) FROM sale_return_item r WHERE r.saleItemId = i.id), 0) AS returnedMilli
           FROM sale_item i WHERE i.saleId = :saleId ORDER BY i.id"""
    )
    suspend fun saleLines(saleId: Long): List<ReturnableLine>

    @Query(
        """SELECT i.id, i.productId, i.name, i.qtyMilli, i.lineTotalPaisa, i.unitCostPaisa,
                  COALESCE((SELECT SUM(r.qtyMilli) FROM purchase_return_item r WHERE r.purchaseItemId = i.id), 0) AS returnedMilli
           FROM purchase_item i WHERE i.purchaseId = :purchaseId ORDER BY i.id"""
    )
    suspend fun purchaseLines(purchaseId: Long): List<ReturnableLine>

    @Insert
    suspend fun insertSaleReturn(r: SaleReturnEntity): Long

    @Insert
    suspend fun insertSaleReturnItems(items: List<SaleReturnItemEntity>)

    @Insert
    suspend fun insertPurchaseReturn(r: PurchaseReturnEntity): Long

    @Insert
    suspend fun insertPurchaseReturnItems(items: List<PurchaseReturnItemEntity>)

    @Query(
        """SELECT r.id AS id, r.number AS number, r.createdAt AS createdAt, r.totalPaisa AS totalPaisa, r.dueCutPaisa AS dueCutPaisa, r.refundPaisa AS refundPaisa, 0 AS isPurchase,
                  c.name AS party, s.number AS fromNumber
           FROM sale_return r LEFT JOIN customer c ON c.id = r.customerId LEFT JOIN sale s ON s.id = r.saleId
           UNION ALL
           SELECT r.id AS id, r.number AS number, r.createdAt AS createdAt, r.totalPaisa AS totalPaisa, r.dueCutPaisa AS dueCutPaisa, r.refundPaisa AS refundPaisa, 1 AS isPurchase,
                  sp.name AS party, p.number AS fromNumber
           FROM purchase_return r LEFT JOIN supplier sp ON sp.id = r.supplierId LEFT JOIN purchase p ON p.id = r.purchaseId
           ORDER BY createdAt DESC LIMIT :limit"""
    )
    fun recent(limit: Int): Flow<List<ReturnRow>>
}

@Dao
interface StaffDao {
    @Query("SELECT * FROM staff WHERE active = 1 ORDER BY name COLLATE NOCASE")
    fun all(): Flow<List<StaffEntity>>

    @Query("SELECT * FROM staff WHERE id = :id")
    suspend fun get(id: Long): StaffEntity?

    @Insert
    suspend fun insert(s: StaffEntity): Long

    @Update
    suspend fun update(s: StaffEntity)
}

@Dao
interface DocCounterDao {
    @Query("SELECT last FROM doc_counter WHERE type = :type AND year = :year")
    suspend fun last(type: String, year: Int): Int?

    @Query("SELECT last FROM doc_counter WHERE type = :type AND year = :year")
    fun lastFlow(type: String, year: Int): Flow<Int?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun set(c: DocCounterEntity)
}

@Dao
interface MfsDao {
    @Query("SELECT * FROM mfs_wallet ORDER BY id")
    fun wallets(): Flow<List<MfsWalletEntity>>

    @Query("SELECT * FROM mfs_wallet ORDER BY id")
    suspend fun allWallets(): List<MfsWalletEntity>

    @Query("SELECT * FROM mfs_wallet WHERE id = :id")
    suspend fun wallet(id: Long): MfsWalletEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertWallets(w: List<MfsWalletEntity>)

    @Update
    suspend fun updateWallet(w: MfsWalletEntity)

    @Query("UPDATE mfs_wallet SET balancePaisa = balancePaisa + :delta WHERE id = :id")
    suspend fun addBalance(id: Long, delta: Long)

    @Insert
    suspend fun insertTxn(t: MfsTxnEntity): Long

    @Query(
        """SELECT t.*, w.provider AS provider FROM mfs_txn t JOIN mfs_wallet w ON w.id = t.walletId
           WHERE t.createdAt >= :from ORDER BY t.createdAt DESC LIMIT :limit"""
    )
    fun recent(from: Long, limit: Int): Flow<List<MfsTxnRow>>

    @Query(
        """SELECT COUNT(*) AS count, COALESCE(SUM(earningPaisa), 0) AS earningPaisa, COALESCE(SUM(cashDeltaPaisa), 0) AS cashPaisa
           FROM mfs_txn WHERE createdAt >= :from"""
    )
    fun summary(from: Long): Flow<MfsSummary>

    /** Customer numbers seen on past entries, for matching masked numbers in a shared SMS. */
    @Query("SELECT DISTINCT customerPhone FROM mfs_txn WHERE customerPhone != '' UNION SELECT phone FROM customer")
    suspend fun knownNumbers(): List<String>
}

data class MfsTxnRow(
    val id: Long,
    val number: String,
    val walletId: Long,
    val type: String,
    val customerPhone: String,
    val amountPaisa: Long,
    val chargePaisa: Long,
    val feePaisa: Long,
    val walletDeltaPaisa: Long,
    val cashDeltaPaisa: Long,
    val earningPaisa: Long,
    val via: String,
    val trxId: String,
    val note: String,
    val staffId: Long?,
    val createdAt: Long,
    val provider: String,
)

data class MfsSummary(val count: Int, val earningPaisa: Long, val cashPaisa: Long)

@Dao
interface OrderDao {
    @Query("SELECT * FROM order_doc WHERE kind = :kind ORDER BY status = 'OPEN' DESC, createdAt DESC LIMIT 200")
    fun list(kind: String): Flow<List<OrderDocEntity>>

    @Query("SELECT * FROM order_doc WHERE id = :id")
    suspend fun get(id: Long): OrderDocEntity?

    @Query("SELECT * FROM order_doc_item WHERE docId = :id ORDER BY id")
    suspend fun items(id: Long): List<OrderDocItemEntity>

    @Insert
    suspend fun insert(d: OrderDocEntity): Long

    @Update
    suspend fun update(d: OrderDocEntity)

    @Query("DELETE FROM order_doc_item WHERE docId = :id")
    suspend fun clearItems(id: Long)

    @Insert
    suspend fun insertItems(items: List<OrderDocItemEntity>)

    @Query("UPDATE order_doc SET status = :status, doneAs = :doneAs WHERE id = :id")
    suspend fun setStatus(id: Long, status: String, doneAs: String)

    @Query("SELECT COUNT(*) FROM order_doc WHERE kind = :kind AND status = 'OPEN'")
    fun openCount(kind: String): Flow<Int>
}

data class MethodBalance(val method: String, val balancePaisa: Long)
data class LedgerEntry(val number: String, val kind: String, val createdAt: Long, val delta: Long, val method: String)

@Dao
interface MoneyDao {
    @Query("SELECT method, COALESCE(SUM(delta), 0) AS balancePaisa FROM ($MONEY_FLOWS) GROUP BY method")
    fun balances(): Flow<List<MethodBalance>>

    @Query("SELECT * FROM ($MONEY_FLOWS) WHERE method = :method ORDER BY createdAt DESC LIMIT 300")
    fun ledger(method: String): Flow<List<LedgerEntry>>

    @Query("SELECT COALESCE(SUM(delta), 0) FROM ($MONEY_FLOWS) WHERE method = :method AND createdAt < :before")
    suspend fun balanceBefore(method: String, before: Long): Long

    @Query("SELECT COALESCE(SUM(delta), 0) FROM ($MONEY_FLOWS) WHERE method = 'CASH' AND createdAt >= :from AND kind != 'OPENING'")
    suspend fun cashFlowSince(from: Long): Long

    @Query("SELECT COALESCE(SUM(delta), 0) FROM ($MONEY_FLOWS) WHERE method = 'CASH' AND createdAt >= :from AND kind != 'OPENING'")
    fun watchCashFlowSince(from: Long): Flow<Long>

    @Insert
    suspend fun insertMove(m: MoneyMoveEntity): Long

    @Query("SELECT * FROM loan ORDER BY outstandingPaisa > 0 DESC, createdAt DESC")
    fun loans(): Flow<List<LoanEntity>>

    @Query("SELECT * FROM loan WHERE id = :id")
    fun watchLoan(id: Long): Flow<LoanEntity?>

    @Query("SELECT * FROM loan WHERE id = :id")
    suspend fun loan(id: Long): LoanEntity?

    @Insert
    suspend fun insertLoan(l: LoanEntity): Long

    @Query("UPDATE loan SET outstandingPaisa = outstandingPaisa + :delta WHERE id = :id")
    suspend fun addOutstanding(id: Long, delta: Long)

    @Query("SELECT * FROM money_move WHERE loanId = :id ORDER BY createdAt DESC")
    fun loanMoves(id: Long): Flow<List<MoneyMoveEntity>>

    @Query("SELECT * FROM cash_session WHERE closedAt IS NULL ORDER BY openedAt DESC LIMIT 1")
    fun openSession(): Flow<CashSessionEntity?>

    @Query("SELECT * FROM cash_session WHERE closedAt IS NULL ORDER BY openedAt DESC LIMIT 1")
    suspend fun currentSession(): CashSessionEntity?

    @Query("SELECT * FROM cash_session WHERE closedAt IS NOT NULL ORDER BY openedAt DESC LIMIT 30")
    fun pastSessions(): Flow<List<CashSessionEntity>>

    @Insert
    suspend fun insertSession(c: CashSessionEntity): Long

    @Update
    suspend fun updateSession(c: CashSessionEntity)
}

@Dao
interface PromoDao {
    @Query("SELECT * FROM promo WHERE kind = :kind ORDER BY active DESC, createdAt DESC")
    fun byKind(kind: PromoKind): Flow<List<PromoEntity>>

    /** Offers running right now. */
    @Query("SELECT * FROM promo WHERE kind = 'OFFER' AND active = 1 AND (startAt IS NULL OR startAt <= :now) AND (endAt IS NULL OR endAt >= :now)")
    fun liveOffers(now: Long): Flow<List<PromoEntity>>

    @Query("SELECT * FROM promo WHERE kind = :kind AND code = :code AND active = 1 LIMIT 1")
    suspend fun byCode(kind: PromoKind, code: String): PromoEntity?

    @Query("SELECT COUNT(*) FROM promo WHERE kind != 'OFFER' AND code = :code AND id != :exceptId")
    suspend fun codeTaken(code: String, exceptId: Long): Int

    @Insert
    suspend fun insert(p: PromoEntity): Long

    @Query("UPDATE promo SET active = :active WHERE id = :id")
    suspend fun setActive(id: Long, active: Boolean)

    /** Counts one use; 0 rows when the limit is already reached. */
    @Query("UPDATE promo SET uses = uses + 1 WHERE id = :id AND (maxUses = 0 OR uses < maxUses)")
    suspend fun use(id: Long): Int

    /** Spends from a voucher; 0 rows when the balance is too low. */
    @Query("UPDATE promo SET balancePaisa = balancePaisa - :amount, uses = uses + 1 WHERE id = :id AND balancePaisa >= :amount")
    suspend fun spend(id: Long, amount: Long): Int

    @Insert
    suspend fun insertSalePromos(rows: List<SalePromoEntity>)

    @Query("SELECT * FROM sale_promo WHERE saleId = :saleId")
    suspend fun forSale(saleId: Long): List<SalePromoEntity>

    @Query("SELECT COALESCE(SUM(amountPaisa), 0) FROM sale_promo sp JOIN sale s ON s.id = sp.saleId WHERE sp.promoId = :promoId")
    fun given(promoId: Long): Flow<Long>
}

data class MonthMark(val employeeId: Long, val absent: Int, val half: Int, val leave: Int, val present: Int)
data class MonthPaid(val employeeId: Long, val paidPaisa: Long)

@Dao
interface HrDao {
    @Query("SELECT * FROM employee ORDER BY active DESC, name")
    fun employees(): Flow<List<EmployeeEntity>>

    @Upsert
    suspend fun saveEmployee(e: EmployeeEntity): Long

    @Query("SELECT * FROM attendance WHERE day = :day")
    fun day(day: Int): Flow<List<AttendanceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun mark(a: AttendanceEntity)

    @Query("DELETE FROM attendance WHERE employeeId = :employeeId AND day = :day")
    suspend fun unmark(employeeId: Long, day: Int)

    @Query(
        "SELECT employeeId, SUM(status = 'ABSENT') AS absent, SUM(status = 'HALF') AS half, SUM(status = 'LEAVE') AS leave, " +
            "SUM(status = 'PRESENT') AS present FROM attendance WHERE day BETWEEN :from AND :to GROUP BY employeeId"
    )
    fun month(from: Int, to: Int): Flow<List<MonthMark>>

    // Joined to the expense, so a salary expense deleted from Expenses no longer counts as paid.
    @Query(
        "SELECT sp.employeeId AS employeeId, SUM(sp.amountPaisa) AS paidPaisa FROM salary_payment sp " +
            "JOIN expense e ON e.id = sp.expenseId WHERE sp.month = :month GROUP BY sp.employeeId"
    )
    fun paid(month: Int): Flow<List<MonthPaid>>

    @Insert
    suspend fun insertPayment(p: SalaryPaymentEntity): Long
}

data class BatchRow(
    val id: Long, val productId: Long, val productName: String, val unit: String,
    val batchNo: String, val expiryDay: Long?, val qtyMilli: Long,
)

@Dao
interface BatchDao {
    @Insert
    suspend fun insert(b: StockBatchEntity): Long

    /** Batches a sale can take from, soonest expiry first; expired ones are left alone. */
    @Query("SELECT * FROM stock_batch WHERE productId = :productId AND qtyMilli > 0 AND (expiryDay IS NULL OR expiryDay >= :today) ORDER BY expiryDay IS NULL, expiryDay")
    suspend fun sellable(productId: Long, today: Long): List<StockBatchEntity>

    @Query("UPDATE stock_batch SET qtyMilli = qtyMilli - :qty WHERE id = :id")
    suspend fun take(id: Long, qty: Long)

    @Query(
        "SELECT b.id AS id, b.productId AS productId, p.name AS productName, p.unit AS unit, b.batchNo AS batchNo, " +
            "b.expiryDay AS expiryDay, b.qtyMilli AS qtyMilli FROM stock_batch b JOIN product p ON p.id = b.productId " +
            "WHERE b.qtyMilli > 0 AND (:untilDay < 0 OR (b.expiryDay IS NOT NULL AND b.expiryDay <= :untilDay)) " +
            "ORDER BY b.expiryDay IS NULL, b.expiryDay, p.name"
    )
    fun list(untilDay: Long): Flow<List<BatchRow>>

    @Query("SELECT * FROM stock_batch WHERE id = :id")
    suspend fun get(id: Long): StockBatchEntity?

    /** Soonest live expiry per product, for the sale screen. */
    @Query("SELECT MIN(expiryDay) FROM stock_batch WHERE productId = :productId AND qtyMilli > 0 AND expiryDay IS NOT NULL AND expiryDay >= :today")
    suspend fun nextExpiry(productId: Long, today: Long): Long?
}

data class TableState(val id: Long, val name: String, val orderId: Long?, val totalPaisa: Long, val items: Int)

@Dao
interface TableDao {
    @Query(
        "SELECT t.id AS id, t.name AS name, o.id AS orderId, " +
            "COALESCE((SELECT SUM((i.unitPricePaisa * i.qtyMilli + 500) / 1000) FROM table_order_item i WHERE i.orderId = o.id), 0) AS totalPaisa, " +
            "COALESCE((SELECT COUNT(*) FROM table_order_item i WHERE i.orderId = o.id), 0) AS items " +
            "FROM dining_table t LEFT JOIN table_order o ON o.tableId = t.id AND o.status = 'OPEN' WHERE t.active = 1 ORDER BY t.id"
    )
    fun tables(): Flow<List<TableState>>

    @Insert
    suspend fun insertTable(t: DiningTableEntity): Long

    @Query("UPDATE dining_table SET active = 0 WHERE id = :id")
    suspend fun hideTable(id: Long)

    @Query("SELECT * FROM dining_table WHERE id = :id")
    fun watchTable(id: Long): Flow<DiningTableEntity?>

    @Query("SELECT * FROM table_order WHERE tableId = :tableId AND status = 'OPEN' LIMIT 1")
    suspend fun openOrder(tableId: Long): TableOrderEntity?

    @Query("SELECT * FROM table_order WHERE tableId = :tableId AND status = 'OPEN' LIMIT 1")
    fun watchOpenOrder(tableId: Long): Flow<TableOrderEntity?>

    @Insert
    suspend fun insertOrder(o: TableOrderEntity): Long

    @Query("SELECT * FROM table_order_item WHERE orderId = :orderId ORDER BY id")
    fun items(orderId: Long): Flow<List<TableOrderItemEntity>>

    @Query("SELECT * FROM table_order_item WHERE orderId = :orderId ORDER BY id")
    suspend fun itemsNow(orderId: Long): List<TableOrderItemEntity>

    @Insert
    suspend fun insertItem(i: TableOrderItemEntity): Long

    @Query("UPDATE table_order_item SET qtyMilli = :qty WHERE id = :id")
    suspend fun setQty(id: Long, qty: Long)

    @Query("DELETE FROM table_order_item WHERE id = :id")
    suspend fun deleteItem(id: Long)

    @Query("UPDATE table_order_item SET sentMilli = qtyMilli WHERE orderId = :orderId")
    suspend fun markSent(orderId: Long)

    @Query("UPDATE table_order SET status = 'BILLED', saleNumber = :saleNumber WHERE id = :orderId")
    suspend fun bill(orderId: Long, saleNumber: String)

    @Query("DELETE FROM table_order WHERE id = :orderId")
    suspend fun deleteOrder(orderId: Long)
}

data class JobRow(val id: Long, val number: String, val outName: String, val outQtyMilli: Long, val costPaisa: Long, val createdAt: Long, val inputs: String?)
data class SerialRow(val id: Long, val serial: String, val productId: Long, val productName: String, val saleId: Long?, val saleNumber: String?, val soldAt: Long?, val createdAt: Long)
data class WarehouseRow(val id: Long, val name: String, val products: Int)
data class WarehouseLine(val productId: Long, val name: String, val unit: String, val qtyMilli: Long)

@Dao
interface StockToolsDao {
    @Insert
    suspend fun insertJob(j: StockJobEntity): Long

    @Insert
    suspend fun insertJobInputs(rows: List<StockJobInputEntity>)

    @Query(
        "SELECT j.id AS id, j.number AS number, j.outName AS outName, j.outQtyMilli AS outQtyMilli, j.costPaisa AS costPaisa, j.createdAt AS createdAt, " +
            "(SELECT GROUP_CONCAT(name, ', ') FROM stock_job_input WHERE jobId = j.id) AS inputs FROM stock_job j ORDER BY j.createdAt DESC LIMIT 50"
    )
    fun jobs(): Flow<List<JobRow>>

    /** Weighted average cost after [qty] more at [unitCost] arrive. */
    @Query(
        "UPDATE product SET costPaisa = CASE WHEN stockMilli + :qty > 0 THEN (costPaisa * MAX(stockMilli, 0) + :unitCost * :qty) / (MAX(stockMilli, 0) + :qty) ELSE :unitCost END, " +
            "stockMilli = stockMilli + :qty WHERE id = :id"
    )
    suspend fun produce(id: Long, qty: Long, unitCost: Long)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSerials(rows: List<ProductSerialEntity>): List<Long>

    @Query("SELECT * FROM product_serial WHERE serial = :serial AND saleId IS NULL LIMIT 1")
    suspend fun inStock(serial: String): ProductSerialEntity?

    @Query("UPDATE product_serial SET saleId = :saleId, soldAt = :at WHERE serial = :serial AND saleId IS NULL")
    suspend fun sell(serial: String, saleId: Long, at: Long): Int

    @Query(
        "SELECT ps.id AS id, ps.serial AS serial, ps.productId AS productId, p.name AS productName, ps.saleId AS saleId, s.number AS saleNumber, " +
            "ps.soldAt AS soldAt, ps.createdAt AS createdAt FROM product_serial ps JOIN product p ON p.id = ps.productId " +
            "LEFT JOIN sale s ON s.id = ps.saleId WHERE (:q = '' OR ps.serial LIKE '%' || :q || '%' OR p.name LIKE '%' || :q || '%') " +
            "ORDER BY ps.createdAt DESC LIMIT 100"
    )
    fun serials(q: String): Flow<List<SerialRow>>

    @Query("SELECT serial FROM product_serial WHERE saleId = :saleId")
    suspend fun serialsOfSale(saleId: Long): List<String>

    @Query("SELECT w.id AS id, w.name AS name, (SELECT COUNT(*) FROM warehouse_stock ws WHERE ws.warehouseId = w.id AND ws.qtyMilli > 0) AS products FROM warehouse w WHERE w.active = 1 ORDER BY w.id")
    fun warehouses(): Flow<List<WarehouseRow>>

    @Insert
    suspend fun insertWarehouse(w: WarehouseEntity): Long

    @Query("SELECT * FROM warehouse WHERE id = :id")
    fun watchWarehouse(id: Long): Flow<WarehouseEntity?>

    @Query(
        "SELECT ws.productId AS productId, p.name AS name, p.unit AS unit, ws.qtyMilli AS qtyMilli FROM warehouse_stock ws " +
            "JOIN product p ON p.id = ws.productId WHERE ws.warehouseId = :id AND ws.qtyMilli > 0 ORDER BY p.name COLLATE NOCASE"
    )
    fun warehouseStock(id: Long): Flow<List<WarehouseLine>>

    @Query("SELECT COALESCE((SELECT qtyMilli FROM warehouse_stock WHERE warehouseId = :w AND productId = :p), 0)")
    suspend fun heldIn(w: Long, p: Long): Long

    @Query("SELECT COALESCE(SUM(qtyMilli), 0) FROM warehouse_stock WHERE productId = :p")
    suspend fun heldAll(p: Long): Long

    @Upsert
    suspend fun setHeld(row: WarehouseStockEntity)
}

data class AuditRow(val id: Long, val at: Long, val staffName: String?, val action: String, val detail: String)

@Dao
interface AuditDao {
    @Insert
    suspend fun insert(a: AuditLogEntity)

    @Query(
        "SELECT a.id AS id, a.at AS at, s.name AS staffName, a.`action` AS `action`, a.detail AS detail FROM audit_log a " +
            "LEFT JOIN staff s ON s.id = a.staffId WHERE (:staff < 0 OR COALESCE(a.staffId, 0) = :staff) " +
            "AND (:q = '' OR a.detail LIKE '%' || :q || '%') ORDER BY a.at DESC LIMIT 300"
    )
    fun list(staff: Long, q: String): Flow<List<AuditRow>>

    /** Keeps the log from growing without end: about a year. */
    @Query("DELETE FROM audit_log WHERE at < :before")
    suspend fun trim(before: Long)
}

data class DeleteRequestRow(val id: Long, val saleId: Long, val number: String, val reason: String, val staffName: String?, val createdAt: Long, val totalPaisa: Long)
data class DeletedSaleRow(val id: Long, val number: String, val totalPaisa: Long, val saleAt: Long, val deletedAt: Long, val reason: String, val itemsText: String, val requestedByName: String?)
data class InstallmentRow(val id: Long, val saleNumber: String, val seq: Int, val dueDay: Long, val amountPaisa: Long, val paidPaisa: Long)

/** Sale corrections: delete requests, deleted sales and instalment plans. */
@Dao
interface SaleFixDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun request(r: SaleDeleteRequestEntity)

    @Query("SELECT * FROM sale_delete_request WHERE saleId = :saleId")
    suspend fun requestFor(saleId: Long): SaleDeleteRequestEntity?

    @Query("SELECT * FROM sale_delete_request WHERE saleId = :saleId")
    fun watchRequest(saleId: Long): Flow<SaleDeleteRequestEntity?>

    @Query("DELETE FROM sale_delete_request WHERE saleId = :saleId")
    suspend fun dropRequest(saleId: Long)

    @Query(
        "SELECT r.id AS id, r.saleId AS saleId, r.number AS number, r.reason AS reason, st.name AS staffName, r.createdAt AS createdAt, " +
            "COALESCE(s.totalPaisa, 0) AS totalPaisa FROM sale_delete_request r LEFT JOIN staff st ON st.id = r.staffId " +
            "LEFT JOIN sale s ON s.id = r.saleId ORDER BY r.createdAt DESC"
    )
    fun requests(): Flow<List<DeleteRequestRow>>

    @Query("SELECT COUNT(*) FROM sale_delete_request")
    fun requestCount(): Flow<Int>

    @Insert
    suspend fun insertDeleted(d: DeletedSaleEntity)

    @Query(
        "SELECT d.id AS id, d.number AS number, d.totalPaisa AS totalPaisa, d.saleAt AS saleAt, d.deletedAt AS deletedAt, d.reason AS reason, " +
            "d.itemsText AS itemsText, st.name AS requestedByName FROM deleted_sale d LEFT JOIN staff st ON st.id = d.requestedBy " +
            "ORDER BY d.deletedAt DESC LIMIT 300"
    )
    fun deleted(): Flow<List<DeletedSaleRow>>

    @Query("DELETE FROM sale WHERE id = :id")
    suspend fun deleteSale(id: Long)

    @Query("SELECT COUNT(*) FROM sale_return WHERE saleId = :saleId")
    suspend fun returnCount(saleId: Long): Int

    @Query("UPDATE product_serial SET saleId = NULL, soldAt = NULL WHERE saleId = :saleId")
    suspend fun unsellSerials(saleId: Long)

    @Query("UPDATE promo SET uses = MAX(uses - 1, 0) WHERE id = :id")
    suspend fun unusePromo(id: Long)

    @Query("UPDATE promo SET balancePaisa = balancePaisa + :amount, uses = MAX(uses - 1, 0) WHERE id = :id")
    suspend fun refundVoucher(id: Long, amount: Long)

    @Insert
    suspend fun insertInstallments(rows: List<InstallmentEntity>)

    @Query("SELECT * FROM installment WHERE customerId = :customerId AND paidPaisa < amountPaisa ORDER BY dueDay, seq, id")
    suspend fun unpaid(customerId: Long): List<InstallmentEntity>

    @Query("UPDATE installment SET paidPaisa = :paid WHERE id = :id")
    suspend fun setPaid(id: Long, paid: Long)

    @Query("DELETE FROM installment WHERE saleNumber = :saleNumber")
    suspend fun dropPlan(saleNumber: String)

    @Query("SELECT id, saleNumber, seq, dueDay, amountPaisa, paidPaisa FROM installment WHERE customerId = :customerId ORDER BY dueDay, seq, id")
    fun forCustomer(customerId: Long): Flow<List<InstallmentRow>>

    @Query("SELECT id, saleNumber, seq, dueDay, amountPaisa, paidPaisa FROM installment WHERE saleNumber = :saleNumber ORDER BY seq")
    fun forSale(saleNumber: String): Flow<List<InstallmentRow>>

    /** Instalments due on or before [day] and not fully paid, across all customers. */
    @Query("SELECT COUNT(*) FROM installment WHERE dueDay <= :day AND paidPaisa < amountPaisa")
    fun lateCount(day: Long): Flow<Int>
}
