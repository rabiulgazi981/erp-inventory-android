package com.bongshaltech.erpinventory.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

data class TailorOrderRow(
    val id: Long, val number: String, val customerId: Long, val saleId: Long, val deliveryDay: Long?, val status: String,
    val note: String, val createdAt: Long, val deliveredAt: Long?,
    val customerName: String?, val customerPhone: String?, val totalPaisa: Long, val paidPaisa: Long, val customerDuePaisa: Long,
    val pieces: Int, val garments: String?,
)

data class WorkerBalance(val workerId: Long, val earnedPaisa: Long, val paidPaisa: Long)

data class TailorTaskRow(
    val id: Long, val orderId: Long, val itemId: Long, val workerId: Long, val task: String, val qty: Int, val ratePaisa: Long,
    val amountPaisa: Long, val createdAt: Long, val workerName: String?, val orderNumber: String?,
)

private const val TAILOR_ORDER_ROW = """
    SELECT o.id, o.number, o.customerId, o.saleId, o.deliveryDay, o.status, o.note, o.createdAt, o.deliveredAt,
           c.name AS customerName, c.phone AS customerPhone, COALESCE(s.totalPaisa, 0) AS totalPaisa, COALESCE(s.paidPaisa, 0) AS paidPaisa,
           COALESCE(c.duePaisa, 0) AS customerDuePaisa,
           (SELECT COALESCE(SUM(i.qty), 0) FROM tailor_item i WHERE i.orderId = o.id) AS pieces,
           (SELECT GROUP_CONCAT(i.garment, ',') FROM tailor_item i WHERE i.orderId = o.id) AS garments
    FROM tailor_order o LEFT JOIN customer c ON c.id = o.customerId LEFT JOIN sale s ON s.id = o.saleId
"""

@Dao
interface TailorDao {
    @Query("$TAILOR_ORDER_ROW ORDER BY o.createdAt DESC LIMIT 300")
    fun orders(): Flow<List<TailorOrderRow>>

    @Query("$TAILOR_ORDER_ROW WHERE o.id = :id")
    fun watchOrder(id: Long): Flow<TailorOrderRow?>

    @Query("SELECT * FROM tailor_order WHERE id = :id")
    suspend fun order(id: Long): TailorOrderEntity?

    @Insert
    suspend fun insertOrder(o: TailorOrderEntity): Long

    @Query("UPDATE tailor_order SET status = :status, deliveredAt = :deliveredAt WHERE id = :id")
    suspend fun setStatus(id: Long, status: String, deliveredAt: Long?)

    @Query("UPDATE tailor_order SET deliveryDay = :day, note = :note WHERE id = :id")
    suspend fun setDelivery(id: Long, day: Long?, note: String)

    @Insert
    suspend fun insertItems(items: List<TailorItemEntity>)

    @Query("SELECT * FROM tailor_item WHERE orderId = :orderId ORDER BY id")
    fun items(orderId: Long): Flow<List<TailorItemEntity>>

    @Query("SELECT * FROM tailor_item WHERE orderId = :orderId ORDER BY id")
    suspend fun itemsNow(orderId: Long): List<TailorItemEntity>

    @Query("UPDATE tailor_item SET measurements = :m, style = :style WHERE id = :id")
    suspend fun updateItem(id: Long, m: String, style: String)

    @Query("SELECT * FROM tailor_measure WHERE customerId = :customerId")
    suspend fun measures(customerId: Long): List<TailorMeasureEntity>

    @Query("SELECT * FROM tailor_measure WHERE customerId = :customerId ORDER BY updatedAt DESC")
    fun watchMeasures(customerId: Long): Flow<List<TailorMeasureEntity>>

    @Query("SELECT * FROM tailor_measure WHERE customerId = :customerId AND garment = :garment")
    suspend fun measure(customerId: Long, garment: String): TailorMeasureEntity?

    @Upsert
    suspend fun saveMeasure(m: TailorMeasureEntity)

    @Query("SELECT * FROM tailor_worker ORDER BY active DESC, name COLLATE NOCASE")
    fun workers(): Flow<List<TailorWorkerEntity>>

    @Query("SELECT * FROM tailor_worker WHERE id = :id")
    suspend fun worker(id: Long): TailorWorkerEntity?

    @Upsert
    suspend fun saveWorker(w: TailorWorkerEntity): Long

    @Insert
    suspend fun insertTask(t: TailorTaskEntity): Long

    @Query("DELETE FROM tailor_task WHERE id = :id")
    suspend fun deleteTask(id: Long)

    @Query(
        """SELECT t.*, w.name AS workerName, o.number AS orderNumber FROM tailor_task t
           LEFT JOIN tailor_worker w ON w.id = t.workerId LEFT JOIN tailor_order o ON o.id = t.orderId
           WHERE t.orderId = :orderId ORDER BY t.createdAt"""
    )
    fun tasks(orderId: Long): Flow<List<TailorTaskRow>>

    @Query(
        """SELECT t.*, w.name AS workerName, o.number AS orderNumber FROM tailor_task t
           LEFT JOIN tailor_worker w ON w.id = t.workerId LEFT JOIN tailor_order o ON o.id = t.orderId
           WHERE t.workerId = :workerId ORDER BY t.createdAt DESC LIMIT 200"""
    )
    fun workerTasks(workerId: Long): Flow<List<TailorTaskRow>>

    @Query(
        """SELECT w.id AS workerId,
                  (SELECT COALESCE(SUM(amountPaisa), 0) FROM tailor_task t WHERE t.workerId = w.id) AS earnedPaisa,
                  (SELECT COALESCE(SUM(amountPaisa), 0) FROM worker_pay p WHERE p.trade = 'TAILOR' AND p.workerId = w.id) AS paidPaisa
           FROM tailor_worker w"""
    )
    fun balances(): Flow<List<WorkerBalance>>

    @Query("SELECT * FROM worker_pay WHERE trade = :trade AND workerId = :workerId ORDER BY createdAt DESC LIMIT 200")
    fun pays(trade: String, workerId: Long): Flow<List<WorkerPayEntity>>

    @Insert
    suspend fun insertPay(p: WorkerPayEntity): Long

    /** Making charges, worker cost and fabric sold, for orders made from [from]. */
    @Query(
        """SELECT COUNT(*) AS orders,
                  COALESCE(SUM((SELECT SUM(i.qty * i.makingPaisa) FROM tailor_item i WHERE i.orderId = o.id)), 0) AS makingPaisa,
                  COALESCE(SUM((SELECT SUM(i.fabricMilli * i.fabricPricePaisa / 1000) FROM tailor_item i WHERE i.orderId = o.id)), 0) AS fabricPaisa,
                  COALESCE(SUM((SELECT SUM(t.amountPaisa) FROM tailor_task t WHERE t.orderId = o.id)), 0) AS labourPaisa,
                  COALESCE(SUM(CASE WHEN o.status = 'DELIVERED' THEN 1 ELSE 0 END), 0) AS delivered,
                  COALESCE(SUM(CASE WHEN o.status NOT IN ('DELIVERED', 'CANCELLED') THEN 1 ELSE 0 END), 0) AS open
           FROM tailor_order o WHERE o.createdAt >= :from AND o.status != 'CANCELLED'"""
    )
    fun report(from: Long): Flow<TailorReport>
}

data class TailorReport(val orders: Int, val makingPaisa: Long, val fabricPaisa: Long, val labourPaisa: Long, val delivered: Int, val open: Int)

data class GOrderRow(
    val id: Long, val number: String, val styleId: Long, val customerId: Long?, val buyerPo: String, val deliveryDay: Long?,
    val unitPricePaisa: Long, val status: String, val note: String, val wipPaisa: Long, val writeOffPaisa: Long, val createdAt: Long,
    val styleCode: String?, val styleName: String?, val buyerName: String?, val buyerPhone: String?,
    val ordered: Long, val packed: Long, val delivered: Long,
)

data class StageTotal(val stage: String, val qty: Long, val rejects: Long)

data class LineDay(val lineId: Long, val qty: Long)

data class GOutputRow(
    val id: Long, val orderId: Long, val stage: String, val lineId: Long?, val workerId: Long?, val operation: String, val ratePaisa: Long,
    val amountPaisa: Long, val size: String, val color: String, val qty: Long, val rejects: Long, val day: Long, val note: String,
    val createdAt: Long, val workerName: String?, val lineName: String?, val orderNumber: String?,
)

private const val G_ORDER_ROW = """
    SELECT o.*, s.code AS styleCode, s.name AS styleName, c.name AS buyerName, c.phone AS buyerPhone,
           (SELECT COALESCE(SUM(qty), 0) FROM g_cell x WHERE x.orderId = o.id) AS ordered,
           (SELECT COALESCE(SUM(packed), 0) FROM g_cell x WHERE x.orderId = o.id) AS packed,
           (SELECT COALESCE(SUM(delivered), 0) FROM g_cell x WHERE x.orderId = o.id) AS delivered
    FROM g_order o LEFT JOIN g_style s ON s.id = o.styleId LEFT JOIN customer c ON c.id = o.customerId
"""

private const val G_OUTPUT_ROW = """
    SELECT g.*, w.name AS workerName, l.name AS lineName, o.number AS orderNumber FROM g_output g
    LEFT JOIN g_worker w ON w.id = g.workerId LEFT JOIN g_line l ON l.id = g.lineId LEFT JOIN g_order o ON o.id = g.orderId
"""

@Dao
interface GarmentDao {
    @Query("SELECT * FROM g_line ORDER BY active DESC, name COLLATE NOCASE")
    fun lines(): Flow<List<GLineEntity>>

    @Upsert
    suspend fun saveLine(l: GLineEntity): Long

    @Query("SELECT * FROM g_worker ORDER BY active DESC, name COLLATE NOCASE")
    fun workers(): Flow<List<GWorkerEntity>>

    @Upsert
    suspend fun saveWorker(w: GWorkerEntity): Long

    @Query("SELECT * FROM g_worker WHERE id = :id")
    suspend fun worker(id: Long): GWorkerEntity?

    @Query("SELECT * FROM g_style ORDER BY active DESC, createdAt DESC")
    fun styles(): Flow<List<GStyleEntity>>

    @Query("SELECT * FROM g_style WHERE id = :id")
    suspend fun style(id: Long): GStyleEntity?

    @Upsert
    suspend fun saveStyle(s: GStyleEntity): Long

    @Query("$G_ORDER_ROW ORDER BY o.createdAt DESC LIMIT 300")
    fun orders(): Flow<List<GOrderRow>>

    @Query("$G_ORDER_ROW WHERE o.id = :id")
    fun watchOrder(id: Long): Flow<GOrderRow?>

    @Query("SELECT * FROM g_order WHERE id = :id")
    suspend fun order(id: Long): GOrderEntity?

    @Insert
    suspend fun insertOrder(o: GOrderEntity): Long

    @Update
    suspend fun updateOrder(o: GOrderEntity)

    @Query("UPDATE g_order SET wipPaisa = wipPaisa + :delta WHERE id = :id")
    suspend fun addWip(id: Long, delta: Long)

    @Insert
    suspend fun insertCells(c: List<GCellEntity>)

    @Query("SELECT * FROM g_cell WHERE orderId = :orderId ORDER BY id")
    fun cells(orderId: Long): Flow<List<GCellEntity>>

    @Query("SELECT * FROM g_cell WHERE orderId = :orderId ORDER BY id")
    suspend fun cellsNow(orderId: Long): List<GCellEntity>

    @Query("UPDATE g_cell SET packed = packed + :qty WHERE id = :id")
    suspend fun addPacked(id: Long, qty: Long)

    @Query("UPDATE g_cell SET delivered = delivered + :qty WHERE id = :id")
    suspend fun addDelivered(id: Long, qty: Long)

    @Insert
    suspend fun insertOutput(o: GOutputEntity): Long

    @Query("$G_OUTPUT_ROW WHERE g.orderId = :orderId ORDER BY g.createdAt DESC")
    fun outputs(orderId: Long): Flow<List<GOutputRow>>

    @Query("$G_OUTPUT_ROW WHERE g.workerId = :workerId ORDER BY g.createdAt DESC LIMIT 200")
    fun workerOutputs(workerId: Long): Flow<List<GOutputRow>>

    @Query("SELECT stage, COALESCE(SUM(qty), 0) AS qty, COALESCE(SUM(rejects), 0) AS rejects FROM g_output WHERE orderId = :orderId GROUP BY stage")
    fun stageTotals(orderId: Long): Flow<List<StageTotal>>

    @Query("SELECT COALESCE(SUM(qty), 0) FROM g_output WHERE orderId = :orderId AND stage = :stage")
    suspend fun stageDone(orderId: Long, stage: String): Long

    @Query("SELECT lineId, COALESCE(SUM(qty), 0) AS qty FROM g_output WHERE day = :day AND stage = 'SEWING' AND lineId IS NOT NULL GROUP BY lineId")
    fun lineDay(day: Long): Flow<List<LineDay>>

    @Insert
    suspend fun insertIssue(i: GIssueEntity): Long

    @Query("SELECT * FROM g_issue WHERE orderId = :orderId ORDER BY createdAt DESC")
    fun issues(orderId: Long): Flow<List<GIssueEntity>>

    @Query("SELECT COALESCE(SUM(qtyMilli), 0) FROM g_issue WHERE orderId = :orderId AND productId = :productId")
    suspend fun issuedNow(orderId: Long, productId: Long): Long

    @Query(
        """SELECT w.id AS workerId,
                  (SELECT COALESCE(SUM(amountPaisa), 0) FROM g_output g WHERE g.workerId = w.id) AS earnedPaisa,
                  (SELECT COALESCE(SUM(amountPaisa), 0) FROM worker_pay p WHERE p.trade = 'GARMENT' AND p.workerId = w.id) AS paidPaisa
           FROM g_worker w"""
    )
    fun balances(): Flow<List<WorkerBalance>>

    @Query(
        """SELECT COUNT(*) AS orders,
                  COALESCE(SUM(CASE WHEN status IN ('CONFIRMED', 'IN_PRODUCTION', 'READY') THEN 1 ELSE 0 END), 0) AS open,
                  COALESCE(SUM((SELECT SUM(qty) FROM g_cell x WHERE x.orderId = o.id)), 0) AS pieces,
                  COALESCE(SUM((SELECT SUM(packed) FROM g_cell x WHERE x.orderId = o.id)), 0) AS packed,
                  COALESCE(SUM((SELECT SUM(delivered) FROM g_cell x WHERE x.orderId = o.id)), 0) AS delivered,
                  COALESCE(SUM((SELECT SUM(delivered) FROM g_cell x WHERE x.orderId = o.id) * o.unitPricePaisa), 0) AS deliveredPaisa,
                  COALESCE(SUM(o.wipPaisa), 0) AS wipPaisa,
                  COALESCE(SUM(o.writeOffPaisa), 0) AS writeOffPaisa
           FROM g_order o WHERE o.createdAt >= :from AND o.status != 'CANCELLED'"""
    )
    fun report(from: Long): Flow<GReport>

    @Query("SELECT COALESCE(SUM(qty), 0) AS qty, COALESCE(SUM(rejects), 0) AS rejects, COALESCE(SUM(amountPaisa), 0) AS amountPaisa FROM g_output WHERE createdAt >= :from")
    fun outputSince(from: Long): Flow<GOutputSum>
}

data class GReport(
    val orders: Int, val open: Int, val pieces: Long, val packed: Long, val delivered: Long, val deliveredPaisa: Long,
    val wipPaisa: Long, val writeOffPaisa: Long,
)

data class GOutputSum(val qty: Long, val rejects: Long, val amountPaisa: Long)
