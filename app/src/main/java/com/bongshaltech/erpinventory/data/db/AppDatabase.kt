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
        ExpenseEntity::class, MfsWalletEntity::class, MfsTxnEntity::class,
        SaleReturnEntity::class, SaleReturnItemEntity::class, PurchaseReturnEntity::class, PurchaseReturnItemEntity::class,
        OrderDocEntity::class, OrderDocItemEntity::class,
        MoneyMoveEntity::class, LoanEntity::class, CashSessionEntity::class,
        PromoEntity::class, SalePromoEntity::class,
        EmployeeEntity::class, AttendanceEntity::class, SalaryPaymentEntity::class,
        StockBatchEntity::class, DiningTableEntity::class, TableOrderEntity::class, TableOrderItemEntity::class,
    ],
    version = 13,
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
    abstract fun reports(): ReportDao
    abstract fun mfs(): MfsDao
    abstract fun returns(): ReturnDao
    abstract fun orders(): OrderDao
    abstract fun money(): MoneyDao
    abstract fun promos(): PromoDao
    abstract fun hr(): HrDao
    abstract fun batches(): BatchDao
    abstract fun tables(): TableDao

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

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE sale_item ADD COLUMN unitCostPaisa INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `mfs_wallet` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `provider` TEXT NOT NULL, " +
                        "`personal` INTEGER NOT NULL, `number` TEXT NOT NULL, `balancePaisa` INTEGER NOT NULL, `cashInPct` REAL NOT NULL, " +
                        "`cashOutPct` REAL NOT NULL, `paymentPct` REAL NOT NULL, `loadPct` REAL NOT NULL, `active` INTEGER NOT NULL)"
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_mfs_wallet_provider` ON `mfs_wallet` (`provider`)")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `mfs_txn` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `number` TEXT NOT NULL, " +
                        "`walletId` INTEGER NOT NULL, `type` TEXT NOT NULL, `customerPhone` TEXT NOT NULL, `amountPaisa` INTEGER NOT NULL, " +
                        "`chargePaisa` INTEGER NOT NULL, `feePaisa` INTEGER NOT NULL, `walletDeltaPaisa` INTEGER NOT NULL, " +
                        "`cashDeltaPaisa` INTEGER NOT NULL, `earningPaisa` INTEGER NOT NULL, `via` TEXT NOT NULL, `trxId` TEXT NOT NULL, " +
                        "`note` TEXT NOT NULL, `staffId` INTEGER, `createdAt` INTEGER NOT NULL)"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_mfs_txn_walletId` ON `mfs_txn` (`walletId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_mfs_txn_createdAt` ON `mfs_txn` (`createdAt`)")
            }
        }

        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `sale_return` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `number` TEXT NOT NULL, " +
                        "`saleId` INTEGER NOT NULL, `customerId` INTEGER, `totalPaisa` INTEGER NOT NULL, `dueCutPaisa` INTEGER NOT NULL, " +
                        "`refundPaisa` INTEGER NOT NULL, `method` TEXT NOT NULL, `costPaisa` INTEGER NOT NULL, `note` TEXT NOT NULL, " +
                        "`staffId` INTEGER, `createdAt` INTEGER NOT NULL)"
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_sale_return_number` ON `sale_return` (`number`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_sale_return_saleId` ON `sale_return` (`saleId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_sale_return_customerId` ON `sale_return` (`customerId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_sale_return_createdAt` ON `sale_return` (`createdAt`)")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `sale_return_item` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `returnId` INTEGER NOT NULL, " +
                        "`saleItemId` INTEGER NOT NULL, `productId` INTEGER NOT NULL, `name` TEXT NOT NULL, `qtyMilli` INTEGER NOT NULL, " +
                        "`lineTotalPaisa` INTEGER NOT NULL, `unitCostPaisa` INTEGER NOT NULL, " +
                        "FOREIGN KEY(`returnId`) REFERENCES `sale_return`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_sale_return_item_returnId` ON `sale_return_item` (`returnId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_sale_return_item_saleItemId` ON `sale_return_item` (`saleItemId`)")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `purchase_return` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `number` TEXT NOT NULL, " +
                        "`purchaseId` INTEGER NOT NULL, `supplierId` INTEGER, `totalPaisa` INTEGER NOT NULL, `dueCutPaisa` INTEGER NOT NULL, " +
                        "`refundPaisa` INTEGER NOT NULL, `method` TEXT NOT NULL, `note` TEXT NOT NULL, `staffId` INTEGER, `createdAt` INTEGER NOT NULL)"
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_purchase_return_number` ON `purchase_return` (`number`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_purchase_return_purchaseId` ON `purchase_return` (`purchaseId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_purchase_return_supplierId` ON `purchase_return` (`supplierId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_purchase_return_createdAt` ON `purchase_return` (`createdAt`)")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `purchase_return_item` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `returnId` INTEGER NOT NULL, " +
                        "`purchaseItemId` INTEGER NOT NULL, `productId` INTEGER NOT NULL, `name` TEXT NOT NULL, `qtyMilli` INTEGER NOT NULL, " +
                        "`lineTotalPaisa` INTEGER NOT NULL, " +
                        "FOREIGN KEY(`returnId`) REFERENCES `purchase_return`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_purchase_return_item_returnId` ON `purchase_return_item` (`returnId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_purchase_return_item_purchaseItemId` ON `purchase_return_item` (`purchaseItemId`)")
            }
        }

        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `order_doc` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `number` TEXT NOT NULL, " +
                        "`kind` TEXT NOT NULL, `status` TEXT NOT NULL, `partyName` TEXT NOT NULL, `partyPhone` TEXT NOT NULL, " +
                        "`supplierId` INTEGER, `subtotalPaisa` INTEGER NOT NULL, `discountPaisa` INTEGER NOT NULL, `totalPaisa` INTEGER NOT NULL, " +
                        "`note` TEXT NOT NULL, `doneAs` TEXT NOT NULL, `staffId` INTEGER, `createdAt` INTEGER NOT NULL)"
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_order_doc_number` ON `order_doc` (`number`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_order_doc_kind` ON `order_doc` (`kind`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_order_doc_createdAt` ON `order_doc` (`createdAt`)")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `order_doc_item` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `docId` INTEGER NOT NULL, " +
                        "`productId` INTEGER NOT NULL, `name` TEXT NOT NULL, `qtyMilli` INTEGER NOT NULL, `unitPricePaisa` INTEGER NOT NULL, " +
                        "`lineTotalPaisa` INTEGER NOT NULL, " +
                        "FOREIGN KEY(`docId`) REFERENCES `order_doc`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_order_doc_item_docId` ON `order_doc_item` (`docId`)")
            }
        }

        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `money_move` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `number` TEXT NOT NULL, " +
                        "`kind` TEXT NOT NULL, `fromMethod` TEXT, `toMethod` TEXT, `amountPaisa` INTEGER NOT NULL, `loanId` INTEGER, " +
                        "`note` TEXT NOT NULL, `staffId` INTEGER, `createdAt` INTEGER NOT NULL)"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_money_move_createdAt` ON `money_move` (`createdAt`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_money_move_loanId` ON `money_move` (`loanId`)")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `loan` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `party` TEXT NOT NULL, " +
                        "`phone` TEXT NOT NULL, `given` INTEGER NOT NULL, `principalPaisa` INTEGER NOT NULL, `outstandingPaisa` INTEGER NOT NULL, " +
                        "`note` TEXT NOT NULL, `createdAt` INTEGER NOT NULL)"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_loan_createdAt` ON `loan` (`createdAt`)")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `cash_session` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `openedAt` INTEGER NOT NULL, " +
                        "`openingPaisa` INTEGER NOT NULL, `closedAt` INTEGER, `expectedPaisa` INTEGER, `countedPaisa` INTEGER, " +
                        "`note` TEXT NOT NULL, `staffId` INTEGER)"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_cash_session_openedAt` ON `cash_session` (`openedAt`)")
            }
        }

        private val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `promo` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `kind` TEXT NOT NULL, `name` TEXT NOT NULL, " +
                        "`code` TEXT NOT NULL, `percentBp` INTEGER NOT NULL, `fixedPaisa` INTEGER NOT NULL, `productId` INTEGER, `category` TEXT NOT NULL, " +
                        "`minPaisa` INTEGER NOT NULL, `balancePaisa` INTEGER NOT NULL, `startAt` INTEGER, `endAt` INTEGER, `maxUses` INTEGER NOT NULL, " +
                        "`uses` INTEGER NOT NULL, `active` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL)"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_promo_kind` ON `promo` (`kind`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_promo_code` ON `promo` (`code`)")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `sale_promo` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `saleId` INTEGER NOT NULL, " +
                        "`promoId` INTEGER, `kind` TEXT NOT NULL, `code` TEXT NOT NULL, `amountPaisa` INTEGER NOT NULL, " +
                        "FOREIGN KEY(`saleId`) REFERENCES `sale`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_sale_promo_saleId` ON `sale_promo` (`saleId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_sale_promo_promoId` ON `sale_promo` (`promoId`)")
            }
        }

        private val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `employee` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `phone` TEXT NOT NULL, " +
                        "`role` TEXT NOT NULL, `salaryPaisa` INTEGER NOT NULL, `active` INTEGER NOT NULL, `joinedAt` INTEGER NOT NULL)"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `attendance` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `employeeId` INTEGER NOT NULL, " +
                        "`day` INTEGER NOT NULL, `status` TEXT NOT NULL)"
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_attendance_employeeId_day` ON `attendance` (`employeeId`, `day`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_attendance_day` ON `attendance` (`day`)")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `salary_payment` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `employeeId` INTEGER NOT NULL, " +
                        "`month` INTEGER NOT NULL, `amountPaisa` INTEGER NOT NULL, `expenseId` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL)"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_salary_payment_employeeId` ON `salary_payment` (`employeeId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_salary_payment_month` ON `salary_payment` (`month`)")
            }
        }

        private val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `product` ADD COLUMN `generic` TEXT NOT NULL DEFAULT ''")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `stock_batch` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `productId` INTEGER NOT NULL, " +
                        "`batchNo` TEXT NOT NULL, `expiryDay` INTEGER, `qtyMilli` INTEGER NOT NULL, `purchaseId` INTEGER, `createdAt` INTEGER NOT NULL)"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_stock_batch_productId` ON `stock_batch` (`productId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_stock_batch_expiryDay` ON `stock_batch` (`expiryDay`)")
                db.execSQL("CREATE TABLE IF NOT EXISTS `dining_table` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `active` INTEGER NOT NULL)")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `table_order` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `tableId` INTEGER NOT NULL, " +
                        "`status` TEXT NOT NULL, `saleNumber` TEXT, `createdAt` INTEGER NOT NULL)"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_table_order_tableId` ON `table_order` (`tableId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_table_order_status` ON `table_order` (`status`)")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `table_order_item` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `orderId` INTEGER NOT NULL, " +
                        "`productId` INTEGER NOT NULL, `name` TEXT NOT NULL, `unitPricePaisa` INTEGER NOT NULL, `qtyMilli` INTEGER NOT NULL, " +
                        "`sentMilli` INTEGER NOT NULL, FOREIGN KEY(`orderId`) REFERENCES `table_order`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_table_order_item_orderId` ON `table_order_item` (`orderId`)")
            }
        }

        fun open(context: Context): AppDatabase {
            System.loadLibrary("sqlcipher")
            val factory = SupportOpenHelperFactory(DbKey.get(context))
            return Room.databaseBuilder(context, AppDatabase::class.java, "shop.db")
                .openHelperFactory(factory)
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13)
                .build()
        }
    }
}
