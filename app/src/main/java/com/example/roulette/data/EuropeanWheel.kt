package com.example.roulette.data

enum class PocketColor { RED, BLACK, GREEN }

object EuropeanWheel {
    val pocketOrder = listOf(
        0, 32, 15, 19, 4, 21, 2, 25, 17, 34, 6, 27, 13, 36,
        11, 30, 8, 23, 10, 5, 24, 16, 33, 1, 20, 14, 31, 9,
        22, 18, 29, 7, 28, 12, 35, 3, 26
    )

    private val redNumbers = setOf(
        1, 3, 5, 7, 9, 12, 14, 16, 18, 19, 21, 23, 25, 27, 30, 32, 34, 36
    )

    fun colorFor(number: Int): PocketColor = when {
        number == 0 -> PocketColor.GREEN
        number in redNumbers -> PocketColor.RED
        else -> PocketColor.BLACK
    }
}
