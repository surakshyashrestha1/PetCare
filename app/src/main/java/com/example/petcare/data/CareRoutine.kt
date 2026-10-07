package com.example.petcare.data

data class CareRoutine(
    var id: String = "",
    var userId: String = "",
    var petId: String = "",
    var petName: String = "",
    var routineName: String = "",
    var taskName: String = "",
    var category: String = "",
    var time: String = "",
    var frequency: String = "",
    var repeatDays: List<String> = emptyList(),
    var suppliesNeeded: String = "",
    var instructions: String = "",
    var notes: String = "",
    var photoUrl: String = "",
    var createdAt: Long = 0L
)