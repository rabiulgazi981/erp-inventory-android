package com.bongshaltech.erpinventory.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.bongshaltech.erpinventory.security.DbKey
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

class Converters {
    @TypeConverter fun payToString(m: PayMethod) = m.name
    @TypeConverter fun stringToPay(s: String) = PayMethod.valueOf(s)
}

@Database(
    entities = [
        ProductEntity::class, CustomerEntity::class, SaleEntity::class, SaleItemEntity::class,
        StaffEntity::class, DocCounterEntity::class, DuePaymentEntity::class,
        SupplierEntity::class, PurchaseEntity::class, PurchaseItemEntity::class, SupplierPaymentEntity::class,
        ExpenseEntity::class,
    ],
    version = 5,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun products(): ProductDao
    abstract fun customers(): CustomerDao
    abstract fun sales(): SaleDao
    abstract fun staff(): StaffDao
    abstract fun docCounters(): DocCounterDao
    abstract fun suppliers(): SupplierDao
    abstract fun purchases(): PurchaseDao
    abstract fun expenses(): ExpenseDao

    companion object {
        // Schema changes must be additive migrations: shop data on the phone is never wiped.
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE product ADD COLUMN category TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE staff ADD COLUMN role TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE customer ADD COLUMN type TEXT NOT NULL DEFAULT 'RETAIL'")
                db.execSQL("ALTER TABLE customer ADD COLUMN address TEXT NOT NULL DEFAULT ''")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `due_payment` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `number` TEXT NOT NULL, " +
                        "`customerId` INTEGER NOT NULL, `amountPaisa` INTEGER NOT NULL, `method` TEXT NOT NULL, `note` TEXT NOT NULL, " +
                        "`staffId` INTEGER, `createdAt` INTEGER NOT NULL)"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_due_payment_customerId` ON `due_payment` (`customerId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_due_payment_createdAt` ON `due_payment` (`createdAt`)")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `supplier` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, " +
                        "`phone` TEXT NOT NULL, `address` TEXT NOT NULL, `duePaisa` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL)"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_supplier_phone` ON `supplier` (`phone`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_supplier_name` ON `supplier` (`name`)")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `purchase` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `number` TEXT NOT NULL, " +
                        "`supplierId` INTEGER, `subtotalPaisa` INTEGER NOT NULL, `discountPaisa` INTEGER NOT NULL, `totalPaisa` INTEGER NOT NULL, " +
                        "`paidPaisa` INTEGER NOT NULL, `method` TEXT NOT NULL, `staffId` INTEGER, `createdAt` INTEGER NOT NULL)"
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_purchase_number` ON `purchase` (`number`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_purchase_createdAt` ON `purchase` (`createdAt`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_purchase_supplierId` ON `purchase` (`supplierId`)")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `purchase_item` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `purchaseId` INTEGER NOT NULL, " +
                        "`productId` INTEGER NOT NULL, `name` TEXT NOT NULL, `qtyMilli` INTEGER NOT NULL, `unitCostPaisa` INTEGER NOT NULL, " +
                        "`lineTotalPaisa` INTEGER NOT NULL, " +
                        "FOREIGN KEY(`purchaseId`) REFERENCES `purchase`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_purchase_item_purchaseId` ON `purchase_item` (`purchaseId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_purchase_item_productId` ON `purchase_item` (`productId`)")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `supplier_payment` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `number` TEXT NOT NULL, " +
                        "`supplierId` INTEGER NOT NULL, `amountPaisa` INTEGER NOT NULL, `method` TEXT NOT NULL, `note` TEXT NOT NULL, " +
                        "`staffId` INTEGER, `createdAt` INTEGER NOT NULL)"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_supplier_payment_supplierId` ON `supplier_payment` (`supplierId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_supplier_payment_createdAt` ON `supplier_payment` (`createdAt`)")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `expense` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `number` TEXT NOT NULL, " +
                        "`category` TEXT NOT NULL, `amountPaisa` INTEGER NOT NULL, `method` TEXT NOT NULL, `note` TEXT NOT NULL, " +
                        "`staffId` INTEGER, `createdAt` INTEGER NOT NULL)"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_expense_createdAt` ON `expense` (`createdAt`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_expense_category` ON `expense` (`category`)")
            }
        }

        fun open(context: Context): AppDatabase {
            System.loadLibrary("sqlcipher")
            val factory = SupportOpenHelperFactory(DbKey.get(context))
            return Room.databaseBuilder(context, AppDatabase::class.java, "shop.db")
                .openHelperFactory(factory)
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                .build()
        }
    }
}
