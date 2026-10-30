package com.bongshaltech.erpinventory.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.bongshaltech.erpinventory.security.DbKey
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

class Converters {
    @TypeConverter fun payToString(m: PayMethod) = m.name
    @TypeConverter fun stringToPay(s: String) = PayMethod.valueOf(s)
}

@Database(
    entities = [
        ProductEntity::class, CustomerEntity::class, SaleEntity::class, SaleItemEntity::class,
        StaffEntity::class, DocCounterEntity::class,
    ],
    version = 1,
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
        fun open(context: Context): AppDatabase {
            System.loadLibrary("sqlcipher")
            val factory = SupportOpenHelperFactory(DbKey.get(context))
            return Room.databaseBuilder(context, AppDatabase::class.java, "shop.db")
                .openHelperFactory(factory)
                .build()
        }
    }
}
