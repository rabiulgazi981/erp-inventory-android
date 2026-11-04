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
    ],
    version = 3,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun products(): ProductDao
    abstract fun customers(): CustomerDao
    abstract fun sales(): SaleDao
    abstract fun staff(): StaffDao
    abstract fun docCounters(): DocCounterDao

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

        fun open(context: Context): AppDatabase {
            System.loadLibrary("sqlcipher")
            val factory = SupportOpenHelperFactory(DbKey.get(context))
            return Room.databaseBuilder(context, AppDatabase::class.java, "shop.db")
                .openHelperFactory(factory)
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .build()
        }
    }
}
