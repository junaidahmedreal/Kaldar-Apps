package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.RoomEntity
import com.example.data.local.entity.RoomExpenseEntity
import com.example.data.local.entity.RoomMemberEntity
import com.example.data.repository.RoomRepository
import com.example.data.repository.RoomSettlementSlip
import com.example.data.security.SecurePreferencesManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RoomViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = RoomRepository(application)
    private val prefs = SecurePreferencesManager(application)

    val allRooms: StateFlow<List<RoomEntity>> = repository.getAllRooms()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedRoomId = MutableStateFlow<Long?>(null)
    val selectedRoomId: StateFlow<Long?> = _selectedRoomId.asStateFlow()

    private val _currentMonthYear = MutableStateFlow(
        SimpleDateFormat("yyyy-MM", Locale.US).format(Date())
    )
    val currentMonthYear: StateFlow<String> = _currentMonthYear.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val selectedRoom: StateFlow<RoomEntity?> = _selectedRoomId.flatMapLatest { id ->
        if (id != null) repository.getRoom(id) else flowOf(null)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val roomMembers: StateFlow<List<RoomMemberEntity>> = _selectedRoomId.flatMapLatest { id ->
        if (id != null) repository.getMembers(id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val roomExpenses: StateFlow<List<RoomExpenseEntity>> = _selectedRoomId.flatMapLatest { id ->
        if (id != null) repository.getExpenses(id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val roomMonthExpenses: StateFlow<List<RoomExpenseEntity>> = _selectedRoomId.flatMapLatest { id ->
        if (id != null) repository.getExpensesForMonth(id, _currentMonthYear.value) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val monthlyTotalSpent: StateFlow<Double> = _selectedRoomId.flatMapLatest { id ->
        if (id != null) repository.getTotalSpentForMonth(id, _currentMonthYear.value) else flowOf(0.0)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    private val _settlementSlip = MutableStateFlow<RoomSettlementSlip?>(null)
    val settlementSlip: StateFlow<RoomSettlementSlip?> = _settlementSlip.asStateFlow()

    private val _isGeneratingSlip = MutableStateFlow(false)
    val isGeneratingSlip: StateFlow<Boolean> = _isGeneratingSlip.asStateFlow()

    init {
        // Automatically select the first room if available
        viewModelScope.launch {
            allRooms.collect { rooms ->
                if (_selectedRoomId.value == null && rooms.isNotEmpty()) {
                    _selectedRoomId.value = rooms.first().id
                }
            }
        }
    }

    fun selectRoom(roomId: Long) {
        _selectedRoomId.value = roomId
        _settlementSlip.value = null // reset slip when room changes
    }

    fun setMonthYear(monthYear: String) {
        _currentMonthYear.value = monthYear
        _settlementSlip.value = null
    }

    fun createRoom(
        name: String,
        adminName: String = "You",
        friends: List<String>,
        currency: String = "$"
    ) {
        viewModelScope.launch {
            val newId = repository.createRoom(
                name = name,
                adminName = adminName,
                initialFriends = friends,
                currency = currency
            )
            _selectedRoomId.value = newId
        }
    }

    fun inviteFriend(name: String, phoneOrEmail: String = "") {
        val roomId = _selectedRoomId.value ?: return
        viewModelScope.launch {
            repository.addMember(roomId, name, phoneOrEmail)
        }
    }

    fun removeMember(memberId: Long) {
        viewModelScope.launch {
            repository.removeMember(memberId)
        }
    }

    fun addRoomExpense(
        title: String,
        amount: Double,
        paidBy: String,
        category: String = "Groceries",
        date: String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()),
        notes: String = ""
    ) {
        val room = selectedRoom.value ?: return
        viewModelScope.launch {
            repository.addExpense(
                roomId = room.id,
                title = title,
                amount = amount,
                paidBy = paidBy,
                category = category,
                date = date,
                currency = room.currency,
                notes = notes
            )
        }
    }

    fun deleteExpense(expenseId: Long) {
        viewModelScope.launch {
            repository.deleteExpense(expenseId)
        }
    }

    fun deleteRoom(roomId: Long) {
        viewModelScope.launch {
            repository.deleteRoom(roomId)
            if (_selectedRoomId.value == roomId) {
                _selectedRoomId.value = allRooms.value.firstOrNull { it.id != roomId }?.id
            }
        }
    }

    fun generateSettlementSlip() {
        val roomId = _selectedRoomId.value ?: return
        val monthYear = _currentMonthYear.value
        viewModelScope.launch {
            _isGeneratingSlip.value = true
            try {
                val slip = repository.calculateSettlementSlip(roomId, monthYear)
                _settlementSlip.value = slip
            } finally {
                _isGeneratingSlip.value = false
            }
        }
    }

    fun dismissSettlementSlip() {
        _settlementSlip.value = null
    }
}
