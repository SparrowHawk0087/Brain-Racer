package com.example.brainracer.domain.entities

data class Category(
    val categoryId: String = "",
    val name: String = "",
    val iconUrl: String = "",
    val order: Int = 0
)