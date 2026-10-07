package com.example.petcare.data

data class Pet(

    var id: String = "",

    var userId: String = "",

    var name: String = "",

    var species: String = "",

    var breed: String = "",

    var age: Int = 0,

    var weight: Double = 0.0,

    var dietaryPreference: String = "",

    var allergies: String = "",

    var vaccinationHistory: String = "",

    var favouriteToy: String = "",

    var notes: String = "",

    var photoUrl: String = ""
)