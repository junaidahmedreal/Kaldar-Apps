package com.example.data.repository

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.local.entity.RoomEntity
import com.example.data.local.entity.RoomExpenseEntity
import com.example.data.local.entity.RoomMemberEntity
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.math.abs

// Model for the settlement slip
data class MemberBalance(
    val memberName: String,
    val totalPaid: Double,
    val fairShare: Double,
    val netBalance: Double // > 0 means gets back, < 0 means owes
)

data class DebtTransfer(
    val fromMember: String,
    val toMember: String,
    val amount: Double
)

data class RoomSettlementSlip(
    val roomId: Long,
    val roomName: String,
    val monthYear: String, // e.g. "2026-09"
    val currency: String,
    val totalSpent: Double,
    val perPersonShare: Double,
    val memberCount: Int,
    val balances: List<MemberBalance>,
    val transfers: List<DebtTransfer>,
    val generatedAt: Long = System.currentTimeMillis()
)

class RoomRepository(
    private val context: Context,
    private val database: AppDatabase = AppDatabase.getDatabase(context)
) {
    private val roomDao = database.roomDao()

    fun getAllRooms(): Flow<List<RoomEntity>> = roomDao.getAllRooms()

    fun getRoom(roomId: Long): Flow<RoomEntity?> = roomDao.getRoomByIdFlow(roomId)

    fun getMembers(roomId: Long): Flow<List<RoomMemberEntity>> = roomDao.getMembersForRoom(roomId)

    fun getExpenses(roomId: Long): Flow<List<RoomExpenseEntity>> = roomDao.getExpensesForRoom(roomId)

    fun getExpensesForMonth(roomId: Long, monthYear: String): Flow<List<RoomExpenseEntity>> =
        roomDao.getExpensesForRoomMonth(roomId, monthYear)

    fun getTotalSpentForMonth(roomId: Long, monthYear: String): Flow<Double> =
        roomDao.getTotalSpentForRoomMonth(roomId, monthYear)

    suspend fun createRoom(
        name: String,
        adminName: String = "You",
        initialFriends: List<String> = emptyList(),
        currency: String = "$"
    ): Long {
        val inviteCode = "EN-" + UUID.randomUUID().toString().substring(0, 6).uppercase(Locale.US)
        val room = RoomEntity(
            name = name.trim(),
            adminName = adminName.trim(),
            inviteCode = inviteCode,
            currency = currency
        )
        val roomId = roomDao.insertRoom(room)

        // Add admin member
        val adminMember = RoomMemberEntity(
            roomId = roomId,
            name = if (adminName.isNotBlank()) adminName.trim() else "You",
            isAdmin = true
        )
        roomDao.insertMember(adminMember)

        // Add initial friends
        initialFriends.filter { it.isNotBlank() }.forEach { friendName ->
            roomDao.insertMember(
                RoomMemberEntity(
                    roomId = roomId,
                    name = friendName.trim(),
                    isAdmin = false
                )
            )
        }

        return roomId
    }

    suspend fun addMember(roomId: Long, memberName: String, phoneOrEmail: String = ""): Long {
        val member = RoomMemberEntity(
            roomId = roomId,
            name = memberName.trim(),
            isAdmin = false,
            phoneOrEmail = phoneOrEmail.trim()
        )
        return roomDao.insertMember(member)
    }

    suspend fun removeMember(memberId: Long) {
        roomDao.deleteMemberById(memberId)
    }

    suspend fun addExpense(
        roomId: Long,
        title: String,
        amount: Double,
        paidBy: String,
        category: String = "General",
        date: String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()),
        currency: String = "$",
        notes: String = ""
    ): Long {
        val monthYear = if (date.length >= 7) date.substring(0, 7) else SimpleDateFormat("yyyy-MM", Locale.US).format(Date())
        val expense = RoomExpenseEntity(
            roomId = roomId,
            title = title.trim(),
            amount = amount,
            paidByMemberName = paidBy.trim(),
            category = category,
            date = date,
            monthYear = monthYear,
            currency = currency,
            notes = notes.trim()
        )
        return roomDao.insertExpense(expense)
    }

    suspend fun deleteExpense(expenseId: Long) {
        roomDao.deleteExpenseById(expenseId)
    }

    suspend fun deleteRoom(roomId: Long) {
        roomDao.deleteRoomById(roomId)
    }

    /**
     * Generates a monthly Settlement Slip:
     * 1. Totals all expenses in the month
     * 2. Finds who paid what
     * 3. Calculates equal fair share (total / num_members)
     * 4. Determines Net Balance (paid - fairShare)
     * 5. Solves transfers: debtors pay creditors so everyone's balance becomes equal
     */
    suspend fun calculateSettlementSlip(roomId: Long, monthYear: String): RoomSettlementSlip {
        val room = roomDao.getRoomById(roomId) ?: RoomEntity(name = "Room", currency = "$")
        val members = roomDao.getMembersForRoomSync(roomId)
        val expenses = roomDao.getExpensesForRoomMonthSync(roomId, monthYear)

        val totalSpent = expenses.sumOf { it.amount }
        val memberCount = if (members.isNotEmpty()) members.size else 1
        val perPersonShare = if (memberCount > 0) totalSpent / memberCount else 0.0

        // Map member name to total paid
        val paidMap = members.associate { it.name to 0.0 }.toMutableMap()
        for (expense in expenses) {
            val current = paidMap[expense.paidByMemberName] ?: 0.0
            paidMap[expense.paidByMemberName] = current + expense.amount
        }

        // Calculate balances
        val balances = members.map { member ->
            val paid = paidMap[member.name] ?: 0.0
            val net = paid - perPersonShare
            MemberBalance(
                memberName = member.name,
                totalPaid = paid,
                fairShare = perPersonShare,
                netBalance = net
            )
        }

        // Debt settling algorithm (greedy matching debtors with creditors)
        class Participant(val name: String, var amount: Double)

        val debtors = mutableListOf<Participant>()
        val creditors = mutableListOf<Participant>()

        for (b in balances) {
            // Rounded to 2 decimal places to prevent float precision drift
            val rounded = kotlin.math.round(b.netBalance * 100) / 100.0
            if (rounded < -0.01) {
                debtors.add(Participant(b.memberName, abs(rounded)))
            } else if (rounded > 0.01) {
                creditors.add(Participant(b.memberName, rounded))
            }
        }

        val transfers = mutableListOf<DebtTransfer>()
        var dIndex = 0
        var cIndex = 0

        while (dIndex < debtors.size && cIndex < creditors.size) {
            val debtor = debtors[dIndex]
            val creditor = creditors[cIndex]

            val settleAmount = minOf(debtor.amount, creditor.amount)
            if (settleAmount > 0.01) {
                transfers.add(
                    DebtTransfer(
                        fromMember = debtor.name,
                        toMember = creditor.name,
                        amount = settleAmount
                    )
                )
            }

            debtor.amount -= settleAmount
            creditor.amount -= settleAmount

            if (debtor.amount <= 0.01) dIndex++
            if (creditor.amount <= 0.01) cIndex++
        }

        return RoomSettlementSlip(
            roomId = roomId,
            roomName = room.name,
            monthYear = monthYear,
            currency = room.currency,
            totalSpent = totalSpent,
            perPersonShare = perPersonShare,
            memberCount = memberCount,
            balances = balances,
            transfers = transfers
        )
    }
}
