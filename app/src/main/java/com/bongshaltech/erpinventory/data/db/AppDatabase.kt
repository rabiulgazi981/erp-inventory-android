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
        StockJobEntity::class, StockJobInputEntity::class, ProductSerialEntity::class, WarehouseEntity::class, WarehouseStockEntity::class,
        AuditLogEntity::class,
        DeletedSaleEntity::class, SaleDeleteRequestEntity::class, InstallmentEntity::class,
        StockAdjustEntity::class, HrLockEntity::class,
        InvestorEntity::class, LedgerAccountEntity::class, JournalEntryEntity::class, JournalLineEntity::class, RecurringExpenseEntity::class,
        RecipeEntity::class, RecipeItemEntity::class, WorkOrderEntity::class, WorkLineEntity::class, KotEntity::class,
        CourierParcelEntity::class,
        PurchasePayEntity::class, TailorWorkerEntity::class, TailorMeasureEntity::class, TailorOrderEntity::class, TailorItemEntity::class,
        TailorTaskEntity::class, WorkerPayEntity::class, GLineEntity::class, GWorkerEntity::class, GStyleEntity::class, GOrderEntity::class,
        GCellEntity::class, GOutputEntity::class, GIssueEntity::class,
    ],
    version = 24,
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
    abstract fun stockTools(): StockToolsDao
    abstract fun audit(): AuditDao
    abstract fun saleFix(): SaleFixDao
    abstract fun stockMore(): StockMoreDao
    abstract fun books(): BooksDao
    abstract fun making(): MakingDao
    abstract fun kitchen(): KitchenDao
    abstract fun couriers(): CourierDao
    abstract fun reportSet(): ReportSetDao
    abstract fun tailor(): TailorDao
    abstract fun garments(): GarmentDao

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

        private val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `stock_job` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `number` TEXT NOT NULL, `outProductId` INTEGER NOT NULL, " +
                        "`outName` TEXT NOT NULL, `outQtyMilli` INTEGER NOT NULL, `costPaisa` INTEGER NOT NULL, `note` TEXT NOT NULL, `staffId` INTEGER, `createdAt` INTEGER NOT NULL)"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_stock_job_createdAt` ON `stock_job` (`createdAt`)")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `stock_job_input` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `jobId` INTEGER NOT NULL, `productId` INTEGER NOT NULL, " +
                        "`name` TEXT NOT NULL, `qtyMilli` INTEGER NOT NULL, `unitCostPaisa` INTEGER NOT NULL, " +
                        "FOREIGN KEY(`jobId`) REFERENCES `stock_job`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_stock_job_input_jobId` ON `stock_job_input` (`jobId`)")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `product_serial` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `productId` INTEGER NOT NULL, " +
                        "`serial` TEXT NOT NULL, `saleId` INTEGER, `soldAt` INTEGER, `createdAt` INTEGER NOT NULL)"
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_product_serial_serial` ON `product_serial` (`serial`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_product_serial_productId` ON `product_serial` (`productId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_product_serial_saleId` ON `product_serial` (`saleId`)")
                db.execSQL("CREATE TABLE IF NOT EXISTS `warehouse` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `active` INTEGER NOT NULL)")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `warehouse_stock` (`warehouseId` INTEGER NOT NULL, `productId` INTEGER NOT NULL, `qtyMilli` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`warehouseId`, `productId`))"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_warehouse_stock_productId` ON `warehouse_stock` (`productId`)")
            }
        }

        private val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `audit_log` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `at` INTEGER NOT NULL, `staffId` INTEGER, " +
                        "`action` TEXT NOT NULL, `detail` TEXT NOT NULL)"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_audit_log_at` ON `audit_log` (`at`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_audit_log_action` ON `audit_log` (`action`)")
            }
        }

        private val MIGRATION_15_16 = object : Migration(15, 16) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `deleted_sale` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `saleId` INTEGER NOT NULL, " +
                        "`number` TEXT NOT NULL, `customerId` INTEGER, `totalPaisa` INTEGER NOT NULL, `paidPaisa` INTEGER NOT NULL, " +
                        "`method` TEXT NOT NULL, `saleAt` INTEGER NOT NULL, `itemsText` TEXT NOT NULL, `reason` TEXT NOT NULL, " +
                        "`requestedBy` INTEGER, `deletedAt` INTEGER NOT NULL)"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_deleted_sale_deletedAt` ON `deleted_sale` (`deletedAt`)")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `sale_delete_request` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `saleId` INTEGER NOT NULL, " +
                        "`number` TEXT NOT NULL, `reason` TEXT NOT NULL, `staffId` INTEGER, `createdAt` INTEGER NOT NULL)"
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_sale_delete_request_saleId` ON `sale_delete_request` (`saleId`)")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `installment` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `customerId` INTEGER NOT NULL, " +
                        "`saleNumber` TEXT NOT NULL, `seq` INTEGER NOT NULL, `dueDay` INTEGER NOT NULL, `amountPaisa` INTEGER NOT NULL, " +
                        "`paidPaisa` INTEGER NOT NULL)"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_installment_customerId` ON `installment` (`customerId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_installment_saleNumber` ON `installment` (`saleNumber`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_installment_dueDay` ON `installment` (`dueDay`)")
            }
        }

        private val MIGRATION_16_17 = object : Migration(16, 17) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE product ADD COLUMN packMilli INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE product ADD COLUMN packName TEXT NOT NULL DEFAULT ''")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `stock_adjust` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `productId` INTEGER NOT NULL, " +
                        "`qtyMilli` INTEGER NOT NULL, `reason` TEXT NOT NULL, `note` TEXT NOT NULL, `costPaisa` INTEGER NOT NULL, " +
                        "`staffId` INTEGER, `createdAt` INTEGER NOT NULL)"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_stock_adjust_productId` ON `stock_adjust` (`productId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_stock_adjust_createdAt` ON `stock_adjust` (`createdAt`)")
            }
        }

        private val MIGRATION_17_18 = object : Migration(17, 18) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE customer ADD COLUMN creditLimitPaisa INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE customer ADD COLUMN area TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE customer ADD COLUMN points INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE sale ADD COLUMN pointsEarned INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE sale ADD COLUMN pointsUsed INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_18_19 = object : Migration(18, 19) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE employee ADD COLUMN department TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE employee ADD COLUMN shift TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE employee ADD COLUMN leaveDays INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE salary_payment ADD COLUMN kind TEXT NOT NULL DEFAULT 'SALARY'")
                db.execSQL("CREATE TABLE IF NOT EXISTS `hr_lock` (`month` INTEGER NOT NULL, `lockedAt` INTEGER NOT NULL, PRIMARY KEY(`month`))")
            }
        }

        private val MIGRATION_19_20 = object : Migration(19, 20) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE money_move ADD COLUMN investorId INTEGER")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `investor` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, " +
                        "`phone` TEXT NOT NULL, `shareBp` INTEGER NOT NULL, `note` TEXT NOT NULL, `createdAt` INTEGER NOT NULL)"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `ledger_account` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, " +
                        "`type` TEXT NOT NULL, `active` INTEGER NOT NULL)"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `journal_entry` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `number` TEXT NOT NULL, " +
                        "`day` INTEGER NOT NULL, `note` TEXT NOT NULL, `staffId` INTEGER, `createdAt` INTEGER NOT NULL)"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_journal_entry_day` ON `journal_entry` (`day`)")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `journal_line` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `entryId` INTEGER NOT NULL, " +
                        "`accountId` INTEGER NOT NULL, `debitPaisa` INTEGER NOT NULL, `creditPaisa` INTEGER NOT NULL, " +
                        "FOREIGN KEY(`entryId`) REFERENCES `journal_entry`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_journal_line_entryId` ON `journal_line` (`entryId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_journal_line_accountId` ON `journal_line` (`accountId`)")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `recurring_expense` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `category` TEXT NOT NULL, " +
                        "`amountPaisa` INTEGER NOT NULL, `method` TEXT NOT NULL, `dayOfMonth` INTEGER NOT NULL, `note` TEXT NOT NULL, " +
                        "`lastMonth` INTEGER NOT NULL, `active` INTEGER NOT NULL)"
                )
            }
        }

        private val MIGRATION_20_21 = object : Migration(20, 21) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `recipe` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `productId` INTEGER NOT NULL, " +
                        "`name` TEXT NOT NULL, `outMilli` INTEGER NOT NULL, `note` TEXT NOT NULL, `active` INTEGER NOT NULL)"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_recipe_productId` ON `recipe` (`productId`)")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `recipe_item` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `recipeId` INTEGER NOT NULL, " +
                        "`productId` INTEGER NOT NULL, `name` TEXT NOT NULL, `qtyMilli` INTEGER NOT NULL, " +
                        "FOREIGN KEY(`recipeId`) REFERENCES `recipe`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_recipe_item_recipeId` ON `recipe_item` (`recipeId`)")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `work_order` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `number` TEXT NOT NULL, " +
                        "`kind` TEXT NOT NULL, `supplierId` INTEGER NOT NULL, `status` TEXT NOT NULL, `note` TEXT NOT NULL, " +
                        "`chargePaisa` INTEGER NOT NULL, `staffId` INTEGER, `createdAt` INTEGER NOT NULL)"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_work_order_supplierId` ON `work_order` (`supplierId`)")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `work_line` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `orderId` INTEGER NOT NULL, " +
                        "`productId` INTEGER NOT NULL, `name` TEXT NOT NULL, `qtyMilli` INTEGER NOT NULL, `ratePaisa` INTEGER NOT NULL, " +
                        "`doneMilli` INTEGER NOT NULL, `scrapMilli` INTEGER NOT NULL, " +
                        "FOREIGN KEY(`orderId`) REFERENCES `work_order`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_work_line_orderId` ON `work_line` (`orderId`)")
            }
        }

        private val MIGRATION_21_22 = object : Migration(21, 22) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE sale ADD COLUMN patientName TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE sale ADD COLUMN doctorName TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE sale ADD COLUMN prescriptionNo TEXT NOT NULL DEFAULT ''")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `kot` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `number` TEXT NOT NULL, " +
                        "`tableName` TEXT NOT NULL, `itemsText` TEXT NOT NULL, `status` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `readyAt` INTEGER)"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_kot_status` ON `kot` (`status`)")
            }
        }

        private val MIGRATION_22_23 = object : Migration(22, 23) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `courier_parcel` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `saleId` INTEGER NOT NULL, " +
                        "`saleNumber` TEXT NOT NULL, `courier` TEXT NOT NULL, `consignmentId` TEXT NOT NULL, `trackingCode` TEXT NOT NULL, " +
                        "`status` TEXT NOT NULL, `codPaisa` INTEGER NOT NULL, `name` TEXT NOT NULL, `phone` TEXT NOT NULL, `address` TEXT NOT NULL, " +
                        "`createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL)"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_courier_parcel_saleId` ON `courier_parcel` (`saleId`)")
            }
        }

        private val MIGRATION_23_24 = object : Migration(23, 24) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Product codes P0001… for every existing product, in the order they were made.
                db.execSQL("ALTER TABLE product ADD COLUMN code TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE product ADD COLUMN groupId INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE product ADD COLUMN variant TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE product ADD COLUMN service INTEGER NOT NULL DEFAULT 0")
                db.execSQL("UPDATE product SET code = 'P' || substr('0000' || (SELECT COUNT(*) FROM product p2 WHERE p2.id <= product.id), -4, 4) WHERE (SELECT COUNT(*) FROM product p2 WHERE p2.id <= product.id) < 10000")
                db.execSQL("UPDATE product SET code = 'P' || (SELECT COUNT(*) FROM product p2 WHERE p2.id <= product.id) WHERE code = ''")
                db.execSQL("INSERT OR REPLACE INTO doc_counter (type, year, last) VALUES ('PRODUCT_CODE', 0, (SELECT COUNT(*) FROM product))")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_product_code` ON `product` (`code`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_product_groupId` ON `product` (`groupId`)")
                db.execSQL("ALTER TABLE mfs_wallet ADD COLUMN kind TEXT NOT NULL DEFAULT 'AGENT'")
                db.execSQL("UPDATE mfs_wallet SET kind = 'PERSONAL' WHERE personal = 1")
                db.execSQL("CREATE TABLE IF NOT EXISTS `purchase_pay` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `purchaseId` INTEGER NOT NULL, `method` TEXT NOT NULL, `amountPaisa` INTEGER NOT NULL)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_purchase_pay_purchaseId` ON `purchase_pay` (`purchaseId`)")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `tailor_worker` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `phone` TEXT NOT NULL, " +
                        "`rates` TEXT NOT NULL, `note` TEXT NOT NULL, `active` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL)"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `tailor_measure` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `customerId` INTEGER NOT NULL, `garment` TEXT NOT NULL, " +
                        "`measurements` TEXT NOT NULL, `notes` TEXT NOT NULL, `updatedAt` INTEGER NOT NULL)"
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_tailor_measure_customerId_garment` ON `tailor_measure` (`customerId`, `garment`)")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `tailor_order` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `number` TEXT NOT NULL, `customerId` INTEGER NOT NULL, " +
                        "`saleId` INTEGER NOT NULL, `deliveryDay` INTEGER, `status` TEXT NOT NULL, `note` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `deliveredAt` INTEGER)"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_tailor_order_saleId` ON `tailor_order` (`saleId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_tailor_order_customerId` ON `tailor_order` (`customerId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_tailor_order_status` ON `tailor_order` (`status`)")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `tailor_item` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `orderId` INTEGER NOT NULL, `garment` TEXT NOT NULL, " +
                        "`garmentName` TEXT NOT NULL, `qty` INTEGER NOT NULL, `makingPaisa` INTEGER NOT NULL, `measurements` TEXT NOT NULL, `style` TEXT NOT NULL, " +
                        "`fabricProductId` INTEGER, `fabricName` TEXT NOT NULL, `fabricMilli` INTEGER NOT NULL, `fabricPricePaisa` INTEGER NOT NULL, " +
                        "FOREIGN KEY(`orderId`) REFERENCES `tailor_order`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_tailor_item_orderId` ON `tailor_item` (`orderId`)")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `tailor_task` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `orderId` INTEGER NOT NULL, `itemId` INTEGER NOT NULL, " +
                        "`workerId` INTEGER NOT NULL, `task` TEXT NOT NULL, `qty` INTEGER NOT NULL, `ratePaisa` INTEGER NOT NULL, `amountPaisa` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL)"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_tailor_task_orderId` ON `tailor_task` (`orderId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_tailor_task_workerId` ON `tailor_task` (`workerId`)")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `worker_pay` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `trade` TEXT NOT NULL, `workerId` INTEGER NOT NULL, " +
                        "`number` TEXT NOT NULL, `type` TEXT NOT NULL, `amountPaisa` INTEGER NOT NULL, `method` TEXT NOT NULL, `note` TEXT NOT NULL, `createdAt` INTEGER NOT NULL)"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_worker_pay_trade_workerId` ON `worker_pay` (`trade`, `workerId`)")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `g_line` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `supervisor` TEXT NOT NULL, " +
                        "`dailyTarget` INTEGER NOT NULL, `workers` INTEGER NOT NULL, `active` INTEGER NOT NULL)"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `g_worker` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `phone` TEXT NOT NULL, " +
                        "`role` TEXT NOT NULL, `lineId` INTEGER, `active` INTEGER NOT NULL)"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_g_worker_lineId` ON `g_worker` (`lineId`)")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `g_style` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `code` TEXT NOT NULL, `name` TEXT NOT NULL, `customerId` INTEGER, " +
                        "`sizes` TEXT NOT NULL, `colors` TEXT NOT NULL, `pricePaisa` INTEGER NOT NULL, `materials` TEXT NOT NULL, `operations` TEXT NOT NULL, " +
                        "`otherCosts` TEXT NOT NULL, `groupId` INTEGER NOT NULL, `active` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL)"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_g_style_groupId` ON `g_style` (`groupId`)")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `g_order` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `number` TEXT NOT NULL, `styleId` INTEGER NOT NULL, `customerId` INTEGER, " +
                        "`buyerPo` TEXT NOT NULL, `deliveryDay` INTEGER, `unitPricePaisa` INTEGER NOT NULL, `status` TEXT NOT NULL, `note` TEXT NOT NULL, " +
                        "`wipPaisa` INTEGER NOT NULL, `writeOffPaisa` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL)"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_g_order_styleId` ON `g_order` (`styleId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_g_order_status` ON `g_order` (`status`)")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `g_cell` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `orderId` INTEGER NOT NULL, `size` TEXT NOT NULL, " +
                        "`color` TEXT NOT NULL, `qty` INTEGER NOT NULL, `packed` INTEGER NOT NULL, `delivered` INTEGER NOT NULL, " +
                        "FOREIGN KEY(`orderId`) REFERENCES `g_order`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_g_cell_orderId` ON `g_cell` (`orderId`)")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `g_output` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `orderId` INTEGER NOT NULL, `stage` TEXT NOT NULL, " +
                        "`lineId` INTEGER, `workerId` INTEGER, `operation` TEXT NOT NULL, `ratePaisa` INTEGER NOT NULL, `amountPaisa` INTEGER NOT NULL, " +
                        "`size` TEXT NOT NULL, `color` TEXT NOT NULL, `qty` INTEGER NOT NULL, `rejects` INTEGER NOT NULL, `day` INTEGER NOT NULL, " +
                        "`note` TEXT NOT NULL, `createdAt` INTEGER NOT NULL)"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_g_output_orderId` ON `g_output` (`orderId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_g_output_workerId` ON `g_output` (`workerId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_g_output_day` ON `g_output` (`day`)")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `g_issue` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `orderId` INTEGER NOT NULL, `productId` INTEGER NOT NULL, " +
                        "`name` TEXT NOT NULL, `qtyMilli` INTEGER NOT NULL, `costPaisa` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL)"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_g_issue_orderId` ON `g_issue` (`orderId`)")
            }
        }

        fun open(context: Context): AppDatabase {
            System.loadLibrary("sqlcipher")
            val factory = SupportOpenHelperFactory(DbKey.get(context))
            return Room.databaseBuilder(context, AppDatabase::class.java, "shop.db")
                .openHelperFactory(factory)
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14, MIGRATION_14_15, MIGRATION_15_16, MIGRATION_16_17, MIGRATION_17_18, MIGRATION_18_19, MIGRATION_19_20, MIGRATION_20_21, MIGRATION_21_22, MIGRATION_22_23, MIGRATION_23_24)
                .build()
        }
    }
}
