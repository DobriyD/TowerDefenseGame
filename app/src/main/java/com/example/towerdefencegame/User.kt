package com.example.towerdefencegame

data class User (
    val uid: String = "",
    val email: String = "",
    val firstLoginData: Long = System.currentTimeMillis(),
    val score: Int = 0
    )