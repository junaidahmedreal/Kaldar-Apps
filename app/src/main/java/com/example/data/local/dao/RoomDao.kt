package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.RoomEntity
import com.example.data.local.entity.RoomExpenseEntity
import com.example.data.local.entity.RoomMemberEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RoomDao {

    // --- Rooms ---
    @Query("SELECT * FROM rooms ORDER BY createdAt DESC")
    fun getAllRooms(): Flow<List<RoomEntity>>

    @Query("SELECT * FROM rooms WHERE id = :roomId LIMIT 1")
    fun getRoomByIdFlow(roomId: Long): Flow<RoomEntity?>

    @Query("SELECT * FROM rooms WHERE id = :roomId LIMIT 1")
    suspend fun getRoomById(roomId: Long): RoomEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoom(room: RoomEntity): Long

    @Update
    suspend fun updateRoom(room: RoomEntity)

    @Delete
    suspend fun deleteRoom(room: RoomEntity)

    @Query("DELETE FROM rooms WHERE id = :roomId")
    suspend fun deleteRoomById(roomId: Long)

    // --- Room Members ---
    @Query("SELECT * FROM room_members WHERE roomId = :roomId ORDER BY isAdmin DESC, name ASC")
    fun getMembersForRoom(roomId: Long): Flow<List<RoomMemberEntity>>

    @Query("SELECT * FROM room_members WHERE roomId = :roomId")
    suspend fun getMembersForRoomSync(roomId: Long): List<RoomMemberEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMember(member: RoomMemberEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMembers(members: List<RoomMemberEntity>)

    @Delete
    suspend fun deleteMember(member: RoomMemberEntity)

    @Query("DELETE FROM room_members WHERE id = :memberId")
    suspend fun deleteMemberById(memberId: Long)

    // --- Room Expenses ---
    @Query("SELECT * FROM room_expenses WHERE roomId = :roomId ORDER BY date DESC, createdAt DESC")
    fun getExpensesForRoom(roomId: Long): Flow<List<RoomExpenseEntity>>

    @Query("SELECT * FROM room_expenses WHERE roomId = :roomId AND monthYear = :monthYear ORDER BY date DESC, createdAt DESC")
    fun getExpensesForRoomMonth(roomId: Long, monthYear: String): Flow<List<RoomExpenseEntity>>

    @Query("SELECT * FROM room_expenses WHERE roomId = :roomId AND monthYear = :monthYear")
    suspend fun getExpensesForRoomMonthSync(roomId: Long, monthYear: String): List<RoomExpenseEntity>

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM room_expenses WHERE roomId = :roomId AND monthYear = :monthYear")
    fun getTotalSpentForRoomMonth(roomId: Long, monthYear: String): Flow<Double>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: RoomExpenseEntity): Long

    @Delete
    suspend fun deleteExpense(expense: RoomExpenseEntity)

    @Query("DELETE FROM room_expenses WHERE id = :expenseId")
    suspend fun deleteExpenseById(expenseId: Long)
}
