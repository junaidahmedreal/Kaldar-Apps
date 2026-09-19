package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.dao.BudgetDao
import com.example.data.local.dao.CategoryDao
import com.example.data.local.dao.RoomDao
import com.example.data.local.dao.TransactionDao
import com.example.data.local.entity.BudgetEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.RoomEntity
import com.example.data.local.entity.RoomExpenseEntity
import com.example.data.local.entity.RoomMemberEntity
import com.example.data.local.entity.TransactionEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        TransactionEntity::class,
        BudgetEntity::class,
        CategoryEntity::class,
        RoomEntity::class,
        RoomMemberEntity::class,
        RoomExpenseEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun budgetDao(): BudgetDao
    abstract fun categoryDao(): CategoryDao
    abstract fun roomDao(): RoomDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope = CoroutineScope(Dispatchers.IO)): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "enexpense_database.db"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(DatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }

        val DEFAULT_CATEGORIES = listOf(
            CategoryEntity(name = "Food", icon = "restaurant", color = 0xFFEF4444L, isDefault = true),
            CategoryEntity(name = "Groceries", icon = "shopping_cart", color = 0xFF10B981L, isDefault = true),
            CategoryEntity(name = "Fuel", icon = "local_gas_station", color = 0xFFF59E0BL, isDefault = true),
            CategoryEntity(name = "Medical", icon = "medical_services", color = 0xFF06B6D4L, isDefault = true),
            CategoryEntity(name = "Travel", icon = "directions_car", color = 0xFF3B82F6L, isDefault = true),
            CategoryEntity(name = "Shopping", icon = "storefront", color = 0xFF8B5CF6L, isDefault = true),
            CategoryEntity(name = "Utilities", icon = "receipt_long", color = 0xFFEC4899L, isDefault = true),
            CategoryEntity(name = "Entertainment", icon = "movie", color = 0xFFF97316L, isDefault = true),
            CategoryEntity(name = "Office", icon = "work", color = 0xFF64748BL, isDefault = true),
            CategoryEntity(name = "Other", icon = "category", color = 0xFF94A3B8L, isDefault = true)
        )

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch {
                        database.categoryDao().insertAll(DEFAULT_CATEGORIES)
                    }
                }
            }
        }
    }
}
