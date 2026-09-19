package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions WHERE accountMode = :accountMode ORDER BY date DESC, time DESC, id DESC")
    fun getTransactionsByMode(accountMode: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE accountMode = :accountMode AND date LIKE :monthPrefix || '%' ORDER BY date DESC, id DESC")
    fun getTransactionsForMonth(accountMode: String, monthPrefix: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE isPendingParse = 1 ORDER BY createdAt ASC")
    fun getPendingTransactionsFlow(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE isPendingParse = 1 ORDER BY createdAt ASC")
    suspend fun getPendingTransactions(): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE id = :id")
    fun getTransactionById(id: Long): Flow<TransactionEntity?>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getTransactionByIdDirect(id: Long): TransactionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Delete
    suspend fun deleteTransaction(transaction: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransactionById(id: Long)

    @Query("SELECT COUNT(*) FROM transactions WHERE isPendingParse = 1")
    fun getPendingCount(): Flow<Int>
}
