package com.bongshaltech.erpinventory.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {
    @Query("SELECT * FROM product WHERE active = 1 AND (:q = '' OR name LIKE '%' || :q || '%' OR barcode = :q) ORDER BY name COLLATE NOCASE")
    fun search(q: String): Flow<List<ProductEntity>>

    @Query("SELECT * FROM product WHERE id = :id")
    suspend fun get(id: Long): ProductEntity?

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

    /** Returns rows changed: 0 means there was not enough stock. */
    @Query("UPDATE product SET stockMilli = stockMilli - :qtyMilli WHERE id = :id AND stockMilli >= :qtyMilli")
    suspend fun takeStock(id: Long, qtyMilli: Long): Int
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
}

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

    @Query("SELECT COALESCE(SUM(totalPaisa), 0) FROM sale WHERE createdAt >= :from")
    fun totalSince(from: Long): Flow<Long>

    @Query("SELECT COUNT(*) FROM sale WHERE createdAt >= :from")
    fun countSince(from: Long): Flow<Int>

    @Query("SELECT COALESCE(SUM(totalPaisa), 0) FROM sale WHERE createdAt >= :from AND createdAt < :to")
    fun totalBetween(from: Long, to: Long): Flow<Long>

    /** Cash taken today: what was paid on cash sales. */
    @Query("SELECT COALESCE(SUM(paidPaisa), 0) FROM sale WHERE createdAt >= :from AND method = 'CASH'")
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
